package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.astralcompat.registry.ModEntities;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Short-lived authoritative effects; no persisted player lock or saved anchors. */
public final class ArcanumEffect extends Projectile implements AntiMagicSusceptible {
    public static final int CUTTER=0, ECHO=1, TETHER=2, BEAM=3, SWARM=4, BURST=5;
    private static final EntityDataAccessor<Integer> MODE=SynchedEntityData.defineId(ArcanumEffect.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE=SynchedEntityData.defineId(ArcanumEffect.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH=SynchedEntityData.defineId(ArcanumEffect.class,EntityDataSerializers.FLOAT);
    private int duration=30, hits, lastPaidTick;
    private boolean paidPulse;
    private float damage;
    private String spell="";
    public ArcanumEffect(EntityType<? extends ArcanumEffect> type, Level level) { super(type,level); setNoGravity(true); noPhysics=true; }
    @Override protected void defineSynchedData() { entityData.define(MODE,0); entityData.define(SIZE,1F); entityData.define(LENGTH,0F); }
    public int mode() { return entityData.get(MODE); }
    public float size() { return entityData.get(SIZE); }
    public float length() { return entityData.get(LENGTH); }
    public void setHits(int count) { hits=count; }
    public static ArcanumEffect create(LivingEntity owner,int mode,String spell,float damage,float radius,int duration) {
        ArcanumEffect effect=new ArcanumEffect(ModEntities.ARCANUM_EFFECT.get(),owner.level());
        effect.setOwner(owner); effect.entityData.set(MODE,mode); effect.entityData.set(SIZE,radius);
        effect.spell=spell; effect.damage=damage; effect.duration=duration;
        effect.setPos(owner.position()); owner.level().addFreshEntity(effect); return effect;
    }
    public static void channel(LivingEntity owner,int mode,String spell,float damage) {
        ArcanumEffect effect=owner.level().getEntitiesOfClass(ArcanumEffect.class,owner.getBoundingBox().inflate(8),e->e.getOwner()==owner && e.mode()==mode && e.spell.equals(spell)).stream().findFirst().orElse(null);
        if(effect==null) effect=create(owner,mode,spell,damage,1,120);
        effect.lastPaidTick=effect.tickCount; effect.damage=damage; effect.paidPulse=true;
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) return;
        if(!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || tickCount>duration) {
            discard(); return;
        }
        if(mode()==TETHER) {
            if(!hasPassenger(owner) || hits<=0) { discard(); return; }
            owner.setDeltaMovement(Vec3.ZERO); owner.resetFallDistance();
            if(tickCount%10==0) particles(ParticleTypes.END_ROD,position().add(0,1,0),8,.65);
        } else if(mode()==CUTTER && tickCount>=30) {
            Vec3 center=position();
            for(LivingEntity target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(10),t->ArcanumEvents.hostile(owner,t))) {
                double distance=target.getBoundingBox().getCenter().distanceToSqr(center);
                if(distance<100 && visible(center.add(0,.2,0),target.getBoundingBox().getCenter(),owner)) {
                    DamageSources.applyDamage(target,damage*(float)(1-distance/100),SpellRegistry.getSpell(spell).getDamageSource(this,owner));
                }
            }
            particles(ParticleTypes.FLASH,center,1,0); particles(ParticleTypes.ELECTRIC_SPARK,center,40,2);
            playSound(SoundEvents.AMETHYST_BLOCK_BREAK,1,1); discard();
        } else if(mode()==BEAM || mode()==SWARM) {
            MagicData magic=owner instanceof io.redspace.ironsspellbooks.api.entity.IMagicEntity mage
                    ?mage.getMagicData():MagicData.getPlayerMagicData(owner);
            if(tickCount-lastPaidTick>12 || !magic.isCasting() || !spell.equals(magic.getCastingSpellId())) { discard(); return; }
            setPos(owner.getEyePosition().add(0,-.25,0)); setYRot(owner.getYRot()); setXRot(owner.getXRot());
            if(mode()==BEAM) {
                Vec3 start=position(),direction=owner.getLookAngle().normalize();
                Vec3 end=level().clip(new ClipContext(start,start.add(direction.scale(18)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner)).getLocation();
                entityData.set(LENGTH,(float)start.distanceTo(end));
                if(paidPulse) {
                    for(LivingEntity target:level().getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(.45),t->ArcanumEvents.hostile(owner,t))) {
                        if(target.getBoundingBox().inflate(.3).clip(start,end).isPresent() || target.getBoundingBox().inflate(.3).contains(start)) {
                            DamageSources.applyDamage(target,damage,SpellRegistry.getSpell(spell).getDamageSource(this,owner));
                        }
                    }
                }
            } else if(tickCount%12==0 || tickCount%20==0) {
                Vec3 look=owner.getLookAngle().normalize();
                Vec3 right=new Vec3(0,1,0).cross(look).normalize();
                if(right.lengthSqr()<.01) right=new Vec3(1,0,0);
                Vec3 up=look.cross(right).normalize();
                for(int i=0;i<5;i++) {
                    double a=i*.8;
                    Vec3 gate=position().add(right.scale(Math.cos(a)*2)).add(up.scale(Math.sin(a)*2));
                    // Wall clipping prevents gates firing through nearby cover.
                    if(!visible(owner.getEyePosition(),gate,owner)) continue;
                    for(int n=0;n<3;n++) {
                        Vec3 jitter=new Vec3(random.nextDouble()-.5,random.nextDouble()-.5,random.nextDouble()-.5);
                        ArcanumProjectile.spawn(level(),owner,spell,gate.add(jitter),look.scale(2.5),damage,false,.6F);
                    }
                }
            }
            paidPulse=false;
        }
    }
    private boolean visible(Vec3 start,Vec3 end,Entity owner) {
        return level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner)).getType()==HitResult.Type.MISS;
    }
    private void particles(net.minecraft.core.particles.ParticleOptions type,Vec3 pos,int count,double spread) {
        MagicManager.spawnParticles(level(),type,pos.x,pos.y,pos.z,count,spread,spread,spread,.04,false);
    }
    public boolean absorb(DamageSource source) {
        if(mode()!=TETHER || hits<=0 || source.getEntity()==null || !(getOwner() instanceof LivingEntity owner)
                || !ArcanumEvents.hostile(owner,source.getEntity())) return false;
        --hits; playSound(SoundEvents.AMETHYST_BLOCK_BREAK,.5F,1.4F);
        if(hits==0) discard();
        return true;
    }
    @Override public boolean hurt(DamageSource source,float amount) { return !level().isClientSide && absorb(source); }
    @Override public boolean isPickable() { return mode()==TETHER; }
    @Override public boolean shouldRiderSit() { return false; }
    @Override public double getPassengersRidingOffset() { return 0; }
    @Override protected void positionRider(Entity rider,Entity.MoveFunction move) { move.accept(rider,getX(),getY(),getZ()); }
    @Override public void onAntiMagic(MagicData data) {
        if(level().isClientSide) return;
        if(mode()==ECHO && getOwner() instanceof LivingEntity owner && owner.isAlive()
                && owner.level()==level() && level().noCollision(owner,owner.getBoundingBox().move(position().subtract(owner.position())))) {
            if(owner.isPassenger()) owner.stopRiding();
            io.redspace.ironsspellbooks.api.util.Utils.handleSpellTeleport(SpellRegistry.getSpell(spell),owner,position());
            owner.resetFallDistance();
        }
        discard();
    }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); discard(); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
