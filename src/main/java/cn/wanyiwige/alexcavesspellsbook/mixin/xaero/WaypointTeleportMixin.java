package cn.wanyiwige.alexcavesspellsbook.mixin.xaero;

import cn.wanyiwige.alexcavesspellsbook.traveller.client.MapTravelClient;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.waypoint.WaypointTeleport", remap = false)
public abstract class WaypointTeleportMixin {
    @Inject(method = "canTeleport", at = @At("HEAD"), cancellable = true, remap = false)
    private void wan$canTeleport(boolean teleportable, @Coerce Object world, CallbackInfoReturnable<Boolean> cir) {
        if (MapTravelClient.active()) cir.setReturnValue(MapTravelClient.waypointAllowed(this, world));
    }

    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void wan$waypointTeleport(@Coerce Object waypoint, @Coerce Object world, Screen screen,
                                      boolean respectHiddenCoords, CallbackInfo ci) {
        if (MapTravelClient.active()) {
            ci.cancel();
            MapTravelClient.waypointTeleport(this, waypoint, world);
        }
    }
}
