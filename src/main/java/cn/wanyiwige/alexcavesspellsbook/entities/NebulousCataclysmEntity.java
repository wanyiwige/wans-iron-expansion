package cn.wanyiwige.alexcavesspellsbook.entities;

import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.util.SaciliteSpellSupport;
import io.redspace.ironsspellbooks.api.entity.NoKnockbackProjectile;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;
import java.util.*;

/** Server-owned gameplay; synchronized sprite age and delayed, one-shot nova entities. */
public final class NebulousCataclysmEntity extends Projectile implements NoKnockbackProjectile {
    public static final double SPEED = 0.45;
    public static final int CLOUD_LIFETIME = 100, NOVA_DURATION = 20;
    public static final double NOVA_RADIUS = 2.5;
    private static final EntityDataAccessor<Boolean> NOVA = SynchedEntityData.defineId(
            NebulousCataclysmEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> BIRTH = SynchedEntityData.defineId(
            NebulousCataclysmEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(
            NebulousCataclysmEntity.class, EntityDataSerializers.INT);
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.85F, 0.16F, 0.9F), 1.1F);
    private final Set<UUID> hitEntities = new HashSet<>();
    private LivingEntity target;
    private float damage;
    private boolean detonated, novaApplied;
    private Vec3 interpolationTarget = Vec3.ZERO;
    private int interpolationSteps;

    public NebulousCataclysmEntity(EntityType<? extends NebulousCataclysmEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }
    public NebulousCataclysmEntity(Level level, LivingEntity owner, float damage, LivingEntity target) {
        this(ModEntities.NEBULOUS_CATACLYSM.get(), level);
        setOwner(owner);
        this.damage = damage;
        this.target = target;
        entityData.set(BIRTH, level.getGameTime());
    }
    @Override protected void defineSynchedData() {
        entityData.define(NOVA, false);
        entityData.define(BIRTH, 0L);
        entityData.define(DELAY, 0);
    }
    public boolean isNova() { return entityData.get(NOVA); }
    public float visualAge(float partial) {
        return (float) (level().getGameTime() - entityData.get(BIRTH) - entityData.get(DELAY)) + partial;
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (interpolationSteps > 0) setPos(position().lerp(interpolationTarget, 1.0 / interpolationSteps--));
            return;
        }
        if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                || owner.level() != level() || !level().hasChunkAt(blockPosition())) { discard(); return; }
        if (isNova()) {
            int age = (int) visualAge(0);
            if (age < 0) return;
            if (!novaApplied) { novaApplied = true; applyNova(owner); }
            if (age >= NOVA_DURATION) discard();
            return;
        }
        if (tickCount >= CLOUD_LIFETIME) { detonate(owner); return; }
        if (target != null && (!validTarget(owner, target) || distanceToSqr(target) > 16 * 16)) target = null;
        if (target == null && tickCount % 4 == 0) {
            Vec3 forward = getDeltaMovement().normalize();
            target = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12),
                    e -> validTarget(owner, e) && distanceToSqr(e) <= 144
                            && e.getBoundingBox().getCenter().subtract(position()).normalize().dot(forward) > -0.15)
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-6) velocity = owner.getLookAngle().scale(SPEED);
        if (target != null) {
            Vec3 desired = target.getBoundingBox().getCenter().subtract(position()).normalize();
            Vec3 turn = velocity.normalize().scale(0.86).add(desired.scale(0.14));
            if (turn.lengthSqr() > 1.0E-6) velocity = turn.normalize().scale(SPEED);
        }
        Vec3 start = position(), end = start.add(velocity);
        if (!level().hasChunkAt(BlockPos.containing(end))) { discard(); return; }
        BlockHitResult wall = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 stop = wall.getLocation();
        List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, new AABB(start, stop).inflate(0.35),
                e -> SaciliteSpellSupport.canHit(owner, e) && !hitEntities.contains(e.getUUID())
                        && (e.getBoundingBox().inflate(0.25).contains(start)
                        || e.getBoundingBox().inflate(0.25).clip(start, stop).isPresent()));
        victims.sort(Comparator.comparingDouble(e -> contact(e, start, stop).distanceToSqr(start)));
        for (LivingEntity victim : victims) {
            Vec3 contact = contact(victim, start, stop);
            if (wall.getType() != HitResult.Type.MISS && contact.distanceToSqr(start) >= stop.distanceToSqr(start)) continue;
            hitEntities.add(victim.getUUID());
            DamageSources.applyDamage(victim, damage, ModSpells.NEBULOUS_CATACLYSM.get()
                    .getDamageSource(this, owner).setIFrames(0));
            if (victim == target) target = null;
            if (hitEntities.size() >= 3) { setPos(contact); detonate(owner); return; }
        }
        setDeltaMovement(velocity);
        setPos(stop);
        hasImpulse = true;
        if (wall.getType() != HitResult.Type.MISS) {
            setPos(stop.subtract(velocity.normalize().scale(0.05)));
            detonate(owner);
            return;
        }
        if (tickCount % 3 == 0 && level() instanceof ServerLevel server)
            server.sendParticles(DUST, getX(), getY(), getZ(), 2, 0.15, 0.15, 0.15, 0);
    }
    private boolean validTarget(LivingEntity owner, LivingEntity victim) {
        return !victim.isRemoved() && victim.level() == level() && !hitEntities.contains(victim.getUUID())
                && SaciliteSpellSupport.canHit(owner, victim) && visible(position(), victim.getBoundingBox().getCenter());
    }
    private boolean visible(Vec3 from, Vec3 to) {
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }
    private static Vec3 contact(LivingEntity victim, Vec3 start, Vec3 end) {
        AABB box = victim.getBoundingBox().inflate(0.25);
        return box.contains(start) ? start : box.clip(start, end).orElse(end);
    }
    private void detonate(LivingEntity owner) {
        if (detonated || isNova() || level().isClientSide) return;
        detonated = true;
        int count = 6 + random.nextInt(3);
        double phase = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            // One central nova, with evenly spaced satellites well outside its sprite.
            double angle = phase + (i - 1) * Math.PI * 2 / (count - 1);
            double radius = i == 0 ? 0 : 4.0 + random.nextDouble() * 0.5;
            Vec3 point = position().add(Math.cos(angle) * radius,
                    i == 0 ? 0 : (random.nextDouble() - 0.5) * 1.2,
                    Math.sin(angle) * radius);
            // Keep satellite explosions on the visible side of intervening walls.
            var wall = level().clip(new ClipContext(position(), point, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (wall.getType() != HitResult.Type.MISS)
                point = wall.getLocation().subtract(point.subtract(position()).normalize().scale(0.05));
            if (!level().hasChunkAt(BlockPos.containing(point))) point = position();
            var nova = new NebulousCataclysmEntity(level(), owner, damage * 0.5F, null);
            nova.entityData.set(NOVA, true);
            nova.entityData.set(DELAY, i * 2);
            nova.setPos(point);
            level().addFreshEntity(nova);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.DRAGON_FIREBALL_EXPLODE,
                SoundSource.PLAYERS, 0.8F, 1.25F);
        discard();
    }
    private void applyNova(LivingEntity owner) {
        Vec3 center = position();
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(NOVA_RADIUS), e -> SaciliteSpellSupport.canHit(owner, e))) {
            AABB box = victim.getBoundingBox();
            Vec3 nearest = new Vec3(Mth.clamp(center.x, box.minX, box.maxX),
                    Mth.clamp(center.y, box.minY, box.maxY), Mth.clamp(center.z, box.minZ, box.maxZ));
            if (nearest.distanceToSqr(center) <= NOVA_RADIUS * NOVA_RADIUS
                    && visible(center, box.getCenter()))
                DamageSources.applyDamage(victim, damage, ModSpells.NEBULOUS_CATACLYSM.get()
                        .getDamageSource(this, owner).setIFrames(0));
        }
        if (level() instanceof ServerLevel server)
            server.sendParticles(DUST, getX(), getY(), getZ(), 10, 0.7, 0.7, 0.7, 0);
    }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        interpolationTarget = new Vec3(x, y, z);
        interpolationSteps = Math.max(1, steps);
    }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); discard(); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
