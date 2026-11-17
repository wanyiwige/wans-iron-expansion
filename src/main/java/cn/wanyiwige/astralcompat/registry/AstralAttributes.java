package cn.wanyiwige.astralcompat.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The school keeps the original namespace so existing Astral equipment remains valid. */
public final class AstralAttributes {
    // Retain the original registry ids so existing equipment modifiers keep working.
    public static final String NAMESPACE = "kubejs";
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, NAMESPACE);

    public static final RegistryObject<Attribute> SPELL_POWER = ATTRIBUTES.register("astral_spell_power",
            () -> new RangedAttribute("attribute.kubejs.astral_spell_power", 0.0D, 0.0D, 10000.0D)
                    .setSyncable(true));
    public static final RegistryObject<Attribute> MAGIC_RESISTANCE = ATTRIBUTES.register("astral_spell_resistance",
            () -> new RangedAttribute("attribute.kubejs.astral_spell_resistance", 0.0D, 0.0D, 10000.0D)
                    .setSyncable(true));

    private AstralAttributes() {
    }
}
