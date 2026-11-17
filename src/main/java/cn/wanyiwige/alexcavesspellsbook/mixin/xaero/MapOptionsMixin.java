package cn.wanyiwige.alexcavesspellsbook.mixin.xaero;

import cn.wanyiwige.alexcavesspellsbook.traveller.client.MapTravelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class MapOptionsMixin {
    // Xaero World Map 1.47.0: coordinates, waypoints, teleportAllowed are the three boolean locals.
    @ModifyVariable(method = "getRightClickOptions", at = @At("STORE"), ordinal = 2, remap = false)
    private boolean wan$allowSpellTeleport(boolean original) { return original || MapTravelClient.active(); }
}
