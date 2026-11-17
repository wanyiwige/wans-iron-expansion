package cn.wanyiwige.alexcavesspellsbook.toxic;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyCombat;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

public final class ToxicCombat {
    public static boolean enemy(LivingEntity owner,LivingEntity target){
        if(target!=null&&target.getPersistentData().hasUUID("WanRadiationLureOwner")
                &&owner.getUUID().equals(target.getPersistentData().getUUID("WanRadiationLureOwner")))return false;
        return CandyCombat.enemy(owner,target);
    }
    public static boolean visible(ServerLevel level,Vec3 from,Vec3 to,Entity context) {
        return level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,context)).getType()==HitResult.Type.MISS;
    }
    public static Vec3 end(ServerLevel level,LivingEntity owner,double range) {
        Vec3 from=owner.getEyePosition();
        return level.clip(new ClipContext(from,from.add(owner.getLookAngle().scale(range)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner)).getLocation();
    }
    public static LivingEntity aimedEnemy(ServerLevel level,LivingEntity owner,double range) {
        Vec3 from=owner.getEyePosition(),to=end(level,owner,range);
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(1),e->enemy(owner,e)).stream()
                .filter(e->e.getBoundingBox().inflate(.3).contains(from)||e.getBoundingBox().inflate(.3).clip(from,to).isPresent())
                .filter(e->visible(level,from,e.getBoundingBox().getCenter(),owner))
                .min(Comparator.comparingDouble(e->e.distanceToSqr(from))).orElse(null);
    }
    public static LivingEntity aimedFriend(ServerLevel level,LivingEntity owner,double range) {
        Vec3 from=owner.getEyePosition(),to=end(level,owner,range);
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(1),e->e!=owner&&e.isAlive()
                && (owner.isAlliedTo(e)||CandyCombat.principal(e).getUUID().equals(CandyCombat.principal(owner).getUUID()))).stream()
                .filter(e->e.getBoundingBox().inflate(.3).clip(from,to).isPresent()&&visible(level,from,e.getBoundingBox().getCenter(),owner))
                .min(Comparator.comparingDouble(e->e.distanceToSqr(from))).orElse(owner);
    }
    public static List<LivingEntity> sphere(ServerLevel level,LivingEntity owner,Vec3 center,double radius) {
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),e->enemy(owner,e)
                &&e.getBoundingBox().getCenter().distanceToSqr(center)<=radius*radius&&visible(level,center.add(0,.15,0),e.getBoundingBox().getCenter(),owner))
                .stream().sorted(Comparator.comparingDouble(e->e.distanceToSqr(center))).limit(32).toList();
    }
    public static boolean damage(ToxicEffectEntity effect,LivingEntity target,float factor) {
        return damageFrom(effect,target,factor,effect.damageOrigin());
    }
    public static boolean damageFrom(ToxicEffectEntity effect,LivingEntity target,float factor,Vec3 origin) {
        LivingEntity owner=effect.owner();
        if(owner==null||!enemy(owner,target)||!Float.isFinite(effect.power())||effect.power()<=0)return false;
        effect.damageOrigin(origin);
        try{return DamageSources.applyDamage(target,effect.power()*factor,effect.spell().getDamageSource(effect,owner).setIFrames(0));}
        finally{effect.damageOrigin(null);}
    }
    public static void irradiate(LivingEntity owner,LivingEntity target,int ticks) {
        if(enemy(owner,target)&&!target.getType().is(ACTagRegistry.RESISTS_RADIATION))
            target.addEffect(new MobEffectInstance(ACEffectRegistry.IRRADIATED.get(),ticks,0),owner);
    }
    private ToxicCombat(){}
}
