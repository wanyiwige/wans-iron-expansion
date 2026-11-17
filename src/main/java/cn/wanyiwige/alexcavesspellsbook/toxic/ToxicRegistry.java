package cn.wanyiwige.alexcavesspellsbook.toxic;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.*;

/** Common registration only. Optional integrations never load their client or entity classes here. */
public final class ToxicRegistry {
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID,path); }
    public static final ResourceLocation SCHOOL_ID=id("toxic");
    public static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,id("toxic_magic"));
    public static final TagKey<Item> FOCUS=TagKey.create(Registries.ITEM,id("toxic_focus"));
    public static final DeferredRegister<Attribute> ATTRIBUTES=DeferredRegister.create(ForgeRegistries.ATTRIBUTES,AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<Attribute> POWER=ATTRIBUTES.register("toxic_spell_power",()->new MagicPercentAttribute("attribute.alex_caves_spellsbook.toxic_spell_power",1,-100,100).setSyncable(true));
    public static final RegistryObject<Attribute> RESIST=ATTRIBUTES.register("toxic_magic_resist",()->new MagicPercentAttribute("attribute.alex_caves_spellsbook.toxic_magic_resist",1,-100,100).setSyncable(true));
    public static final DeferredRegister<SchoolType> SCHOOLS=DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY,AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<SchoolType> SCHOOL=SCHOOLS.register("toxic",()->new SchoolType(SCHOOL_ID,FOCUS,
            Component.translatable("school.alex_caves_spellsbook.toxic").withStyle(s->s.withColor(0xC9E72D)),POWER,RESIST,()->SoundEvents.BEACON_ACTIVATE,DAMAGE));
    public static final DeferredRegister<AbstractSpell> SPELLS=DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY,AlexCavesSpellsbook.MOD_ID);
    public static final Map<String,RegistryObject<AbstractSpell>> ENTRIES=new LinkedHashMap<>();
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,AlexCavesSpellsbook.MOD_ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,AlexCavesSpellsbook.MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<MobEffect> MARK_VISUAL=EFFECTS.register("radiation_mark",()->new MobEffect(MobEffectCategory.NEUTRAL,0xDBEF38){});
    public static final RegistryObject<Item> WASTE_PROP=ITEMS.register("toxic_waste_prop",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> REACTOR_PROP=ITEMS.register("toxic_reactor_prop",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> LEAD_PROP=ITEMS.register("toxic_lead_prop",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> RADIATION_GRIMOIRE=ITEMS.register("radiation_grimoire",()->new RadiationEquipment.Focus(10,.15,150));
    public static final RegistryObject<Item> MECHANICAL_RADIATION_EYE=ITEMS.register("mechanical_radiation_eye",()->new RadiationEquipment.Focus(8,.25,100));
    public static final RegistryObject<Item> FISSION_STAFF=ITEMS.register("fission_staff",RadiationEquipment.FissionStaff::new);
    public static final RegistryObject<Item> CRITICAL_WARDEN_HELMET=ITEMS.register("critical_warden_helmet",()->new CriticalWardenArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> CRITICAL_WARDEN_CHESTPLATE=ITEMS.register("critical_warden_chestplate",()->new CriticalWardenArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> CRITICAL_WARDEN_LEGGINGS=ITEMS.register("critical_warden_leggings",()->new CriticalWardenArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> CRITICAL_WARDEN_BOOTS=ITEMS.register("critical_warden_boots",()->new CriticalWardenArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final RegistryObject<EntityType<GammaJetEntity>> GAMMA=entity("gamma_jet_effect",GammaJetEntity::new);
    public static final RegistryObject<EntityType<WasteDrumSpellEntity>> DRUM=entity("waste_drum_spell",WasteDrumSpellEntity::new);
    public static final RegistryObject<EntityType<RaycatPurificationEntity>> PURIFICATION=entity("raycat_purification_effect",RaycatPurificationEntity::new);
    public static final RegistryObject<EntityType<ProtonLanceEntity>> LANCE=entity("proton_lance_effect",ProtonLanceEntity::new);
    public static final RegistryObject<EntityType<CriticalFissionEntity>> FISSION=entity("critical_fission_effect",CriticalFissionEntity::new);
    public static final RegistryObject<EntityType<ReactorWardEntity>> WARD=entity("reactor_ward_effect",ReactorWardEntity::new);
    public static final Map<ToxicSpec,RegistryObject<EntityType<RadiationNewEffectEntity>>> NEW_EFFECTS=new EnumMap<>(ToxicSpec.class);
    static {
        for(ToxicSpec spec:List.of(ToxicSpec.ISOTOPE,ToxicSpec.NEUTRON,ToxicSpec.MODERATOR,ToxicSpec.LEAD,ToxicSpec.CHAIN,
                ToxicSpec.FILM,ToxicSpec.CLOUD,ToxicSpec.CHERENKOV,ToxicSpec.DECOY,ToxicSpec.MATRIX))
            NEW_EFFECTS.put(spec,entity(spec.id+"_effect",(type,level)->new RadiationNewEffectEntity(type,level,spec)));
    }
    static { for(ToxicSpec spec:ToxicSpec.values()) ENTRIES.put(spec.id,SPELLS.register(spec.id,()->new ToxicSpell(spec))); }
    private static <T extends ToxicEffectEntity> RegistryObject<EntityType<T>> entity(String name,EntityType.EntityFactory<T> factory) {
        return ENTITIES.register(name,()->EntityType.Builder.of(factory,MobCategory.MISC).sized(0.4F,0.4F)
                .clientTrackingRange(16).updateInterval(1).noSave().build(name));
    }
    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);SCHOOLS.register(bus);SPELLS.register(bus);ENTITIES.register(bus);ITEMS.register(bus);EFFECTS.register(bus);
        bus.addListener(ToxicRegistry::attributes);
    }
    private static void attributes(EntityAttributeModificationEvent event) {
        for(var type:event.getTypes()) {
            if(!event.has(type,POWER.get()))event.add(type,POWER.get());
            if(!event.has(type,RESIST.get()))event.add(type,RESIST.get());
        }
    }
    private ToxicRegistry() {}
}
