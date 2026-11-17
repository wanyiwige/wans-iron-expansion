package cn.wanyiwige.astralcompat.recreation;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.*;
import net.minecraft.sounds.*;

public final class MothspawnProjectile extends ThrowableItemProjectile {
    public MothspawnProjectile(EntityType<? extends MothspawnProjectile> type,Level level) { super(type,level); }
    public MothspawnProjectile(Level level,LivingEntity owner) { super(ArcanumContent.THROWN_EGG.get(),owner,level); }
    public MothspawnProjectile(Level level,double x,double y,double z) { super(ArcanumContent.THROWN_EGG.get(),x,y,z,level); }
    @Override protected Item getDefaultItem() { return ArcanumContent.MOTHSPAWN.get(); }
    @Override protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if(!level().isClientSide && hit.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING,80,1));living.addEffect(new MobEffectInstance(MobEffects.LEVITATION,80,1));
        }
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if(!level().isClientSide) {
            if(random.nextInt(8)==0) {
                int count=random.nextInt(32)==0?4:1;
                for(int i=0;i<count;i++) {
                    ArcanumInsect moth=ArcanumContent.LUNAR_MOTH.get().create(level());
                    if(moth!=null) { moth.setPos(position());level().addFreshEntity(moth); }
                }
            }
            level().broadcastEntityEvent(this,(byte)3);discard();
        }
    }
    public static final class EggItem extends Item {
        public EggItem() { super(new Item.Properties().stacksTo(16)); }
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
            ItemStack stack=player.getItemInHand(hand);
            level.playSound(null,player.blockPosition(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.5F,1);
            if(!level.isClientSide) {
                MothspawnProjectile egg=new MothspawnProjectile(level,player);egg.setItem(stack);egg.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.5F,1);level.addFreshEntity(egg);
            }
            if(!player.getAbilities().instabuild)stack.shrink(1);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
    }
}
