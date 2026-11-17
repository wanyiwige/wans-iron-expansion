package cn.wanyiwige.alexcavesspellsbook.mixin;

import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records the two spell identities involved in Taboo's optional staff casting path. */
@Pseudo
@Mixin(targets = "com.core.dream_sakura.taboo.compat.irons.qx7wvy48o", remap = false)
public abstract class TabooStaffSelectionTraceMixin {
    private static final Logger CANDY_TRACE = LogUtils.getLogger();

    @Inject(method = "useSelectedSpell", at = @At("HEAD"), remap = false, require = 0)
    private static void candy$before(Level level, Player player, InteractionHand hand, ItemStack stack,
                                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        var selection = new SpellSelectionManager(player).getSelection();
        String selected = selection == null || selection.spellData == null || selection.spellData.getSpell() == null
                ? "none" : selection.spellData.getSpell().getSpellId();
        String contents = ISpellContainer.isSpellContainer(stack)
                ? ISpellContainer.get(stack).getActiveSpells().stream()
                    .map(slot -> slot.getSpell().getSpellId()).limit(35).toList().toString() : "no-container";
        CANDY_TRACE.info("[CANDY_TABOO_CAST] side={} hand={} selected={} contents={}",
                level.isClientSide ? "client" : "server", hand, selected, contents);
    }

    @Inject(method = "useSelectedSpell", at = @At("RETURN"), remap = false, require = 0)
    private static void candy$after(Level level, Player player, InteractionHand hand, ItemStack stack,
                                    CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        CANDY_TRACE.info("[CANDY_TABOO_CAST] side={} hand={} result={}",
                level.isClientSide ? "client" : "server", hand, callback.getReturnValue().getResult());
    }
}
