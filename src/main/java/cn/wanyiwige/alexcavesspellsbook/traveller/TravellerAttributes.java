package cn.wanyiwige.alexcavesspellsbook.traveller;

import io.redspace.ironsspellbooks.api.attribute.MagicRangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Keeps the original KubeJS registry IDs used by existing equipment and saves. */
public final class TravellerAttributes {
    public static final String NAMESPACE = "kubejs";
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, NAMESPACE);

    public static final RegistryObject<Attribute> SPELL_POWER = ATTRIBUTES.register("traveller_spell_power",
            () -> new MagicRangedAttribute("attribute.kubejs.traveller_spell_power", 1.0D, 0.0D, 10000.0D)
                    .setSyncable(true));
    public static final RegistryObject<Attribute> SPELL_RESISTANCE = ATTRIBUTES.register("traveller_spell_resistance",
            () -> new MagicRangedAttribute("attribute.kubejs.traveller_spell_resistance", 1.0D, 0.0D, 10000.0D)
                    .setSyncable(true));

    private TravellerAttributes() {
    }
}
