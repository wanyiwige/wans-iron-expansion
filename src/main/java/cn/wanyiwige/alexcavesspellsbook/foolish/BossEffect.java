package cn.wanyiwige.alexcavesspellsbook.foolish;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.util.Mth;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Comparator;

/** Physical projectiles/fields and timeline controller; collision and damage run only on the server. */
public final class BossEffect extends Projectile implements GeoEntity {
    public enum Kind {DIRECTOR,FIREBALL,SMALL_FIREBALL,RADIUM,GAS_BOLT,GAS_FIELD,POLLEN,CAPSULE,SHOCK,THUNDER,MINE,SEED,METEOR,BLADE,STARBOMB,SLASH}
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(BossEffect.class,EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public BossCast cast;
    private int delay,lifetime=100,age,fuse=-1;
    private Vec3 interpolation=Vec3.ZERO;
    private int interpolationSteps;
    private float yawTarget,pitchTarget;
    public BossEffect(EntityType<? extends BossEffect> type,Level level){super(type,level);setNoGravity(true);}
    public void attach(BossCast cast,Kind kind,int delay,int lifetime){this.cast=cast;this.delay=delay;this.lifetime=lifetime;entityData.set(KIND,kind.ordinal());}
    public Kind kind(){return Kind.values()[Math.max(0,Math.min(Kind.values().length-1,entityData.get(KIND)))];}
    @Override protected void defineSynchedData(){entityData.define(KIND,0);}
    @Override public EntityDimensions getDimensions(Pose pose){
        return switch(kind()){case STARBOMB->EntityDimensions.scalable(1,1);
            case MINE,METEOR->EntityDimensions.scalable(0.6F,1.8F);default->super.getDimensions(pose);};
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);if(KIND.equals(key))refreshDimensions();
    }
    public void face(Vec3 v){if(v.lengthSqr()<1E-6)return;
        float yaw=(float)(Mth.atan2(-v.x,v.z)*Mth.RAD_TO_DEG),pitch=(float)(-Mth.atan2(v.y,v.horizontalDistance())*Mth.RAD_TO_DEG);
        setYRot(getYRot()+Mth.wrapDegrees(yaw-getYRot()));setXRot(pitch);}
    @Override public void tick(){
        yRotO=getYRot();xRotO=getXRot();super.tick();
        if(level().isClientSide){if(interpolationSteps>0){float t=1F/interpolationSteps--;setPos(position().lerp(interpolation,t));
            setYRot(getYRot()+Mth.wrapDegrees(yawTarget-getYRot())*t);setXRot(Mth.lerp(t,getXRot(),pitchTarget));}return;}
        if(cast==null||cast.owner()==null||++age>lifetime||!level().hasChunkAt(blockPosition())){discard();return;}
        if(age<=delay)return;
        if(kind()==Kind.DIRECTOR){cast.director(this,age-delay);return;}
        if(kind()==Kind.SLASH){setPos(cast.owner().getEyePosition().add(cast.owner().getLookAngle().scale(1.4)));return;}
        if(kind()==Kind.GAS_FIELD){
            cast.particle("corrosive_gas",position(),8,2.5);
            if(age%10==0)for(LivingEntity victim:cast.enemies(position(),3+cast.rank*0.5))if(cast.visible(position(),victim)){
                cast.damage(this,victim,1,10,30);cast.status(victim,"minecraft:poison",60,1);cast.status(victim,"foolish:corrosion",60,0);
            }return;
        }
        if(kind()==Kind.STARBOMB&&fuse>=0){setDeltaMovement(Vec3.ZERO);cast.particle("stardust_pink",position(),12,0.7);
            if(--fuse<=0){cast.blast(this,5,1);discard();}return;}
        LivingEntity target=cast.target();Vec3 velocity=getDeltaMovement();
        if(kind()==Kind.MINE||kind()==Kind.BLADE||kind()==Kind.STARBOMB){
            String particle=kind()==Kind.MINE?"lightfire_spark":"stardust_yellow";
            cast.particle(particle,position(),kind()==Kind.MINE?3:6,kind()==Kind.STARBOMB?0.6:0.2);
            if(target!=null){
                Vec3 desired=target.getBoundingBox().getCenter().subtract(position()).normalize();
                double speed=kind()==Kind.MINE?0.22:kind()==Kind.BLADE?0.65:0.35;
                velocity=velocity.normalize().scale(0.7).add(desired.scale(0.3)).normalize().scale(speed);
            }
        }
        if(kind()==Kind.SEED)velocity=velocity.add(0,-0.015,0);
        Vec3 start=position(),end=start.add(velocity);
        if(!level().hasChunkAt(net.minecraft.core.BlockPos.containing(end))){discard();return;}
        var wall=level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        Vec3 stop=wall.getLocation();
        LivingEntity victim=level().getEntitiesOfClass(LivingEntity.class,new AABB(start,stop).inflate(0.4),
                e->cast.enemy(e)&&(e.getBoundingBox().inflate(0.2).contains(start)||e.getBoundingBox().inflate(0.2).clip(start,stop).isPresent()))
                .stream().min(Comparator.comparingDouble(e->contact(e,start,stop).distanceToSqr(start))).orElse(null);
        if(victim!=null&&(wall.getType()==HitResult.Type.MISS||contact(victim,start,stop).distanceToSqr(start)<stop.distanceToSqr(start))){
            setPos(contact(victim,start,stop));impact(victim);return;
        }
        setDeltaMovement(velocity);setPos(stop);face(velocity);hasImpulse=true;
        if(wall.getType()!=HitResult.Type.MISS){setPos(stop.subtract(velocity.normalize().scale(0.05)));impact(null);return;}
        if(kind()==Kind.FIREBALL||kind()==Kind.SMALL_FIREBALL)cast.vanilla(ParticleTypes.FLAME,position(),2,0.1);
        if(kind()==Kind.METEOR)cast.particle("stardust_pink",position(),3,0.4);
    }
    private static Vec3 contact(LivingEntity e,Vec3 start,Vec3 stop){var box=e.getBoundingBox().inflate(0.2);return box.contains(start)?start:box.clip(start,stop).orElse(stop);}
    private void impact(LivingEntity victim){
        switch(kind()){
            case GAS_BOLT -> {cast.effect(Kind.GAS_FIELD,position(),Vec3.ZERO,0,160+cast.rank*40);}
            case CAPSULE -> {
                for(int i=0;i<2+cast.rank;i++){
                    BossCompanion.Kind bug=i==0&&cast.rank>=2?BossCompanion.Kind.ASSISTANT:i==1&&cast.rank==3?BossCompanion.Kind.SENTINEL:BossCompanion.Kind.BUG;
                    cast.companion(bug,cast.ground(position().add((i%2)*0.8,0,(i/2)*0.8)),400+cast.rank*100);
                }
                cast.particle("scarlet_firefly",position(),12,0.5);
            }
            case POLLEN -> {if(victim!=null){cast.damage(this,victim,1,0,1);cast.status(victim,"minecraft:blindness",100,1);cast.status(victim,"minecraft:slowness",100,0);}
                cast.particle("pollen",position(),20,0.5);}
            case RADIUM -> {cast.blast(this,2,1);if(victim!=null)cast.status(victim,"foolish:corrosion",100,1);}
            case SHOCK -> {if(victim!=null){cast.damage(this,victim,1,0,6);cast.status(victim,"foolish:paralysis",150,1);}
                cast.vanilla(ParticleTypes.ELECTRIC_SPARK,position(),12,0.3);}
            case THUNDER -> {cast.lightning(position());cast.blast(this,2,0.6F);}
            case MINE -> {cast.lightning(position());cast.blast(this,3,1);}
            case SEED -> {cast.blast(this,3,1);cast.particle("pollen",position(),18,1);}
            case METEOR -> {cast.blast(this,3.5,1);cast.particle("stardust_pink",position(),24,1);}
            case BLADE -> {if(victim!=null)cast.damage(this,victim,1,10,8);cast.particle("starsplitter_pierce",position(),8,0.4);
                if(victim!=null&&age<lifetime-10){setPos(position().add(getDeltaMovement().normalize().scale(0.8)));return;}}
            case STARBOMB -> {fuse=20;setDeltaMovement(Vec3.ZERO);return;}
            case FIREBALL,SMALL_FIREBALL -> {
                if(cast.spec==BossSpellSpec.INCINERATOR_STREAM){if(victim!=null&&cast.damage(this,victim,1,4,10))victim.setSecondsOnFire(5);}
                else{cast.blast(this,2.5,1);if(victim!=null)victim.setSecondsOnFire(5);}
            }
            default -> {}
        }discard();
    }
    @Override public boolean isPickable(){return kind()==Kind.STARBOMB||kind()==Kind.MINE;}
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide||cast==null)return false;
        if(kind()==Kind.STARBOMB){fuse=1;return true;}
        if(kind()==Kind.MINE&&source.getEntity()!=cast.owner()){discard();return true;}return false;
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){
        interpolation=new Vec3(x,y,z);interpolationSteps=Math.max(1,steps);yawTarget=getYRot()+Mth.wrapDegrees(yaw-getYRot());pitchTarget=pitch;
    }
    @Override public boolean shouldBeSaved(){return false;}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);discard();}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar){
        registrar.add(new AnimationController<>(this,"main",0,state->{
            String anim=switch(kind()){case MINE->"animation.stormmine.idle";case SEED->"animation.seedbomb.fly";
                case METEOR->"animation.meteor.spin";case BLADE,SLASH->"animation.starblade.spin";case STARBOMB->"animation.starbomb.spin";default->null;};
            return anim==null?software.bernie.geckolib.core.object.PlayState.STOP:state.setAndContinue(RawAnimation.begin().thenLoop(anim));}));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
