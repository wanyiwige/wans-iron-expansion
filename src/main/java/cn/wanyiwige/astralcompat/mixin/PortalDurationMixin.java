package cn.wanyiwige.astralcompat.mixin;

import io.redspace.ironsspellbooks.spells.ender.PortalSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PortalSpell normally derives a portal's lifetime directly from spell power.
 * The spell's high base power makes this several minutes even with no gear, so
 * Traveller portals have a fixed, predictable 30-second lifetime instead.
 */
@Mixin(value = PortalSpell.class, remap = false)
public abstract class PortalDurationMixin {
    private static final int TRAVELLER_PORTAL_DURATION_TICKS = 20 * 30;

    @Inject(method = "getPortalDuration", at = @At("RETURN"), cancellable = true, remap = false)
    private void astralCompat$setFixedPortalDuration(int spellLevel, LivingEntity caster,
            CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(TRAVELLER_PORTAL_DURATION_TICKS);
    }
}
