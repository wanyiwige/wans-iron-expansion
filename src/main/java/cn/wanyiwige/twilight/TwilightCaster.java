package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** The druid and ominous lich retain distinct goal pools and persistent boss phases. */
public final class TwilightCaster extends AbstractSpellCastingMob implements Enemy {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(TwilightCaster.class,EntityDataSerializers.INT);
    private final io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.ExtendedServerBossEvent bossBar;
    private Vec3 respawn;
    public TwilightCaster(EntityType<? extends TwilightCaster> type,Level world){
        super(type,world);xpReward=boss()?60:15;
        bossBar=new io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.ExtendedServerBossEvent(getUUID(),getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
        if(boss())setPersistenceRequired();
    }
    public boolean boss(){return getType()==TwilightEntities.OMINOUS_LICH.get();}
    public int phase(){return entityData.get(PHASE);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PHASE,0);}
    public static AttributeSupplier.Builder attributes(boolean boss){
        var values=Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE,boss?10:3).add(Attributes.MAX_HEALTH,boss?600:25)
                .add(Attributes.MOVEMENT_SPEED,boss?.3:.25).add(Attributes.FOLLOW_RANGE,boss?80:25)
                .add(Attributes.ATTACK_KNOCKBACK,boss?.5:0).add(AttributeRegistry.SPELL_POWER.get(),boss?1.5:.75)
                .add(AttributeRegistry.SPELL_RESIST.get(),boss?2:1).add(AttributeRegistry.MAX_MANA.get(),boss?1000:100);
        if(boss)values.add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.ARMOR,20).add(Attributes.ARMOR_TOUGHNESS,10)
                .add(TwilightAttributes.RESIST_PENETRATION.get(),.1).add(TwilightAttributes.MANA_REND.get(),.1).add(TwilightAttributes.MANA_STEAL.get(),.1);
        return values;
    }
    private AbstractSpell iron(String id){return SpellRegistry.getSpell(new ResourceLocation("irons_spellbooks",id));}
    private List<AbstractSpell> ironList(String... ids){return Arrays.stream(ids).map(this::iron).filter(s->!s.getSpellName().equals("none")).toList();}
    @Override protected void registerGoals(){
        goalSelector.addGoal(1,new FloatGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,IronGolem.class,true));
        if(boss())targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,AbstractSpellCastingMob.class,10,true,false,enemy->enemy instanceof io.redspace.ironsspellbooks.entity.mobs.keeper.KeeperEntity||enemy instanceof io.redspace.ironsspellbooks.entity.mobs.wizards.priest.PriestEntity||enemy instanceof io.redspace.ironsspellbooks.entity.mobs.wizards.cultist.CultistEntity||enemy instanceof io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossEntity||enemy instanceof io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingBoss));
        configureSpells(false);
        if(boss())goalSelector.addGoal(5,new PatrolNearLocationGoal(this,32,.9));else{goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,1));goalSelector.addGoal(10,new WizardRecoverGoal(this));}
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
    }
    private void configureSpells(boolean second){
        goalSelector.removeAllGoals(goal->goal instanceof WizardAttackGoal || goal instanceof SpellBarrageGoal || goal instanceof ComboGoal);
        if(!boss()){
            goalSelector.addGoal(4,new WizardAttackGoal(this,1.25F,35,80).setSpells(List.of(TwilightSpells.DRUID_BOLT.get(),iron("poison_arrow")),List.of(),List.of(),List.of(iron("oakskin"))).setSingleUseSpell(iron("summon_horse"),80,350,4,5).setDrinksPotions());
            return;
        }
        ArrayList<AbstractSpell> attacks=new ArrayList<>(ironList("sonic_boom","fire_arrow","blood_slash","flaming_barrage","blaze_storm","ray_of_siphoning","acupuncture"));attacks.add(TwilightSpells.TWILIGHT_BOLT.get());if(second)attacks.add(TwilightSpells.get("exanimated_fireball"));
        ArrayList<AbstractSpell> defense=new ArrayList<>(ironList("counterspell","shield","gust","heartstop","fortify","oakskin","heat_surge"));defense.add(TwilightSpells.FORTIFYING_SHIELDS.get());if(second)defense.add(iron("abyssal_shroud"));
        goalSelector.addGoal(2,new SpellBarrageGoal(this,TwilightSpells.get("exanimated_ray"),1,3,second?50:80,second?80:150,second?3:1));
        if(second){goalSelector.addGoal(2,new SpellBarrageGoal(this,TwilightSpells.get("exanimated_trident"),1,3,50,80,3));goalSelector.addGoal(3,new ComboGoal(this));}
        var wizard=new WizardAttackGoal(this,1.25F,second?35:50,second?50:75).setSpells(attacks,defense,List.of(TwilightSpells.get("exanimated_step")),List.of(TwilightSpells.get("summon_loyal_zombie"),TwilightSpells.get("summon_death_library"),iron("counterspell"),TwilightSpells.get("life_sacrifice"))).setSpellQuality(second?1.2F:1F,second?1.2F:1F);
        if(second)wizard.setSingleUseSpell(TwilightSpells.get("summon_loyal_zombie"),100,100,8,9);else wizard.setSingleUseSpell(iron("fireball"),70,100,3,5);
        goalSelector.addGoal(4,wizard);
    }
    @Override public void aiStep(){
        super.aiStep();if(level().isClientSide||!boss())return;
        bossBar.setProgress(getHealth()/getMaxHealth());
        if(phase()==0&&getHealth()<=getMaxHealth()*.5F&&isAlive()){
            entityData.set(PHASE,1);setHealth(getMaxHealth()*.5F);cancelCast();configureSpells(true);
            getAttribute(AttributeRegistry.SPELL_POWER.get()).setBaseValue(2);getAttribute(AttributeRegistry.SPELL_RESIST.get()).setBaseValue(2);
            playSound(SoundEvents.WITHER_SPAWN,1.5F,1);
            for(ServerPlayer player:bossBar.getPlayers())player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("display.wans_twilight_spellbooks.phase_2")));
        }else if(phase()==1&&getHealth()<=getMaxHealth()*.25F&&isAlive()){entityData.set(PHASE,2);setHealth(getMaxHealth()*.25F);}
    }
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);if(boss()){bossBar.addPlayer(player);player.connection.send(new ClientboundEntityEventPacket(this,(byte)61));}}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);if(boss()){bossBar.removePlayer(player);player.connection.send(new ClientboundEntityEventPacket(this,(byte)62));}}
    @Override public void handleEntityEvent(byte event){if(level().isClientSide&&boss()&&(event==61||event==62))TwilightBossClient.track(this,event==61);else super.handleEntityEvent(event);}
    @Override public boolean isAlliedTo(Entity entity){return boss()?entity instanceof IMagicSummon summon&&summon.getSummoner()==this:super.isAlliedTo(entity);}
    @Override public boolean hurt(DamageSource source,float amount){return !(boss()&&source.is(DamageTypes.DROWN))&&super.hurt(source,amount);}
    @Override protected SoundEvent getHurtSound(DamageSource source){return boss()?SoundEvents.BLAZE_HURT:SoundEvents.SKELETON_HURT;}
    @Override protected SoundEvent getDeathSound(){return boss()?SoundEvents.BLAZE_DEATH:SoundEvents.SKELETON_DEATH;}
    @Override public ResourceLocation getDefaultLootTable(){return TwilightFoundation.id("entities/"+(boss()?"ominous_lich":"advanced_druid"));}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){
        var result=super.finalizeSpawn(world,difficulty,reason,data,tag);
        if(boss()){
            for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
                String suffix=switch(slot){case HEAD->"helmet";case CHEST->"chestplate";case LEGS->"leggings";default->"boots";};setItemSlot(slot,new ItemStack(TwilightEquipment.get("exanimated_lich_"+suffix)));setDropChance(slot,0);
            }
            setItemSlot(EquipmentSlot.MAINHAND,TwilightEquipment.get("lich_greatsword").getDefaultInstance());setDropChance(EquipmentSlot.MAINHAND,0);
            setItemSlot(EquipmentSlot.OFFHAND,TwilightEquipment.get("ultimate_scepter").getDefaultInstance());setDropChance(EquipmentSlot.OFFHAND,0);
            if(respawn==null)respawn=position();
            restrictTo(net.minecraft.core.BlockPos.containing(respawn),32);
        }else setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.GOLDEN_HOE));return result;
    }
    @Override public void die(DamageSource source){super.die(source);if(boss()&&!level().isClientSide){castComplete();bossBar.removeAllPlayers();TwilightSoul.spawn(level(),"lich_soul",position(),respawn==null?position():respawn);}}
    public void setRespawn(Vec3 position){respawn=position;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("TwilightPhase",phase());if(respawn!=null){tag.putDouble("HomeX",respawn.x);tag.putDouble("HomeY",respawn.y);tag.putDouble("HomeZ",respawn.z);}}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(PHASE,tag.getInt("TwilightPhase"));if(tag.contains("HomeX"))respawn=new Vec3(tag.getDouble("HomeX"),tag.getDouble("HomeY"),tag.getDouble("HomeZ"));configureSpells(phase()>0);}
    private static final class ComboGoal extends Goal{
        private final TwilightCaster caster;private int cooldown=100,index;private String[] spells;
        ComboGoal(TwilightCaster caster){this.caster=caster;setFlags(EnumSet.of(Flag.LOOK));}
        public boolean canUse(){return caster.getTarget()!=null&&--cooldown<=0&&!caster.isCasting();}
        public boolean canContinueToUse(){return caster.getTarget()!=null&&index<3;}
        public void start(){index=0;spells=caster.random.nextBoolean()?new String[]{"counterspell","root","ray_of_siphoning"}:new String[]{"counterspell","slow","flaming_strike"};}
        public void tick(){if(!caster.isCasting()&&index<3)caster.initiateCastSpell(caster.iron(spells[index++]),3);}
        public void stop(){cooldown=100+caster.random.nextInt(151);}
    }
}
