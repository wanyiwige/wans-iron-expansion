package cn.wanyiwige.alexcavesspellsbook.traveller;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TravellerItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TravellerAttributes.NAMESPACE);
    public static final RegistryObject<Item> TRAVELLER_CODEX = ITEMS.register("traveller_codex",
            () -> new TravellerCodexItem(new Item.Properties().stacksTo(1)));

    private TravellerItems() {
    }
}
