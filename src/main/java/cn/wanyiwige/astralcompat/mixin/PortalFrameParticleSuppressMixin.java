package cn.wanyiwige.astralcompat.mixin;

import io.redspace.ironsspellbooks.entity.spells.portal.PortalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PortalEntity's client tick only emits the vanilla purple PORTAL_FRAME ring.
 * A single-player client also hosts an integrated server, so the logical-side
 * guard is required: cancelling the complete tick there would stop portal
 * detection and teleportation on that server.
 */
@Mixin(value = PortalEntity.class, remap = false)
public abstract class PortalFrameParticleSuppressMixin {
    private static final long MAX_PORTAL_LIFETIME_TICKS = 20L * 30L;

    @Shadow(remap = false)
    private long ticksToLive;

    @Inject(method = "m_8119_", at = @At("HEAD"), cancellable = true, remap = false)
    private void astralCompat$suppressVanillaPortalFrame(CallbackInfo callback) {
        if (((PortalEntity) (Object) this).level().isClientSide) {
            callback.cancel();
            return;
        }

        // Existing portal entities persist their old ticksToLive value in the
        // world save. Cap that value on the server after upgrading as well.
        if (ticksToLive > MAX_PORTAL_LIFETIME_TICKS) {
            ticksToLive = MAX_PORTAL_LIFETIME_TICKS;
        }
    }
}
