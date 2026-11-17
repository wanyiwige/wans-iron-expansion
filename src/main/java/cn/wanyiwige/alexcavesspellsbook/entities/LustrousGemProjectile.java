package cn.wanyiwige.alexcavesspellsbook.entities;

import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import io.redspace.ironsspellbooks.api.entity.NoKnockbackProjectile;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.List;

/** Server-authoritative lock -> pursuit -> inward spiral, with swept piercing hits. */
public final class LustrousGemProjectile extends Projectile implements NoKnockbackProjectile {
    public static final int LIFETIME = 140;
    public static final int ORBIT_TICKS = 64;
    public static final int HIT_INTERVAL = 10;
    public static final int MAX_HITS_PER_VICTIM = 6;
    private static final double SPEED = 0.85;
    private static final double MAX_DISTANCE = 96;
    private static final EntityDataAccessor<Integer> GEM_KIND = SynchedEntityData.defineId(
            LustrousGemProjectile.class, EntityDataSerializers.INT);
    private final Map<UUID, Integer> nextHit = new HashMap<>();
    private final Map<UUID, Integer> hitCount = new HashMap<>();
    private LivingEntity lockedTarget;
    private Vec3 straightDirection = new Vec3(0, 0, 1);
    private float damage;
    private int launchDelay;
    private int orbitAge = -1;
    private double orbitAngle;
    private double orbitRadius;
    private double phase;
    private double travelled;
    private int interpolationSteps;
    private Vec3 interpolationTarget = Vec3.ZERO;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    public List<Vec3> getTrail() { return List.copyOf(trail); }

    public LustrousGemProjectile(EntityType<? extends LustrousGemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public LustrousGemProjectile(Level level, LivingEntity owner, LivingEntity target, Vec3 direction,
                                 int kind, int delay, float damage, double phase) {
        this(ModEntities.LUSTROUS_GEM.get(), level);
        setOwner(owner);
        lockedTarget = target;
        straightDirection = direction.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : direction.normalize();
        entityData.set(GEM_KIND, Mth.clamp(kind, 0, 8));
        launchDelay = delay;
        this.damage = damage;
        this.phase = phase;
        if (delay == 0) setDeltaMovement(straightDirection.scale(SPEED));
    }

    @Override protected void defineSynchedData() { entityData.define(GEM_KIND, 0); }
    public int getGemKind() { return entityData.get(GEM_KIND); }

    public static Vec3 casterOrigin(LivingEntity caster) {
        double yaw = Math.toRadians(caster.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        return caster.position().add(0, caster.getBbHeight() * 0.89, 0)
                .add(right.scale(0.78)).subtract(forward.scale(0.10));
    }

    public static boolean canDamage(LivingEntity owner, LivingEntity target) {
        if (owner == target || !target.isAlive() || target.isSpectator() || !target.isPickable()
                || owner.isAlliedTo(target) || target.isAlliedTo(owner)) return false;
        if (target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        return !(owner instanceof Player player && target instanceof Player other && !player.canHarmPlayer(other));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 before = position();
            if (interpolationSteps > 0) {
                setPos(position().lerp(interpolationTarget, 1.0 / interpolationSteps--));
            }
            if (before.distanceToSqr(position()) > 16) trail.clear();
            if (before.distanceToSqr(position()) > 1.0E-6) {
                trail.addFirst(before);
                while (trail.size() > 16) trail.removeLast();
            }
            if (getDeltaMovement().lengthSqr() > 0.01 && (tickCount & 1) == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }
        if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || tickCount > LIFETIME) {
            discard(); return;
        }
        if (tickCount <= launchDelay) {
            setPos(casterOrigin(owner)); return;
        }
        if (lockedTarget != null && (!canDamage(owner, lockedTarget)
                || lockedTarget.level() != level() || lockedTarget.isRemoved())) {
            lockedTarget = null;
            orbitAge = -1;
            // Freeze this direction once; an unlocked projectile never selects another victim.
            if (getDeltaMovement().lengthSqr() > 1.0E-8) straightDirection = getDeltaMovement().normalize();
        }
        if (orbitAge >= ORBIT_TICKS) { discard(); return; }
        Vec3 step = nextMotion();
        Vec3 start = position();
        Vec3 end = start.add(step);
        HitResult wall = level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 stop = wall.getType() == HitResult.Type.MISS ? end : wall.getLocation();
        applySweptHits(owner, start, stop, wall.getType() != HitResult.Type.MISS);
        travelled += start.distanceTo(stop);
        setDeltaMovement(stop.subtract(start));
        setPos(stop);
        hasImpulse = true;
        if (wall.getType() != HitResult.Type.MISS || travelled >= MAX_DISTANCE) discard();
    }

    private Vec3 nextMotion() {
        if (lockedTarget == null) return straightDirection.scale(SPEED);
        Vec3 center = lockedTarget.getBoundingBox().getCenter();
        Vec3 relative = position().subtract(center);
        if (orbitAge < 0 && relative.lengthSqr() <= 3.2 * 3.2) {
            orbitAge = 0;
            orbitAngle = Math.atan2(relative.z, relative.x) + phase;
            orbitRadius = Mth.clamp(Math.sqrt(relative.x * relative.x + relative.z * relative.z), 1.8, 3.2);
        }
        if (orbitAge >= 0) {
            double progress = (double) ++orbitAge / ORBIT_TICKS;
            double radius = 0.08 + (orbitRadius - 0.08) * Math.pow(1.0 - progress, 1.6);
            orbitAngle += 0.48;
            Vec3 waypoint = center.add(Math.cos(orbitAngle) * radius,
                    Math.sin(orbitAngle * 0.7 + phase) * 0.38 * (1.0 - progress),
                    Math.sin(orbitAngle) * radius);
            Vec3 desired = waypoint.subtract(position());
            // Bound correction toward fast moving targets without overshooting the shrinking path.
            return desired.lengthSqr() > 1.35 * 1.35 ? desired.normalize().scale(1.35) : desired;
        }
        Vec3 desired = center.subtract(position()).normalize();
        Vec3 current = getDeltaMovement().normalize();
        Vec3 blended = current.scale(0.35).add(desired.scale(0.65));
        return blended.lengthSqr() < 1.0E-8 ? desired.scale(SPEED) : blended.normalize().scale(SPEED);
    }

    private void applySweptHits(LivingEntity owner, Vec3 start, Vec3 stop, boolean blocked) {
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(start, stop).inflate(0.35), target -> canDamage(owner, target))) {
            UUID id = victim.getUUID();
            if (tickCount < nextHit.getOrDefault(id, 0)
                    || hitCount.getOrDefault(id, 0) >= MAX_HITS_PER_VICTIM) continue;
            AABB hitBox = victim.getBoundingBox().inflate(0.14);
            Vec3 contact = hitBox.contains(start) ? start : hitBox.clip(start, stop).orElse(null);
            if (contact == null || blocked && start.distanceToSqr(contact) >= start.distanceToSqr(stop)) continue;
            nextHit.put(id, tickCount + HIT_INTERVAL);
            // Iron's own per-hit i-frame setting lets the other gems hit in the same wave.
            boolean hit = DamageSources.applyDamage(victim, damage,
                    ModSpells.LAND_OF_THE_LUSTROUS.get().getDamageSource(this, owner).setIFrames(0));
            if (hit) {
                hitCount.merge(id, 1, Integer::sum);
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, contact.x, contact.y, contact.z,
                            3, 0.08, 0.08, 0.08, 0.02);
                }
            }
            // Do not discard or clear the target: the gem passes through and keeps spiralling.
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        interpolationTarget = new Vec3(x, y, z);
        interpolationSteps = Math.max(1, steps);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
