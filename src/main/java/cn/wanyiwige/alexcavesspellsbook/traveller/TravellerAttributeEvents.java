package cn.wanyiwige.alexcavesspellsbook.traveller;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TravellerAttributeEvents {
    @SubscribeEvent
    public static void addTravellerAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            if (!event.has(type, TravellerAttributes.SPELL_POWER.get())) {
                event.add(type, TravellerAttributes.SPELL_POWER.get());
            }
            if (!event.has(type, TravellerAttributes.SPELL_RESISTANCE.get())) {
                event.add(type, TravellerAttributes.SPELL_RESISTANCE.get());
            }
        });
    }

    private TravellerAttributeEvents() {
    }
}
