package cn.wanyiwige.alexcavesspellsbook.toxic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.Comparator;

/** Uses a native barrel appearance, but never places a waste block or creates an uncontrolled explosion. */
public final class WasteDrumSpellEntity extends ToxicEffectEntity {
    private int impact=-1;
    public WasteDrumSpellEntity(EntityType<?> type,Level level){super(type,level,ToxicSpec.DRUM);}
    @Override protected void setup(LivingEntity owner) {
        setPos(owner.getEyePosition().add(owner.getLookAngle().scale(.5)).add(0,-.2,0));
        setDeltaMovement(owner.getLookAngle().scale(.9).add(0,.15,0));
    }
    @Override protected void serverTick(ServerLevel level,LivingEntity owner) {
        if(impact<0) {
            Vec3 from=position(),to=from.add(getDeltaMovement());
            BlockHitResult block=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            Vec3 stop=block.getLocation();
            LivingEntity victim=level.getEntitiesOfClass(LivingEntity.class,new AABB(from,stop).inflate(.4),e->ToxicCombat.enemy(owner,e)).stream()
                    .filter(e->e.getBoundingBox().inflate(.3).contains(from)||e.getBoundingBox().inflate(.3).clip(from,stop).isPresent())
                    .min(Comparator.comparingDouble(e->e.distanceToSqr(from))).orElse(null);
            if(victim!=null) {setPos(victim.position());if(ToxicCombat.damage(this,victim,.8F))mark(victim);land();}
            else if(block.getType()!=HitResult.Type.MISS) {setPos(stop.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.04)));land();}
            else {setPos(to);setDeltaMovement(getDeltaMovement().add(0,-.045,0));}
            if(impact<0&&age>=40) {discard();return;}
        }
        if(impact>=0&&age-impact>=120){discard();return;}
        if(impact>=0&&(age-impact)%20==0) {
            for(LivingEntity target:ToxicCombat.sphere(level,owner,position(),3)) {
                if(ToxicCombat.damage(this,target,.2F)){mark(target);ToxicCombat.irradiate(owner,target,60);}
            }
        }
    }
    private void land(){
        // A wall hit spills down to the nearest floor rather than suspending a horizontal decal.
        Vec3 start=position().add(0,.1,0);
        BlockHitResult floor=level().clip(new ClipContext(start,start.add(0,-6,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if(floor.getType()==HitResult.Type.BLOCK)setPos(floor.getLocation().add(0,.015,0));
        else {discard();return;}
        impact=age;value("Impact",age);setDeltaMovement(Vec3.ZERO);life=age+120;
    }
}
