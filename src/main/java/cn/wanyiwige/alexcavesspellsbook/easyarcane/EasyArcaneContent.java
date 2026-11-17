package cn.wanyiwige.alexcavesspellsbook.easyarcane;

import com.mojang.serialization.Codec;
import cn.wanyiwige.alexcavesspellsbook.registry.ModCreativeTabs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;

/** Content recreation from Easy Arcane 1.0.0; the supplied models and textures are retained. */
public final class EasyArcaneContent {
    public static final String NS = "easy_arcane";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, NS);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NS);
    private static final DeferredRegister<Codec<? extends BiomeModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, NS);
    public static final RegistryObject<Block> ARCANE_ORE = BLOCKS.register("arcane_ore", () -> new ArcaneOre(false));
    public static final RegistryObject<Block> DEEPSLATE_ARCANE_ORE = BLOCKS.register("deepslate_arcane_ore", () -> new ArcaneOre(true));
    public static final RegistryObject<Item> ORE_ITEM = ITEMS.register("arcane_ore", () -> new BlockItem(ARCANE_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEEPSLATE_ORE_ITEM = ITEMS.register("deepslate_arcane_ore", () -> new BlockItem(DEEPSLATE_ARCANE_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GLOW_INK = ITEMS.register("bottle_of_glow_ink", () -> new Item(new Item.Properties()) {
        @Override public boolean isFoil(ItemStack stack) { return true; }
    });
    public static final RegistryObject<Item> ENCHANTED_INK = ITEMS.register("bottle_of_enchanted_ink", () -> new Item(new Item.Properties()));
    private static final RegistryObject<Codec<? extends BiomeModifier>> ORES = MODIFIERS.register("add_ores", () -> Ores.CODEC);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MODIFIERS.register(bus);
        bus.addListener(EasyArcaneContent::setup);
        bus.addListener(EasyArcaneContent::creative);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> BrewingRecipeRegistry.addRecipe(Ingredient.of(GLOW_INK.get()),
                Ingredient.of(Items.NETHER_WART), new ItemStack(ENCHANTED_INK.get())));
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ModCreativeTabs.ALEX_CAVES_SPELLSBOOK.getKey())) {
            event.accept(ORE_ITEM.get());
            event.accept(DEEPSLATE_ORE_ITEM.get());
            event.accept(GLOW_INK.get());
            event.accept(ENCHANTED_INK.get());
        } else if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)) {
            event.accept(ORE_ITEM.get());
            event.accept(DEEPSLATE_ORE_ITEM.get());
        } else if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(GLOW_INK.get());
            event.accept(ENCHANTED_INK.get());
        }
    }

    private static final class ArcaneOre extends Block {
        private ArcaneOre(boolean deepslate) {
            super(properties(deepslate));
        }
        private static BlockBehaviour.Properties properties(boolean deepslate) {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .mapColor(deepslate ? MapColor.DEEPSLATE : MapColor.STONE)
                    .strength(3, 10).requiresCorrectToolForDrops();
            return deepslate ? properties.sound(SoundType.DEEPSLATE).noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false) : properties;
        }
        @Override public int getExpDrop(BlockState state, LevelReader level, RandomSource random,
                                        BlockPos pos, int fortune, int silkTouch) {
            return random.nextInt(4) + 2;
        }
        @Override public net.minecraft.world.phys.shapes.VoxelShape getVisualShape(BlockState state,
                net.minecraft.world.level.BlockGetter level, BlockPos pos,
                net.minecraft.world.phys.shapes.CollisionContext context) {
            return state.is(DEEPSLATE_ARCANE_ORE.get()) ? net.minecraft.world.phys.shapes.Shapes.empty()
                    : super.getVisualShape(state, level, pos, context);
        }
    }

    /** Original uses an unrestricted biome selector; rock replacement still limits actual generation. */
    public record Ores(HolderSet<PlacedFeature> features) implements BiomeModifier {
        public static final Codec<Ores> CODEC = PlacedFeature.LIST_CODEC.fieldOf("features")
                .xmap(Ores::new, Ores::features).codec();
        @Override public Codec<? extends BiomeModifier> codec() { return ORES.get(); }
        @Override public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase == Phase.ADD) features.forEach(feature ->
                    builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature));
        }
    }
    private EasyArcaneContent() {}
}
