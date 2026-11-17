package cn.wanyiwige.alexcavesspellsbook.item;

import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
import javax.annotation.Nullable;

/** A twelve-slot spellbook with a locked level-three Land of the Lustrous spell. */
public final class LandOfTheLustrousItem extends SpellBook {
    public LandOfTheLustrousItem(Item.Properties properties) {
        super(12, properties);
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || ISpellContainer.isSpellContainer(stack)) return;
        ISpellContainerMutable container = ISpellContainer.create(getMaxSpellSlots(), true, true).mutableCopy();
        if (!container.addSpell(ModSpells.LAND_OF_THE_LUSTROUS.get(), 3, true)) {
            throw new IllegalStateException("Could not install Land of the Lustrous in its spellbook");
        }
        ISpellContainer.set(stack, container.toImmutable());
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.alex_caves_spellsbook.land_of_the_lustrous")
                .withStyle(ChatFormatting.GRAY));
    }
}
