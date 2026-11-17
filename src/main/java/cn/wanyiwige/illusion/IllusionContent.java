package cn.wanyiwige.illusion;

import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.*;

/** Integrated content, registered by Wan's entry point; no second mod or newer loader required. */
public final class IllusionContent {
    public static final String ID="illusionist_grimoire";
    public static ResourceLocation id(String path){return new ResourceLocation(ID,path);}
    public static final TagKey<Item> FOCUS=TagKey.create(Registries.ITEM,id("illusion_focus"));
    public static final DeferredRegister<SchoolType> SCHOOLS=DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY,ID);
    public static final RegistryObject<SchoolType> SCHOOL=SCHOOLS.register("illusion",()->new SchoolType(id("illusion"),FOCUS,
            Component.translatable("school.illusionist_grimoire.illusion").withStyle(s->s.withColor(0xB6A1E8)),AttributeRegistry.EVOCATION_SPELL_POWER,AttributeRegistry.EVOCATION_MAGIC_RESIST,SoundRegistry.EVOCATION_CAST,ISSDamageTypes.EVOCATION_MAGIC));
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,ID);
    public static final Map<String,RegistryObject<MobEffect>> EFFECT_MAP=new LinkedHashMap<>();
    static{for(String name:List.of("depth_trick","disguised","paranoia","emptiness","feared","squish"))EFFECT_MAP.put(name,EFFECTS.register(name,()->new MagicMobEffect(name.equals("disguised")?MobEffectCategory.BENEFICIAL:name.equals("squish")?MobEffectCategory.NEUTRAL:MobEffectCategory.HARMFUL,0x49413A)));}
    public static MobEffect effect(String name){return EFFECT_MAP.get(name).get();}
    public static final DeferredRegister<AbstractSpell> SPELLS=DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY,ID);
    public static final Map<String,RegistryObject<AbstractSpell>> SPELL_MAP=new LinkedHashMap<>();
    static{for(String name:List.of("illusion_wall","depth_trick","paranoia","disguise","emptiness","fear_bolt","squish"))SPELL_MAP.put(name,SPELLS.register(name,()->new IllusionSpell(name)));}
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final RegistryObject<Item> PICTURE_BOOK=ITEMS.register("picture_book",PictureBook::new);
    public static final RegistryObject<Item> TRAP_BOOK=ITEMS.register("spell_trap_book",TrapBook::new);
    public static final RegistryObject<Item> MIRAGE_PRISM=ITEMS.register("mirage_prism",()->new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
    public static final RegistryObject<Block> WALL=BLOCKS.register("illusion_block",()->new IllusionBlock(false));
    public static final RegistryObject<Block> TRAP=BLOCKS.register("spell_trap",()->new IllusionBlock(true));
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
    public static final RegistryObject<BlockEntityType<IllusionBlockEntity>> BLOCK_ENTITY=BLOCK_ENTITIES.register("illusion",()->BlockEntityType.Builder.of(IllusionBlockEntity::new,WALL.get(),TRAP.get()).build(null));
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
    public static final RegistryObject<EntityType<FearBolt>> FEAR_BOLT=ENTITIES.register("fear_bolt",()->EntityType.Builder.<FearBolt>of(FearBolt::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(64).updateInterval(1).build(id("fear_bolt").toString()));
    public static final RegistryObject<EntityType<TrapCaster>> TRAP_CASTER=ENTITIES.register("spell_trap_dummy",()->EntityType.Builder.<TrapCaster>of(TrapCaster::new,MobCategory.MISC).sized(.1F,.1F).clientTrackingRange(32).build(id("spell_trap_dummy").toString()));
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ID);
    public static final RegistryObject<SoundEvent> FLASH=SOUNDS.register("flashbang",()->SoundEvent.createVariableRangeEvent(id("flashbang")));
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
    static{TABS.register("main_tab",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.illusionist_grimoire.main_tab")).icon(()->PICTURE_BOOK.get().getDefaultInstance()).displayItems((p,out)->{
        out.accept(PICTURE_BOOK.get());out.accept(TRAP_BOOK.get());out.accept(MIRAGE_PRISM.get());for(var entry:SPELL_MAP.values()){ItemStack scroll=new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(entry.get(),1,scroll);out.accept(scroll);}
    }).build());}
    public static void register(IEventBus bus){
        SCHOOLS.register(bus);EFFECTS.register(bus);SPELLS.register(bus);ITEMS.register(bus);BLOCKS.register(bus);BLOCK_ENTITIES.register(bus);ENTITIES.register(bus);SOUNDS.register(bus);TABS.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e)->e.put(TRAP_CASTER.get(),TrapCaster.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeModificationEvent e)->{for(var attribute:ForgeRegistries.ATTRIBUTES)if(!e.has(TRAP_CASTER.get(),attribute))e.add(TRAP_CASTER.get(),attribute);});
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(IllusionNetwork::register));
        bus.addListener((net.minecraftforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTabKey().location().equals(new ResourceLocation("alex_caves_spellsbook","alex_caves_spellsbook"))){e.accept(PICTURE_BOOK.get());e.accept(TRAP_BOOK.get());e.accept(MIRAGE_PRISM.get());for(var entry:SPELL_MAP.values())for(int rank=1;rank<=entry.get().getMaxLevel();rank++){ItemStack scroll=new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(entry.get(),rank,scroll);e.accept(scroll);}}});
        MinecraftForge.EVENT_BUS.register(IllusionEvents.class);
    }
    private IllusionContent(){}
}
