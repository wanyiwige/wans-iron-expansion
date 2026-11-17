package cn.wanyiwige.alexcavesspellsbook.hazen;

import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class HazenRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,"alex_caves_spellsbook");
    private static final RegistryObject<RecipeSerializer<Smithing>> SMITHING=SERIALIZERS.register("hazen_smithing",Serializer::new);
    public static void register(IEventBus bus){
        SERIALIZERS.register(bus);
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->CraftingHelper.register(new EnabledSerializer())));
    }
    private static final class Enabled implements ICondition {
        private final String item;
        private Enabled(String item){this.item=item;}
        @Override public ResourceLocation getID(){return HazenEquipment.id("hazen_enabled");}
        @Override public boolean test(IContext context){return HazenConfig.ENABLED.get()&&HazenConfig.RECIPES.get()
                &&(!item.equals("@survival")||HazenSurvival.enabled())
                &&!HazenConfig.DISABLED_ITEMS.get().contains(item)
                &&(!HazenEquipment.WEAPONS.containsKey(item)||HazenConfig.itemEnabled(item))
                &&(!HazenArmor.ITEMS.containsKey(item)||HazenArmor.ITEMS.get(item).get().enabled());}
    }
    private static final class EnabledSerializer implements IConditionSerializer<Enabled>{
        public void write(JsonObject json,Enabled value){json.addProperty("item",value.item);}
        public Enabled read(JsonObject json){return new Enabled(GsonHelper.getAsString(json,"item",""));}
        public ResourceLocation getID(){return HazenEquipment.id("hazen_enabled");}
    }
    /** Preserve base enchantments/name but replace its spell container with the result's preset. */
    public static final class Smithing extends SmithingTransformRecipe{
        private final Ingredient templatePart,basePart,additionPart;private final ItemStack resultPart;
        private Smithing(ResourceLocation id,Ingredient template,Ingredient base,Ingredient addition,ItemStack result){
            super(id,template,base,addition,result);templatePart=template;basePart=base;additionPart=addition;resultPart=result;
        }
        @Override public ItemStack assemble(Container inventory,RegistryAccess access){
            var result=super.assemble(inventory,access);
            if(result.getItem() instanceof IPresetSpellContainer preset){
                // Armor keeps the player's existing imbue; weapons install their own fixed preset.
                if(!(result.getItem() instanceof HazenArmorItem))ISpellContainer.remove(result);
                preset.initializeSpellContainer(result);
            }
            return result;
        }
        @Override public RecipeSerializer<?> getSerializer(){return SMITHING.get();}
    }
    private static final class Serializer implements RecipeSerializer<Smithing>{
        public Smithing fromJson(ResourceLocation id,JsonObject json){return new Smithing(id,
                Ingredient.fromJson(json.get("template")),Ingredient.fromJson(json.get("base")),Ingredient.fromJson(json.get("addition")),
                ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json,"result")));}
        public Smithing fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new Smithing(id,Ingredient.fromNetwork(buffer),Ingredient.fromNetwork(buffer),Ingredient.fromNetwork(buffer),buffer.readItem());}
        public void toNetwork(FriendlyByteBuf buffer,Smithing recipe){recipe.templatePart.toNetwork(buffer);recipe.basePart.toNetwork(buffer);recipe.additionPart.toNetwork(buffer);buffer.writeItem(recipe.resultPart);}
    }
    private HazenRecipes(){}
}
