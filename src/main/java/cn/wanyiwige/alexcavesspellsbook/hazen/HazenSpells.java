package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HazenSpells {
    public static final DeferredRegister<AbstractSpell> REGISTRY = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, "alex_caves_spellsbook");
    public static final Map<String, RegistryObject<AbstractSpell>> ENTRIES = new LinkedHashMap<>();
    static {
        for (String name : new String[]{"thorn_chakram", "shooting_star", "spectral_axe", "nights_edge_strike",
                "terraprismic_barrage", "call_forth_terraprisma", "prismatic_shift"})
            ENTRIES.put(name, REGISTRY.register("hazen_" + name, () -> new HazenSpell(name)));
        for(var spec:HazenSecondaryRegistry.SPELLS)
            ENTRIES.put(spec.id(),REGISTRY.register("hazen_"+spec.id(),()->new HazenSecondarySpell(spec)));
    }
    public static void register(IEventBus bus) { REGISTRY.register(bus); }
    public static AbstractSpell get(String name) { return ENTRIES.get(name).get(); }
    public static AbstractSpell resolve(String originalField) {
        if (originalField.startsWith("HnSSpellRegistries.")) return get(originalField.substring(originalField.indexOf('.') + 1).toLowerCase(java.util.Locale.ROOT));
        String field = originalField.substring(originalField.indexOf('.') + 1);
        try {
            return ((java.util.function.Supplier<AbstractSpell>) SpellRegistry.class.getField(field).get(null)).get();
        } catch (ReflectiveOperationException ex) { throw new IllegalArgumentException(originalField, ex); }
    }
    public static void addScrolls(CreativeModeTab.Output output) {
        for (var entry : ENTRIES.entrySet()) if (entry.getValue().get().isEnabled()) {
            var spell = entry.getValue().get();
            for (int level = 1; level <= spell.getMaxLevel(); level++) {
                var stack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(spell, level, stack);
                output.accept(stack);
            }
        }
    }
    private HazenSpells() {}
}
