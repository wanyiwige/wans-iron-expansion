package cn.wanyiwige.astralcompat.registry;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class AstralSchools {
    public static final ResourceLocation ASTRAL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(AstralAttributes.NAMESPACE, "astral");
    private static final TagKey<Item> ASTRAL_FOCUS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(AstralAttributes.NAMESPACE, "astral_focus"));
    private static final ResourceKey<DamageType> ASTRAL_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AstralAttributes.NAMESPACE, "astral_spell_damage_type"));

    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, AstralAttributes.NAMESPACE);
    public static final RegistryObject<SchoolType> ASTRAL = SCHOOLS.register("astral", () -> new SchoolType(
            ASTRAL_RESOURCE,
            ASTRAL_FOCUS,
            Component.literal("星辉").withStyle(style -> style.withColor(0x6E57FF)),
            AstralAttributes.SPELL_POWER,
            AstralAttributes.MAGIC_RESISTANCE,
            () -> SoundEvents.AMETHYST_BLOCK_CHIME,
            ASTRAL_DAMAGE,
            false,
            true) {
        @Override
        public double getPowerFor(net.minecraft.world.entity.LivingEntity caster) {
            // This retained KubeJS attribute stores a bonus around zero.
            var attribute = AstralAttributes.SPELL_POWER.get();
            return caster.getAttributes().hasAttribute(attribute) ? 1.0 + caster.getAttributeValue(attribute) : 1.0;
        }
    });

    private AstralSchools() {
    }
}
