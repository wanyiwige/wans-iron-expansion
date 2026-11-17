package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonCloud;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.*;
import java.util.function.Supplier;

/** Distinct source collision, bounce, homing, cloud and recovery behavior on native projectiles. */
public final class HazenSecondaryProjectile extends AbstractMagicProjectile implements GeoEntity {
    public final String kind;
    public int spellLevel=1,delay;
    public float radius;
    public Vec3 launchDirection;
    public boolean trackOwner;
    private int waited,hits,bounces;
    private Vec3 previousOwner;
    private final Map<UUID,Integer> victims=new HashMap<>();
    private LivingEntity lockedTarget;
    private boolean embedded;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public HazenSecondaryProjectile(EntityType<? extends Projectile> type,Level level,String kind){
        super(type,level);this.kind=kind;
        setNoGravity(!Set.of("bone_bolt","syringe","flesh_chunk","flesh_piece","shadow_dagger","ichor_stream","ice_arrow").contains(kind));
        if(Set.of("cosmic_bolt","water_bolt","razorblade_typhoon","horn_shell","death_sentence","scorching_blade").contains(kind))setInfinitePiercing();
    }
    public HazenSecondaryRegistry.Visual visual(){return HazenSecondaryRegistry.VISUALS.get(kind);}
    public AbstractSpell spell(){return HazenSpells.get(visual().spell());}
    @Override public float getSpeed(){return HazenSecondaryRegistry.VISUALS.get(kind).speed();}
    public boolean stationary(){return Set.of("ice_cloud","endraconic_breath","flesh_mound").contains(kind);}
    @Override public boolean collidesWithBlocks(){return !stationary()&&!embedded;}
    @Override public void tick(){
        if(!level().isClientSide&&!HazenSecondaryRegistry.enabled(visual().spell())){discard();return;}
        if(waited++<delay){
            if(trackOwner&&getOwner()!=null){if(previousOwner!=null)setPos(position().add(getOwner().position().subtract(previousOwner)));previousOwner=getOwner().position();}
            if(waited==delay){shoot(launchDirection!=null?launchDirection:getOwner()!=null?getOwner().getLookAngle():new Vec3(0,0,1));setNoGravity(true);}
            return;
        }
        if(stationary()||embedded){
            setDeltaMovement(Vec3.ZERO);
            if(level().isClientSide)trailParticles();
            if(!level().isClientSide){
                if(!kind.equals("flesh_mound")&&!embedded&&tickCount%10==0)cloudDamage();
                if(tickCount>=(kind.equals("flesh_mound")||kind.equals("endraconic_breath")?300:200))discard();
            }return;
        }
        if(!level().isClientSide&&tickCount>10&&Set.of("razorblade_typhoon","soul_seeker").contains(kind)){
            if(lockedTarget==null||!lockedTarget.isAlive())lockedTarget=level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(12),e->canHitEntity(e)&&e.isAlive()).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if(lockedTarget!=null){var motion=getDeltaMovement();Vec3 desired=lockedTarget.getEyePosition().subtract(position()).normalize();setDeltaMovement(motion.normalize().scale(.95).add(desired.scale(.05)).normalize().scale(Math.max(.001,motion.length())));}
        }
        super.tick();
        int maxAge=switch(kind){case "water_bolt"->100;case "razorblade_typhoon"->150;case "death_sentence"->30;case "scorching_blade"->15;case "ichor_stream"->20;default->300;};
        if(tickCount>maxAge)discard();
    }
    @Override protected boolean canHitEntity(Entity target){
        if(target==getOwner()||getOwner()!=null&&DamageSources.isFriendlyFireBetween(getOwner(),target)||stationary())return false;
        if(kind.equals("fiery_dagger_magic_projectile")&&radius>0)return false;
        var previous=victims.get(target.getUUID());
        if(previous!=null&&(kind.equals("horn_shell")||tickCount-previous<10))return false;
        return super.canHitEntity(target);
    }
    private LivingEntity owner(){return getOwner()instanceof LivingEntity living?living:null;}
    private boolean damage(Entity target,float amount){
        if(owner()==null)return false;
        boolean accepted=DamageSources.applyDamage(target,amount,spell().getDamageSource(this,owner()));
        if(accepted&&kind.equals("soul_flame_bolt")&&target instanceof LivingEntity living)HazenSecondaryEffects.soulFire(living,owner());
        return accepted;
    }
    @Override protected void onHitEntity(EntityHitResult hit){
        if(level().isClientSide||owner()==null)return;
        Entity target=hit.getEntity();Vec3 center=target.position();
        switch(kind){
            case "hydrobullet","flesh_chunk"->{area(center,3,false,false);discard();return;}
            case "shadow_reaver"->{area(center,4,false,true);discard();return;}
            case "spider_lily","soul_seeker"->{area(center,kind.equals("spider_lily")?3:6,true,false);if(kind.equals("spider_lily")&&target.getType()!=EntityType.ENDERMAN)poison(center);discard();return;}
            case "endraconic_meteor"->{meteor(hit.getLocation());return;}
            case "ningu_demonbane_petalblade_shuriken"->{area(center,4,false,false);discard();return;}
            case "ice_chunk_a","ice_chunk_b","ice_chunk_c"->{area(center,Math.max(1,radius),false,false);discard();return;}
        }
        if(kind.equals("evercomet")){
            DamageSources.applyDamage(target,getDamage()*.5f,HazenSecondaryEffects.schoolDamage(level(),ISSDamageTypes.ICE_MAGIC,this,owner()));
            damage(target,getDamage()*.5f);
        }else damage(target,getDamage());
        victims.put(target.getUUID(),tickCount);hits++;
        if(target instanceof LivingEntity living){
            if(kind.equals("syringe"))HazenSecondaryEffects.infested(living,owner());
            if(kind.equals("ichor_stream")){
                float power=((HazenSecondarySpell)spell()).getSpellPower(spellLevel,owner());
                living.addEffect(new MobEffectInstance(HazenSecondaryEffects.get("ichor"),100+spellLevel*40+(int)(power*20),Math.max(0,(spellLevel-1)/2)+(int)(power/10)));
            }
            if(Set.of("cosmic_bolt","water_bolt","razorblade_typhoon").contains(kind))DamageSources.ignoreNextKnockback(living);
        }
        impactParticles(center.x,center.y,center.z);
        if(kind.equals("ice_arrow")){if(target.getType()!=EntityType.ENDERMAN)createCloud("ice_cloud",center,getDamage()*.5f,3);discard();return;}
        if(kind.equals("cosmic_bolt")&&hits>=Math.max(1,spellLevel)){discard();return;}
        if(!Set.of("cosmic_bolt","water_bolt","razorblade_typhoon","horn_shell","death_sentence","scorching_blade").contains(kind))discard();
    }
    @Override protected void onHitBlock(BlockHitResult hit){
        if(level().isClientSide||owner()==null)return;Vec3 p=hit.getLocation();
        if(Set.of("water_bolt","razorblade_typhoon").contains(kind)){
            Vec3 v=getDeltaMovement();setDeltaMovement(switch(hit.getDirection().getAxis()){case X->v.multiply(-1,1,1);case Y->v.multiply(1,-1,1);case Z->v.multiply(1,1,-1);});
            setPos(p.add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(.03)));if(++bounces>=6)discard();return;
        }
        switch(kind){
            case "hydrobullet","flesh_chunk"->{area(p,3,false,false);if(kind.equals("flesh_chunk"))createCloud("flesh_mound",p,0,0);}
            case "shadow_reaver"->area(p,4,false,true);
            case "spider_lily","soul_seeker"->{area(p,kind.equals("spider_lily")?3:6,true,false);if(kind.equals("spider_lily"))poison(p);}
            case "ice_arrow"->{createCloud("ice_cloud",p,getDamage()*.5f,3);embedded=true;setPos(p);setDeltaMovement(Vec3.ZERO);return;}
            case "endraconic_meteor"->{meteor(p);return;}
            case "ice_chunk_a","ice_chunk_b","ice_chunk_c"->area(p,Math.max(1,radius),false,false);
            case "ningu_demonbane_petalblade_shuriken"->area(p,4,false,false);
            case "fiery_dagger_magic_projectile"->{if(radius>0){
                var zone=new io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity(level());zone.setOwner(owner());zone.setDamage(getDamage());zone.setExplosionRadius(radius);zone.createDaggerZone(Utils.moveToRelativeGroundLevel(level(),p,3));
            }}
        }
        impactParticles(p.x,p.y,p.z);discard();
    }
    private void area(Vec3 center,float range,boolean counter,boolean hex){
        for(var target:level().getEntitiesOfClass(LivingEntity.class,AABB.ofSize(center,range*2,range*2,range*2),e->e!=owner()&&e.isAlive()&&!DamageSources.isFriendlyFireBetween(owner(),e))){
            if(!counter&&target.distanceToSqr(center)>range*range)continue;
            if(counter)HazenSecondaryEffects.counter(owner(),target);
            damage(target,getDamage());if(hex)target.addEffect(new MobEffectInstance(HazenSecondaryEffects.get("hexed"),100,0));
        }impactParticles(center.x,center.y,center.z);
    }
    private void poison(Vec3 point){var cloud=new PoisonCloud(level());cloud.setOwner(owner());cloud.setDuration(200);cloud.setDamage(getDamage());cloud.moveTo(point);level().addFreshEntity(cloud);}
    private void meteor(Vec3 point){
        float range=Math.max(1,radius);
        for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(range),e->e!=owner()&&!DamageSources.isFriendlyFireBetween(owner(),e))){
            double distance=target.distanceToSqr(point);if(distance<range*range&&Utils.hasLineOfSight(level(),point.add(0,2,0),target.getBoundingBox().getCenter(),true))damage(target,(float)(getDamage()*(1-distance/(range*range))));
        }
        if(io.redspace.ironsspellbooks.config.ServerConfigs.SPELL_GREIFING.get())level().explode(this,getX(),getY(),getZ(),range*.5f,true,Level.ExplosionInteraction.MOB);
        createCloud("endraconic_breath",point,getDamage()*.5f,range);impactParticles(point.x,point.y,point.z);HazenSecondaryEffects.sound(level(),point,"endraconic_meteor_impact");discard();
    }
    public HazenSecondaryProjectile createCloud(String type,Vec3 point,float damage,float radius){
        var cloud=HazenSecondaryRegistry.projectile(type,owner(),damage,spellLevel);cloud.setPos(point);cloud.setDeltaMovement(Vec3.ZERO);cloud.radius=radius;level().addFreshEntity(cloud);return cloud;
    }
    private void cloudDamage(){
        if(owner()==null)return;float range=radius>0?radius:3;
        for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(range),e->e!=owner()&&!DamageSources.isFriendlyFireBetween(owner(),e))){
            if(kind.equals("ice_cloud")){DamageSources.ignoreNextKnockback(target);target.hurt(HazenSecondaryEffects.schoolDamage(level(),ISSDamageTypes.ICE_MAGIC,this,owner()),getDamage());target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(),target.getTicksFrozen()+100));}
            else target.hurt(HazenSecondaryEffects.schoolDamage(level(),ISSDamageTypes.ENDER_MAGIC,this,owner()),getDamage());
        }
    }
    @Override public boolean isPickable(){return kind!=null&&kind.equals("flesh_mound");}
    @Override public InteractionResult interact(Player player,InteractionHand hand){
        if(!kind.equals("flesh_mound"))return super.interact(player,hand);
        if(!level().isClientSide&&hand==InteractionHand.MAIN_HAND){int food=Math.max(1,Math.min(10,spellLevel));player.getFoodData().eat(food,.1f+(food-1)/9f*.9f);
            player.addEffect(new MobEffectInstance(HazenSecondaryEffects.get("gluttonous_hunger"),20*(5+spellLevel*5),1));
            MagicData.getPlayerMagicData(player).getPlayerCooldowns().removeCooldown(HazenSpells.get("violent_regurgitation").getSpellId());HazenSecondaryEffects.sound(level(),position(),"flesh_mound_bite");discard();
        }return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public Optional<Supplier<SoundEvent>> getImpactSound(){
        if(Set.of("ice_arrow","ichor_stream","ice_cloud","flesh_mound","endraconic_breath").contains(kind))return Optional.empty();
        return Optional.of(()->HazenSounds.get("secondary_"+switch(kind){case "bone_bolt"->"bone_impact";case "energy_burst"->"electric_impact";case "syringe"->"syringe_barrage_hit";case "cosmic_bolt"->hits<2?"cosmic_bolt_hit":hits<4?"cosmic_bolt_okay":hits<6?"cosmic_bolt_good":"cosmic_bolt_excellent";case "evercomet"->"evercomet_impact";case "shadow_reaver","soul_seeker"->"brimstone_hellblast_impact";default->"terraria_impact";}));
    }
    @Override public void trailParticles(){
        if(!level().isClientSide)return;
        String particle=switch(kind){case "cosmic_bolt","endraconic_meteor"->"cosmic_ember_particle";case "soul_seeker"->"eldritch_soul_particle";case "shadow_reaver","shadow_dagger"->"nights_edge_particle";case "spider_lily"->"leaf_particle";default->null;};
        if(particle!=null)level().addParticle(HazenParticles.get("secondary_"+particle),getX(),getY(),getZ(),0,0,0);
        else if(kind.equals("soul_flame_bolt"))level().addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,getX(),getY(),getZ(),0,0,0);
        else if(kind.equals("ice_cloud"))level().addParticle(new io.redspace.ironsspellbooks.particle.FogParticleOptions(new org.joml.Vector3f(.9f,1,.91f),1),getRandomX(radius),getY()+.3,getRandomZ(radius),0,0,0);
        else if(kind.equals("ichor_stream"))level().addParticle(new io.redspace.ironsspellbooks.particle.SparkParticleOptions(new org.joml.Vector3f(.95f,.7f,.23f)),getX(),getY(),getZ(),0,0,0);
    }
    @Override public void impactParticles(double x,double y,double z){
        if(kind.equals("cosmic_bolt")||kind.equals("endraconic_meteor"))HazenParticles.spawn(level(),"secondary_star_impact_particle",new Vec3(x,y,z),10,.1,.1,.1,.15);
        if(kind.equals("shadow_reaver"))HazenParticles.spawn(level(),"secondary_nights_edge_particle",new Vec3(x,y,z),25,0,0,0,.18);
        if(level()instanceof net.minecraft.server.level.ServerLevel server&&kind.startsWith("ice_"))server.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,x,y,z,15,.1,.1,.1,.1);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("HazenSpellLevel",spellLevel);tag.putInt("HazenDelay",delay);tag.putInt("HazenWaited",waited);tag.putFloat("HazenRadius",radius);tag.putBoolean("HazenEmbedded",embedded);tag.putInt("HazenHits",hits);tag.putInt("HazenBounces",bounces);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);spellLevel=Math.max(1,tag.getInt("HazenSpellLevel"));delay=tag.getInt("HazenDelay");waited=tag.getInt("HazenWaited");radius=tag.getFloat("HazenRadius");embedded=tag.getBoolean("HazenEmbedded");hits=tag.getInt("HazenHits");bounces=tag.getInt("HazenBounces");}
    @Override public void writeSpawnData(net.minecraft.network.FriendlyByteBuf buffer){
        super.writeSpawnData(buffer);buffer.writeVarInt(spellLevel);buffer.writeVarInt(delay);buffer.writeVarInt(waited);buffer.writeFloat(radius);buffer.writeBoolean(trackOwner);
        buffer.writeBoolean(launchDirection!=null);if(launchDirection!=null){buffer.writeDouble(launchDirection.x);buffer.writeDouble(launchDirection.y);buffer.writeDouble(launchDirection.z);}
    }
    @Override public void readSpawnData(net.minecraft.network.FriendlyByteBuf buffer){
        super.readSpawnData(buffer);spellLevel=buffer.readVarInt();delay=buffer.readVarInt();waited=buffer.readVarInt();radius=buffer.readFloat();trackOwner=buffer.readBoolean();
        launchDirection=buffer.readBoolean()?new Vec3(buffer.readDouble(),buffer.readDouble(),buffer.readDouble()):null;
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new AnimationController<>(this,"hazen_secondary",0,event->{String animation=visual().animation_name();if(animation==null)return PlayState.STOP;event.getController().setAnimation(RawAnimation.begin().thenLoop(animation));return PlayState.CONTINUE;}));
    }
}
