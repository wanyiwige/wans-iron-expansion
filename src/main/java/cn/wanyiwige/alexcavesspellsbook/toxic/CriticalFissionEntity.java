package cn.wanyiwige.alexcavesspellsbook.toxic;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class CriticalFissionEntity extends ToxicEffectEntity {
    private UUID targetUUID;
    public CriticalFissionEntity(EntityType<?> type,Level level){super(type,level,ToxicSpec.FISSION);}
    @Override protected void setup(LivingEntity owner) {
        LivingEntity target=ToxicCombat.aimedEnemy((ServerLevel)level(),owner,20);
        if(target!=null){targetUUID=target.getUUID();setPos(target.getBoundingBox().getCenter());value("Target",target.getId());}
    }
    @Override protected void serverTick(ServerLevel level,LivingEntity owner) {
        if(age!=1)return;
        Entity raw=targetUUID==null?null:level.getEntity(targetUUID);
        if(!(raw instanceof LivingEntity target)||!ToxicCombat.enemy(owner,target)||owner.distanceToSqr(target)>400
                ||!ToxicCombat.visible(level,owner.getEyePosition(),target.getBoundingBox().getCenter(),this)){discard();return;}
        setPos(target.getBoundingBox().getCenter());
        int stacks=ToxicMarks.count(target,ownerUUID(),level.getGameTime());
        if(ToxicCombat.damage(this,target,1+.4F*stacks)) {
            ToxicMarks.consume(target,ownerUUID(),level.getGameTime());value("Stacks",stacks);value("Detonated",1);
            if(target.hasEffect(ACEffectRegistry.IRRADIATED.get()))
                for(LivingEntity nearby:ToxicCombat.sphere(level,owner,position(),3))if(nearby!=target)ToxicCombat.damage(this,nearby,.3F);
        }
    }
}
