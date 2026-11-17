package cn.wanyiwige.astralcompat.entity;

import cn.wanyiwige.astralcompat.registry.ModEntities;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative swept collision, five distinct victims, bounded homing. */
public final class PiercingStarEntity extends Projectile {
    public static final double SEEK_RADIUS = 16.0;
    public static final double MAX_DISTANCE = 40.0;
    public static final int MAX_HITS = 5;
    private static final double SPEED = 0.85;
    private final Set<UUID> struck = new HashSet<>();
    private final List<Vec3> trail = new ArrayList<>();
    private LivingEntity tracking;
    private Vec3 fallbackDirection = new Vec3(0, 0, 1);
    private float damage = 1.5F;
    private double distanceFlown;
    private int launchDelay;

    public PiercingStarEntity(EntityType<? extends PiercingStarEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public PiercingStarEntity(Level level, LivingEntity owner, Vec3 look, int launchDelay, float damage) {
        this(ModEntities.PIERCING_STAR.get(), level);
        setOwner(owner);
        this.fallbackDirection = look.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : look.normalize();
        this.launchDelay = launchDelay;
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData() {
    }

    public List<Vec3> getTrail() {
        return trail;
    }

    private boolean eligible(LivingEntity candidate) {
        Entity owner = getOwner();
        return candidate != owner && candidate.isAlive() && !candidate.isSpectator()
                && candidate.isPickable() && !struck.contains(candidate.getUUID())
                && (owner == null || (!owner.isAlliedTo(candidate) && !candidate.isAlliedTo(owner)))
                && !(candidate instanceof TamableAnimal pet && owner != null
                && owner.getUUID().equals(pet.getOwnerUUID()));
    }

    private boolean visible(LivingEntity candidate) {
        return level().clip(new ClipContext(position(), candidate.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void steer() {
        if (tracking == null || !eligible(tracking)
                || distanceToSqr(tracking) > SEEK_RADIUS * SEEK_RADIUS || !visible(tracking)) {
            tracking = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SEEK_RADIUS),
                            target -> eligible(target) && distanceToSqr(target) <= SEEK_RADIUS * SEEK_RADIUS)
                    .stream().filter(this::visible)
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        Vec3 direction;
        if (tracking == null) {
            // No target: continue in the caster's view direction recorded at release.
            direction = fallbackDirection;
        } else {
            Vec3 desired = tracking.getBoundingBox().getCenter().subtract(position()).normalize();
            Vec3 current = getDeltaMovement().normalize();
            // Desired direction must dominate: otherwise an exactly rearward target can never turn the star.
            direction = current.lengthSqr() < 1.0E-8 ? desired : current.scale(0.35).add(desired.scale(0.65)).normalize();
            if (direction.lengthSqr() < 1.0E-8) direction = desired;
        }
        setDeltaMovement(direction.scale(SPEED));
        hasImpulse = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            trail.add(0, position());
            if (trail.size() > 12) trail.remove(trail.size() - 1);
            Vec3 motion = getDeltaMovement();
            setPos(position().add(motion));
            if (motion.lengthSqr() > 0.001) {
                for (int i = 0; i < 3; i++) {
                    Vec3 p = position().subtract(motion.scale(i / 3.0));
                    level().addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0, 0);
                }
            }
            return;
        }
        if (tickCount > 160 || getOwner() == null || !getOwner().isAlive()) {
            discard();
            return;
        }
        if (tickCount <= launchDelay) return;
        steer();

        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement().normalize().scale(Math.min(SPEED, MAX_DISTANCE - distanceFlown)));
        HitResult block = level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        List<Impact> hits = new ArrayList<>();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(start, stop).inflate(0.6), this::eligible)) {
            AABB box = target.getBoundingBox().inflate(0.16);
            Vec3 contact = box.contains(start) ? start : box.clip(start, stop).orElse(null);
            // Resolve victims strictly before the blocking surface, never behind walls.
            if (contact != null && (block.getType() == HitResult.Type.MISS
                    || start.distanceToSqr(contact) < start.distanceToSqr(stop))) {
                hits.add(new Impact(target, contact));
            }
        }
        hits.sort(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.position())));
        for (Impact hit : hits) {
            if (!struck.add(hit.target().getUUID())) continue;
            DamageSources.applyDamage(hit.target(), damage,
                    SpellRegistry.getSpell("asterismarcanum:piercing_light").getDamageSource(this, getOwner()));
            impact(hit.position());
            tracking = null;
            if (struck.size() >= MAX_HITS) {
                setPos(hit.position());
                discard();
                return;
            }
        }
        distanceFlown += start.distanceTo(stop);
        setPos(stop);
        if (block.getType() != HitResult.Type.MISS || distanceFlown >= MAX_DISTANCE - 1.0E-6) {
            impact(stop);
            discard();
        }
    }

    private void impact(Vec3 point) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 8, 0.12, 0.12, 0.12, 0.05);
        }
    }

    private record Impact(LivingEntity target, Vec3 position) { }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
