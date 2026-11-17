package cn.wanyiwige.alexcavesspellsbook.candy;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

public final class CandyCombat {
    private CandyCombat() { }
    /** Resolve up to eight ownership hops without trusting reused entity network IDs. */
    public static Entity principal(Entity entity) {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 8 && seen.add(entity.getUUID()); i++) {
            Entity next = entity instanceof IMagicSummon summon ? summon.getSummoner()
                    : entity instanceof OwnableEntity owned ? owned.getOwner() : null;
            if (next == null || next == entity) break;
            entity = next;
        }
        return entity;
    }
    public static boolean enemy(LivingEntity owner, LivingEntity target) {
        if (target == null || !owner.isAlive() || !target.isAlive() || target.isSpectator()
                || owner.level() != target.level() || target == owner || owner.isAlliedTo(target) || target.isAlliedTo(owner)) return false;
        Entity a = principal(owner), b = principal(target);
        if (a == b || a.getUUID().equals(b.getUUID()) || a.isAlliedTo(b) || b.isAlliedTo(a)) return false;
        if (a instanceof Player player && b instanceof Player other) {
            if (!(owner.level() instanceof ServerLevel server) || !server.getServer().isPvpAllowed() || !player.canHarmPlayer(other)) return false;
        }
        return owner.canAttack(target) && !DamageSources.isFriendlyFireBetween(owner, target);
    }
    public static boolean visible(ServerLevel level, Vec3 from, LivingEntity target) {
        return level.clip(new ClipContext(from, target.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS;
    }
    public static Vec3 aim(ServerLevel level, LivingEntity owner, double range) {
        Vec3 from = owner.getEyePosition(), to = from.add(owner.getLookAngle().scale(Math.max(0, Math.min(32, range))));
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getLocation();
    }
    public static List<LivingEntity> targets(ServerLevel level, LivingEntity owner, Vec3 center, double radius) {
        double r = Math.max(0, Math.min(8, radius));
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r), e -> enemy(owner, e)
                        && e.getBoundingBox().getCenter().distanceToSqr(center) <= r*r && visible(level, center, e)).stream()
                .sorted(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(center)).thenComparing(Entity::getUUID))
                .limit(32).toList();
    }
    /** power is the caster's getSpellPower snapshot; Iron handles resistance and spell events once. */
    public static boolean damage(CandyEffectEntity effect, LivingEntity target, AbstractSpell spell, float base) {
        return damageFrom(effect,target,spell,base,effect.position());
    }
    public static boolean damageFrom(CandyEffectEntity effect, LivingEntity target, AbstractSpell spell, float base,Vec3 origin) {
        if (!(effect.level() instanceof ServerLevel server) || !Float.isFinite(base) || base <= 0) return false;
        LivingEntity owner = effect.owner();
        if (owner == null || !enemy(owner, target) || !visible(server, origin, target)) return false;
        CandySpecs.Spec spec = effect.spec();
        if (!spell.getSpellResource().equals(CandyRegistry.id(spec.id()))) return false;
        if (!effect.ledger().claim(target.getUUID(), server.getGameTime(), spec.hitInterval(), spec.maxHits())) return false;
        // Iron's expects (direct effect/projectile, causing caster); kills belong to the caster.
        return DamageSources.applyDamage(target, base * effect.power(), spell.getDamageSource(effect, owner));
    }
    public static boolean slow(CandyEffectEntity effect, LivingEntity target, int ticks, int amplifier) {
        LivingEntity owner = effect.owner();
        if (!(effect.level() instanceof ServerLevel server) || owner == null || !enemy(owner, target)
                || !visible(server, effect.position(), target) || target.getType().is(CandyRegistry.CONTROL_IMMUNE)) return false;
        int duration = Math.min(effect.spec().controlTicks(), Math.max(0, ticks));
        if (duration == 0) return false;
        return target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, Math.max(0, Math.min(1, amplifier))), owner);
    }
    public static void impulse(CandyEffectEntity effect, LivingEntity target, Vec3 vector) {
        LivingEntity owner = effect.owner();
        if (!(effect.level() instanceof ServerLevel server) || owner == null || !enemy(owner, target)
                || !visible(server, effect.position(), target) || target.getType().is(CandyRegistry.CONTROL_IMMUNE)) return;
        double length = vector.length();
        if (!Double.isFinite(length) || length < 1e-6) return;
        double resist = Math.max(0, Math.min(1, target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
        target.setDeltaMovement(target.getDeltaMovement().add(vector.scale(Math.min(0.35, length)/length * (1-resist))));
        target.hasImpulse = true;
    }
}
