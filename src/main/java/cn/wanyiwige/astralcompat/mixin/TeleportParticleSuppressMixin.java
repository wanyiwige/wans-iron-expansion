package cn.wanyiwige.astralcompat.mixin;

import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TeleportParticlesPacket is emitted by Iron's Teleport spell only in this
 * game version.  Suppressing its client handler removes the original purple
 * cloud without changing destination selection or the server-side teleport.
 */
@Mixin(value = ClientSpellCastHelper.class, remap = false)
public abstract class TeleportParticleSuppressMixin {
    @Inject(method = "handleClientboundTeleport", at = @At("HEAD"), cancellable = true, remap = false)
    private static void astralCompat$hideTeleportParticleCloud(Vec3 origin, Vec3 destination, CallbackInfo callback) {
        callback.cancel();
    }
}
