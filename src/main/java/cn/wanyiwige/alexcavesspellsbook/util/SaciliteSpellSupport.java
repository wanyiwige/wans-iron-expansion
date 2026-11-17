package cn.wanyiwige.alexcavesspellsbook.util;

import cn.wanyiwige.alexcavesspellsbook.entities.SaciliteSpellProjectile;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Comparator;

public final class SaciliteSpellSupport {
    private SaciliteSpellSupport() { }
    private static Entity principal(Entity entity) {
        var seen = new java.util.HashSet<java.util.UUID>();
        for (int i = 0; i < 8 && seen.add(entity.getUUID()); i++) {
            Entity next = entity instanceof IMagicSummon summon ? summon.getSummoner()
                    : entity instanceof OwnableEntity owned ? owned.getOwner() : null;
            if (next == null || next == entity) break;
            entity = next;
        }
        return entity;
    }
    public static boolean canHit(LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isSpectator() || !target.isPickable()
                || !owner.canAttack(target) || DamageSources.isFriendlyFireBetween(owner, target)
                || owner.isAlliedTo(target) || target.isAlliedTo(owner)) return false;
        if (target instanceof TamableAnimal pet && owner.getUUID().equals(pet.getOwnerUUID())) return false;
        if (target instanceof IMagicSummon summon && summon.getSummoner() != null
                && owner.getUUID().equals(summon.getSummoner().getUUID())) return false;
        Entity a = principal(owner), b = principal(target);
        if (a.getUUID().equals(b.getUUID()) || a.isAlliedTo(b) || b.isAlliedTo(a)) return false;
        if (a instanceof Player player && b instanceof Player other)
            return owner.level() instanceof ServerLevel server && server.getServer().isPvpAllowed() && player.canHarmPlayer(other);
        return true;
    }

    public static LivingEntity selectTarget(Level level, LivingEntity caster, double range) {
        if (caster instanceof Mob mob && mob.getTarget() != null) {
            LivingEntity target = mob.getTarget();
            return canHit(caster, target) && caster.distanceToSqr(target) <= range * range
                    && caster.hasLineOfSight(target) ? target : null;
        }
        Vec3 eye = caster.getEyePosition(), end = aimPoint(level, caster, range);
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(0.8),
                e -> canHit(caster, e) && caster.hasLineOfSight(e)
                        && e.getBoundingBox().inflate(0.3).clip(eye, end).isPresent())
                .stream().min(Comparator.comparingDouble(e -> eye.distanceToSqr(
                        e.getBoundingBox().inflate(0.3).clip(eye, end).orElse(end)))).orElse(null);
    }

    public static Vec3 aimPoint(Level level, LivingEntity caster, double range) {
        Vec3 eye = caster.getEyePosition();
        return level.clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
    }

    public static Vec3 rainOrigin(Level level, LivingEntity caster, Vec3 floor, double height) {
        // Find the ceiling first: indoor casts never create crystals above an opaque roof.
        BlockHitResult ceiling = level.clip(new ClipContext(floor, floor.add(0, height, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 top = ceiling.getType() == HitResult.Type.MISS ? ceiling.getLocation() : ceiling.getLocation().add(0, -0.6, 0);
        return top.y - floor.y >= 0.8 && top.y < level.getMaxBuildHeight() - 1
                && level.hasChunkAt(net.minecraft.core.BlockPos.containing(top)) ? top : null;
    }

    public static void limitProjectiles(Level level, LivingEntity owner, int incoming) {
        // Lifetimes/range are bounded; oldest local crystals are retired when rapidly recasting.
        var active = level.getEntitiesOfClass(SaciliteSpellProjectile.class, owner.getBoundingBox().inflate(100),
                e -> e.getOwner() != null && owner.getUUID().equals(e.getOwner().getUUID()))
                .stream().sorted(Comparator.comparingInt((SaciliteSpellProjectile e) -> e.tickCount).reversed()).toList();
        for (int i = 0; i < active.size() + incoming - 32; i++) active.get(i).discard();
    }

    public static void sound(Level level, Vec3 pos, String name, float volume, float pitch) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("foolish", name));
        if (sound != null) level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
    public static void particles(ServerLevel level, Vec3 pos, int count, double spread) {
        var particle = ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation.fromNamespaceAndPath("foolish", "sacilite_particle"));
        level.sendParticles(particle instanceof SimpleParticleType simple ? simple : ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z, count, spread, spread, spread, 0.02);
    }
}
