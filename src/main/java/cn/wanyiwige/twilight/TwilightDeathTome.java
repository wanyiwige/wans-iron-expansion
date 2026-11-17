package cn.wanyiwige.twilight;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
/** Native Twilight combat plus spell-summon ownership and lifecycle. */
public final class TwilightDeathTome extends twilightforest.entity.monster.DeathTome implements IMagicSummon {
 public TwilightDeathTome(EntityType type,Level world){super(type,world);xpReward=0;}
 @Override protected void registerGoals(){super.registerGoals();TwilightSummons.goals(this,this,goalSelector,targetSelector);}
 @Override public Entity getSummoner(){return TwilightSummons.owner(this);}
 @Override public boolean isAlliedTo(Entity target){return target==this || target==getSummoner() || isAlliedHelper(target);}
 @Override public boolean hurt(DamageSource source,float amount){return !shouldIgnoreDamage(source)&&super.hurt(source,amount);}
 @Override public boolean doHurtTarget(Entity target){return Utils.doMeleeAttack(this,target,TwilightSpells.get("summon_death_library").getDamageSource(this,getSummoner()));}
 @Override protected boolean shouldDropLoot(){return false;}
 @Override public void onUnSummon(){TwilightSummons.dismiss(this);}
 @Override public void die(DamageSource source){onDeathHelper();super.die(source);}
 @Override public void onRemovedFromWorld(){onRemovedHelper(this);super.onRemovedFromWorld();}
}
