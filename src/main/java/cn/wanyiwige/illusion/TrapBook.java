package cn.wanyiwige.illusion;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.*;
public final class TrapBook extends Item implements IPresetSpellContainer {
    public TrapBook(){super(new Properties().stacksTo(1));}
    @Override public void initializeSpellContainer(ItemStack stack){if(!ISpellContainer.isSpellContainer(stack))ISpellContainer.set(stack,ISpellContainer.create(1,true,true));}
    @Override public InteractionResult useOn(UseOnContext context){
        Player p=context.getPlayer();if(p==null)return InteractionResult.FAIL;Level level=context.getLevel();BlockPos clicked=context.getClickedPos();
        if(p.isShiftKeyDown()&&level.getBlockEntity(clicked) instanceof IllusionBlockEntity b&&b.getBlockState().is(IllusionContent.TRAP.get())){if(!level.isClientSide&&p.getUUID().equals(b.owner))level.removeBlock(clicked,false);return InteractionResult.sidedSuccess(level.isClientSide);}
        var container=ISpellContainer.get(context.getItemInHand());if(container.isEmpty()){p.displayClientMessage(Component.translatable("message.illusionist_grimoire.trap.no_spell_bound"),true);return InteractionResult.FAIL;}
        SpellData spell=container.getSpellAtIndex(0);if(spell==null||!IllusionBlockEntity.allowed(spell.getSpell())){p.displayClientMessage(Component.translatable("message.illusionist_grimoire.trap.spell_not_allowed"),true);return InteractionResult.FAIL;}
        Direction face=context.getClickedFace();BlockPos pos=clicked.relative(face);
        if(!p.mayUseItemAt(pos,face,context.getItemInHand())||!level.mayInteract(p,pos)||!level.getBlockState(pos).canBeReplaced()||!level.getBlockState(clicked).isFaceSturdy(level,clicked,face))return InteractionResult.FAIL;
        if(!level.isClientSide){
            if(!PictureBook.pay(p,spell.getSpell().getManaCost(spell.getLevel())+1.2F))return InteractionResult.FAIL;
            level.setBlock(pos,IllusionContent.TRAP.get().defaultBlockState().setValue(IllusionBlock.FACING,face),3);
            if(level.getBlockEntity(pos) instanceof IllusionBlockEntity b){b.owner=p.getUUID();b.spell=spell.getSpell().getSpellResource();b.rank=spell.getLevel();b.captureAttributes(p);b.setChanged();level.sendBlockUpdated(pos,b.getBlockState(),b.getBlockState(),3);}
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean isFoil(ItemStack stack){return !ISpellContainer.get(stack).isEmpty();}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.illusionist_grimoire.trap.usage"));}
}
