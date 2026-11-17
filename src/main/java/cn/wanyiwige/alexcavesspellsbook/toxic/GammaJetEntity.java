package cn.wanyiwige.alexcavesspellsbook.toxic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GammaJetEntity extends ToxicEffectEntity {
    public GammaJetEntity(EntityType<?> type,Level level){super(type,level,ToxicSpec.GAMMA);}
    @Override protected void serverTick(ServerLevel level,LivingEntity owner) {
        Vec3 from=owner.getEyePosition().add(0,-.2,0);setPos(from);vector("Direction",owner.getLookAngle());
        if(age!=1&&age!=9&&age!=17)return;
        int pulse=(age-1)/8;value("Pulse",pulse);double cosine=Math.cos(Math.toRadians(15+pulse*7));
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(8),e->ToxicCombat.enemy(owner,e))
                .stream().sorted(java.util.Comparator.comparingDouble(e->e.distanceToSqr(owner))).limit(32).toList()) {
            Vec3 delta=target.getBoundingBox().getCenter().subtract(from);
            if(delta.lengthSqr()>64||delta.normalize().dot(owner.getLookAngle())<cosine
                    ||!ToxicCombat.visible(level,from,target.getBoundingBox().getCenter(),this))continue;
            if(ToxicCombat.damage(this,target,.45F)){mark(target);ToxicCombat.irradiate(owner,target,80);}
        }
    }
}
