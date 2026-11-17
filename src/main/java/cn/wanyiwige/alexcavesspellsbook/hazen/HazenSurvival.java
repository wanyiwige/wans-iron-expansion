package cn.wanyiwige.alexcavesspellsbook.hazen;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Native survival hooks; source items and registries remain stable when disabled. */
public final class HazenSurvival {
    private static final String NS="alex_caves_spellsbook";
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,NS);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,NS);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,NS);
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOMES=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,NS);
    private static final DeferredRegister<LootItemConditionType> CONDITIONS=DeferredRegister.create(Registries.LOOT_CONDITION_TYPE,NS);
    public static final Map<String,RegistryObject<Block>> ORES=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Block>> WOOD=new LinkedHashMap<>();
    private static final RegistryObject<LootItemConditionType> ENABLED=CONDITIONS.register("hazen_survival_enabled",()->new LootItemConditionType(new EnabledSerializer()));
    static {
        for(String name:new String[]{"chlorophyte_ore","dreadstone_ore","solar_core_blackstone_ore","solar_core_netherrack_ore"}){
            RegistryObject<Block> block=BLOCKS.register("hazen_"+name,()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE)
                    .strength(3).sound(SoundType.ANCIENT_DEBRIS),UniformInt.of(2,4)));
            ORES.put(name,block);
            ITEMS.register("hazen_"+name,()->new BlockItem(block.get(),new Item.Properties()));
        }
        for(String name:new String[]{"wisewood_log","wisewood_wood","stripped_wisewood_log","stripped_wisewood_wood","wisewood_planks"}){
            RegistryObject<Block> block=BLOCKS.register("hazen_"+name,()->name.equals("wisewood_planks")
                    ?new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS))
                    :new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)){
                        @Override public net.minecraft.world.level.block.state.BlockState getToolModifiedState(
                                net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.item.context.UseOnContext context,
                                net.minecraftforge.common.ToolAction action,boolean simulate){
                            if(action==net.minecraftforge.common.ToolActions.AXE_STRIP&&!name.startsWith("stripped_"))
                                return WOOD.get("stripped_"+name).get().defaultBlockState().setValue(AXIS,state.getValue(AXIS));
                            return super.getToolModifiedState(state,context,action,simulate);
                        }
                    });
            WOOD.put(name,block);ITEMS.register("hazen_"+name,()->new BlockItem(block.get(),new Item.Properties()));
        }
        LOOT.register("hazen_append",()->Append.CODEC);
        BIOMES.register("hazen_features",()->Features.CODEC);
    }
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);LOOT.register(bus);BIOMES.register(bus);CONDITIONS.register(bus);}
    public static boolean enabled(){return HazenConfig.ENABLED.get()&&HazenConfig.SURVIVAL.get();}
    public static boolean allowed(ItemStack stack){
        ResourceLocation id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        if(id==null||!id.getNamespace().equals(NS)||!id.getPath().startsWith("hazen_"))return true;
        String name=id.getPath().substring(6);
        if(HazenConfig.DISABLED_ITEMS.get().contains(name))return false;
        if(HazenEquipment.WEAPONS.containsKey(name))return HazenConfig.itemEnabled(name);
        if(HazenArmor.ITEMS.containsKey(name))return HazenArmor.ITEMS.get(name).get().enabled();
        return true;
    }
    public static final class Append extends LootModifier {
        public static final Codec<Append> CODEC=RecordCodecBuilder.create(i->codecStart(i)
                .and(ResourceLocation.CODEC.fieldOf("table").forGetter(v->v.table)).apply(i,Append::new));
        private final ResourceLocation table;
        public Append(LootItemCondition[] conditions,ResourceLocation table){super(conditions);this.table=table;}
        @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC;}
        @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
            if(enabled()&&HazenConfig.SURVIVAL_LOOT.get())
                context.getLevel().getServer().getLootData().getLootTable(table).getRandomItemsRaw(context,
                        stack->{if(allowed(stack))loot.add(stack);});
            return loot;
        }
    }
    public record Features(HolderSet<Biome> biomes,HolderSet<PlacedFeature> features,GenerationStep.Decoration step) implements BiomeModifier {
        public static final Codec<Features> CODEC=RecordCodecBuilder.create(i->i.group(
                Biome.LIST_CODEC.fieldOf("biomes").forGetter(Features::biomes),
                PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(Features::features),
                GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(Features::step)).apply(i,Features::new));
        @Override public Codec<? extends BiomeModifier> codec(){return CODEC;}
        @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){
            if(phase==Phase.ADD&&enabled()&&HazenConfig.SURVIVAL_WORLDGEN.get()&&biomes.contains(biome))
                features.forEach(f->builder.getGenerationSettings().addFeature(step,f));
        }
    }
    private static final class Enabled implements LootItemCondition {
        @Override public LootItemConditionType getType(){return ENABLED.get();}
        @Override public boolean test(LootContext context){return enabled()&&HazenConfig.SURVIVAL_PROGRESS.get();}
    }
    private static final class EnabledSerializer implements Serializer<Enabled> {
        public void serialize(JsonObject json,Enabled value,JsonSerializationContext context){}
        public Enabled deserialize(JsonObject json,JsonDeserializationContext context){return new Enabled();}
    }
    private HazenSurvival(){}
}
