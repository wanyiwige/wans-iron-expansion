package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyRegistry;
import cn.wanyiwige.alexcavesspellsbook.item.MagneticPolarityScrollItem;
import cn.wanyiwige.alexcavesspellsbook.item.CandySpellScrollItem;
import cn.wanyiwige.alexcavesspellsbook.item.MagneticMageArmorItem;
import cn.wanyiwige.alexcavesspellsbook.item.MagneticSpellbookItem;
import cn.wanyiwige.alexcavesspellsbook.item.DarkSparkPrismItem;
import cn.wanyiwige.alexcavesspellsbook.item.LandOfTheLustrousItem;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.UpgradeOrbItem;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AlexCavesSpellsbook.MOD_ID);

    public static final RegistryObject<Item> MAGNETIC_MAGE_SPAWN_EGG = ITEMS.register(
            "magnetic_mage_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.MAGNETIC_MAGE, 0x3B424B, 0xB64B51, new Item.Properties()));

    public static final RegistryObject<Item> MAGNETIC_POLARITY_SPELL_SCROLL = ITEMS.register("magnetic_polarity_spell_scroll", () -> new MagneticPolarityScrollItem(new Item.Properties()));
    public static final RegistryObject<Item> CANDY_SPELL_SCROLL = ITEMS.register("candy_spell_scroll", () -> new CandySpellScrollItem(new Item.Properties()));
    public static final RegistryObject<Item> CANDY_RUNE = register("candy_rune");
    public static final RegistryObject<Item> CANDY_STARCATCHER = ITEMS.register("candy_starcatcher",
            cn.wanyiwige.alexcavesspellsbook.candy.CandyStarcatcherItem::new);
    public static final RegistryObject<Item> CANDY_WIZARD_HAT = ITEMS.register("candy_wizard_hat",()->new cn.wanyiwige.alexcavesspellsbook.candy.CandyWizardArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> CANDY_WIZARD_ROBE = ITEMS.register("candy_wizard_robe",()->new cn.wanyiwige.alexcavesspellsbook.candy.CandyWizardArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> CANDY_WIZARD_LEGGINGS = ITEMS.register("candy_wizard_leggings",()->new cn.wanyiwige.alexcavesspellsbook.candy.CandyWizardArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> CANDY_WIZARD_BOOTS = ITEMS.register("candy_wizard_boots",()->new cn.wanyiwige.alexcavesspellsbook.candy.CandyWizardArmorItem(ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> CANDY_UPGRADE_ORB = ITEMS.register("candy_upgrade_orb",
            () -> new UpgradeOrbItem(new Item.Properties(), ModUpgradeOrbTypes.CANDY_POWER));
    public static final RegistryObject<Item> PURE_DARKNESS_SPELL_SCROLL = ITEMS.register(
            "pure_darkness_spell_scroll", () -> new Scroll(new Item.Properties()));
    public static final RegistryObject<Item> PURE_DARKNESS_RUNE = register("pure_darkness_rune");
    public static final RegistryObject<Item> PURE_DARKNESS_UPGRADE_ORB = ITEMS.register(
            "pure_darkness_upgrade_orb",
            () -> new UpgradeOrbItem(new Item.Properties(), ModUpgradeOrbTypes.PURE_DARKNESS_POWER));

    public static final RegistryObject<Item> MAGNETIC_POLARITY_RUNE = register("magnetic_polarity_rune");
    public static final RegistryObject<Item> MAGNETIC_POLARITY_UPGRADE_ORB = ITEMS.register(
            "magnetic_polarity_upgrade_orb",
            () -> new UpgradeOrbItem(new Item.Properties(), ModUpgradeOrbTypes.MAGNETIC_POWER));
    public static final RegistryObject<Item> MAGNET_SPELLBOOK = ITEMS.register(
            "magnet_spellbook",
            () -> new MagneticSpellbookItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DARK_SPARK_PRISM = ITEMS.register(
            "dark_spark_prism",
            () -> new DarkSparkPrismItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NEBULOUS_CATACLYSM_WAND = ITEMS.register(
            "nebulous_cataclysm_wand", () -> new cn.wanyiwige.alexcavesspellsbook.item.NebulousCataclysmWandItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> LAND_OF_THE_LUSTROUS = ITEMS.register(
            "land_of_the_lustrous",
            () -> new LandOfTheLustrousItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MAGNETIC_MAGE_HELMET = ITEMS.register(
            "magnetic_mage_helmet", () -> new MagneticMageArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> MAGNETIC_MAGE_CHESTPLATE = ITEMS.register(
            "magnetic_mage_chestplate", () -> new MagneticMageArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> MAGNETIC_MAGE_LEGGINGS = ITEMS.register(
            "magnetic_mage_leggings", () -> new MagneticMageArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> MAGNETIC_MAGE_BOOTS = ITEMS.register(
            "magnetic_mage_boots", () -> new MagneticMageArmorItem(ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> BIPOLAR_NEODYMIUM_INGOT = register("bipolar_neodymium_ingot");
    public static final RegistryObject<Item> VERDANT_INGOT = register("verdant_ingot");
    public static final RegistryObject<Item> EMERALD_CRYSTAL_INGOT = register("emerald_crystal_ingot");
    public static final RegistryObject<Item> MAGIC_ICE_CRYSTAL_INGOT = register("magic_ice_crystal_ingot");
    public static final RegistryObject<Item> MIRACLE_MATTER = register("miracle_matter");
    public static final RegistryObject<Item> LIFE_ALLOY = register("life_alloy");
    public static final RegistryObject<Item> SKYBLUE_INGOT = register("skyblue_ingot");
    public static final RegistryObject<Item> CONSTANT_ENERGY = register("constant_energy");
    public static final RegistryObject<Item> DIVINE_SOUL_ESSENCE = register("divine_soul_essence");
    public static final RegistryObject<Item> DESTRUCTION_SPIRIT = register("destruction_spirit");
    public static final RegistryObject<Item> NIGHTMARE_MANA = register("nightmare_mana");
    public static final RegistryObject<Item> SHADOW_INGOT = register("shadow_ingot");
    public static final RegistryObject<Item> ECLIPSE_SHARD = register("eclipse_shard");
    public static final RegistryObject<Item> SLAG_INGOT = register("slag_ingot");
    public static final RegistryObject<Item> SACRED_GOLD_INGOT = register("sacred_gold_ingot");
    public static final RegistryObject<Item> STELLAR_PRISM = register("stellar_prism");
    public static final RegistryObject<Item> RADIANT_STAR_INGOT = register("radiant_star_ingot");
    public static final RegistryObject<Item> BLOOD_BEAD = register("blood_bead");
    public static final RegistryObject<Item> ANNIHILATION_EMBER = register("annihilation_ember");
    public static final RegistryObject<Item> COSMIC_INGOT = register("cosmic_ingot");

    public static Item scrollForSchool(SchoolType school) {
        if (school == ModSchools.PURE_DARKNESS.get()) return PURE_DARKNESS_SPELL_SCROLL.get();
        if (school == ModSchools.MAGNETIC.get()) return MAGNETIC_POLARITY_SPELL_SCROLL.get();
        if (school == CandyRegistry.CANDY.get()) return CANDY_SPELL_SCROLL.get();
        return ItemRegistry.SCROLL.get();
    }

    private ModItems() { }

    private static RegistryObject<Item> register(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }
}
