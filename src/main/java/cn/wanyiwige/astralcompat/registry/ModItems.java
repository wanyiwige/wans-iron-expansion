package cn.wanyiwige.astralcompat.registry;

import cn.wanyiwige.astralcompat.AstralSpellCompat;
import cn.wanyiwige.astralcompat.item.AstralLanceItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Player weapon registry.  The model is a separate authored asset, not a spell particle. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AstralSpellCompat.MOD_ID);

    public static final RegistryObject<Item> ASTRAL_LANCE = ITEMS.register("astral_lance",
            () -> new AstralLanceItem(Tiers.DIAMOND, 7, -2.6F,
                    new Item.Properties().durability(1780).rarity(Rarity.RARE).fireResistant()));

    private ModItems() { }
}
