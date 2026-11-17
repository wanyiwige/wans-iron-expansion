package cn.wanyiwige.alexcavesspellsbook.entities;

import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.util.SaciliteSpellSupport;
import io.redspace.ironsspellbooks.api.entity.NoKnockbackProjectile;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Comparator;

/** One-shot swept projectiles; no Foolish mob AI, block placement or vanilla explosion damage. */
public final class SaciliteSpellProjectile extends Projectile implements GeoEntity, NoKnockbackProjectile {
    private static final EntityDataAccessor<Boolean> RAIN = SynchedEntityData.defineId(
            SaciliteSpellProjectile.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private LivingEntity target;
    private Vec3 direction = new Vec3(0, 0, 1);
    private float damage;
    private int delay;
    private int fuse = -1;
    private double travelled;
    private Vec3 interpolationTarget = Vec3.ZERO;
    private int interpolationSteps;
    private float targetYaw, targetPitch;

    public SaciliteSpellProjectile(EntityType<? extends SaciliteSpellProjectile> type, Level level) {
        super(type, level); setNoGravity(true);
    }
    public SaciliteSpellProjectile(Level level, LivingEntity owner, LivingEntity target,
                                  boolean rain, int delay, float damage, Vec3 direction) {
        this(rain ? ModEntities.SACILITE_RAIN_CRYSTAL.get() : ModEntities.SEEKING_SACILITE_CRYSTAL.get(), level);
        setOwner(owner); this.target = target; this.delay = delay; this.damage = damage;
        this.direction = direction.normalize(); entityData.set(RAIN, rain);
    }
    @Override protected void defineSynchedData() { entityData.define(RAIN, false); }
    public boolean isRain() { return entityData.get(RAIN); }

    public void aimAt(Vec3 heading) {
        if (heading.lengthSqr() > 1.0E-6) direction = heading.normalize();
        face(direction);
        yRotO = getYRot(); xRotO = getXRot();
    }
    private void face(Vec3 heading) {
        float yaw = (float) (Mth.atan2(-heading.x, heading.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(heading.y, heading.horizontalDistance()) * Mth.RAD_TO_DEG);
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
        setXRot(getXRot() + Mth.wrapDegrees(pitch - getXRot()));
    }

    private void armRain() {
        fuse = 20;
        setDeltaMovement(Vec3.ZERO); hasImpulse = true;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 1, 0.6F);
    }

    @Override public void tick() {
        yRotO = getYRot(); xRotO = getXRot();
        super.tick();
        if (level().isClientSide) {
            if (interpolationSteps > 0) {
                double alpha = 1.0 / interpolationSteps--;
                setPos(position().lerp(interpolationTarget, alpha));
                setYRot(getYRot() + Mth.wrapDegrees(targetYaw - getYRot()) * (float) alpha);
                setXRot(getXRot() + Mth.wrapDegrees(targetPitch - getXRot()) * (float) alpha);
            }
            return;
        }
        if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                || owner.level() != level() || tickCount > 120 || travelled > 64
                || !level().hasChunkAt(blockPosition())) { discard(); return; }
        if (fuse >= 0) {
            setDeltaMovement(Vec3.ZERO);
            if (--fuse <= 0) { impact(owner, null); return; }
            if (fuse % 4 == 0 && level() instanceof ServerLevel server)
                SaciliteSpellSupport.particles(server, position().add(0, 0.5, 0), 2, 0.3);
            return;
        }
        if (tickCount <= delay) return;
        if (target != null && (target.level() != level() || target.isRemoved()
                || !SaciliteSpellSupport.canHit(owner, target))) target = null;
        Vec3 velocity;
        if (isRain()) {
            velocity = new Vec3(0, Math.max(-1.5, getDeltaMovement().y - 0.085), 0);
        } else {
            Vec3 current = getDeltaMovement().lengthSqr() < 1.0E-6 ? direction : getDeltaMovement().normalize();
            Vec3 desired = target == null ? direction : target.getBoundingBox().getCenter().subtract(position()).normalize();
            Vec3 turn = current.scale(0.82).add(desired.scale(0.18));
            velocity = turn.lengthSqr() < 1.0E-6 ? current.scale(0.75) : turn.normalize().scale(0.75);
            direction = velocity.normalize();
        }
        Vec3 start = position(), end = start.add(velocity);
        if (!level().hasChunkAt(net.minecraft.core.BlockPos.containing(end))) { discard(); return; }
        BlockHitResult wall = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 stop = wall.getLocation();
        LivingEntity victim = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, stop).inflate(0.4),
                e -> SaciliteSpellSupport.canHit(owner, e) && (e.getBoundingBox().inflate(0.2).contains(start)
                        || e.getBoundingBox().inflate(0.2).clip(start, stop).isPresent()))
                .stream().min(Comparator.comparingDouble(e -> contact(e, start, stop).distanceToSqr(start))).orElse(null);
        if (victim != null && (wall.getType() == HitResult.Type.MISS
                || contact(victim, start, stop).distanceToSqr(start) < stop.distanceToSqr(start))) {
            // Rain crystals pass through living bodies and settle on terrain before detonating.
            if (!isRain()) { setPos(contact(victim, start, stop)); impact(owner, victim); return; }
        }
        setDeltaMovement(velocity); setPos(stop); travelled += start.distanceTo(stop); hasImpulse = true;
        if (!isRain()) face(velocity);
        if (wall.getType() != HitResult.Type.MISS) {
            if (isRain()) { setPos(stop.add(0, 0.02, 0)); armRain(); }
            else impact(owner, null);
            return;
        }
        if ((tickCount & 3) == 0 && level() instanceof ServerLevel server)
            SaciliteSpellSupport.particles(server, position(), 1, 0.05);
    }

    private static Vec3 contact(LivingEntity victim, Vec3 start, Vec3 stop) {
        AABB box = victim.getBoundingBox().inflate(0.2);
        return box.contains(start) ? start : box.clip(start, stop).orElse(stop);
    }
    private void impact(LivingEntity owner, LivingEntity direct) {
        if (!(level() instanceof ServerLevel server)) return;
        var spell = isRain() ? ModSpells.SACILITE_RAIN.get() : ModSpells.SEEKING_SACILITE_SWARM.get();
        if (isRain()) {
            Vec3 center = position().add(0, 0.5, 0);
            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(2.5), e -> SaciliteSpellSupport.canHit(owner, e))) {
                Vec3 nearest = new Vec3(Mth.clamp(center.x, victim.getBoundingBox().minX, victim.getBoundingBox().maxX),
                        Mth.clamp(center.y, victim.getBoundingBox().minY, victim.getBoundingBox().maxY),
                        Mth.clamp(center.z, victim.getBoundingBox().minZ, victim.getBoundingBox().maxZ));
                if (nearest.distanceToSqr(center) > 6.25) continue;
                HitResult cover = level().clip(new ClipContext(center.add(0, 0.1, 0), victim.getBoundingBox().getCenter(),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (victim == direct || cover.getType() == HitResult.Type.MISS)
                    DamageSources.applyDamage(victim, damage, spell.getDamageSource(this, owner).setIFrames(0));
            }
            SaciliteSpellSupport.sound(level(), center, "abominationstomp", 0.6F, 1.2F);
            level().playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.PLAYERS, 1, 0.6F);
            level().playSound(null, center.x, center.y, center.z, SoundEvents.DRAGON_FIREBALL_EXPLODE,
                    SoundSource.PLAYERS, 1, 0.9F);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    center.x, center.y, center.z, 1, 0, 0, 0, 0);
        } else if (direct != null) {
            DamageSources.applyDamage(direct, damage, spell.getDamageSource(this, owner).setIFrames(0));
        }
        SaciliteSpellSupport.particles(server, position(), isRain() ? 24 : 8, isRain() ? 0.7 : 0.2);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                0.7F, isRain() ? 0.5F : 1.2F);
        discard();
    }

    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        interpolationTarget = new Vec3(x, y, z); interpolationSteps = Math.max(1, steps);
        // Rotation interpolates on the same timeline as the network position updates.
        targetYaw = getYRot() + Mth.wrapDegrees(yaw - getYRot());
        targetPitch = getXRot() + Mth.wrapDegrees(pitch - getXRot());
    }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); discard(); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fly", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.seekingsacilite.fly"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
