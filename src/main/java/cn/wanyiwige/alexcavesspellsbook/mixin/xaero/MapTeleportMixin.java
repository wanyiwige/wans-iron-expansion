package cn.wanyiwige.alexcavesspellsbook.mixin.xaero;

import cn.wanyiwige.alexcavesspellsbook.traveller.client.MapTravelClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.map.teleport.MapTeleporter", remap = false)
public abstract class MapTeleportMixin {
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true, remap = false)
    private void wan$mapTeleport(Screen screen, @Coerce Object world, int x, int y, int z,
                                 ResourceKey<Level> dimension, CallbackInfo ci) {
        if (MapTravelClient.active()) {
            ci.cancel();
            MapTravelClient.mapTeleport(world, x, y, z, dimension);
        }
    }
}
