package cn.wanyiwige.alexcavesspellsbook.traveller;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class TravellerSchools {
    public static final ResourceLocation RESOURCE =
            ResourceLocation.fromNamespaceAndPath(TravellerAttributes.NAMESPACE, "traveller");
    private static final TagKey<Item> FOCUS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TravellerAttributes.NAMESPACE, "traveller_focus"));
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TravellerAttributes.NAMESPACE, "traveller_spell_damage_type"));

    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, TravellerAttributes.NAMESPACE);
    public static final RegistryObject<SchoolType> TRAVELLER = SCHOOLS.register("traveller", () -> new SchoolType(
            RESOURCE,
            FOCUS,
            Component.literal("旅者").withStyle(ChatFormatting.DARK_GREEN),
            TravellerAttributes.SPELL_POWER,
            TravellerAttributes.SPELL_RESISTANCE,
            () -> SoundEvents.ENCHANTMENT_TABLE_USE,
            DAMAGE));

    private TravellerSchools() {
    }
}
