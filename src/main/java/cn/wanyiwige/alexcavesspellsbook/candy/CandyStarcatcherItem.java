package cn.wanyiwige.alexcavesspellsbook.candy;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** A sprite-based candy focus using native spellbook slots, inscription and Curios attributes. */
public final class CandyStarcatcherItem extends SpellBook {
    public CandyStarcatcherItem() {
        super(10,new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        withSpellbookAttributes(
                new AttributeContainer(CandyRegistry.POWER,.15,AttributeModifier.Operation.MULTIPLY_BASE),
                new AttributeContainer(AttributeRegistry.MAX_MANA,200,AttributeModifier.Operation.ADDITION),
                new AttributeContainer(AttributeRegistry.MANA_REGEN,.10,AttributeModifier.Operation.MULTIPLY_BASE));
    }
    @Override public ItemStack getDefaultInstance() {
        ItemStack stack=super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }
}
