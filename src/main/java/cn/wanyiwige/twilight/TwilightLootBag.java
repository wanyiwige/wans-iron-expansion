package cn.wanyiwige.twilight;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

public final class TwilightLootBag extends Item {
    public TwilightLootBag(){super(new Properties().stacksTo(8).fireResistant().rarity(Rarity.EPIC));}
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(world instanceof ServerLevel server){
            LootParams params=new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN,player.position()).withParameter(LootContextParams.THIS_ENTITY,player).withParameter(LootContextParams.DAMAGE_SOURCE,player.damageSources().generic()).withOptionalParameter(LootContextParams.KILLER_ENTITY,player).withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER,player).create(LootContextParamSets.ENTITY);
            for(ItemStack loot:server.getServer().getLootData().getLootTable(TwilightFoundation.id("entities/ominous_lich_bag")).getRandomItems(params))if(!player.getInventory().add(loot))player.drop(loot,false);
            if(!player.isCreative())stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
    }
}
