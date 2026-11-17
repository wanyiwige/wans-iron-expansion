package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Original four schools, owned by Wan's rather than a transplanted library. */
public final class HazenSchools {
    private static final DeferredRegister<SchoolType> REGISTRY = DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, "alex_caves_spellsbook");
    public static final Map<String, RegistryObject<SchoolType>> ENTRIES = new LinkedHashMap<>();
    static {
        add("cosmic", 0x473196);
        add("radiance", 0xe4a6ea);
        add("shadow", 0x553a7f);
        add("hydro", 0x36156c);
    }
    private static void add(String school, int color) {
        var key = HazenEquipment.id("hazen_" + school);
        ENTRIES.put(school, REGISTRY.register("hazen_" + school, () -> new SchoolType(key,
                TagKey.create(Registries.ITEM, HazenEquipment.id("hazen/" + school + "_focus")),
                Component.translatable("school.alex_caves_spellsbook.hazen_" + school).withStyle(s -> s.withColor(color)),
                HazenAttributes.ENTRIES.get(school + "_spell_power"), HazenAttributes.ENTRIES.get(school + "_magic_resist"),
                () -> school.equals("cosmic") ? HazenSounds.get("cosmic_cast_instant") : io.redspace.ironsspellbooks.registries.SoundRegistry.EVOCATION_CAST.get(),
                ResourceKey.create(Registries.DAMAGE_TYPE, HazenEquipment.id("hazen/" + school + "_magic")), false, true)));
    }
    public static void register(IEventBus bus) { REGISTRY.register(bus); }
    private HazenSchools() {}
}
