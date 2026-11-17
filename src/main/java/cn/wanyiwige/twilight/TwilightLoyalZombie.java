package cn.wanyiwige.twilight;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
/** Native Twilight combat plus spell-summon ownership and lifecycle. */
public final class TwilightLoyalZombie extends io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob implements IMagicSummon,io.redspace.ironsspellbooks.entity.mobs.IAnimatedAttacker {
 private software.bernie.geckolib.core.animation.RawAnimation pending;
 public TwilightLoyalZombie(EntityType type,Level world){super(type,world);xpReward=0;}
 @Override public void registerGoals(){
  goalSelector.addGoal(2,new io.redspace.ironsspellbooks.entity.mobs.wizards.GenericAnimatedWarlockAttackGoal<>(this,1.2,50,75)
   .setMoveset(java.util.List.of(new io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData(43,"sword_double_slash",13,29),new io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData(26,"sword_single_upward",13),new io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData(28,"sword_single_horizontal",12),new io.redspace.ironsspellbooks.entity.mobs.goals.melee.AttackAnimationData(21,"sword_stab",11))).setComboChance(.4F));
  TwilightSummons.goals(this,this,goalSelector,targetSelector);
 }
 @Override public void playAnimation(String animation){pending=software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay(animation);}
 @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar registrar){
  super.registerControllers(registrar);registrar.add(new software.bernie.geckolib.core.animation.AnimationController<>(this,"twilight_melee",0,state->{if(pending!=null){state.getController().forceAnimationReset();state.getController().setAnimation(pending);pending=null;}return software.bernie.geckolib.core.object.PlayState.CONTINUE;}));
 }
 @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor world,net.minecraft.world.DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,net.minecraft.nbt.CompoundTag tag){
  if(getRandom().nextFloat()<.25F)setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));return data;
 }
 @Override public Entity getSummoner(){return TwilightSummons.owner(this);}
 @Override public boolean isAlliedTo(Entity target){return target==this || target==getSummoner() || isAlliedHelper(target);}
 @Override public boolean hurt(DamageSource source,float amount){return !shouldIgnoreDamage(source)&&super.hurt(source,amount);}
 @Override public boolean doHurtTarget(Entity target){return Utils.doMeleeAttack(this,target,TwilightSpells.get("summon_loyal_zombie").getDamageSource(this,getSummoner()));}
 @Override protected boolean shouldDropLoot(){return false;}
 @Override public void onUnSummon(){TwilightSummons.dismiss(this);}
 @Override public void die(DamageSource source){onDeathHelper();super.die(source);}
 @Override public void onRemovedFromWorld(){onRemovedHelper(this);super.onRemovedFromWorld();}
}
