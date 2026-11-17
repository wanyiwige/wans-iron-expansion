package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class TwilightSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, TwilightFoundation.NAMESPACE);
    public static final RegistryObject<AbstractSpell> TWILIGHT_BOLT =
            SPELLS.register("twilight_bolt", TwilightBoltSpell::new);
    public static final RegistryObject<AbstractSpell> DRUID_BOLT =
            SPELLS.register("druid_bolt", DruidBoltSpell::new);
    public static final RegistryObject<AbstractSpell> FORTIFYING_SHIELDS =
            SPELLS.register("fortifying_shields", FortifyingShieldsSpell::new);
    public static final RegistryObject<AbstractSpell> AURORA_MISSILE =
            SPELLS.register("aurora_missile", AuroraMissileSpell::new);
    public static final java.util.Map<String, RegistryObject<AbstractSpell>> RECREATED = new java.util.LinkedHashMap<>();
    static {
        for (TwilightSpellDefinition definition : TwilightSpellDefinition.ALL) {
            if (java.util.Set.of("twilight_bolt", "druid_bolt", "fortifying_shields", "aurora_missile").contains(definition.id())) continue;
            RECREATED.put(definition.id(), SPELLS.register(definition.id(), () -> new TwilightRecreatedSpell(definition)));
        }
    }
    public static AbstractSpell get(String id) {
        return switch (id) {
            case "twilight_bolt" -> TWILIGHT_BOLT.get();
            case "druid_bolt" -> DRUID_BOLT.get();
            case "fortifying_shields" -> FORTIFYING_SHIELDS.get();
            case "aurora_missile" -> AURORA_MISSILE.get();
            default -> RECREATED.get(id).get();
        };
    }

    private TwilightSpells() {}
}
