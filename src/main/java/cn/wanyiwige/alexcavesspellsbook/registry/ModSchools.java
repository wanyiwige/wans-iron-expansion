package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.util.ModTags;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSchools {
    public static final ResourceLocation MAGNETIC_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "magnetic");

    public static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(
            SchoolRegistry.SCHOOL_REGISTRY_KEY, AlexCavesSpellsbook.MOD_ID);

    public static final RegistryObject<SchoolType> MAGNETIC = SCHOOLS.register("magnetic", () ->
            new SchoolType(
                    MAGNETIC_RESOURCE,
                    ModTags.MAGNETIC_FOCUS,
                    Component.translatable("school.alex_caves_spellsbook.magnetic")
                            .withStyle(style -> style.withColor(0xB34CFF)),
                    ModAttributes.MAGNETIC_SPELL_POWER,
                    ModAttributes.MAGNETIC_MAGIC_RESIST,
                    () -> ACSoundRegistry.MAGNETRON_ASSEMBLE.get(),
                    ModDamageTypes.MAGNETIC_MAGIC));

    public static final ResourceLocation CLOUD_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "cloud");

    public static final RegistryObject<SchoolType> CLOUD = SCHOOLS.register("cloud", () ->
            new SchoolType(
                    CLOUD_RESOURCE,
                    ModTags.CLOUD_FOCUS,
                    Component.translatable("school.alex_caves_spellsbook.cloud")
                            .withStyle(style -> style.withColor(0xB9DDF0)),
                    ModAttributes.CLOUD_SPELL_POWER,
                    ModAttributes.CLOUD_MAGIC_RESIST,
                    () -> net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                    ModDamageTypes.CLOUD_MAGIC));

    public static final ResourceLocation PURE_DARKNESS_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "pure_darkness");

    public static final RegistryObject<SchoolType> PURE_DARKNESS = SCHOOLS.register("pure_darkness", () ->
            new SchoolType(
                    PURE_DARKNESS_RESOURCE,
                    ModTags.PURE_DARKNESS_FOCUS,
                    Component.translatable("school.alex_caves_spellsbook.pure_darkness")
                            .withStyle(style -> style.withColor(0xD32931)),
                    ModAttributes.PURE_DARKNESS_SPELL_POWER,
                    ModAttributes.PURE_DARKNESS_MAGIC_RESIST,
                    () -> ACSoundRegistry.UNDERZEALOT_CHANT.get(),
                    ModDamageTypes.PURE_DARKNESS_MAGIC));

    private ModSchools() {
    }
}
