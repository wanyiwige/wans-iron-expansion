package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import twilightforest.init.TFSounds;

import java.util.Optional;
import java.util.function.Supplier;

public final class TwilightBoltProjectile extends AbstractMagicProjectile {
    public TwilightBoltProjectile(EntityType<? extends TwilightBoltProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public TwilightBoltProjectile(Level level, LivingEntity owner) {
        this(TwilightEntities.TWILIGHT_BOLT.get(), level);
        setOwner(owner);
    }

    @Override public float getSpeed() { return 1.75F; }
    @Override public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.of(() -> TFSounds.SCEPTER_PEARL.get());
    }
    @Override protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        discard();
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (owner instanceof LivingEntity living && living.isAlliedTo(target)) return;
        super.onHitEntity(result);
        if (!level().isClientSide) {
            target.hurt(new net.minecraft.world.damagesource.DamageSource(DamageSources.getHolderFromResource(target,twilightforest.init.TFDamageTypes.TWILIGHT_SCEPTER),this,owner),getDamage());
            consumeEntityImpact(result, true);
        }
    }
    @Override public void trailParticles() {
        for (int i = 0; i < 5; i++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + .25 * (random.nextDouble() - random.nextDouble()),
                    getY() + .25 * (random.nextDouble() - random.nextDouble()),
                    getZ() + .25 * (random.nextDouble() - random.nextDouble()), 0, 0, 0);
        }
    }
    @Override public void impactParticles(double x, double y, double z) {
        level().addParticle(ParticleTypes.DRAGON_BREATH, x, y, z, 0, .05, 0);
    }
}
