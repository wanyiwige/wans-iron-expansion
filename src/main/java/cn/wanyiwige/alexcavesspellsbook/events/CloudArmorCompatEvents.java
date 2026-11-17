package cn.wanyiwige.alexcavesspellsbook.events;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.registry.ModAttributes;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

/** Adds cloud power alongside the Legendary Spellbooks armor's existing lightning power. */
@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID)
public final class CloudArmorCompatEvents {
    private CloudArmorCompatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addCloudSpellPower(ItemAttributeModifierEvent event) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (itemId == null || !"legendary_spellbooks".equals(itemId.getNamespace())) {
            return;
        }
        EquipmentSlot expectedSlot = switch (itemId.getPath()) {
            case "stormmancer_hood" -> EquipmentSlot.HEAD;
            case "stormmancer_robe" -> EquipmentSlot.CHEST;
            case "stormmancer_leggings" -> EquipmentSlot.LEGS;
            case "stormmancer_boots" -> EquipmentSlot.FEET;
            default -> null;
        };
        if (expectedSlot == null || event.getSlotType() != expectedSlot) {
            return;
        }

        // Mirror the effective lightning amount and operation, including pack adjustments.
        // Snapshot before adding entries to the same multimap.
        var lightningModifiers = new ArrayList<>(
                event.getModifiers().get(AttributeRegistry.LIGHTNING_SPELL_POWER.get()));
        for (AttributeModifier lightning : lightningModifiers) {
            UUID cloudId = UUID.nameUUIDFromBytes((AlexCavesSpellsbook.MOD_ID
                    + ":cloud_armor:" + expectedSlot.getName() + ":" + lightning.getId())
                    .getBytes(StandardCharsets.UTF_8));
            boolean alreadyPresent = event.getModifiers().get(ModAttributes.CLOUD_SPELL_POWER.get())
                    .stream().anyMatch(modifier -> modifier.getId().equals(cloudId));
            if (!alreadyPresent) {
                event.addModifier(ModAttributes.CLOUD_SPELL_POWER.get(),
                        new AttributeModifier(cloudId, "Stormmancer cloud spell power",
                                lightning.getAmount(), lightning.getOperation()));
            }
        }
    }
}
