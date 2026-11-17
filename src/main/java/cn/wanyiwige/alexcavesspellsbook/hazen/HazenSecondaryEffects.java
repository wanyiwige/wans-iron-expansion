package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.*;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;

public final class HazenSecondaryEffects {
    private static final DeferredRegister<MobEffect> REGISTRY=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,"alex_caves_spellsbook");
    public static final Map<String,RegistryObject<MobEffect>> EFFECTS=new LinkedHashMap<>();
    private static final Map<LivingEntity,Entity> CREDIT=new WeakHashMap<>();
    private static final Map<LivingEntity,Set<MobEffect>> PENDING_REMOVAL=new WeakHashMap<>();
    static{
        for(String name:new String[]{"flame_kissed","moonkissed","ichor","infested","spores","electrocuted","hexed","soul_fire","gluttonous_hunger"})
            EFFECTS.put(name,REGISTRY.register("hazen_"+name,()->create(name)));
    }
    private static MobEffect create(String name){
        // Forge invokes initializeClient from MobEffect's constructor, before fields in
        // Effect have been assigned. Keep the soul-fire client extension in a subtype
        // whose callback does not depend on constructor state.
        MobEffect effect=name.equals("soul_fire")?new SoulFireEffect():new Effect(name);
        if(name.equals("flame_kissed"))attr(effect,name,AttributeRegistry.FIRE_SPELL_POWER.get(),.03);
        if(name.equals("moonkissed")){
            attr(effect,name,HazenAttributes.get("cosmic_spell_power"),.05);attr(effect,name,HazenAttributes.get("hydro_spell_power"),.05);
            attr(effect,name,ForgeMod.SWIM_SPEED.get(),1);
        }
        if(name.equals("ichor")){attr(effect,name,Attributes.ARMOR,-.1);attr(effect,name,Attributes.ARMOR_TOUGHNESS,-.1);attr(effect,name,AttributeRegistry.SPELL_RESIST.get(),-.05);}
        if(name.equals("gluttonous_hunger")){
            effect.addAttributeModifier(Attributes.MAX_HEALTH,uuid(name,"health"),.05,AttributeModifier.Operation.ADDITION);
            attr(effect,name,HazenAttributes.get("hunger_steal"),.05);
        }
        return effect;
    }
    private static String uuid(String name,String key){return UUID.nameUUIDFromBytes(("hazen_secondary/"+name+"/"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();}
    private static void attr(MobEffect effect,String name,Attribute attribute,double value){effect.addAttributeModifier(attribute,uuid(name,ForgeRegistries.ATTRIBUTES.getKey(attribute).toString()),value,AttributeModifier.Operation.MULTIPLY_TOTAL);}
    public static MobEffect get(String name){return EFFECTS.get(name).get();}
    public static void register(IEventBus bus){REGISTRY.register(bus);MinecraftForge.EVENT_BUS.register(HazenSecondaryEffects.class);}
    public static DamageSource corruptDamage(Level level){return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,HazenEquipment.id("hazen/corrupt_magic"))));}
    public static DamageSource schoolDamage(Level level,ResourceKey<net.minecraft.world.damagesource.DamageType> key,Entity direct,Entity owner){return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),direct,owner);}
    public static void soulFire(LivingEntity target,Entity caster){
        if(target.fireImmune())return;
        target.addEffect(new MobEffectInstance(get("soul_fire"),60,0,false,false,false));
    }
    public static void infested(LivingEntity target,Entity caster){
        var old=target.getEffect(get("infested"));CREDIT.put(target,caster);
        target.addEffect(new MobEffectInstance(get("infested"),300,old==null?0:old.getAmplifier()+1,false,false,true));
    }
    private static class Effect extends MagicMobEffect implements io.redspace.ironsspellbooks.effect.ISyncedMobEffect{
        private final String kind;
        Effect(String kind){super(List.of("flame_kissed","moonkissed","gluttonous_hunger").contains(kind)?MobEffectCategory.BENEFICIAL:MobEffectCategory.HARMFUL,kind.equals("ichor")?0xE7D12D:0x2E2EFF);this.kind=kind;}
        @Override public boolean isDurationEffectTick(int duration,int amplifier){
            if(kind.equals("infested"))return amplifier>=2;
            return switch(kind){case "spores","electrocuted"->duration%Math.max(1,20>>Math.min(amplifier,30))==0;case "soul_fire"->duration%20==0;case "gluttonous_hunger"->duration%Math.max(1,50>>Math.min(amplifier,30))==0;default->false;};
        }
        @Override public void applyEffectTick(LivingEntity entity,int amplifier){
            if(entity.level().isClientSide||!HazenConfig.ENABLED.get()||!HazenConfig.EXTRA_SPELLS.get())return;
            switch(kind){
                case "infested"->{
                    if(PENDING_REMOVAL.getOrDefault(entity,Set.of()).contains(this))return;
                    Entity owner=CREDIT.remove(entity);PENDING_REMOVAL.computeIfAbsent(entity,key->new HashSet<>()).add(this);
                    if(owner!=entity)entity.addEffect(new MobEffectInstance(get("spores"),100,1));
                    for(var target:entity.level().getEntitiesOfClass(LivingEntity.class,entity.getBoundingBox().inflate(6),e->e!=entity&&e!=owner&&!DamageSources.isFriendlyFireBetween(owner,e)))
                        if(Utils.hasLineOfSight(entity.level(),entity.position(),target.getBoundingBox().getCenter(),true))target.addEffect(new MobEffectInstance(get("spores"),100,1));
                }
                case "electrocuted"->entity.hurt(schoolDamage(entity.level(),ISSDamageTypes.LIGHTNING_MAGIC,null,null),1+amplifier);
                case "spores"->entity.hurt(new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,HazenEquipment.id("hazen/spores")))),1+amplifier);
                case "soul_fire"->entity.hurt(entity.damageSources().onFire(),2);
                case "gluttonous_hunger"->entity.heal(1.5f);
            }
        }
        @Override public void clientTick(LivingEntity entity,MobEffectInstance instance){
            if(kind.equals("soul_fire"))entity.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,entity.getRandomX(.4),entity.getRandomY(),entity.getRandomZ(.4),0,.03,0);
            else if(kind.equals("infested")||kind.equals("spores"))entity.level().addParticle(HazenParticles.get("secondary_mushroom_particle"),entity.getRandomX(.4),entity.getRandomY(),entity.getRandomZ(.4),0,.02,0);
            else if(kind.equals("electrocuted"))entity.level().addParticle(io.redspace.ironsspellbooks.util.ParticleHelper.ELECTRIC_SPARKS,entity.getRandomX(.4),entity.getRandomY(),entity.getRandomZ(.4),0,.02,0);
        }
    }
    private static final class SoulFireEffect extends Effect{
        SoulFireEffect(){super("soul_fire");}
        @Override @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer){
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientMobEffectExtensions(){
                @Override public boolean isVisibleInInventory(MobEffectInstance instance){return false;}
            });
        }
    }
    public static boolean hasTaggedEquipment(LivingEntity entity,String name){
        var tag=TagKey.create(Registries.ITEM,HazenEquipment.id("hazen/secondary/"+name));
        for(var slot:EquipmentSlot.values())if(entity.getItemBySlot(slot).is(tag))return true;
        return CuriosApi.getCuriosInventory(entity).map(c->!c.findCurios(s->s.is(tag)).isEmpty()).orElse(false);
    }
    public static void counter(LivingEntity caster,LivingEntity target){
        var event=new CounterSpellEvent(caster,target);MinecraftForge.EVENT_BUS.post(event);if(event.isCanceled())return;
        var data=MagicData.getPlayerMagicData(target);
        if(target instanceof AntiMagicSusceptible susceptible){
            if(!(target instanceof IMagicSummon summon)||summon.getSummoner()!=caster||target instanceof Mob mob&&mob.getTarget()==null)susceptible.onAntiMagic(data);
        }else if(target instanceof ServerPlayer player){Utils.serverSideCancelCast(player,true);data.getPlayerRecasts().removeAll(RecastResult.COUNTERSPELL);}
        else if(target instanceof IMagicEntity magic)magic.cancelCast();
        for(var effect:List.copyOf(target.getActiveEffects()))if(effect.getEffect()instanceof MagicMobEffect)target.removeEffect(effect.getEffect());
    }
    public static void sound(Level level,Vec3 pos,String sound){HazenSounds.play(level,pos,"secondary_"+sound,1,1);}
    public static void slashParticle(Level level,String spell,Vec3 center,Vec3 forward){
        String particle=switch(spell){case "soul_flaming_strike"->"soul_flaming_strike";case "shard_sword"->"nature_slash_particle";case "ionic_slash"->"ionic_slash_particle";default->"scorching_slash_particle";};
        HazenParticles.spawn(level,"secondary_"+particle,center,0,forward.x,forward.y,forward.z,1);
    }
    public static void trace(Level level,Vec3 from,Vec3 to){if(level instanceof ServerLevel server)for(int i=0;i<15;i++){var p=from.add(Utils.getRandomVec3(1));Vec3 end=p.add(to.subtract(from));server.sendParticles(new io.redspace.ironsspellbooks.particle.TraceParticleOptions(new org.joml.Vector3f((float)end.x,(float)end.y,(float)end.z),new org.joml.Vector3f(.34f,.23f,.5f)),p.x,p.y,p.z,1,0,0,0,from.distanceTo(to)/12*.75);}}
    @SubscribeEvent public static void underwaterMining(PlayerEvent.BreakSpeed event){
        var effect=event.getEntity().getEffect(get("moonkissed"));if(effect!=null&&event.getEntity().isUnderWater()&&HazenConfig.EXTRA_SPELLS.get())event.setNewSpeed(event.getNewSpeed()*(2+effect.getAmplifier()));
    }
    @SubscribeEvent public static void hexedCast(io.redspace.ironsspellbooks.api.events.SpellPreCastEvent event){
        if(HazenConfig.ENABLED.get()&&HazenConfig.EXTRA_SPELLS.get()&&event.getEntity().hasEffect(get("hexed")))event.getEntity().hurt(corruptDamage(event.getEntity().level()),Math.max(1,event.getEntity().getMaxHealth()*.15f));
    }
    @SubscribeEvent public static void hungerSteal(net.minecraftforge.event.entity.living.LivingDamageEvent event){
        if(event.getAmount()<=0||!(event.getSource().getDirectEntity() instanceof ServerPlayer player)||!HazenConfig.EXTRA_SPELLS.get()||player.getAttackStrengthScale(0)<1)return;
        double value=player.getAttributeValue(HazenAttributes.get("hunger_steal"));if(value<=0)return;
        player.getFoodData().setFoodLevel((int)Math.max(0,Math.min(20,player.getFoodData().getFoodLevel()+value)));
        if(event.getEntity()instanceof net.minecraft.world.entity.player.Player target)target.getFoodData().setFoodLevel((int)Math.max(0,Math.min(20,target.getFoodData().getFoodLevel()-value)));
    }
    @SubscribeEvent public static void configCleanup(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event){
        var entity=event.getEntity();if(entity.level().isClientSide)return;
        // LivingTickEvent runs before tickEffects creates its iterator; defer stack consumption here.
        var pending=PENDING_REMOVAL.remove(entity);if(pending!=null)pending.forEach(entity::removeEffect);
        if(entity.tickCount%20!=0)return;
        for(var entry:EFFECTS.entrySet()){
            String spell=switch(entry.getKey()){case "flame_kissed"->"cinderous_step";case "ichor"->"golden_shower";case "infested","spores"->"syringe_barrage";case "electrocuted"->"ionic_slash";case "hexed"->"shadow_reaver";case "gluttonous_hunger"->"violent_regurgitation";default->entry.getKey();};
            boolean enabled=entry.getKey().equals("soul_fire")?HazenSecondaryRegistry.enabled("soul_flaming_strike")||HazenSecondaryRegistry.enabled("soul_flame_bolt"):HazenSecondaryRegistry.enabled(spell);
            if(!enabled)entity.removeEffect(entry.getValue().get());
        }
    }
    private HazenSecondaryEffects(){}
}
