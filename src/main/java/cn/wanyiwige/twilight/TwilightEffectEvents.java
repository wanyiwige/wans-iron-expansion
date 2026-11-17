package cn.wanyiwige.twilight;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

final class TwilightEffectEvents {
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);

    static void reduceHealing(LivingHealEvent event) {
        if (event.getEntity().hasEffect(TwilightFoundation.OMINOUS_BURN.get())) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }

    static void reflectDamage(LivingHurtEvent event) {
        if (REFLECTING.get()) return;
        if (!(event.getEntity() instanceof Player player)
                || !player.hasEffect(TwilightFoundation.HYDRA_REVENGE.get())
                || !(event.getSource().getDirectEntity() instanceof LivingEntity attacker)
                || attacker == player) return;
        try {
            REFLECTING.set(true);
            attacker.hurt(new net.minecraft.world.damagesource.DamageSource(io.redspace.ironsspellbooks.damage.DamageSources.getHolderFromResource(attacker,io.redspace.ironsspellbooks.damage.ISSDamageTypes.FIRE_MAGIC),player,player), event.getAmount() * 0.25F);
        } finally {
            REFLECTING.set(false);
        }
    }

    static MobEffectInstance addHydraFireStack(LivingEntity target) {
        MobEffectInstance previous = target.getEffect(TwilightFoundation.HYDRA_FIRE.get());
        int nextLevel = previous == null ? 0 : previous.getAmplifier() + 1;
        MobEffectInstance effect = new MobEffectInstance(TwilightFoundation.HYDRA_FIRE.get(), 20 * 15, nextLevel);
        target.addEffect(effect);
        return effect;
    }

    private TwilightEffectEvents() {}
}
