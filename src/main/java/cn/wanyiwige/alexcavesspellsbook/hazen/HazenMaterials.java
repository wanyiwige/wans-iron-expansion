package cn.wanyiwige.alexcavesspellsbook.hazen;

import io.redspace.ironsspellbooks.item.UpgradeOrbItem;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HazenMaterials {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,"alex_caves_spellsbook");
    public static final Map<String,RegistryObject<Item>> ENTRIES=new LinkedHashMap<>();
    static {
        for(String school:new String[]{"cosmic","radiance","shadow","hydro"}){
            ENTRIES.put(school+"_rune",ITEMS.register("hazen_"+school+"_rune",()->new Item(new Item.Properties())));
            ENTRIES.put(school+"_upgrade_orb",ITEMS.register("hazen_"+school+"_upgrade_orb",()->new UpgradeOrbItem(
                    new Item.Properties().rarity(Rarity.UNCOMMON),ResourceKey.create(UpgradeOrbTypeRegistry.UPGRADE_ORB_REGISTRY_KEY,
                    HazenEquipment.id("hazen/"+school+"_power")))));
        }
        try(var reader=new java.io.InputStreamReader(HazenMaterials.class.getResourceAsStream("/hazen-materials.json"),java.nio.charset.StandardCharsets.UTF_8)){
            for(var spec:new com.google.gson.Gson().fromJson(reader,MaterialSpec[].class)){
                if(!spec.kind.equals("material"))continue;
                ENTRIES.put(spec.id,ITEMS.register("hazen_"+spec.id,()->{
                    var properties=new Item.Properties().rarity(HazenRarities.get(spec.rarity));
                    if(spec.fire_resistant)properties.fireResistant();
                    return new Item(properties);
                }));
            }
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    private record MaterialSpec(String id,String kind,String rarity,boolean fire_resistant){}
    public static void register(IEventBus bus){ITEMS.register(bus);}
    public static void addToTab(CreativeModeTab.Output out){if(HazenConfig.ENABLED.get())ENTRIES.values().forEach(item->out.accept(item.get()));}
    public static Ingredient repairIngredient(){
        var key=new ResourceLocation("hazennstuff","zenalite_ingot");
        return ForgeRegistries.ITEMS.containsKey(key)?Ingredient.of(ForgeRegistries.ITEMS.getValue(key)):Ingredient.EMPTY;
    }
    private HazenMaterials(){}
}
