package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.spells.MagneticSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PolarityCollapseSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.MagneticRailLanceSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.DipoleAnvilSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.MagnetizedFractureChainSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PolarityReversalSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.FerrousSandstormSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.SupernovaSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.DarkSparkSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.LandOfTheLustrousSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.SaciliteSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessRiteSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessWardSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessFinaleSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessFieldSpell;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessFieldSpell.Kind;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, AlexCavesSpellsbook.MOD_ID);

    public static final RegistryObject<AbstractSpell> SUPERNOVA = SPELLS.register("supernova", SupernovaSpell::new);
    public static final RegistryObject<AbstractSpell> DARK_SPARK = SPELLS.register("dark_spark", DarkSparkSpell::new);
    public static final RegistryObject<AbstractSpell> NEBULOUS_CATACLYSM = SPELLS.register("nebulous_cataclysm",
            cn.wanyiwige.alexcavesspellsbook.spells.NebulousCataclysmSpell::new);
    public static final RegistryObject<AbstractSpell> SACILITE_RAIN = SPELLS.register("sacilite_rain", () -> new SaciliteSpell(true));
    public static final RegistryObject<AbstractSpell> SEEKING_SACILITE_SWARM = SPELLS.register("seeking_sacilite_swarm", () -> new SaciliteSpell(false));
    public static final RegistryObject<AbstractSpell> LAND_OF_THE_LUSTROUS = SPELLS.register(
            "land_of_the_lustrous", LandOfTheLustrousSpell::new);

    public static final RegistryObject<AbstractSpell> MAGNETIZE = register("magnetize", SpellRarity.COMMON, 10, CastType.INSTANT, 4, 15, 5, 0, MagneticSpell.Behaviour.MAGNETIZE);
    public static final RegistryObject<AbstractSpell> SCARLET_ATTRACTION = register("scarlet_attraction", SpellRarity.COMMON, 10, CastType.INSTANT, 5, 20, 7, 0, MagneticSpell.Behaviour.ATTRACT);
    public static final RegistryObject<AbstractSpell> AZURE_REPULSION = register("azure_repulsion", SpellRarity.COMMON, 10, CastType.INSTANT, 5, 20, 7, 0, MagneticSpell.Behaviour.REPEL);
    public static final RegistryObject<AbstractSpell> TELETOR_ARMAMENT = register("teletor_armament", SpellRarity.UNCOMMON, 8, CastType.LONG, 9, 45, 10, 14, MagneticSpell.Behaviour.ARMAMENT);
    public static final RegistryObject<AbstractSpell> MAGNETIC_LEVITATION = register("magnetic_levitation", SpellRarity.UNCOMMON, 5, CastType.INSTANT, 12, 35, 10, 0, MagneticSpell.Behaviour.LEVITATION);
    public static final RegistryObject<AbstractSpell> NOTOR_SCAN = register("notor_scan", SpellRarity.UNCOMMON, 5, CastType.LONG, 10, 25, 10, 12, MagneticSpell.Behaviour.SCAN);
    public static final RegistryObject<AbstractSpell> BOUNDROID_DROP = register("boundroid_drop", SpellRarity.RARE, 5, CastType.LONG, 18, 80, 25, 20, MagneticSpell.Behaviour.BOUNDROID_DROP);
    public static final RegistryObject<AbstractSpell> FERROUSLIME_COHESION = register("ferrouslime_cohesion", SpellRarity.RARE, 5, CastType.LONG, 60, 90, 25, 30, MagneticSpell.Behaviour.FERROUSLIME_COHESION);
    public static final RegistryObject<AbstractSpell> TESLA_ARC_NODE = register("tesla_arc_node", SpellRarity.RARE, 5, CastType.LONG, 35, 90, 25, 20, MagneticSpell.Behaviour.TESLA_ARC_NODE);
    public static final RegistryObject<AbstractSpell> DIPOLE_VORTEX = register("dipole_vortex", SpellRarity.EPIC, 3, CastType.LONG, 35, 150, 40, 25, MagneticSpell.Behaviour.DIPOLE_VORTEX);

    // Independent magnetic combat spells with explicit level/rarity mappings.
    public static final RegistryObject<AbstractSpell> POLARITY_COLLAPSE = SPELLS.register("polarity_collapse", PolarityCollapseSpell::new);
    public static final RegistryObject<AbstractSpell> MAGNETIC_RAIL_LANCE = SPELLS.register("magnetic_rail_lance", MagneticRailLanceSpell::new);
    public static final RegistryObject<AbstractSpell> DIPOLE_ANVIL = SPELLS.register("dipole_anvil", DipoleAnvilSpell::new);
    public static final RegistryObject<AbstractSpell> MAGNETIZED_FRACTURE_CHAIN = SPELLS.register("magnetized_fracture_chain", MagnetizedFractureChainSpell::new);
    public static final RegistryObject<AbstractSpell> POLARITY_REVERSAL = SPELLS.register("polarity_reversal", PolarityReversalSpell::new);
    public static final RegistryObject<AbstractSpell> FERROUS_SANDSTORM = SPELLS.register("ferrous_sandstorm", FerrousSandstormSpell::new);

    public static final RegistryObject<AbstractSpell> DESOLATE_BLOODBLADE = SPELLS.register(
            cn.wanyiwige.alexcavesspellsbook.spells.DesolateBloodbladeSpell.PATH,
            cn.wanyiwige.alexcavesspellsbook.spells.DesolateBloodbladeSpell::new);

    public static final RegistryObject<AbstractSpell> CLOSING_EYE = registerPureDarknessField(Kind.CLOSING_EYE);
    public static final RegistryObject<AbstractSpell> MOTHSCALE_MIST = registerPureDarknessField(Kind.MOTHSCALE_MIST);
    public static final RegistryObject<AbstractSpell> CORRODENT_BURROW = registerPureDarknessField(Kind.CORRODENT_BURROW);
    public static final RegistryObject<AbstractSpell> VESPER_PERCH = registerPureDarknessField(Kind.VESPER_PERCH);

    public static AbstractSpell pureDarknessField(Kind kind) {
        return switch (kind) {
            case CLOSING_EYE -> CLOSING_EYE.get();
            case MOTHSCALE_MIST -> MOTHSCALE_MIST.get();
            case CORRODENT_BURROW -> CORRODENT_BURROW.get();
            case VESPER_PERCH -> VESPER_PERCH.get();
        };
    }

    private static RegistryObject<AbstractSpell> registerPureDarknessField(Kind kind) {
        return SPELLS.register(kind.path, () -> new PureDarknessFieldSpell(kind));
    }

    public static final RegistryObject<AbstractSpell> THORN_GIBBET = registerRite(PureDarknessRiteSpell.Kind.THORN_GIBBET);
    public static final RegistryObject<AbstractSpell> ZEALOT_CHORUS = registerRite(PureDarknessRiteSpell.Kind.ZEALOT_CHORUS);
    public static final RegistryObject<AbstractSpell> MOTHWING_GUARD = registerRite(PureDarknessRiteSpell.Kind.MOTHWING_GUARD);
    public static final RegistryObject<AbstractSpell> BURROWING_PRAYER = registerRite(PureDarknessRiteSpell.Kind.BURROWING_PRAYER);

    public static AbstractSpell pureDarknessRite(PureDarknessRiteSpell.Kind kind) {
        return switch (kind) {
            case THORN_GIBBET -> THORN_GIBBET.get();
            case ZEALOT_CHORUS -> ZEALOT_CHORUS.get();
            case MOTHWING_GUARD -> MOTHWING_GUARD.get();
            case BURROWING_PRAYER -> BURROWING_PRAYER.get();
        };
    }

    private static RegistryObject<AbstractSpell> registerRite(PureDarknessRiteSpell.Kind kind) {
        return SPELLS.register(kind.path, () -> new PureDarknessRiteSpell(kind));
    }

    public static final RegistryObject<AbstractSpell> WATCHER_DREAD = registerWard(PureDarknessWardSpell.Kind.WATCHER_DREAD);
    public static final RegistryObject<AbstractSpell> MARTYR_VESTMENT = registerWard(PureDarknessWardSpell.Kind.MARTYR_VESTMENT);
    public static final RegistryObject<AbstractSpell> FORSAKEN_MAW = registerWard(PureDarknessWardSpell.Kind.FORSAKEN_MAW);

    public static AbstractSpell pureDarknessWard(PureDarknessWardSpell.Kind kind) {
        return switch (kind) {
            case WATCHER_DREAD -> WATCHER_DREAD.get();
            case MARTYR_VESTMENT -> MARTYR_VESTMENT.get();
            case FORSAKEN_MAW -> FORSAKEN_MAW.get();
        };
    }

    private static RegistryObject<AbstractSpell> registerWard(PureDarknessWardSpell.Kind kind) {
        return SPELLS.register(kind.path, () -> new PureDarknessWardSpell(kind));
    }

    public static final RegistryObject<AbstractSpell> HOLLOWS_SCREECH = registerFinale(PureDarknessFinaleSpell.Kind.HOLLOWS_SCREECH);
    public static final RegistryObject<AbstractSpell> DREADBOW_GALLOWS = registerFinale(PureDarknessFinaleSpell.Kind.DREADBOW_GALLOWS);
    public static final RegistryObject<AbstractSpell> DARKBORN_RECOVERY = registerFinale(PureDarknessFinaleSpell.Kind.DARKBORN_RECOVERY);

    public static AbstractSpell pureDarknessFinale(PureDarknessFinaleSpell.Kind kind) {
        return switch (kind) {
            case HOLLOWS_SCREECH -> HOLLOWS_SCREECH.get();
            case DREADBOW_GALLOWS -> DREADBOW_GALLOWS.get();
            case DARKBORN_RECOVERY -> DARKBORN_RECOVERY.get();
        };
    }

    private static RegistryObject<AbstractSpell> registerFinale(PureDarknessFinaleSpell.Kind kind) {
        return SPELLS.register(kind.path, () -> new PureDarknessFinaleSpell(kind));
    }

    private ModSpells() {
    }

    private static RegistryObject<AbstractSpell> register(String name, SpellRarity rarity, int maxLevel, CastType castType,
                                                          double cooldown, int baseMana, int manaPerLevel, int castTime,
                                                          MagneticSpell.Behaviour behaviour) {
        return SPELLS.register(name, () -> new MagneticSpell(name, rarity, maxLevel, castType, cooldown, baseMana, manaPerLevel, castTime, behaviour));
    }

}
