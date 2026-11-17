package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Supplier;

public final class AuroraMissileProjectile extends AbstractMagicProjectile {
    private final java.util.ArrayDeque<net.minecraft.world.phys.Vec3> ribbon=new java.util.ArrayDeque<>();
    public java.util.List<net.minecraft.world.phys.Vec3> ribbon(){return java.util.List.copyOf(ribbon);}
    @Override public void tick(){super.tick();if(level().isClientSide){ribbon.addFirst(position());while(ribbon.size()>24)ribbon.removeLast();}}
    public AuroraMissileProjectile(EntityType<? extends AuroraMissileProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }
    public AuroraMissileProjectile(Level level, LivingEntity caster) {
        this(TwilightEntities.AURORA_MISSILE.get(), level);
        setOwner(caster);
    }
    @Override public float getSpeed() { return 2.5F; }
    @Override public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.of(() -> io.redspace.ironsspellbooks.registries.SoundRegistry.ARCANE_IMPACT.get());
    }
    @Override protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        discard();
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            if (DamageSources.applyDamage(result.getEntity(), getDamage(),
                    TwilightSpells.AURORA_MISSILE.get().getDamageSource(this, getOwner()))
                    && result.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT.get(), 300));
            }
            consumeEntityImpact(result, true);
        }
    }
    @Override public void trailParticles() {
        float phase = (tickCount % 8) / 8F;
        level().addParticle(new DustParticleOptions(new Vector3f(.26F + phase * .45F, .55F, .92F), .7F),
                getX(), getY(), getZ(), 0, 0, 0);
    }
    @Override public void impactParticles(double x, double y, double z) {
        for (int i = 0; i < 15; i++) level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z,
                random.nextGaussian() * .18, random.nextGaussian() * .18, random.nextGaussian() * .18);
    }
}
