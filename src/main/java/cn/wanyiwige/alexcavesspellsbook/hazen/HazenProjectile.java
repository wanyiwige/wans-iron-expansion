package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Shared native collision/network code, with each original projectile's own behavior. */
public final class HazenProjectile extends AbstractMagicProjectile implements GeoEntity {
    public final String kind;
    public int delay;
    private int waited, hits, bounces;
    private final Map<UUID, Integer> victims = new HashMap<>();
    private LivingEntity nextTarget;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public HazenProjectile(EntityType<? extends Projectile> type, Level level, String kind) {
        super(type, level);
        this.kind = kind;
        setNoGravity(!kind.equals("vampire_knife"));
        if (kind.equals("thorn_chakram") || kind.equals("nights_edge_after_slash")) setInfinitePiercing();
    }
    public String spellKind() {
        return switch (kind) { case "nights_edge_after_slash" -> "nights_edge_strike";
            case "terraprisma" -> "terraprismic_barrage"; default -> kind; };
    }
    @Override public float getSpeed() {
        return switch (kind) { case "thorn_chakram" -> 1.2f; case "spectral_axe" -> 1.4f;
            case "nights_edge_after_slash" -> 1.6f; case "vampire_knife" -> 1.5f; default -> 2.5f; };
    }
    @Override public void tick() {
        if (!level().isClientSide && (!HazenConfig.ENABLED.get()
                || (!kind.equals("vampire_knife") && !HazenConfig.spellEnabled(spellKind())))) { discard(); return; }
        if (waited++ < delay) return;
        if (!level().isClientSide && nextTarget != null && nextTarget.isAlive())
            setDeltaMovement(nextTarget.getBoundingBox().getCenter().subtract(position()).normalize().scale(getDeltaMovement().length()));
        super.tick();
        if (kind.equals("nights_edge_after_slash") && tickCount >= 15) discard();
    }
    @Override protected boolean canHitEntity(Entity target) {
        if (target == getOwner() || getOwner() != null && getOwner().isAlliedTo(target)) return false;
        return super.canHitEntity(target) && (!victims.containsKey(target.getUUID()) || tickCount - victims.get(target.getUUID()) >= 10);
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide || !(getOwner() instanceof LivingEntity owner)) return;
        var target = result.getEntity();
        if (kind.equals("vampire_knife")) {
            // Heal only when real damage was accepted; invulnerable/allied targets are not a source of healing.
            if (target.hurt(level().damageSources().indirectMagic(this, owner), getDamage())) owner.heal(getDamage()*.075f);
            HazenSounds.play(level(), position(), "vampire_knife_hit", 2, 1);
            discard(); return;
        }
        if (kind.equals("thorn_chakram") && target instanceof LivingEntity living) DamageSources.ignoreNextKnockback(living);
        DamageSources.applyDamage(target, getDamage(), HazenSpells.get(spellKind()).getDamageSource(this, owner));
        impactParticles(getX(), getY(), getZ());
        victims.put(target.getUUID(), tickCount);
        if (kind.equals("thorn_chakram")) {
            if (++hits >= 3) { discard(); return; }
            nextTarget = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8),
                    e -> e != target && e.isAlive() && canHitEntity(e)).stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        } else if (!kind.equals("nights_edge_after_slash")) discard();
    }
    @Override protected void onHitBlock(BlockHitResult hit) {
        if (level().isClientSide) return;
        impactParticles(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        if (kind.equals("thorn_chakram")) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(switch (hit.getDirection().getAxis()) {
                case X -> motion.multiply(-1, 1, 1); case Y -> motion.multiply(1, -1, 1); case Z -> motion.multiply(1, 1, -1);
            });
            setPos(hit.getLocation().add(hit.getDirection().getStepX()*.03, hit.getDirection().getStepY()*.03, hit.getDirection().getStepZ()*.03));
            if (++bounces >= 6) discard();
        } else discard();
    }
    @Override public void trailParticles() {
        if (kind.equals("shooting_star")) {
            for (int i = 0; i < 2; i++) {
                double angle = (tickCount + i*.5)/7.5 * Math.PI*2;
                Vec3 swirl = new Vec3(Math.cos(angle)*.25, Math.sin(angle)*.25, 0)
                        .yRot((float)-Math.atan2(getDeltaMovement().z, getDeltaMovement().x));
                Vec3 p = position().subtract(getDeltaMovement().scale(i*.5)).add(swirl).add(0, getBbHeight()/2, 0);
                level().addParticle(HazenParticles.get("cosmic_ember_particle"), p.x, p.y, p.z, 0, 0, 0);
            }
        } else if (kind.equals("terraprisma")) {
            level().addParticle(ParticleHelper.UNSTABLE_ENDER, getX(), getY(), getZ(), 0, 0, 0);
        } else if (kind.equals("vampire_knife")) {
            level().addParticle(ParticleHelper.BLOOD, getX(), getY(), getZ(), 0, 0, 0);
        }
    }
    @Override public void impactParticles(double x, double y, double z) {
        String particle = switch (kind) { case "thorn_chakram" -> "leaf_particle";
            case "shooting_star" -> "star_impact_particle"; case "nights_edge_after_slash" -> "nights_edge_particle"; default -> ""; };
        if (!particle.isEmpty()) HazenParticles.spawn(level(), particle, new Vec3(x,y,z), kind.equals("shooting_star") ? 25 : 12, .08,.08,.08,.3);
        else if (kind.equals("terraprisma")) MagicManager.spawnParticles(level(), ParticleHelper.UNSTABLE_ENDER,x,y,z,25,0,0,0,.18,true);
        else if (kind.equals("vampire_knife")) MagicManager.spawnParticles(level(), ParticleHelper.BLOOD,x,y,z,5,.1,.1,.1,.25,true);
    }
    @Override public Optional<Supplier<SoundEvent>> getImpactSound() {
        if (kind.equals("nights_edge_after_slash")) return Optional.empty();
        return Optional.of(() -> HazenSounds.get(kind.equals("spectral_axe") ? "electric_impact" :
                kind.equals("vampire_knife") ? "syringe_barrage_hit" : "terraria_impact"));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String animation = switch (kind) { case "thorn_chakram" -> "animation.thorn_chakram_spell.idle";
            case "shooting_star" -> "animation.shooting_star.idle"; case "spectral_axe" -> "animation.spectral_axe.idle";
            case "nights_edge_after_slash" -> "animation.nights_edge_after_slash.idle";
            case "terraprisma" -> "animation.terraprisma.idle_alt"; default -> "animation.vampire_knife_projectile.idle"; };
        controllers.add(new AnimationController<>(this,"hazen",0, event -> {
            event.getController().setAnimation(RawAnimation.begin().thenLoop(animation)); return PlayState.CONTINUE;
        }));
    }
}
