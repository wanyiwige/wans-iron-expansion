package cn.wanyiwige.twilight;

import com.google.common.collect.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class TwilightBorealBow extends BowItem {
    public TwilightBorealBow(){super(TwilightEquipment.properties("boreal_bow").durability(451));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(!player.isCreative()&&MagicData.getPlayerMagicData(player).getMana()<90){if(!level.isClientSide)player.displayClientMessage(Component.translatable("ui.wans_twilight_spellbooks.not_enough_mana"),true);return InteractionResultHolder.fail(stack);}
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity caster,int remaining){
        if(level.isClientSide || !(caster instanceof Player player))return;
        float charge=BowItem.getPowerForTime(getUseDuration(stack)-remaining);if(charge<.1)return;
        MagicData magic=MagicData.getPlayerMagicData(player);float cost=90*charge;
        if(!player.isCreative()&&magic.getMana()<cost)return;
        TwilightProjectile arrow=TwilightEntities.PROJECTILES.get("ice_arrow").get().create(level);
        arrow.setOwner(caster);arrow.setPos(caster.getEyePosition());arrow.shoot(caster.getLookAngle());
        double arrowBonus=1;
        var attr=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation("irons_lib","arrow_damage"));
        if(attr!=null&&caster.getAttribute(attr)!=null)arrowBonus=caster.getAttributeValue(attr);
        arrow.setDamage((float)(20*charge*caster.getAttributeValue(AttributeRegistry.ICE_SPELL_POWER.get())*arrowBonus));
        arrow.setExplosionRadius(1);level.addFreshEntity(arrow);
        if(!player.isCreative())magic.addMana(-cost);
        stack.hurtAndBreak(1,caster,e->e.broadcastBreakEvent(caster.getUsedItemHand()));
        level.playSound(null,caster.blockPosition(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1,1);
    }
    @Override public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){
        Multimap<Attribute,AttributeModifier> attributes=HashMultimap.create(super.getDefaultAttributeModifiers(slot));
        if(slot==EquipmentSlot.MAINHAND)TwilightEquipment.modifiers("boreal_bow").forEach(attributes::put);return attributes;
    }
}
