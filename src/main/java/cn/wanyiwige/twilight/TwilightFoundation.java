package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Forge 1.20.1 foundation for the RedTablos Twilight content. */
public final class TwilightFoundation {
    public static final String NAMESPACE = "wans_twilight_spellbooks";
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }

    public static final ResourceKey<DamageType> EXANIMATED_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, id("exanimated_magic"));
    public static final TagKey<Item> EXANIMATED_FOCUS =
            TagKey.create(Registries.ITEM, id("exanimated_focus"));

    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, NAMESPACE);
    public static final RegistryObject<SchoolType> EXANIMATED = SCHOOLS.register("exanimated", () ->
            new SchoolType(id("exanimated"), EXANIMATED_FOCUS,
                    Component.translatable("school.wans_twilight_spellbooks.exanimated")
                            .withStyle(style -> style.withColor(11436779)),
                    AttributeRegistry.FIRE_SPELL_POWER,
                    AttributeRegistry.FIRE_MAGIC_RESIST,
                    SoundRegistry.FIRE_CAST,
                    EXANIMATED_DAMAGE));

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, NAMESPACE);
    public static final RegistryObject<MobEffect> OMINOUS_BURN = EFFECTS.register("ominous_burn", () ->
            new TwilightBurnEffect(MobEffectCategory.HARMFUL, 8084223, 1.0F));
    public static final RegistryObject<MobEffect> HYDRA_FIRE = EFFECTS.register("hydra_fire", () ->
            new TwilightBurnEffect(MobEffectCategory.HARMFUL, 5505115, 0.5F)
                    .addAttributeModifier(Attributes.ARMOR, "26c96522-c187-41ce-95ee-7f3c7d8448d7",
                            -0.05D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistryObject<MobEffect> HYDRA_REVENGE = EFFECTS.register("hydra_revenge", () ->
            new MobEffect(MobEffectCategory.HARMFUL, 16775830) {});

    public static void register(IEventBus bus) {
        TwilightItems.register(bus);
        SCHOOLS.register(bus);
        EFFECTS.register(bus);
        TwilightSpells.SPELLS.register(bus);
        TwilightEntities.ENTITIES.register(bus);
        TwilightParticles.PARTICLES.register(bus);
        TwilightAttributes.ATTRIBUTES.register(bus);
        TwilightData.LOOT.register(bus);
        TwilightData.RECIPES.register(bus);
        TwilightContentTab.TABS.register(bus);
        MinecraftForge.EVENT_BUS.register(TwilightGameplayEvents.class);
        bus.addListener(TwilightRegistration::attributes);
        bus.addListener(TwilightRegistration::extraAttributes);
        MinecraftForge.EVENT_BUS.register(TwilightWorldEvents.class);
        MinecraftForge.EVENT_BUS.addListener(TwilightEffectEvents::reduceHealing);
        MinecraftForge.EVENT_BUS.addListener(TwilightEffectEvents::reflectDamage);
    }

    private TwilightFoundation() {}
}
