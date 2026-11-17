package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.*;

public final class TwilightAreaEntity extends Projectile implements AntiMagicSusceptible {
    private static final EntityDataAccessor<Float> RADIUS=SynchedEntityData.defineId(TwilightAreaEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> END=SynchedEntityData.defineId(TwilightAreaEntity.class,EntityDataSerializers.VECTOR3);
    private float damage; private int life=20; private int age; private UUID targetId;
    private final Set<UUID> victims=new HashSet<>();
    public TwilightAreaEntity(EntityType<? extends TwilightAreaEntity> type,Level level) {super(type,level);setNoGravity(true);noPhysics=true;}
    public String kind(){return ForgeRegistries.ENTITY_TYPES.getKey(getType()).getPath();}
    public float radius(){return entityData.get(RADIUS);}
    public Vec3 beamEnd(){return position().add(new Vec3(entityData.get(END)));}
    @Override protected void defineSynchedData(){entityData.define(RADIUS,1F);entityData.define(END,new Vector3f());}
    public static TwilightAreaEntity spawn(LivingEntity owner,String kind,Vec3 pos,float damage,float radius,int duration){return spawn(owner,owner.level(),kind,pos,damage,radius,duration);}
    public static TwilightAreaEntity spawn(Entity owner,Level world,String kind,Vec3 pos,float damage,float radius,int duration){
        TwilightAreaEntity field=TwilightEntities.AREAS.get(kind).get().create(world);
        field.setOwner(owner);field.setPos(pos);field.damage=damage;field.entityData.set(RADIUS,radius);field.life=duration;world.addFreshEntity(field);return field;
    }
    public static void beam(LivingEntity owner,Vec3 start,Vec3 end){TwilightAreaEntity field=spawn(owner,"exanimated_ray",start,0,1,12);field.entityData.set(END,end.subtract(start).toVector3f());}
    public static TwilightAreaEntity findOrCreate(LivingEntity owner,String kind,int duration){
        for(TwilightAreaEntity field:owner.level().getEntitiesOfClass(TwilightAreaEntity.class,owner.getBoundingBox().inflate(16),f->f.kind().equals(kind)&&f.getOwner()==owner)){field.life=duration;return field;}
        return spawn(owner,kind,owner.getEyePosition(),0,1,duration);
    }
    public void setTarget(Entity target){targetId=target==null?null:target.getUUID();}
    public void pulse(float damage,float strength){
        if(!(getOwner() instanceof LivingEntity owner))return;
        setPos(owner.getEyePosition());this.damage=damage;
        for(LivingEntity target:level().getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(10),t->TwilightRecreatedSpell.hostile(owner,t))){
            Vec3 direction=target.getBoundingBox().getCenter().subtract(owner.getEyePosition());
            if(direction.length()>10 || direction.normalize().dot(owner.getLookAngle())<.75 || !owner.hasLineOfSight(target))continue;
            DamageSources.applyDamage(target,damage,TwilightSpells.get("carminite_pull").getDamageSource(this,owner));
            target.setDeltaMovement(target.getDeltaMovement().add(owner.position().subtract(target.position()).normalize().scale(strength)));target.hurtMarked=true;
        }
    }
    @Override public void tick(){
        super.tick();age++;
        Entity owner=getOwner();
        if(level().isClientSide){clientParticles();return;}
        if(--life<0){if(kind().equals("ice_bomb_zone"))level().levelEvent(2001,blockPosition(),net.minecraft.world.level.block.Block.getId(Blocks.ICE.defaultBlockState()));discard();return;}
        switch(kind()){
            case "exanimated_ray" -> {}
            case "carminite_pull" -> {if(owner==null||!owner.isAlive())discard();else setPos(owner.getEyePosition());}
            case "mosquito_swarm" -> {
                Entity target=targetId!=null?((ServerLevel)level()).getEntity(targetId):null;
                if(target!=null&&target.isAlive())setPos(position().add(target.getBoundingBox().getCenter().subtract(position()).normalize().scale(.35)));
                if(age%15==0)for(LivingEntity victim:targets(2)){
                    DamageSources.applyDamage(victim,damage,TwilightSpells.get("mosquito_swarm").getDamageSource(this,owner));
                    if(target==null||!target.isAlive())setTarget(victim);
                }
            }
            case "hydra_fire_field" -> {if(age%10==0)for(LivingEntity victim:targets(radius()))TwilightEffectEvents.addHydraFireStack(victim);}
            case "ice_bomb_zone" -> {
                if(age%20==1)for(LivingEntity victim:targets(radius())){
                    if(victim instanceof twilightforest.entity.monster.Yeti){BlockPos p=victim.blockPosition();level().setBlockAndUpdate(p,Blocks.ICE.defaultBlockState());level().setBlockAndUpdate(p.above(),Blocks.ICE.defaultBlockState());victim.discard();}
                    else {DamageSources.applyDamage(victim,damage,TwilightSpells.get("ice_bomb").getDamageSource(this,owner));TwilightProjectile.chill(victim);}
                }
            }
            case "jet_eruption" -> {
                if(age>20&&age<26)for(LivingEntity victim:level().getEntitiesOfClass(LivingEntity.class,new AABB(position().add(-1,0,-1),position().add(1,6.25,1)),t->TwilightRecreatedSpell.hostile(owner,t))){
                    if(!victims.add(victim.getUUID()))continue;
                    DamageSources.applyDamage(victim,damage,TwilightSpells.get("jet_eruption").getDamageSource(this,owner));
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0,1.5,0));victim.hurtMarked=true;
                }
            }
        }
    }
    private List<LivingEntity> targets(float radius){return level().getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(radius,2,radius),t->TwilightRecreatedSpell.hostile(getOwner(),t));}
    private void clientParticles(){
        if(kind().equals("exanimated_ray"))return;
        var particle=switch(kind()){case "hydra_fire_field"->TwilightParticles.HYDRA_FIRE.get();case "mosquito_swarm"->TwilightParticles.MOSQUITO.get();default->null;};
        int count=kind().equals("mosquito_swarm")?4:8;
        for(int i=0;i<count;i++){
            double x=getX()+(random.nextDouble()-.5)*radius()*2,z=getZ()+(random.nextDouble()-.5)*radius()*2;
            double y=getY()+random.nextDouble()*.5;
            if(kind().equals("jet_eruption")){if(age<20||age>66)continue;y=getY()+random.nextDouble()*6;}
            level().addParticle(particle!=null?particle:kind().equals("ice_bomb_zone")?ParticleTypes.SNOWFLAKE:kind().equals("carminite_pull")?ParticleTypes.ELECTRIC_SPARK:ParticleTypes.FLAME,x,y,z,0,.03,0);
        }
    }
    @Override public void onAntiMagic(MagicData data){discard();}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putFloat("Damage",damage);tag.putFloat("Radius",radius());tag.putInt("Life",life);tag.putInt("Age",age);if(targetId!=null)tag.putUUID("Target",targetId);Vector3f end=entityData.get(END);tag.putFloat("EndX",end.x);tag.putFloat("EndY",end.y);tag.putFloat("EndZ",end.z);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);damage=tag.getFloat("Damage");entityData.set(RADIUS,tag.getFloat("Radius"));life=tag.getInt("Life");age=tag.getInt("Age");targetId=tag.hasUUID("Target")?tag.getUUID("Target"):null;entityData.set(END,new Vector3f(tag.getFloat("EndX"),tag.getFloat("EndY"),tag.getFloat("EndZ")));}
}
