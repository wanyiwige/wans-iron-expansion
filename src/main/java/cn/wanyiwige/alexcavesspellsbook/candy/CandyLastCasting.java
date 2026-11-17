package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Validate terrain and arrival before opening a bounded server cast. */
public final class CandyLastCasting {
    private CandyLastCasting(){}
    public static boolean canCast(ServerLevel level,LivingEntity owner,CandySpecs.Spec spec){
        if(!owner.isAlive()||!CandySession.canStart(level,owner,spec.entityBudget()))return false;
        if(spec.id().equals("rainbow_leap"))return landing(level,owner,spec.range())!=null;
        if(spec.id().equals("candicorn_charge")||spec.id().equals("gum_worm_breach"))
            return chargeStart(level,owner)!=null && (spec.id().equals("candicorn_charge")||groundTarget(level,owner,spec)!=null);
        if(spec.id().equals("gummy_guardian")||spec.id().equals("gingerbread_march")){
            Vec3 eye=owner.getEyePosition();int count=spec.id().equals("gummy_guardian")?1:3;
            for(int i=0;i<count;i++){
                double angle=i*2.094,offset=i==0?1.5:2.2;
                Vec3 point=eye.add(Math.cos(angle)*offset,0,Math.sin(angle)*offset);
                if(CandyFirstCasting.ground(level,point.add(0,0.5,0),0.5,3,owner)==null)return false;
            }
        }
        if(CandyLastRules.groundSpell(spec.id()))return groundTarget(level,owner,spec)!=null;
        return true;
    }
    private static Vec3 groundTarget(ServerLevel level,LivingEntity owner,CandySpecs.Spec spec){
        Vec3 target=CandyFirstCasting.aim(level,owner,spec.range());
        Vec3 floor=CandyFirstCasting.ground(level,target,0.8,6,owner);
        if(floor==null||owner.position().distanceToSqr(floor)>32*32)return null;
        return floor;
    }
    private static Vec3 chargeStart(ServerLevel level,LivingEntity owner){
        Vec3 horizontal=owner.getLookAngle().multiply(1,0,1);
        if(horizontal.lengthSqr()<1e-5)return null;
        return CandyFirstCasting.ground(level,owner.position().add(horizontal.normalize().scale(1.5)),1.2,3,owner);
    }
    public static Vec3 landing(ServerLevel level,LivingEntity owner,double range){
        Vec3 eye=owner.getEyePosition(),look=owner.getLookAngle();
        for(double step=Math.min(12,range);step>=2;step-=0.5){
            Vec3 intended=eye.add(look.scale(step));
            var clip=level.clip(new ClipContext(eye,intended,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
            Vec3 point=clip.getLocation().subtract(look.scale(0.5));
            Vec3 floor=CandyFirstCasting.ground(level,point,1.5,3,owner);
            if(floor==null)continue;
            Vec3 candidate=floor.add(0,0.04,0);
            if(owner.position().distanceToSqr(candidate)<=range*range&&level.noCollision(owner,owner.getBoundingBox().move(candidate.subtract(owner.position()))))return candidate;
        }
        return null;
    }
    public static CandyLastSpellEntity cast(ServerLevel level,LivingEntity owner,CandySpecs.Spec spec,int spellLevel,float power){
        if(!canCast(level,owner,spec))return null;
        Vec3 origin=owner.getEyePosition(),goal=CandyFirstCasting.aim(level,owner,spec.range());
        if(spec.id().equals("rainbow_leap")){goal=landing(level,owner,spec.range());if(goal==null)return null;}
        if(CandyLastRules.groundSpell(spec.id())){goal=groundTarget(level,owner,spec);if(goal==null)return null;origin=goal;}
        if(spec.id().equals("candicorn_charge")||spec.id().equals("gum_worm_breach")){
            origin=chargeStart(level,owner);if(origin==null)return null;
        }
        if(spec.id().equals("sweet_overdrive")||spec.id().equals("candy_cane_whirl")){
            origin=owner.position().add(0,owner.getBbHeight()*0.55,0);goal=origin;
        }
        CandyEffectEntity.Mode mode=CandyLastRules.groundSpell(spec.id())?CandyEffectEntity.Mode.GROUND:CandyEffectEntity.Mode.GLYPH;
        CandyEffectEntity entity=CandyScenes.start(level,owner,spec,spellLevel,power,mode,origin,origin,goal,Vec3.ZERO);
        if(!(entity instanceof CandyLastSpellEntity spell))return null;
        spell.beginLast(goal,owner.getLookAngle());
        if(spec.id().equals("rainbow_leap")){
            if(owner instanceof net.minecraft.server.level.ServerPlayer player)
                player.teleportTo(level,goal.x,goal.y,goal.z,player.getYRot(),player.getXRot());
            else owner.teleportTo(goal.x,goal.y,goal.z);
            owner.fallDistance=0;
        }
        return spell;
    }
}
