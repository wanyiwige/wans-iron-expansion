package cn.wanyiwige.alexcavesspellsbook.candy;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

/** Common-only registration. No references to renderer classes. */
public final class CandyRegistry {
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, path); }
    public static final ResourceLocation SCHOOL_ID = id("candy");
    public static final TagKey<Item> FOCUS = TagKey.create(Registries.ITEM, id("candy_focus"));
    public static final TagKey<EntityType<?>> CONTROL_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, id("candy_control_immune"));
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("candy_magic"));
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<Attribute> POWER = ATTRIBUTES.register("candy_spell_power", () -> new MagicPercentAttribute("attribute.alex_caves_spellsbook.candy_spell_power", 1, -100, 100).setSyncable(true));
    public static final RegistryObject<Attribute> RESIST = ATTRIBUTES.register("candy_magic_resist", () -> new MagicPercentAttribute("attribute.alex_caves_spellsbook.candy_magic_resist", 1, -100, 100).setSyncable(true));
    public static final DeferredRegister<SchoolType> SCHOOLS = DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<SchoolType> CANDY = SCHOOLS.register("candy", () -> new SchoolType(SCHOOL_ID, FOCUS,
            Component.translatable("school.alex_caves_spellsbook.candy").withStyle(s -> s.withColor(0xDB397D)), POWER, RESIST,
            () -> ACSoundRegistry.LICOWITCH_CAST_HEX.get(), DAMAGE));
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, AlexCavesSpellsbook.MOD_ID);
    public static final Map<String, RegistryObject<AbstractSpell>> ENTRIES;
    static {
        Map<String, RegistryObject<AbstractSpell>> entries = new LinkedHashMap<>();
        for (CandySpecs.Spec spec : CandySpecs.ALL) entries.put(spec.id(), SPELLS.register(spec.id(), () ->
                CandyFirstRules.implemented(spec.id()) ? new CandyFirstSpell(spec) : new CandyLastSpell(spec)));
        ENTRIES = Collections.unmodifiableMap(entries);
    }
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<EntityType<CandyEffectEntity>> EFFECT = ENTITIES.register("candy_effect", () -> EntityType.Builder
            .<CandyEffectEntity>of(CandyLastSpellEntity::new, MobCategory.MISC).sized(0.3F, 0.3F)
            .clientTrackingRange(16).updateInterval(1).noSave().build("candy_effect"));
    public static void register(IEventBus bus) {
        CandyConfig.register();
        ATTRIBUTES.register(bus); SCHOOLS.register(bus); SPELLS.register(bus); ENTITIES.register(bus);
        EFFECTS.register(bus);
        bus.addListener(CandyRegistry::attributes);
        bus.addListener(CandyRegistry::summonAttributes);
    }
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<net.minecraft.world.effect.MobEffect> CARAMEL_STICKY = EFFECTS.register("caramel_sticky",CaramelStickyEffect::new);
    public static final RegistryObject<net.minecraft.world.effect.MobEffect> FROSTMINT_CHILL = EFFECTS.register("frostmint_chill",FrostmintChillEffect::new);
    public static final RegistryObject<net.minecraft.world.effect.MobEffect> SWEET_OVERDRIVE = EFFECTS.register("sweet_overdrive",SweetOverdriveEffect::new);
    public static final RegistryObject<EntityType<SummonedCandyBear>> SUMMON_BEAR = ENTITIES.register("summoned_candy_bear",()->EntityType.Builder
            .of(SummonedCandyBear::new,MobCategory.CREATURE).sized(1.1F,1.7F).clientTrackingRange(10).updateInterval(2).noSave().build("summoned_candy_bear"));
    public static final RegistryObject<EntityType<SummonedCandyGinger>> SUMMON_GINGER = ENTITIES.register("summoned_candy_ginger",()->EntityType.Builder
            .of(SummonedCandyGinger::new,MobCategory.MISC).sized(0.65F,0.8F).clientTrackingRange(10).updateInterval(2).noSave().build("summoned_candy_ginger"));
    public static final RegistryObject<EntityType<SummonedCandySentry>> SUMMON_SENTRY = ENTITIES.register("summoned_candy_sentry",()->EntityType.Builder
            .of(SummonedCandySentry::new,MobCategory.MISC).sized(0.9F,1.15F).clientTrackingRange(10).updateInterval(2).noSave().build("summoned_candy_sentry"));
    private static void attributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> { if (!event.has(type, POWER.get())) event.add(type, POWER.get());
            if (!event.has(type, RESIST.get())) event.add(type, RESIST.get()); });
    }
    private static void summonAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event){
        event.put(SUMMON_BEAR.get(),com.github.alexmodguy.alexscaves.server.entity.living.GummyBearEntity.createAttributes().build());
        event.put(SUMMON_GINGER.get(),com.github.alexmodguy.alexscaves.server.entity.living.GingerbreadManEntity.createAttributes().build());
        event.put(SUMMON_SENTRY.get(),com.github.alexmodguy.alexscaves.server.entity.living.GumbeeperEntity.createAttributes().build());
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID)
    public static final class NoSummonLoot {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void drops(net.minecraftforge.event.entity.living.LivingDropsEvent e){
            if(isCandySummon(e.getEntity()))e.setCanceled(true);
        }
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void experience(net.minecraftforge.event.entity.living.LivingExperienceDropEvent e){
            if(isCandySummon(e.getEntity()))e.setDroppedExperience(0);
        }
        private static boolean isCandySummon(net.minecraft.world.entity.LivingEntity entity){
            return entity instanceof SummonedCandyBear||entity instanceof SummonedCandyGinger||entity instanceof SummonedCandySentry;
        }
    }
    private CandyRegistry() { }
}
