package cn.wanyiwige.astralcompat.mixin;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.TextColor;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Iron's API assumes every registered school styles its display name with a colour.
 * A number of addon schools do not, which made Apprentice Codex potion rendering
 * crash while querying the school colour.  Keep normal coloured schools untouched
 * and give unstyled schools a neutral arcane tint instead.
 */
@Mixin(value = SchoolType.class, remap = false)
public abstract class SchoolTypeColorMixin {
    private static final Vector3f FALLBACK_TARGETING_COLOR = new Vector3f(0.62f, 0.45f, 0.82f);

    @Inject(method = "getTargetingColor", at = @At("HEAD"), cancellable = true, remap = false)
    private void astralCompat$provideFallbackColor(CallbackInfoReturnable<Vector3f> callback) {
        SchoolType school = (SchoolType) (Object) this;
        TextColor color = school.getDisplayName().getStyle().getColor();
        if (color == null) {
            callback.setReturnValue(new Vector3f(FALLBACK_TARGETING_COLOR));
        }
    }
}
