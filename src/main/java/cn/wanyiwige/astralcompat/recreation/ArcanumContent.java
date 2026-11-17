package cn.wanyiwige.astralcompat.recreation;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import io.redspace.ironsspellbooks.item.UpgradeOrbItem;

/** The original item/block namespace is kept for recipes and structure palettes. */
public final class ArcanumContent {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"asterismarcanum");
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"asterismarcanum");
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"asterismarcanum");
    public static final Map<String,RegistryObject<Block>> BUILDING=new LinkedHashMap<>();
    public static final RegistryObject<Item> DUST=item("lumine_dust",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> RUNE=item("astral_rune",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> WINGS=item("dragonfly_wings",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> UPGRADE=item("astral_upgrade_orb",()->new UpgradeOrbItem(new Item.Properties().rarity(Rarity.UNCOMMON),
            net.minecraft.resources.ResourceKey.create(UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY,id("astral_spell_power"))));
    public static final RegistryObject<Item> BOOK=item("astrolabe",ArcanumEquipment.Astrolabe::new);
    public static final RegistryObject<Item> STAFF=item("celestial_staff",ArcanumEquipment.CelestialStaff::new);
    public static final RegistryObject<Item> TALISMAN=item("ethereal_talisman",ArcanumEquipment.Talisman::new);
    public static final RegistryObject<Item> MOTHSPAWN=item("mothspawn",MothspawnProjectile.EggItem::new);
    public static final RegistryObject<EntityType<ArcanumInsect>> DRAGONFLY=ENTITIES.register("dragonfly",()->EntityType.Builder.of(ArcanumInsect::new,MobCategory.CREATURE).sized(1,1).clientTrackingRange(64).build(id("dragonfly").toString()));
    public static final RegistryObject<EntityType<ArcanumInsect>> LUNAR_MOTH=ENTITIES.register("lunar_moth",()->EntityType.Builder.of(ArcanumInsect::new,MobCategory.CREATURE).sized(1,1).clientTrackingRange(64).build(id("lunar_moth").toString()));
    public static final RegistryObject<EntityType<SummonedMoth>> SUMMONED_MOTH=ENTITIES.register("summoned_lunar_moth",()->EntityType.Builder.of(SummonedMoth::new,MobCategory.CREATURE).sized(2,2).clientTrackingRange(64).updateInterval(1).build(id("summoned_lunar_moth").toString()));
    public static final RegistryObject<EntityType<Astromancer>> ASTROMANCER=ENTITIES.register("astromancer",()->EntityType.Builder.of(Astromancer::new,MobCategory.MONSTER).sized(.6F,2).clientTrackingRange(64).build(id("astromancer").toString()));
    public static final RegistryObject<EntityType<MothspawnProjectile>> THROWN_EGG=ENTITIES.register("thrown_mothspawn",()->EntityType.Builder.<MothspawnProjectile>of(MothspawnProjectile::new,MobCategory.MISC).sized(.3F,.3F).clientTrackingRange(64).build(id("thrown_mothspawn").toString()));
    static {
        for(ArmorItem.Type type:ArmorItem.Type.values()) item("astral_"+type.getName(),()->new ArcanumEquipment.AstralArmor(type));
        item("dragonfly_spawn_egg",()->new ForgeSpawnEggItem(DRAGONFLY,12442078,14405324,new Item.Properties()));
        item("lunar_moth_spawn_egg",()->new ForgeSpawnEggItem(LUNAR_MOTH,15588841,12223193,new Item.Properties()));
        item("astromancer_spawn_egg",()->new ForgeSpawnEggItem(ASTROMANCER,13945296,11719875,new Item.Properties()));
        for(String family:List.of("lumine","umbral_lumine")) {
            block(family,()->new Block(stone()));
            block("chiseled_"+family,()->new Block(stone()));
            block("polished_"+family,()->new Block(stone()));
            block(family+"_bricks",()->new Block(stone()));
            for(String base:List.of(family,"polished_"+family,family+"_bricks")) {
                block(base+"_slab",()->new SlabBlock(stone()));
                block(base+"_stairs",()->new StairBlock(()->BUILDING.get(base).get().defaultBlockState(),stone()));
                block(base+"_wall",()->new WallBlock(stone().strength(4)));
            }
        }
        block("dragonfly_comb",()->new CombBlock(BlockBehaviour.Properties.copy(Blocks.MUD)));
    }
    public static ResourceLocation id(String path) { return new ResourceLocation("asterismarcanum",path); }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.copy(Blocks.STONE).requiresCorrectToolForDrops(); }
    private static RegistryObject<Item> item(String id,Supplier<Item> factory) { return ITEMS.register(id,factory); }
    private static void block(String name,Supplier<Block> factory) {
        RegistryObject<Block> block=BLOCKS.register(name,factory);BUILDING.put(name,block);
        item(name,()->new BlockItem(block.get(),new Item.Properties()));
    }
    public static Item get(String id) { return ForgeRegistries.ITEMS.getValue(id(id)); }
    public static void register(IEventBus bus) { BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus); }
    private static final class CombBlock extends DirectionalBlock {
        private CombBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP)); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getNearestLookingDirection().getOpposite()); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    }
    private ArcanumContent() {}
}
