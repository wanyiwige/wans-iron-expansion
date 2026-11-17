package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.item.DarkSparkPrismItem;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyRegistry;
import cn.wanyiwige.alexcavesspellsbook.traveller.TravellerItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final ResourceLocation EXO_SCHOOL = ResourceLocation.fromNamespaceAndPath("roacw", "exo");
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AlexCavesSpellsbook.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ALEX_CAVES_SPELLSBOOK = TABS.register("alex_caves_spellsbook", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.alex_caves_spellsbook.alex_caves_spellsbook"))
            .icon(() -> ModItems.MAGNETIC_POLARITY_SPELL_SCROLL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                ModSpells.SPELLS.getEntries().stream()
                        .map(RegistryObject::get)
                        .filter(AbstractSpell::isEnabled)
                        .filter(spell -> !(spell instanceof cn.wanyiwige.alexcavesspellsbook.spells.SaciliteSpell))
                        // Iron's scroll tab already lists every Exo spell with its school model.
                        .filter(spell -> !EXO_SCHOOL.equals(spell.getSchoolType().getId()))
                        .forEach(spell -> addAllLevels(output, spell));
                CandyRegistry.ENTRIES.values().stream().map(RegistryObject::get).filter(AbstractSpell::isEnabled)
                        .forEach(spell -> addAllLevels(output, spell));
                cn.wanyiwige.alexcavesspellsbook.toxic.ToxicRegistry.ENTRIES.values().stream()
                        .map(RegistryObject::get).filter(AbstractSpell::isEnabled).forEach(spell -> addAllLevels(output, spell));
                addExternalSpell(output, "em_pulse");
                addExternalSpell(output, "magnetron_deployment");
                addExternalSpell(output, "galena_shatter");
                output.accept(ModItems.MAGNETIC_POLARITY_RUNE.get());
                output.accept(ModItems.MAGNETIC_POLARITY_UPGRADE_ORB.get());
                output.accept(ModItems.PURE_DARKNESS_RUNE.get());
                output.accept(ModItems.PURE_DARKNESS_UPGRADE_ORB.get());
                output.accept(ModItems.CANDY_RUNE.get());
                output.accept(ModItems.CANDY_UPGRADE_ORB.get());
                output.accept(ModItems.CANDY_STARCATCHER.get().getDefaultInstance());
                cn.wanyiwige.alexcavesspellsbook.toxic.RadiationEquipment.addCreative(output);
                output.accept(ModItems.CANDY_WIZARD_HAT.get());
                output.accept(ModItems.CANDY_WIZARD_ROBE.get().getDefaultInstance());
                output.accept(ModItems.CANDY_WIZARD_LEGGINGS.get());
                output.accept(ModItems.CANDY_WIZARD_BOOTS.get());
                output.accept(ModItems.BIPOLAR_NEODYMIUM_INGOT.get());
                output.accept(ModItems.MAGNET_SPELLBOOK.get());
                output.accept(ModItems.NEBULOUS_CATACLYSM_WAND.get().getDefaultInstance());
                output.accept(ModItems.LAND_OF_THE_LUSTROUS.get().getDefaultInstance());
                cn.wanyiwige.alexcavesspellsbook.hazen.HazenEquipment.addWeapons(output);
                output.accept(TravellerItems.TRAVELLER_CODEX.get());
                addAllLevels(output, cn.wanyiwige.alexcavesspellsbook.traveller.TravellerSpells.MAP_TRAVEL.get());
                output.accept(ModItems.MAGNETIC_MAGE_HELMET.get());
                output.accept(ModItems.MAGNETIC_MAGE_CHESTPLATE.get());
                output.accept(ModItems.MAGNETIC_MAGE_LEGGINGS.get());
                output.accept(ModItems.MAGNETIC_MAGE_BOOTS.get());
                output.accept(ModItems.MAGNETIC_MAGE_SPAWN_EGG.get());
                cn.wanyiwige.twilight.TwilightContentTab.addItems(output);
                cn.wanyiwige.twilight.TwilightContentTab.addScrolls(output);
            })
            .build());

    public static final RegistryObject<CreativeModeTab> TOXIC_SPELLS = TABS.register("toxic_spells", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.alex_caves_spellsbook.toxic_spells"))
            .icon(() -> new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("alexscaves","fissile_core"))))
            .displayItems((parameters,output) -> {
                cn.wanyiwige.alexcavesspellsbook.toxic.RadiationEquipment.addCreative(output);
                cn.wanyiwige.alexcavesspellsbook.toxic.ToxicRegistry.ENTRIES.values().stream().map(RegistryObject::get)
                        .filter(AbstractSpell::isEnabled).forEach(spell -> addAllLevels(output,spell));
                cn.wanyiwige.alexcavesspellsbook.toxic.ToxicMigrations.SPELLS.stream().sorted().forEach(value -> {
                    ResourceLocation id=ResourceLocation.parse(value);AbstractSpell spell=SpellRegistry.getSpell(id);
                    if(id.equals(spell.getSpellResource())&&spell.isEnabled())addAllLevels(output,spell);
                });
            }).build());

    public static final RegistryObject<CreativeModeTab> FOOLISH_BOSS_SPELLS = TABS.register("foolish_boss_spells", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.alex_caves_spellsbook.foolish_boss_spells"))
            .icon(() -> {
                ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(ModSpells.SACILITE_RAIN.get(), 1, scroll);
                return scroll;
            })
            .displayItems((parameters, output) -> {
                if (ModSpells.SACILITE_RAIN.get().isEnabled()) addAllLevels(output, ModSpells.SACILITE_RAIN.get());
                if (ModSpells.SEEKING_SACILITE_SWARM.get().isEnabled()) addAllLevels(output, ModSpells.SEEKING_SACILITE_SWARM.get());
                cn.wanyiwige.alexcavesspellsbook.foolish.FoolishSpells.ENTRIES.values().stream()
                        .map(RegistryObject::get).filter(AbstractSpell::isEnabled).forEach(spell -> addAllLevels(output, spell));
            }).build());

    public static final RegistryObject<CreativeModeTab> CATACLYSM_MATERIALS = TABS.register("cataclysm_materials", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.alex_caves_spellsbook.cataclysm_materials"))
            .icon(() -> ModItems.LIFE_ALLOY.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.VERDANT_INGOT.get());
                output.accept(ModItems.EMERALD_CRYSTAL_INGOT.get());
                output.accept(ModItems.MAGIC_ICE_CRYSTAL_INGOT.get());
                output.accept(ModItems.MIRACLE_MATTER.get());
                output.accept(ModItems.LIFE_ALLOY.get());
                output.accept(ModItems.SKYBLUE_INGOT.get());
                output.accept(ModItems.CONSTANT_ENERGY.get());
                output.accept(ModItems.DIVINE_SOUL_ESSENCE.get());
                output.accept(ModItems.DESTRUCTION_SPIRIT.get());
                output.accept(ModItems.NIGHTMARE_MANA.get());
                output.accept(ModItems.SHADOW_INGOT.get());
                output.accept(ModItems.ECLIPSE_SHARD.get());
                output.accept(ModItems.SLAG_INGOT.get());
                output.accept(ModItems.SACRED_GOLD_INGOT.get());
                output.accept(ModItems.STELLAR_PRISM.get());
                output.accept(ModItems.RADIANT_STAR_INGOT.get());
                output.accept(ModItems.BLOOD_BEAD.get());
                output.accept(ModItems.ANNIHILATION_EMBER.get());
                output.accept(ModItems.COSMIC_INGOT.get());
            })
            .build());

    private ModCreativeTabs() { }

    public static void addRoacwEquipment(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(ResourceLocation.fromNamespaceAndPath("roacw", "roacw_equipment"))) {
            return;
        }
        ItemStack prism = new ItemStack(ModItems.DARK_SPARK_PRISM.get());
        ((DarkSparkPrismItem) ModItems.DARK_SPARK_PRISM.get()).initializeSpellContainer(prism);
        event.accept(prism);
        event.accept(ModItems.NEBULOUS_CATACLYSM_WAND.get().getDefaultInstance());
    }

    private static void addAllLevels(CreativeModeTab.Output output, AbstractSpell spell) {
        for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
            ItemStack scroll = new ItemStack(ModItems.scrollForSchool(spell.getSchoolType()));
            ISpellContainer.createScrollContainer(spell, level, scroll);
            output.accept(scroll);
        }
    }

    private static void addExternalSpell(CreativeModeTab.Output output, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("traveloptics", path);
        AbstractSpell spell = SpellRegistry.getSpell(id);
        if (spell.getSpellResource().equals(id)) {
            addAllLevels(output, spell);
        }
    }
}
