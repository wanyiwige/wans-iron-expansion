package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModAttributeEvents {
    @SubscribeEvent
    public static void addMagneticAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            if (!event.has(type, ModAttributes.PURE_DARKNESS_SPELL_POWER.get())) {
                event.add(type, ModAttributes.PURE_DARKNESS_SPELL_POWER.get());
            }
            if (!event.has(type, ModAttributes.PURE_DARKNESS_MAGIC_RESIST.get())) {
                event.add(type, ModAttributes.PURE_DARKNESS_MAGIC_RESIST.get());
            }
            if (!event.has(type, ModAttributes.CLOUD_SPELL_POWER.get())) {
                event.add(type, ModAttributes.CLOUD_SPELL_POWER.get());
            }
            if (!event.has(type, ModAttributes.CLOUD_MAGIC_RESIST.get())) {
                event.add(type, ModAttributes.CLOUD_MAGIC_RESIST.get());
            }
            if (!event.has(type, ModAttributes.MAGNETIC_SPELL_POWER.get())) {
                event.add(type, ModAttributes.MAGNETIC_SPELL_POWER.get());
            }
            if (!event.has(type, ModAttributes.MAGNETIC_MAGIC_RESIST.get())) {
                event.add(type, ModAttributes.MAGNETIC_MAGIC_RESIST.get());
            }
        });
    }

    private ModAttributeEvents() {
    }
}
