package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.astralcompat.registry.ModEntities;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.Optional;
import java.util.function.Supplier;

public final class ArcanumProjectile extends AbstractMagicProjectile {
    private static final EntityDataAccessor<Boolean> FIRE = SynchedEntityData.defineId(ArcanumProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(ArcanumProjectile.class, EntityDataSerializers.FLOAT);
    private String spell = "asterismarcanum:arcanum_starfire";
    public ArcanumProjectile(EntityType<? extends ArcanumProjectile> type, Level world) { super(type, world); setNoGravity(true); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(FIRE, false); entityData.define(SCALE, .2F); }
    public boolean isStarfire() { return entityData.get(FIRE); }
    public float scale() { return entityData.get(SCALE); }
    public static void spawn(Level world, LivingEntity caster, String spell, Vec3 pos, Vec3 velocity, float damage, boolean fire, float scale) {
        ArcanumProjectile bolt = new ArcanumProjectile(ModEntities.ARCANUM_PROJECTILE.get(), world);
        bolt.setOwner(caster); bolt.spell = spell; bolt.setDamage(damage);
        bolt.entityData.set(FIRE, fire); bolt.entityData.set(SCALE, scale);
        bolt.setPierceLevel(fire ? 2 : 0); bolt.setCanRicochet(fire);
        bolt.setPos(pos); bolt.setDeltaMovement(velocity);
        bolt.setYRot((float)Math.toDegrees(Math.atan2(velocity.x, velocity.z)));
        bolt.setXRot((float)Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance())));
        world.addFreshEntity(bolt);
    }
    @Override protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && (!(getOwner() instanceof LivingEntity caster) || !(target instanceof LivingEntity living) || ArcanumEvents.hostile(caster, living));
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) return;
        DamageSources.applyDamage(hit.getEntity(), getDamage(), SpellRegistry.getSpell(spell).getDamageSource(this, getOwner()));
        if (isStarfire()) consumeEntityImpact(hit, true); else discard();
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide && hit.getType()==HitResult.Type.BLOCK) discard();
    }
    @Override public void tick() { super.tick(); if (!level().isClientSide && tickCount > 100) discard(); }
    @Override public float getSpeed() { return 2.5F; }
    @Override public Optional<Supplier<SoundEvent>> getImpactSound() { return Optional.of(() -> SoundEvents.AMETHYST_BLOCK_CHIME); }
    @Override public void trailParticles() { level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0, 0, 0); }
    @Override public void impactParticles(double x, double y, double z) {
        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(level(), ParticleTypes.ELECTRIC_SPARK, x,y,z, 8,.15,.15,.15,.1,false);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putString("Spell", spell); tag.putBoolean("Fire", isStarfire()); tag.putFloat("Scale", scale()); tag.putInt("Age", tickCount);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); spell=tag.getString("Spell"); entityData.set(FIRE,tag.getBoolean("Fire")); entityData.set(SCALE,tag.getFloat("Scale")); tickCount=tag.getInt("Age");
    }
}
