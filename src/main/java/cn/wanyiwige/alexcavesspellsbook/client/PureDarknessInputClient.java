package cn.wanyiwige.alexcavesspellsbook.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFinaleEntity;
import cn.wanyiwige.alexcavesspellsbook.network.PureDarknessInput;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.KeyMappings;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

import static cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFinaleEntity.*;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, value = Dist.CLIENT)
public final class PureDarknessInputClient {
    private static LocalPlayer trackedPlayer;
    private static PureDarknessFinaleEntity trackedCast;
    private static KeyMapping binding;
    private static long heartbeat;

    private PureDarknessInputClient() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player != trackedPlayer || player == null) {
            trackedPlayer = player;
            trackedCast = null;
            binding = null;
        }
        if (player == null) return;
        if (trackedCast == null || trackedCast.isRemoved() || trackedCast.ending() >= 0) {
            if (trackedCast == null && !ClientMagicData.isCasting() && player.tickCount % 4 != 0) return;
            trackedCast = player.level().getEntitiesOfClass(PureDarknessFinaleEntity.class, player.getBoundingBox().inflate(19),
                    field -> field.ready() && field.owner() == player && field.ending() < 0 && !field.arrow()
                            && field.kind().held()).stream()
                    .min(Comparator.comparingDouble(field -> field.age(0))).orElse(null);
            binding = null;
            if (trackedCast != null) {
                binding = findBinding(client, trackedCast.source());
                if (binding == null || client.screen != null || !client.isWindowActive()) {
                    PureDarknessInput.send(trackedCast, CANCEL);
                    binding = null;
                } else {
                    PureDarknessInput.send(trackedCast, HOLD);
                    heartbeat = player.level().getGameTime();
                }
            }
            return;
        }
        if (binding == null) return;
        if (client.screen != null || !client.isWindowActive() || !player.isAlive()) {
            PureDarknessInput.send(trackedCast, CANCEL);
            binding = null;
        } else if (!binding.isDown()) {
            PureDarknessInput.send(trackedCast, RELEASE);
            binding = null;
        } else if (player.level().getGameTime() - heartbeat >= 5) {
            PureDarknessInput.send(trackedCast, HOLD);
            heartbeat = player.level().getGameTime();
        }
    }

    private static KeyMapping findBinding(Minecraft client, CastSource source) {
        if (source == CastSource.SCROLL || source == CastSource.SWORD) {
            return client.options.keyUse.isDown() ? client.options.keyUse : null;
        }
        if (source != CastSource.SPELLBOOK) return null;
        if (KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP.isDown()) return KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP;
        for (KeyMapping quick : KeyMappings.QUICK_CAST_MAPPINGS) {
            if (quick.isDown()) return quick;
        }
        return client.options.keyUse.isDown() ? client.options.keyUse : null;
    }
}

