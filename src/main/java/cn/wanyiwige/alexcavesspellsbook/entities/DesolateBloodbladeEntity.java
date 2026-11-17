package cn.wanyiwige.alexcavesspellsbook.entities;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyCombat;
import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.registry.PureDarknessParticles;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A dagger that disappears on contact. All daggers in one cast share a hit set. */
public final class DesolateBloodbladeEntity extends Projectile {
    private static final int MAX_FLIGHT_TICKS = 24;
    public static final double SPEED = 2.1;
    private static final DustParticleOptions TRAIL_DUST =
            new DustParticleOptions(new Vector3f(.95F, .09F, .12F), .55F);

    private Volley volley;
    private Vec3 direction = Vec3.ZERO;
    private double remainingRange;
    private float directDamage;

    private static final class Volley {
        private final Set<UUID> hits = new HashSet<>();
        private boolean playedImpactSound;
    }

    public DesolateBloodbladeEntity(EntityType<? extends DesolateBloodbladeEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static void launchVolley(ServerLevel level, LivingEntity caster, Vec3 direction,
                                    int count, double range, float directDamage) {
        Vec3 forward = direction.normalize();
        if (forward.lengthSqr() < 1.0E-8) return;
        Volley volley = new Volley();
        for (int i = 0; i < count; i++) {
            DesolateBloodbladeEntity knife = new DesolateBloodbladeEntity(ModEntities.DESOLATE_BLOODBLADE.get(), level);
            float angle = (float) Math.toRadians((i - (count - 1) / 2.0) * 3.0);
            knife.direction = forward.yRot(angle);
            knife.volley = volley;
            knife.remainingRange = range;
            knife.directDamage = directDamage;
            knife.setOwner(caster);
            knife.setPos(caster.getEyePosition());
            knife.shoot(knife.direction.x, knife.direction.y, knife.direction.z, (float) SPEED, 0);
            level.addFreshEntity(knife);
        }
    }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (volley == null || !(getOwner() instanceof LivingEntity owner)
                || !owner.isAlive() || owner.isRemoved() || owner.level() != server) {
            discard();
            return;
        }
        fly(server, owner);
    }

    private void fly(ServerLevel server, LivingEntity owner) {
        double distance = Math.min(SPEED, remainingRange);
        if (distance <= 1.0E-5 || tickCount > MAX_FLIGHT_TICKS) {
            discard();
            return;
        }
        Vec3 start = position();
        Vec3 end = start.add(direction.scale(distance));
        if (!server.hasChunkAt(net.minecraft.core.BlockPos.containing(end))) {
            discard();
            return;
        }
        BlockHitResult block = clip(server, start, end);
        Vec3 stop = block.getLocation();
        EntityHitResult hit = traceLiving(server, owner, start, stop, .13);
        boolean livingImpact = hit != null && !ForgeEventFactory.onProjectileImpact(this, hit);
        emitTrail(server, start, livingImpact ? hit.getLocation() : stop);
        if (livingImpact) {
            LivingEntity target = (LivingEntity) hit.getEntity();
            if (volley.hits.add(target.getUUID())) {
                DamageSources.applyDamage(target, directDamage,
                        ModSpells.DESOLATE_BLOODBLADE.get().getDamageSource(this, owner));
            }
            impact(server, hit.getLocation().subtract(direction.scale(.035)));
        } else if (block.getType() != HitResult.Type.MISS) {
            if (ForgeEventFactory.onProjectileImpact(this, block)) {
                discard();
                return;
            }
            Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
            // Back off toward the exposed face, including for very shallow impact angles.
            impact(server, stop.add(normal.scale(.06)));
        } else {
            setPos(stop);
            remainingRange -= distance;
        }
    }

    private static void emitTrail(ServerLevel level, Vec3 start, Vec3 stop) {
        double length = start.distanceTo(stop);
        if (length < .08) return;
        int samples = (int) Math.ceil(length / .75);
        for (int i = 0; i < samples; i++) {
            Vec3 point = start.lerp(stop, (i + .5) / samples);
            level.sendParticles(TRAIL_DUST,
                    point.x, point.y, point.z, 1, .018, .018, .018, 0);
        }
    }

    private void impact(ServerLevel server, Vec3 point) {
        server.sendParticles(PureDarknessParticles.SCARLET_DROP.get(),
                point.x, point.y, point.z, 2, .08, .07, .08, .027);
        if (!volley.playedImpactSound) {
            volley.playedImpactSound = true;
            server.playSound(null, point.x, point.y, point.z, ACSoundRegistry.DESOLATE_DAGGER_HIT.get(),
                    SoundSource.PLAYERS, .7F, 1.12F);
        }
        discard();
    }

    private EntityHitResult traceLiving(ServerLevel server, LivingEntity owner, Vec3 from, Vec3 to,
                                       double radius) {
        EntityHitResult nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(radius),
                e -> e.isPickable() && CandyCombat.enemy(owner, e) && CandyCombat.visible(server, from, e))) {
            AABB body = target.getBoundingBox().inflate(radius);
            Vec3 contact = body.contains(from) ? from : body.clip(from, to).orElse(null);
            if (contact == null) continue;
            double distance = from.distanceToSqr(contact);
            if (distance < nearestDistance) {
                nearest = new EntityHitResult(target, contact);
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private BlockHitResult clip(ServerLevel server, Vec3 from, Vec3 to) {
        return server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
