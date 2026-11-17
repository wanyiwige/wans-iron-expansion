package cn.wanyiwige.alexcavesspellsbook.hazen;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HazenEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "alex_caves_spellsbook");
    public static final Map<String, RegistryObject<EntityType<HazenProjectile>>> BOLTS = new LinkedHashMap<>();
    static {
        for (String kind : new String[]{"thorn_chakram", "shooting_star", "spectral_axe", "nights_edge_after_slash", "terraprisma", "vampire_knife"}) {
            BOLTS.put(kind, TYPES.register("hazen_" + kind, () -> EntityType.Builder.<HazenProjectile>of(
                    (type, level) -> new HazenProjectile(type, level, kind), MobCategory.MISC)
                    .sized(kind.equals("nights_edge_after_slash") ? 4 : 1, 1)
                    .clientTrackingRange(64).updateInterval(1).build(HazenEquipment.id("hazen_" + kind).toString())));
        }
    }
    public static final RegistryObject<EntityType<HazenSummonedTerraprisma>> SUMMON = TYPES.register("hazen_summoned_terraprisma", () ->
            EntityType.Builder.of(HazenSummonedTerraprisma::new, MobCategory.CREATURE).sized(.6f, 1.8f).fireImmune()
                    .clientTrackingRange(64).updateInterval(2).build(HazenEquipment.id("hazen_summoned_terraprisma").toString()));
    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(SUMMON.get(), HazenSummonedTerraprisma.attributes().build()));
    }
    private HazenEntities() {}
}
