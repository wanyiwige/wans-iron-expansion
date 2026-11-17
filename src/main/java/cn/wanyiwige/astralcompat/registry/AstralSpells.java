package cn.wanyiwige.astralcompat.registry;

import cn.wanyiwige.astralcompat.spells.AstralSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Existing Wan spells; their IDs remain stable for saved scrolls. */
public final class AstralSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, AstralSpell.NAMESPACE);

    public static final RegistryObject<AbstractSpell> STARFIRE = register("starfire", SpellRarity.COMMON,
            10, 2.0D, 24, 8, 20, 6, 0, CastType.INSTANT);
    public static final RegistryObject<AbstractSpell> BRIGHTBURST = register("brightburst", SpellRarity.RARE,
            6, 28.0D, 90, 25, 54, 12, 0, CastType.INSTANT);
    public static final RegistryObject<AbstractSpell> STAR_SWARM = register("star_swarm", SpellRarity.COMMON,
            10, 8.0D, 12, 4, 16, 4, 100, CastType.CONTINUOUS);
    public static final RegistryObject<AbstractSpell> LUMINOUS_BEAM = register("luminous_beam", SpellRarity.COMMON,
            10, 14.0D, 50, 10, 24, 7, 100, CastType.CONTINUOUS);
    public static final RegistryObject<AbstractSpell> PIERCING_LIGHT = register("piercing_light", SpellRarity.RARE,
            10, 24.0D, 80, 20, 24, 6, 0, CastType.INSTANT);

    // Original Asterism content is distinct from the older Wan implementations.
    public static final RegistryObject<AbstractSpell> ARCANUM_STARFIRE = recreated("arcanum_starfire", "starfire");
    public static final RegistryObject<AbstractSpell> ARCANUM_BRIGHTBURST = recreated("arcanum_brightburst", "brightburst");
    public static final RegistryObject<AbstractSpell> ARCANUM_STAR_SWARM = recreated("arcanum_star_swarm", "star_swarm");
    public static final RegistryObject<AbstractSpell> ARCANUM_LUMINOUS_BEAM = recreated("arcanum_luminous_beam", "luminous_beam");
    public static final RegistryObject<AbstractSpell> ARCANUM_PIERCING_LIGHT = recreated("arcanum_piercing_light", "piercing_light");
    public static final RegistryObject<AbstractSpell> ARCANUM_STARCUTTER = recreated("arcanum_starcutter", "starcutter");
    public static final RegistryObject<AbstractSpell> SILVERY_BARBS = recreated("silvery_barbs", "silvery_barbs");
    public static final RegistryObject<AbstractSpell> CELESTIAL_TETHER = recreated("celestial_tether", "celestial_tether");
    public static final RegistryObject<AbstractSpell> ASTRAL_ECHO = recreated("astral_echo", "astral_echo");
    public static final RegistryObject<AbstractSpell> SUMMON_LUNAR_MOTHS = SPELLS.register("summon_lunar_moths", cn.wanyiwige.astralcompat.recreation.ArcanumTravelSpells.LunarMoths::new);
    public static final RegistryObject<AbstractSpell> ASTRAL_GATEWAY = SPELLS.register("astral_gateway", cn.wanyiwige.astralcompat.recreation.ArcanumTravelSpells.Gateway::new);

    private static RegistryObject<AbstractSpell> recreated(String id, String behavior) {
        return SPELLS.register(id, () -> new cn.wanyiwige.astralcompat.recreation.RecreatedAstralSpell(id, behavior));
    }

    private static RegistryObject<AbstractSpell> register(String id, SpellRarity rarity, int maxLevel,
                                                          double cooldown, int baseMana, int manaPerLevel,
                                                          int basePower, int powerPerLevel, int castTime,
                                                          CastType castType) {
        return SPELLS.register(id, () -> new AstralSpell(id, rarity, maxLevel, cooldown,
                baseMana, manaPerLevel, basePower, powerPerLevel, castTime, castType));
    }

    private AstralSpells() {
    }
}
