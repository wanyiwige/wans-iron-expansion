package cn.wanyiwige.alexcavesspellsbook.traveller;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class TravellerSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, TravellerAttributes.NAMESPACE);
    public static final RegistryObject<AbstractSpell> BROOM = SPELLS.register("broom", BroomSpell::new);
    public static final RegistryObject<AbstractSpell> MAP_TRAVEL = SPELLS.register("map_travel", MapTravelSpell::new);

    private TravellerSpells() {
    }
}
