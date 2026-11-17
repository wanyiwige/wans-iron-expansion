package cn.wanyiwige.alexcavesspellsbook.mixin.roacw;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve another creative-flight source when ROACW removes Mars armor flight. */
@Pseudo
@Mixin(targets = "net.kamaarion.roacw.items.armor.marsarmorset.MarsArmorFlightHandler", remap = false)
public abstract class MarsArmorFlightHandlerMixin {
    private static final ResourceLocation CREATIVE_FLIGHT =
            ResourceLocation.fromNamespaceAndPath("attributeslib", "creative_flight");
    private static final ThreadLocal<Boolean> WAS_FLYING = new ThreadLocal<>();

    @Inject(method = "onPlayerTick", at = @At("HEAD"), remap = false)
    private static void alexCavesSpellsbook$rememberFlight(TickEvent.PlayerTickEvent event, CallbackInfo callback) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && hasOtherFlightSource(player)) {
            WAS_FLYING.set(player.getAbilities().flying);
        } else {
            WAS_FLYING.remove();
        }
    }

    @Inject(method = "onPlayerTick", at = @At("RETURN"), remap = false)
    private static void alexCavesSpellsbook$restoreOtherFlight(TickEvent.PlayerTickEvent event, CallbackInfo callback) {
        Boolean wasFlying = WAS_FLYING.get();
        WAS_FLYING.remove();
        if (wasFlying == null || !(event.player instanceof ServerPlayer player)
                || !hasOtherFlightSource(player)) {
            return;
        }
        if (!player.getAbilities().mayfly || (wasFlying && !player.getAbilities().flying)) {
            player.getAbilities().mayfly = true;
            if (wasFlying) {
                player.getAbilities().flying = true;
            }
            player.onUpdateAbilities();
        }
    }

    private static boolean hasOtherFlightSource(ServerPlayer player) {
        Attribute flight = ForgeRegistries.ATTRIBUTES.getValue(CREATIVE_FLIGHT);
        AttributeInstance instance = flight == null ? null : player.getAttribute(flight);
        return instance != null && instance.getValue() > 0.0D;
    }
}
