package cn.wanyiwige.alexcavesspellsbook.mixin.xaero;

import cn.wanyiwige.alexcavesspellsbook.traveller.client.MapTravelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "xaero.common.gui.GuiWaypoints", remap = false)
public abstract class WaypointMenuMixin {
    @Inject(method = "canTeleport()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void wan$allowWaypointMenu(CallbackInfoReturnable<Boolean> cir) {
        if (MapTravelClient.active()) cir.setReturnValue(MapTravelClient.waypointMenuAllowed(this));
    }
}
