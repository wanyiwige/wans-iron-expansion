package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HazenAttributes {
    private static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "alex_caves_spellsbook");
    public static final Map<String, RegistryObject<Attribute>> ENTRIES = new LinkedHashMap<>();
    static {
        for (String school : new String[]{"cosmic", "radiance", "shadow", "hydro"}) {
            add(school + "_spell_power", 1, -100, 100);
            add(school + "_magic_resist", 1, -100, 100);
        }
        add("mana_rend", 0, 0, 100);
        add("spell_res_penetration", 0, 0, 1);
        add("magic_projectile_crit_chance", 0.05, 0, 1);
        add("armor_pierce", 0, 0, 1);
        add("crit_damage", 1.5, 1, 100);
        add("hunger_steal", 0, -100, 100);
    }
    private static void add(String name, double base, double min, double max) {
        ENTRIES.put(name, REGISTRY.register("hazen_" + name, () -> new MagicPercentAttribute(
                "attribute.alex_caves_spellsbook.hazen_" + name, base, min, max).setSyncable(true)));
    }
    public static Attribute get(String name) { return ENTRIES.get(name).get(); }
    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
        bus.addListener((EntityAttributeModificationEvent event) -> event.getTypes().forEach(type ->
                ENTRIES.values().forEach(attr -> event.add(type, attr.get()))));
    }
    public static Attribute resolve(String original) {
        if (original.startsWith("AttributeRegistry.")) {
            String field = original.substring("AttributeRegistry.".length());
            try {
                return ((RegistryObject<Attribute>) AttributeRegistry.class.getField(field).get(null)).get();
            } catch (ReflectiveOperationException ex) { throw new IllegalArgumentException(original, ex); }
        }
        return switch (original) {
            case "Attributes.ENTITY_INTERACTION_RANGE" -> ForgeMod.ENTITY_REACH.get();
            case "HLAttributeRegistry.COSMIC_SPELL_POWER" -> get("cosmic_spell_power");
            case "HLAttributeRegistry.RADIANCE_SPELL_POWER" -> get("radiance_spell_power");
            case "HLAttributeRegistry.SHADOW_SPELL_POWER" -> get("shadow_spell_power");
            case "ASAttributeRegistry.HYDRO_MAGIC_POWER" -> get("hydro_spell_power");
            case "ASAttributeRegistry.MANA_REND" -> get("mana_rend");
            case "ASAttributeRegistry.SPELL_RES_PENETRATION" -> get("spell_res_penetration");
            case "ASAttributeRegistry.MAGIC_PROJECTILE_CRIT_CHANCE" -> get("magic_projectile_crit_chance");
            case "IronsLibRegistries.AttributeRegistry.ARMOR_PIERCE" -> optional("attributeslib:armor_pierce", "armor_pierce");
            case "IronsLibRegistries.AttributeRegistry.CRIT_DAMAGE" -> optional("attributeslib:crit_damage", "crit_damage");
            default -> throw new IllegalArgumentException("Unmapped original attribute " + original);
        };
    }
    private static Attribute optional(String key, String fallback) {
        var found = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(key));
        return found == null ? get(fallback) : found;
    }
    private HazenAttributes() {}
}
