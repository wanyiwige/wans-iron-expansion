package cn.wanyiwige.twilight;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Material and boss-key items whose source behavior is item storage plus event handling. */
public final class TwilightItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TwilightFoundation.NAMESPACE);

    public static final RegistryObject<Item> IRONWOOD_HELVE = basic("ironwood_helve", Rarity.RARE, true);
    public static final RegistryObject<Item> AURORA_SHARD = basic("aurora_shard", Rarity.UNCOMMON, true);
    public static final RegistryObject<Item> DEATHS_ESSENCE = basic("deaths_essence", Rarity.UNCOMMON, true);
    public static final RegistryObject<Item> EXANIMATE_ESSENCE = basic("exanimate_essence", Rarity.RARE, true);
    public static final RegistryObject<Item> FROST_ESSENCE = basic("frost_essence", Rarity.COMMON, false);
    public static final RegistryObject<Item> SNOW_SILVER_INGOT = basic("snow_silver_ingot", Rarity.COMMON, false);
    public static final RegistryObject<Item> LICH_PHYLACTERY = basic("lich_phylactery", Rarity.UNCOMMON, true);
    public static final RegistryObject<Item> AURORA_PHYLACTERY = basic("aurora_phylactery", Rarity.UNCOMMON, true);
    public static final RegistryObject<Item> CARMINITE_HEART = basic("carminite_heart", Rarity.UNCOMMON, true);
    public static final RegistryObject<Item> REGENERATING_CHOP = basic("regenerating_chop", Rarity.UNCOMMON, true);

    private static RegistryObject<Item> basic(String id, Rarity rarity, boolean fireResistant) {
        return ITEMS.register(id, () -> {
            Item.Properties properties = new Item.Properties().rarity(rarity);
            return new Item(fireResistant ? properties.fireResistant() : properties);
        });
    }

    public static void register(IEventBus bus) {
        TwilightEquipment.bootstrap();
        ITEMS.register(bus);
    }

    private TwilightItems() {}
}
