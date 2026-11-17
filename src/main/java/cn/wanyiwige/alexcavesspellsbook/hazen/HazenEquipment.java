package cn.wanyiwige.alexcavesspellsbook.hazen;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One removable entry point for the Hazen recreation. */
public final class HazenEquipment {
    public static final List<HazenWeaponSpec> SPECS = HazenWeaponSpec.load();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AlexCavesSpellsbook.MOD_ID);
    public static final Map<String, RegistryObject<HazenWeaponItem>> WEAPONS = new LinkedHashMap<>();
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AlexCavesSpellsbook.MOD_ID);
    static {
        for (var spec : SPECS) {
            if (WEAPONS.put(spec.id, ITEMS.register("hazen_" + spec.id, () -> new HazenWeaponItem(spec))) != null)
                throw new IllegalStateException("Duplicate recreated weapon " + spec.id);
        }
        TABS.register("hazen_equipment", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.alex_caves_spellsbook.hazen_equipment"))
                .icon(() -> WEAPONS.get("hazens_excalibur").get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    addWeapons(output);
                    HazenMaterials.addToTab(output);
                    HazenSpells.addScrolls(output);
                }).build());
    }
    public static ResourceLocation id(String path) { return new ResourceLocation(AlexCavesSpellsbook.MOD_ID, path); }
    public static void register(FMLJavaModLoadingContext context, IEventBus bus) {
        context.registerConfig(ModConfig.Type.COMMON, HazenConfig.SPEC, "wans-hazen-recreation-common.toml");
        ITEMS.register(bus);
        TABS.register(bus);
        HazenAttributes.register(bus);
        HazenSchools.register(bus);
        HazenSpells.register(bus);
        HazenEntities.register(bus);
        HazenMaterials.register(bus);
        HazenSurvival.register(bus);
        HazenArmor.register(bus);
        HazenAddedArmor.register(bus);
        HazenSecondaryRegistry.register(bus);
        HazenRecipes.register(bus);
        HazenSounds.register(bus);
        HazenParticles.register(bus);
        HazenCombat.register(bus);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(HazenCombat.class);
    }
    public static void addWeapons(CreativeModeTab.Output output) {
        WEAPONS.forEach((id, item) -> { if (HazenConfig.itemEnabled(id)) output.accept(item.get().getDefaultInstance()); });
        HazenArmor.addToTab(output);
        HazenAddedArmor.addToTab(output);
        if(HazenSurvival.enabled())HazenSurvival.ORES.values().forEach(ore->output.accept(ore.get()));
        if(HazenSurvival.enabled())HazenSurvival.WOOD.values().forEach(block->output.accept(block.get()));
    }
    private HazenEquipment() {}
}
