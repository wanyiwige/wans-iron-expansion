package cn.wanyiwige.alexcavesspellsbook.hazen;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

/** Keep registry IDs stable when a world disables the optional content. */
public final class HazenConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, WEAPONS, SPELLS, RECIPES, ARMOR;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ARMOR_SETS;
    public static final ForgeConfigSpec.BooleanValue EXTRA_SPELLS;
    public static final ForgeConfigSpec.BooleanValue SURVIVAL, SURVIVAL_WORLDGEN, SURVIVAL_LOOT, SURVIVAL_PROGRESS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_SPELLS;
    static {
        var b = new ForgeConfigSpec.Builder();
        b.comment("Original Hazen content recreated for Forge 1.20.1. Registry IDs stay present when disabled.",
                "Restart the game after changing these switches. No original Hazen libraries are required.").push("hazen");
        ENABLED = b.define("enabled", true);
        WEAPONS = b.define("weapons", true);
        ARMOR = b.define("armor", true);
        DISABLED_ARMOR_SETS = b.defineListAllowEmpty("disabledArmorSets", List.of(), x -> x instanceof String);
        SPELLS = b.define("spells", true);
        EXTRA_SPELLS = b.define("extraSpells", true);
        RECIPES = b.define("recipes", true);
        SURVIVAL = b.define("survival", true);
        SURVIVAL_WORLDGEN = b.define("survivalWorldgen", true);
        SURVIVAL_LOOT = b.define("survivalLoot", true);
        SURVIVAL_PROGRESS = b.define("survivalProgress", true);
        DISABLED_ITEMS = b.defineListAllowEmpty("disabledItems", List.of(), x -> x instanceof String);
        DISABLED_SPELLS = b.defineListAllowEmpty("disabledSpells", List.of(), x -> x instanceof String);
        b.pop();
        SPEC = b.build();
    }
    public static boolean itemsEnabled() { return ENABLED.get() && WEAPONS.get(); }
    public static boolean itemEnabled(String id) { return itemsEnabled() && !DISABLED_ITEMS.get().contains(id); }
    public static boolean armorEnabled(String id, String set) {
        return ENABLED.get() && ARMOR.get() && !DISABLED_ITEMS.get().contains(id) && !DISABLED_ARMOR_SETS.get().contains(set);
    }
    public static boolean spellEnabled(String id) {
        return ENABLED.get() && SPELLS.get() && !DISABLED_SPELLS.get().contains(id);
    }
    private HazenConfig() {}
}
