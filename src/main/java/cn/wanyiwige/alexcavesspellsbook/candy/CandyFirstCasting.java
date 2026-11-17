package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.Comparator;

public final class CandyFirstCasting {
    private CandyFirstCasting(){ }
    public static boolean groundSpell(String id){return switch(id){
        case "licorice_hex_wave","sugar_seal_bloom","frostmint_rain","soda_frostburst","caramel_bog"->true;default->false;};}
    public static Vec3 aim(ServerLevel level,LivingEntity owner,double range){
        Vec3 eye=owner.getEyePosition(),end=CandyCombat.aim(level,owner,range);
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(eye,end).inflate(0.5),e->CandyCombat.enemy(owner,e))
                .stream().map(e->e.getBoundingBox().inflate(0.2).clip(eye,end)).flatMap(java.util.Optional::stream)
                .min(Comparator.comparingDouble(eye::distanceToSqr)).orElse(end);
    }
    public static Vec3 ground(ServerLevel level,Vec3 point,double rise,double drop,LivingEntity owner){
        BlockHitResult hit=level.clip(new ClipContext(point.add(0,rise,0),point.add(0,-drop,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
        return hit.getType()==HitResult.Type.BLOCK&&!hit.isInside()&&hit.getDirection()==Direction.UP?hit.getLocation().add(0,0.03,0):null;
    }
    public static boolean canCast(ServerLevel level,LivingEntity owner,CandySpecs.Spec spec){
        if(!owner.isAlive()||!CandySession.canStart(level,owner,spec.entityBudget()))return false;
        return !groundSpell(spec.id())||ground(level,aim(level,owner,spec.range()),0.5,6,owner)!=null;
    }
    public static boolean cast(ServerLevel level,LivingEntity owner,CandySpecs.Spec spec,int spellLevel,float power){
        if(!canCast(level,owner,spec))return false;
        Vec3 target=aim(level,owner,spec.range());
        if(groundSpell(spec.id())){target=ground(level,target,0.5,6,owner);if(target==null)return false;}
        Vec3 from=owner.getEyePosition();
        CandyEffectEntity.Mode mode=CandyEffectEntity.Mode.GLYPH;
        Vec3 origin=groundSpell(spec.id())?target:from;
        if(spec.id().equals("licorice_hex_wave")){
            origin=ground(level,owner.position().add(owner.getLookAngle().multiply(1,0,1).scale(1)),0.5,3,owner);
            if(origin==null)return false;
        }
        if(spec.id().equals("caramel_bog"))mode=CandyEffectEntity.Mode.GROUND;
        if(spec.id().equals("soda_meteor"))mode=CandyEffectEntity.Mode.ROCKET;
        CandyEffectEntity effect=CandyScenes.start(level,owner,spec,spellLevel,power,mode,origin,origin,target,Vec3.ZERO);
        if(!(effect instanceof CandySpellEntity spell))return false;
        spell.begin(target,owner.getLookAngle());
        return true;
    }
}
