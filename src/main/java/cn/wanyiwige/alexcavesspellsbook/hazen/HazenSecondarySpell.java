package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Native spells, preserving each source's cast type, geometry and attack sequence. */
public final class HazenSecondarySpell extends AbstractSpell {
    public final HazenSecondaryRegistry.Spec spec;
    private final DefaultConfig config;
    public HazenSecondarySpell(HazenSecondaryRegistry.Spec spec){
        this.spec=spec;manaCostPerLevel=spec.manaCostPerLevel();baseSpellPower=spec.baseSpellPower();
        spellPowerPerLevel=spec.spellPowerPerLevel();castTime=spec.castTime();baseManaCost=spec.baseManaCost();
        ResourceLocation school;
        if(HazenSchools.ENTRIES.containsKey(spec.school()))school=HazenEquipment.id("hazen_"+spec.school());
        else try{school=(ResourceLocation)SchoolRegistry.class.getField(spec.school().toUpperCase(java.util.Locale.ROOT)+"_RESOURCE").get(null);}
        catch(ReflectiveOperationException ex){throw new IllegalArgumentException(spec.school(),ex);}
        config=new DefaultConfig().setSchoolResource(school).setMinRarity(SpellRarity.valueOf(spec.rarity()))
                .setMaxLevel(spec.max_level()).setCooldownSeconds(spec.cooldown()).setAllowCrafting(spec.craftable()).build();
    }
    @Override public ResourceLocation getSpellResource(){return HazenEquipment.id("hazen_"+spec.id());}
    @Override public DefaultConfig getDefaultConfig(){return config;}
    @Override public CastType getCastType(){return CastType.valueOf(spec.cast_type());}
    @Override public int getCastTime(int level){return castTime+(List.of("evercomet_barrage","trident_jetstream","golden_shower").contains(spec.id())?5*level:0);}
    @Override public boolean isEnabled(){return HazenSecondaryRegistry.enabled(spec.id())&&super.isEnabled();}
    @Override public boolean allowLooting(){return spec.lootable();}
    private Optional<SoundEvent> sourceSound(String ref){
        if(ref==null||ref.isEmpty())return Optional.empty();
        if(ref.startsWith("HnSSounds."))return Optional.of(HazenSounds.get("secondary_"+ref.substring(ref.indexOf('.')+1).toLowerCase(java.util.Locale.ROOT)));
        try{return Optional.of(((java.util.function.Supplier<SoundEvent>)io.redspace.ironsspellbooks.registries.SoundRegistry.class.getField(ref.substring(ref.indexOf('.')+1)).get(null)).get());}
        catch(ReflectiveOperationException ex){throw new IllegalArgumentException("Unresolved native source sound "+ref,ex);}
    }
    @Override public Optional<SoundEvent> getCastStartSound(){return spec.start_sound().isEmpty()&&spec.school().equals("hydro")?Optional.of(HazenSounds.get("secondary_hydro_cast_1")):sourceSound(spec.start_sound());}
    @Override public Optional<SoundEvent> getCastFinishSound(){return sourceSound(spec.finish_sound());}
    @Override public boolean checkPreCastConditions(Level level,int rank,LivingEntity caster,MagicData data){return isEnabled();}
    public float damage(int rank,LivingEntity caster){
        float power=getSpellPower(rank,caster);
        float weapon=caster==null||caster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==null?0:Utils.getWeaponDamage(caster);
        return switch(spec.id()){
            case "soul_flame_bolt"->5+power*.4f;
            case "ice_arrow"->power;
            case "energy_burst","syringe_barrage","umbrashift_barrage","golden_shower"->power*.3f;
            case "water_bolt","trident_jetstream"->power*.4f;
            case "hydrobullet"->power*.6f;
            case "soul_seekers"->power*.75f;
            case "fiery_dagger"->power*.25f;
            case "shadow_reaver","endraconic_meteor","violent_regurgitation","dazzling_obliteration"->5+5*power;
            case "death_sentence"->5+.6f*power;
            case "soul_flaming_strike"->power+weapon+enchant(caster,false);
            case "shard_sword"->power+weapon+enchant(caster,true);
            case "ionic_slash"->power+weapon;
            case "scorching_slash"->5+.7f*power+weapon+enchant(caster,false);
            case "evercomet_barrage"->caster==null?power*7:(float)(4+4*power*(.4*caster.getAttributeValue(AttributeRegistry.ICE_SPELL_POWER.get())+.4*caster.getAttributeValue(HazenAttributes.get("cosmic_spell_power"))));
            default->power*.5f;
        };
    }
    private int enchant(LivingEntity caster,boolean sharp){return caster==null?0:EnchantmentHelper.getItemEnchantmentLevel(sharp?Enchantments.SHARPNESS:Enchantments.FIRE_ASPECT,caster.getMainHandItem());}
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster){
        if(spec.id().equals("moonkissed"))return List.of(Component.translatable("ui.alex_caves_spellsbook.hazen_duration",getSpellPower(level,caster)));
        if(spec.id().equals("cinderous_step"))return List.of(Component.translatable("ui.alex_caves_spellsbook.hazen_distance",distance(level,caster)));
        return List.of(Component.translatable("ui.alex_caves_spellsbook.hazen_damage",Utils.stringTruncation(damage(level,caster),2)));
    }
    @Override public SpellDamageSource getDamageSource(Entity projectile,Entity caster){
        var result=super.getDamageSource(projectile,caster);
        if(List.of("ice_arrow","hailstorm","evercomet_barrage").contains(spec.id()))result.setFreezeTicks(80);
        if(spec.id().equals("scorching_slash"))result.setFireTicks(90);
        if(spec.id().equals("fiery_dagger"))result.setFireTicks(60);
        if(List.of("energy_burst","syringe_barrage","arcane_cards","evercomet_barrage","trident_jetstream","umbrashift_barrage","fiery_dagger").contains(spec.id()))result.setIFrames(0);
        if(spec.id().equals("razorblade_typhoon"))result.setIFrames(3);
        return result;
    }
    @Override public boolean canBeInterrupted(Player player){return !List.of("soul_flaming_strike","shard_sword","ionic_slash","scorching_slash").contains(spec.id())&&super.canBeInterrupted(player);}
    @Override public int getEffectiveCastTime(int rank,LivingEntity caster){return List.of("soul_flaming_strike","shard_sword","ionic_slash","scorching_slash").contains(spec.id())?getCastTime(rank):super.getEffectiveCastTime(rank,caster);}
    @Override public AnimationHolder getCastStartAnimation(){
        return switch(spec.id()){
            case "parry","scorching_slash","violent_regurgitation","fiery_dagger","dazzling_obliteration"->
                    new AnimationHolder(HazenEquipment.id(switch(spec.id()){case "scorching_slash"->"scorching_slash_stance";case "violent_regurgitation"->"violent_regurgitation_charging";case "fiery_dagger"->"fiery_dagger_summon";case "dazzling_obliteration"->"rappa_recast_1";default->"parry";}),true);
            case "soul_flaming_strike","shard_sword","ionic_slash"->SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION;
            case "ice_arrow"->SpellAnimations.BOW_CHARGE_ANIMATION;
            case "hailstorm"->SpellAnimations.ANIMATION_CONTINUOUS_OVERHEAD;
            case "moonkissed"->SpellAnimations.SELF_CAST_ANIMATION;
            case "endraconic_meteor"->SpellAnimations.CHARGE_ANIMATION;
            case "death_sentence"->SpellAnimations.CHARGE_RAISED_HAND;
            case "umbrashift_barrage"->SpellAnimations.SLASH_ANIMATION;
            case "cinderous_step"->AnimationHolder.none();
            default->super.getCastStartAnimation();
        };
    }
    @Override public AnimationHolder getCastFinishAnimation(){
        return switch(spec.id()){
            case "scorching_slash"->new AnimationHolder(HazenEquipment.id("scorching_slash_slash"),true);
            case "violent_regurgitation"->new AnimationHolder(HazenEquipment.id("violent_regurgitation_cast"),true);
            case "death_sentence"->SpellAnimations.TOUCH_GROUND_ANIMATION;
            case "endraconic_meteor"->SpellAnimations.FINISH_ANIMATION;
            case "ice_arrow"->AnimationHolder.none();
            default->super.getCastFinishAnimation();
        };
    }
    private HazenSecondaryProjectile bolt(String kind,Level level,int rank,LivingEntity caster){var b=HazenSecondaryRegistry.projectile(kind,caster,damage(rank,caster),rank);level.addFreshEntity(b);return b;}
    @Override public void onCast(Level level,int rank,LivingEntity caster,CastSource source,MagicData data){
        if(level.isClientSide||!isEnabled())return;
        switch(spec.id()){
            case "bone_bolt","soul_flame_bolt","ice_arrow","shadow_reaver","cosmic_bolt","hydrobullet","water_bolt","razorblade_typhoon","horn_shell"->bolt(spec.id(),level,rank,caster);
            case "counterspell_spider_lily"->bolt("spider_lily",level,rank,caster);
            case "energy_burst","syringe_barrage"->{for(int i=0;i<3;i++){var b=bolt(spec.id().equals("energy_burst")?"energy_burst":"syringe",level,rank,caster);b.delay=i*3;}}
            case "soul_seekers"->{for(int i=-1;i<=1;i++)HazenSecondaryRegistry.spawn("soul_seeker",caster,damage(rank,caster),rank,caster.getEyePosition().add(0,-.25,0),caster.getLookAngle().yRot((float)Math.toRadians(i*10)));}
            case "arcane_cards"->{int count=3+Math.round((Math.max(1,Math.min(10,rank))-1)*2f/9)*2;for(int i=0;i<count;i++)HazenSecondaryRegistry.spawn(new String[]{"club","diamond","heart","spade"}[caster.getRandom().nextInt(4)],caster,damage(rank,caster),rank,caster.getEyePosition().add(0,-.5,0),caster.getLookAngle().yRot((float)Math.toRadians(-15+30.0*i/Math.max(1,count-1))));}
            case "endraconic_meteor"->delayedFan(caster,rank,"endraconic_meteor",3,45,1.2,1,20,6);
            case "fiery_dagger"->{
                if(HazenSecondaryEffects.hasTaggedEquipment(caster,"cinderous_equipment"))for(int i=-1;i<=1;i++){
                    var b=HazenSecondaryRegistry.spawn("fiery_dagger_magic_projectile",caster,damage(rank,caster),rank,caster.getEyePosition().add(0,-.5,0),caster.getLookAngle().yRot((float)Math.toRadians(i*15)));b.radius=1+rank*.25f;b.setNoGravity(false);
                }else delayedFan(caster,rank,"fiery_dagger_magic_projectile",8,120,1.6,1.2,10,2);
            }
            case "violent_regurgitation"->{
                caster.hurt(HazenSecondaryEffects.corruptDamage(level),caster.getHealth()*.4f);
                if(caster instanceof Player player)player.getFoodData().setFoodLevel(Math.max(0,player.getFoodData().getFoodLevel()-Math.min(8,Math.max(3,rank))));
                bolt("flesh_chunk",level,rank,caster);
                for(int i=0,n=4+caster.getRandom().nextInt(8);i<n;i++)HazenSecondaryRegistry.spawn("flesh_piece",caster,damage(rank,caster),rank,caster.getEyePosition().add(Utils.getRandomVec3(.8)),caster.getLookAngle().add(Utils.getRandomVec3(.6)).normalize());
            }
            case "soul_flaming_strike","shard_sword","scorching_slash"->slash(level,rank,caster);
            case "ionic_slash"->{if(!awaken(caster))slash(level,rank,caster);}
            case "death_sentence"->{for(int i=0;i<8;i++){double a=i*Math.PI/4;HazenSecondaryRegistry.spawn("death_sentence",caster,damage(rank,caster),rank,caster.position().add(0,1,0),new Vec3(Math.cos(a),0,Math.sin(a)));}}
            case "parry"->parry(level,caster);
            case "moonkissed"->caster.addEffect(new MobEffectInstance(HazenSecondaryEffects.get("moonkissed"),(int)(getSpellPower(rank,caster)*20),rank-1,false,false,true));
            case "cinderous_step","umbrashift_barrage"->teleport(level,rank,caster,data);
            case "dazzling_obliteration"->dazzle(level,rank,caster,source,data);
            case "hailstorm","golden_shower","trident_jetstream","evercomet_barrage"->{} // Native channel ticks produce their attacks.
            default->throw new IllegalStateException(spec.id());
        }
        super.onCast(level,rank,caster,source,data);
    }
    private void delayedFan(LivingEntity caster,int rank,String kind,int count,double arc,double radius,double height,int delay,int gap){
        Vec3 f=caster.getLookAngle().normalize(),right=f.cross(new Vec3(0,1,0));if(right.lengthSqr()<.001)right=new Vec3(1,0,0);right=right.normalize();Vec3 up=right.cross(f).normalize();
        for(int i=0;i<count;i++){double angle=Math.toRadians(-arc/2+arc*i/Math.max(1,count-1));Vec3 pos=caster.getEyePosition().add(0,height,0).add(f.scale(.5)).add(right.scale(Math.sin(angle)*radius)).add(up.scale(Math.cos(angle)*radius*.7));
            var b=HazenSecondaryRegistry.projectile(kind,caster,damage(rank,caster),rank);b.setPos(pos);b.launchDirection=kind.equals("endraconic_meteor")?f.yRot((float)angle):null;b.delay=delay+i*gap;b.trackOwner=true;b.radius=kind.equals("endraconic_meteor")?2+getSpellPower(rank,caster):0;caster.level().addFreshEntity(b);
        }
    }
    private void slash(Level level,int rank,LivingEntity caster){
        Vec3 f=caster.getForward(),center=caster.position().add(0,caster.getBbHeight()*.3,0).add(f.scale(1.9));
        for(var target:level.getEntitiesOfClass(LivingEntity.class,AABB.ofSize(center,6.5,3.25,6.5),e->e!=caster&&e.isAlive()&&!DamageSources.isFriendlyFireBetween(caster,e)))
            if(caster.distanceToSqr(target)<3.25*3.25&&target.getBoundingBox().getCenter().subtract(caster.getEyePosition()).dot(f)>=0&&Utils.hasLineOfSight(level,caster.getEyePosition(),target.getBoundingBox().getCenter(),true)){
                if(DamageSources.applyDamage(target,damage(rank,caster),getDamageSource(caster))) {
                    if(spec.id().equals("ionic_slash")&&HazenSecondaryEffects.hasTaggedEquipment(caster,"ionic_splitter"))target.addEffect(new MobEffectInstance(HazenSecondaryEffects.get("electrocuted"),100,1));
                    if(spec.id().equals("soul_flaming_strike"))HazenSecondaryEffects.soulFire(target,caster);
                }
            }
        HazenSecondaryEffects.slashParticle(level,spec.id(),center.add(0,.3,0),f);
        if(spec.id().equals("scorching_slash"))bolt("scorching_blade",level,rank,caster);
    }
    private boolean awaken(LivingEntity caster){
        for(var hand:InteractionHand.values()){
            var stack=caster.getItemInHand(hand);
            if(stack.getItem()!=HazenEquipment.WEAPONS.get("ionic_splitter_dormant").get())continue;
            var upgraded=HazenEquipment.WEAPONS.get("ionic_splitter_t1").get().getDefaultInstance();
            if(stack.hasTag())upgraded.setTag(stack.getTag().copy());
            caster.setItemInHand(hand,upgraded);caster.level().playSound(null,caster.blockPosition(),SoundEvents.TRIDENT_THUNDER,SoundSource.PLAYERS,.5f,1.3f);return true;
        }return false;
    }
    private void parry(Level level,LivingEntity caster){
        Vec3 f=caster.getForward();var near=level.getEntitiesOfClass(Projectile.class,caster.getBoundingBox().expandTowards(f.scale(12)).inflate(8),p->p.distanceToSqr(caster)<144&&p.position().subtract(caster.position()).normalize().dot(f)>=.85);
        var closest=near.stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);if(closest==null)return;
        for(var p:level.getEntitiesOfClass(Projectile.class,AABB.ofSize(closest.getBoundingBox().getCenter().add(f.scale(3)),6,7,6),p->!p.noPhysics)){
            float speed=(float)p.getDeltaMovement().length();p.setOwner(caster);p.shoot(f.x,f.y,f.z,speed,0);HazenSecondaryEffects.sound(level,p.position(),"parry");
        }
    }
    private float distance(int rank,LivingEntity caster){return (float)(Utils.softCapFormula(getEntityPowerMultiplier(caster))*getSpellPower(rank,null)*(spec.id().equals("umbrashift_barrage")?.25:1));}
    private void teleport(Level level,int rank,LivingEntity caster,MagicData data){
        Vec3 from=caster.getEyePosition(),dest=null;LivingEntity target=null;
        if(data!=null&&data.getAdditionalCastData() instanceof TeleportSpell.TeleportData t)dest=t.getTeleportTargetPosition();
        var hit=Utils.raycastForEntity(level,caster,distance(rank,caster),true);
        if(dest==null&&hit instanceof EntityHitResult eh&&eh.getEntity()instanceof LivingEntity living){target=living;for(int i=0;i<8;i++){
            Vec3 p=living.position().subtract(new Vec3(0,0,1.5).yRot((float)Math.toRadians(-(living.getYRot()+45*i)))).add(0,1,0);
            if(level.noCollision(caster,caster.getBoundingBox().move(p.subtract(caster.position())))){dest=p;break;}
        }}
        if(dest==null)dest=TeleportSpell.findTeleportLocation(level,caster,distance(rank,caster));
        if(dest==null||!level.noCollision(caster,caster.getBoundingBox().move(dest.subtract(caster.position()))))return;
        if(caster.isPassenger())caster.stopRiding();Utils.handleSpellTeleport(this,caster,dest);caster.resetFallDistance();
        if(target!=null)caster.lookAt(EntityAnchorArgument.Anchor.EYES,target.getEyePosition().add(0,-.15,0));
        if(spec.id().equals("cinderous_step"))caster.addEffect(new MobEffectInstance(HazenSecondaryEffects.get("flame_kissed"),(int)(getSpellPower(rank,caster)*20),rank-1,false,false,true));
        else {int count=Math.min(1+(rank-1)*2,5);for(int i=0;i<count;i++)HazenSecondaryRegistry.spawn("shadow_dagger",caster,damage(rank,caster),rank,caster.getEyePosition().add(0,-.5,0),caster.getLookAngle().yRot((float)Math.toRadians(-15+30.0*i/Math.max(1,count-1))));HazenSecondaryEffects.trace(level,from,dest);}
    }
    @Override public int getRecastCount(int level,LivingEntity caster){return spec.id().equals("dazzling_obliteration")?3:0;}
    private void dazzle(Level level,int rank,LivingEntity caster,CastSource source,MagicData data){
        int remaining=data.getPlayerRecasts().getRemainingRecastsForSpell(getSpellId());int stage=remaining<=0||remaining>=3?1:remaining==2?2:3;
        if(caster instanceof net.minecraft.server.level.ServerPlayer player)io.redspace.ironsspellbooks.setup.Messages.sendToPlayersTrackingEntity(
                new io.redspace.ironsspellbooks.network.debug.PlayPlayerAnimationPacket(player.getUUID(),HazenEquipment.id("rappa_recast_"+stage)),player,true);
        if(!data.getPlayerRecasts().hasRecastForSpell(getSpellId()))data.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(),rank,3,160,source,null),data);
        Vec3 f=caster.getLookAngle(),right=f.cross(new Vec3(0,1,0)).normalize();
        if(stage==1){var b=HazenSecondaryRegistry.spawn("ningu_demonbane_petalblade",caster,damage(rank,caster),rank,caster.getEyePosition().add(right.scale(3)),f);b.setCursorHoming(true);}
        else if(stage==2){var hit=Utils.raycastForEntity(level,caster,45,true);Vec3 center=hit instanceof EntityHitResult e?e.getEntity().getBoundingBox().getCenter():caster.getEyePosition().subtract(f.scale(7));
            for(int sign:new int[]{-1,1}){Vec3 pos=center.subtract(f.scale(3)).add(right.scale(sign*2.5));var b=HazenSecondaryRegistry.spawn("ningu_demonbane_petalblade",caster,damage(rank,caster),rank,pos,hit instanceof EntityHitResult?center.subtract(pos).normalize():f);b.setCursorHoming(true);}}
        else{var b=bolt("ningu_demonbane_petalblade_shuriken",level,rank,caster);b.delay=15;b.trackOwner=true;b.setCursorHoming(true);}
    }
    private static final class RainData implements ICastData {final Vec3 center;RainData(Vec3 p){center=p;}public void reset(){}}
    @Override public void onServerCastTick(Level level,int rank,LivingEntity caster,MagicData data){
        if(data==null||!isEnabled())return;int tick=data.getCastDurationRemaining();
        if(spec.id().equals("hailstorm")){
            if(!(data.getAdditionalCastData() instanceof RainData))data.setAdditionalCastData(new RainData(Utils.moveToRelativeGroundLevel(level,Utils.raycastForEntity(level,caster,40,true).getLocation(),12)));
            if((tick-1)%4!=0)return;Vec3 center=((RainData)data.getAdditionalCastData()).center;
            var targets=level.getEntitiesOfClass(LivingEntity.class,AABB.ofSize(center,18,6,18),e->e!=caster&&!DamageSources.isFriendlyFireBetween(caster,e));
            Vec3 weighted=Vec3.ZERO;for(var t:targets)weighted=weighted.add(t.position().subtract(center).scale(1.0/Math.max(1,targets.size())));
            for(int i=0;i<2;i++){Vec3 target=Utils.moveToRelativeGroundLevel(level,center.add(weighted).add(new Vec3(0,0,caster.getRandom().nextFloat()*6).yRot(caster.getRandom().nextFloat()*6.283f)),3).add(0,.5,0);Vec3 dir=new Vec3(.15,-.85,0).normalize();Vec3 start=Utils.raycastForBlock(level,target,target.subtract(dir.scale(12)),net.minecraft.world.level.ClipContext.Fluid.NONE).getLocation().add(dir);
                var b=HazenSecondaryRegistry.spawn(new String[]{"ice_chunk_a","ice_chunk_b","ice_chunk_c"}[caster.getRandom().nextInt(3)],caster,damage(rank,caster),rank,start,dir);b.radius=3;
            }
        }else if(spec.id().equals("evercomet_barrage")&&(tick+1)%3==0)bolt("evercomet",level,rank,caster);
        else if(spec.id().equals("trident_jetstream")&&(tick+1)%5==0)bolt("trident_jetstream",level,rank,caster);
        else if(spec.id().equals("golden_shower")&&(tick+1)%5==0){var b=bolt("ichor_stream",level,rank,caster);b.setDeltaMovement(caster.getLookAngle().add(0,.6,0).normalize().scale(.8));}
    }
}
