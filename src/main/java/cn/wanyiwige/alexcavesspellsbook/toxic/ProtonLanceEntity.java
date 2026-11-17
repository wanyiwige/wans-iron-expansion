package cn.wanyiwige.alexcavesspellsbook.toxic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

public final class ProtonLanceEntity extends ToxicEffectEntity {
    private final Map<UUID,Integer> hits=new HashMap<>();
    public ProtonLanceEntity(EntityType<?> type,Level level){super(type,level,ToxicSpec.LANCE);}
    @Override protected void serverTick(ServerLevel level,LivingEntity owner) {
        Vec3 from=owner.getEyePosition().add(0,-.15,0),end=ToxicCombat.end(level,owner,24);
        setPos(from);vector("End",end);
        if(age!=1&&age!=6&&age!=11&&age!=16)return;
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.3),e->ToxicCombat.enemy(owner,e)).stream()
                .sorted(Comparator.comparingDouble(e->e.distanceToSqr(from))).limit(32).toList()) {
            if(!target.getBoundingBox().inflate(.15).contains(from)&&target.getBoundingBox().inflate(.15).clip(from,end).isEmpty())continue;
            if(hits.getOrDefault(target.getUUID(),0)>=4||!ToxicCombat.visible(level,from,target.getBoundingBox().getCenter(),this))continue;
            if(ToxicCombat.damage(this,target,.3F)){hits.merge(target.getUUID(),1,Integer::sum);mark(target);}
        }
    }
}
