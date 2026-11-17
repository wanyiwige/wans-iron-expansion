package cn.wanyiwige.astralcompat.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AstralAttributeEvents {
    @SubscribeEvent
    public static void addAstralAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            if (!event.has(type, AstralAttributes.SPELL_POWER.get())) {
                event.add(type, AstralAttributes.SPELL_POWER.get());
            }
            if (!event.has(type, AstralAttributes.MAGIC_RESISTANCE.get())) {
                event.add(type, AstralAttributes.MAGIC_RESISTANCE.get());
            }
        });
    }

    private AstralAttributeEvents() {
    }
}
