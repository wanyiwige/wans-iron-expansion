package cn.wanyiwige.twilight;

import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.registries.*;

import java.util.function.Supplier;

public final class TwilightData {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,TwilightFoundation.NAMESPACE);
    public static final RegistryObject<Codec<AppendLoot>> APPEND=LOOT.register("append_loot",()->AppendLoot.CODEC.get());
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,TwilightFoundation.NAMESPACE);
    public static final RegistryObject<RecipeSerializer<Smithing>> SMITHING=RECIPES.register("smithing_transform",Smithing.Serializer::new);
    public static final class AppendLoot extends LootModifier {
        static final Supplier<Codec<AppendLoot>> CODEC=Suppliers.memoize(()->RecordCodecBuilder.create(instance->codecStart(instance).and(Codec.STRING.fieldOf("loot_table").forGetter(modifier->modifier.table)).apply(instance,AppendLoot::new)));
        private final String table;
        AppendLoot(LootItemCondition[] conditions,String table){super(conditions);this.table=table;}
        @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){context.getLevel().getServer().getLootData().getLootTable(new ResourceLocation(table)).getRandomItemsRaw(context,loot::add);return loot;}
        @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC.get();}
    }
    public static final class Smithing extends SmithingTransformRecipe {
        final Ingredient template,base,addition;final ItemStack output;
        Smithing(ResourceLocation id,Ingredient template,Ingredient base,Ingredient addition,ItemStack output){super(id,template,base,addition,output);this.template=template;this.base=base;this.addition=addition;this.output=output;}
        @Override public ItemStack assemble(Container inputs,RegistryAccess access){
            ItemStack result=output.copy();var tag=inputs.getItem(1).getTag();if(tag!=null)result.getOrCreateTag().merge(tag.copy());
            initialize(result);return result;
        }
        @Override public ItemStack getResultItem(RegistryAccess access){ItemStack result=output.copy();initialize(result);return result;}
        private void initialize(ItemStack stack){
            if(stack.getItem() instanceof IPresetSpellContainer preset)preset.initializeSpellContainer(stack);
            if(stack.getItem() instanceof TwilightEquipment.Book book){
                var container=io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(stack).mutableCopy();container.setMaxSpellCount(book.getMaxSpellSlots());
                io.redspace.ironsspellbooks.api.spells.ISpellContainer.set(stack,container.toImmutable());
            }
        }
        @Override public RecipeSerializer<?> getSerializer(){return SMITHING.get();}
        public static final class Serializer implements RecipeSerializer<Smithing>{
            public Smithing fromJson(ResourceLocation id,JsonObject json){return new Smithing(id,Ingredient.fromJson(json.get("template")),Ingredient.fromJson(json.get("base")),Ingredient.fromJson(json.get("addition")),CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json,"result"),true));}
            public Smithing fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new Smithing(id,Ingredient.fromNetwork(buffer),Ingredient.fromNetwork(buffer),Ingredient.fromNetwork(buffer),buffer.readItem());}
            public void toNetwork(FriendlyByteBuf buffer,Smithing recipe){recipe.template.toNetwork(buffer);recipe.base.toNetwork(buffer);recipe.addition.toNetwork(buffer);buffer.writeItem(recipe.output);}
        }
    }
    private TwilightData(){}
}
