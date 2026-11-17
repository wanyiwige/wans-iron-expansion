package cn.wanyiwige.alexcavesspellsbook.item;

import cn.wanyiwige.alexcavesspellsbook.registry.ModSpells;
import cn.wanyiwige.alexcavesspellsbook.spells.DarkSparkSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** A handheld prism and a twelve-slot Iron's Spellbooks spellbook in one item. */
public final class DarkSparkPrismItem extends SpellBook {
    private static final int BUILT_IN_LEVEL = 3;

    public DarkSparkPrismItem(Item.Properties properties) {
        super(12, properties);
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || ISpellContainer.isSpellContainer(stack)) return;
        ISpellContainerMutable container = ISpellContainer.create(getMaxSpellSlots(), true, true).mutableCopy();
        if (!container.addSpell(ModSpells.DARK_SPARK.get(), BUILT_IN_LEVEL, true)) {
            throw new IllegalStateException("Could not install Dark Spark in its prism");
        }
        ISpellContainer.set(stack, container.toImmutable());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return super.use(level, player, hand);
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        var spell = ModSpells.DARK_SPARK.get();
        String slot = hand == InteractionHand.MAIN_HAND
                ? SpellSelectionManager.MAINHAND : SpellSelectionManager.OFFHAND;
        boolean started = spell.attemptInitiateCast(stack, spell.getLevelFor(BUILT_IN_LEVEL, player),
                level, player, CastSource.SPELLBOOK, false, slot);
        if (started) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (user instanceof ServerPlayer player) {
            MagicData data = MagicData.getPlayerMagicData(player);
            if (data.isCasting() && DarkSparkSpell.SPELL_ID.equals(data.getCastingSpellId())
                    && data.getPlayerCastingItem() == stack) {
                Utils.serverSideCancelCast(player);
            }
        }
        super.releaseUsing(stack, level, user, timeLeft);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
