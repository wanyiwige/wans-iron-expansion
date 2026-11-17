package cn.wanyiwige.alexcavesspellsbook.candy;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

/** One guarded combat/follow loop; native fish, stealing and self-explosion goals are absent. */
public final class CandySummonBrain {
    private CandyEffectEntity parent;
    private final Mob mob;
    private final boolean sentry;
    private int attackClock;
    public CandySummonBrain(Mob mob,boolean sentry){this.mob=mob;this.sentry=sentry;}
    public void attach(CandyEffectEntity parent){this.parent=parent;}
    public void tick(){
        if(!(mob.level() instanceof ServerLevel level))return;
        Entity entity=SummonManager.getOwner(mob);
        if(!(entity instanceof LivingEntity owner)||!owner.isAlive()||parent==null||parent.isRemoved()||parent.session()==null||parent.session().ended()){
            mob.discard();return;
        }
        if(++attackClock<1)attackClock=1;
        LivingEntity target=CandyCombat.targets(level,owner,mob.position().add(0,0.5,0),Math.min(8,parent.spec().range()))
                .stream().filter(e->CandyCombat.enemy(owner,e)).findFirst().orElse(null);
        mob.setTarget(target);
        if(target==null){
            if(!sentry&&mob.distanceToSqr(owner)>12*12){mob.getNavigation().moveTo(owner,1.05);}
            else mob.getNavigation().stop();
            return;
        }
        if(sentry){
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(target);
            if(attackClock>=20&&parent instanceof CandyLastSpellEntity controller){
                attackClock=0;controller.sentryShot(mob,target);
            }
        }else{
            mob.getNavigation().moveTo(target,1.15);
            if(mob.distanceToSqr(target)<4.5&&attackClock>=20&&CandyCombat.visible(level,mob.getEyePosition(),target)){
                attackClock=0;
                CandyCombat.damageFrom(parent,target,CandyRegistry.ENTRIES.get(parent.spec().id()).get(),parent.spec().hitDamage(parent.spellLevel()),mob.getEyePosition());
                // Native animations and reactions remain active; damage comes only from the spell source above.
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }
    public void unsummon(){mob.discard();}
}
