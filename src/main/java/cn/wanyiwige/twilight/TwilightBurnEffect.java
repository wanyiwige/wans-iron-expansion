package cn.wanyiwige.twilight;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

final class TwilightBurnEffect extends io.redspace.ironsspellbooks.effect.MagicMobEffect implements io.redspace.ironsspellbooks.effect.ISyncedMobEffect {
    private final float damage;

    TwilightBurnEffect(MobEffectCategory category, int color, float damage) {
        super(category, color);
        this.damage = damage;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            var type=damage==1F?TwilightFoundation.EXANIMATED_DAMAGE:io.redspace.ironsspellbooks.damage.ISSDamageTypes.POISON_CLOUD;
            entity.hurt(new net.minecraft.world.damagesource.DamageSource(io.redspace.ironsspellbooks.damage.DamageSources.getHolderFromResource(entity,type)), damage);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = 40 >> Math.min(amplifier, 5);
        return interval <= 0 || duration % interval == 0;
    }
    @Override public void clientTick(LivingEntity entity,net.minecraft.world.effect.MobEffectInstance instance){
        var particle=damage==1F?TwilightParticles.EXANIMATED_FIRE.get():TwilightParticles.HYDRA_FIRE.get();
        for(int i=0;i<2;i++)entity.level().addParticle(particle,entity.getRandomX(.4),entity.getRandomY(),entity.getRandomZ(.4),0,.03,0);
    }
}
