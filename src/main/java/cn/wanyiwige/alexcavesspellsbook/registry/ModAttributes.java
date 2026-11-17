package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, AlexCavesSpellsbook.MOD_ID);

    public static final RegistryObject<Attribute> MAGNETIC_SPELL_POWER = ATTRIBUTES.register(
            "magnetic_spell_power",
            () -> new MagicPercentAttribute("attribute.alex_caves_spellsbook.magnetic_spell_power",
                    1.0D, -100.0D, 100.0D).setSyncable(true));

    public static final RegistryObject<Attribute> MAGNETIC_MAGIC_RESIST = ATTRIBUTES.register(
            "magnetic_magic_resist",
            () -> new MagicPercentAttribute("attribute.alex_caves_spellsbook.magnetic_magic_resist",
                    1.0D, -100.0D, 100.0D).setSyncable(true));

    public static final RegistryObject<Attribute> CLOUD_SPELL_POWER = ATTRIBUTES.register(
            "cloud_spell_power",
            () -> new MagicPercentAttribute("attribute.alex_caves_spellsbook.cloud_spell_power",
                    1.0D, -100.0D, 100.0D).setSyncable(true));

    public static final RegistryObject<Attribute> CLOUD_MAGIC_RESIST = ATTRIBUTES.register(
            "cloud_magic_resist",
            () -> new MagicPercentAttribute("attribute.alex_caves_spellsbook.cloud_magic_resist",
                    1.0D, -100.0D, 100.0D).setSyncable(true));

    public static final RegistryObject<Attribute> PURE_DARKNESS_SPELL_POWER = ATTRIBUTES.register(
            "pure_darkness_spell_power",
            () -> new MagicPercentAttribute("attribute.alex_caves_spellsbook.pure_darkness_spell_power",
                    1.0D, -100.0D, 100.0D).setSyncable(true));

    public static final RegistryObject<Attribute> PURE_DARKNESS_MAGIC_RESIST = ATTRIBUTES.register(
            "pure_darkness_magic_resist",
            () -> new MagicPercentAttribute("attribute.alex_caves_spellsbook.pure_darkness_magic_resist",
                    1.0D, -100.0D, 100.0D).setSyncable(true));

    private ModAttributes() {
    }
}
