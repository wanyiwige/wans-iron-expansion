package cn.wanyiwige.illusion;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.*;
public final class PictureBook extends Item {
    public PictureBook(){super(new Properties().stacksTo(1));}
    public static int selected(ItemStack stack){return stack.hasTag()?Math.max(0,Math.min(7,stack.getTag().getInt("Selected"))):0;}
    public static CompoundTag slot(ItemStack stack,int slot){return stack.hasTag()?stack.getTag().getCompound("Pictures").getCompound(Integer.toString(slot)):new CompoundTag();}
    public static void clear(ItemStack stack,int slot){stack.getOrCreateTag().getCompound("Pictures").remove(Integer.toString(slot));}
    public static boolean pay(Player player,float amount){
        if(player.isCreative()&&!ServerConfigs.CREATIVE_MANA_COST.get())return true;
        MagicData magic=MagicData.getPlayerMagicData(player);if(magic.getMana()<amount){player.displayClientMessage(Component.translatable("message.illusionist_grimoire.disguise.no_mana"),true);return false;}
        magic.setMana(magic.getMana()-amount);if(player instanceof ServerPlayer p)io.redspace.ironsspellbooks.setup.Messages.sendToPlayer(new io.redspace.ironsspellbooks.network.SyncManaPacket(magic),p);return true;
    }
    public static LivingEntity target(Player player,double range){
        Vec3 start=player.getEyePosition(),end=start.add(player.getLookAngle().scale(range));
        HitResult block=player.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));end=block.getLocation();
        LivingEntity nearest=null;double best=start.distanceToSqr(end);
        for(LivingEntity entity:player.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1),e->e!=player&&e.isAlive())){
            var hit=entity.getBoundingBox().inflate(.3).clip(start,end);if(hit.isPresent()&&start.distanceToSqr(hit.get())<best){nearest=entity;best=start.distanceToSqr(hit.get());}
        }
        return nearest;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);if(level.isClientSide)return InteractionResultHolder.success(stack);
        int index=selected(stack);ItemStack off=player.getOffhandItem();var spells=ISpellContainer.get(off);
        boolean bind=hand==InteractionHand.MAIN_HAND&&off.getItem() instanceof io.redspace.ironsspellbooks.item.Scroll&&!spells.isEmpty()&&spells.getSpellAtIndex(0).getSpell()==IllusionContent.SPELL_MAP.get("disguise").get();
        if(bind){
            LivingEntity target=target(player,10);if(target==null){player.displayClientMessage(Component.translatable("message.illusionist_grimoire.picture_book.no_entity_targeted"),true);return InteractionResultHolder.fail(stack);}
            if(IllusionState.tagged(target,"disguise_blacklist")){player.displayClientMessage(Component.translatable("message.illusionist_grimoire.disguise.blacklisted_mob"),true);return InteractionResultHolder.fail(stack);}
            if(!pay(player,60))return InteractionResultHolder.fail(stack);
            CompoundTag tag=stack.getOrCreateTag();if(!tag.contains("Pictures",10))tag.put("Pictures",new CompoundTag());tag.getCompound("Pictures").put(Integer.toString(index),IllusionState.capture(target));
            if(!player.isCreative())off.shrink(1);player.displayClientMessage(Component.translatable("message.illusionist_grimoire.picture_book.entity_bound",index+1,target.getDisplayName()),true);
        }else{
            CompoundTag stored=slot(stack,index);if(stored.isEmpty()){player.displayClientMessage(Component.translatable("message.illusionist_grimoire.picture_book.no_entity_in_slot"),true);return InteractionResultHolder.fail(stack);}
            if(!pay(player,60))return InteractionResultHolder.fail(stack);IllusionState.disguise(player,stored,1200);
        }
        player.getCooldowns().addCooldown(this,10);return InteractionResultHolder.success(stack);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){int index=selected(stack);lines.add(Component.translatable("tooltip.illusionist_grimoire.picture_book.selected_slot",index+1));CompoundTag data=slot(stack,index);lines.add(data.isEmpty()?Component.translatable("tooltip.illusionist_grimoire.picture_book.empty"):Component.translatable("tooltip.illusionist_grimoire.picture_book.bound",data.getString("Name")));lines.add(Component.translatable("tooltip.illusionist_grimoire.picture_book.usage"));}
}
