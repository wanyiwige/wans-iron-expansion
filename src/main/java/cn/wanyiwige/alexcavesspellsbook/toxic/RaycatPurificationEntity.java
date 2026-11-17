package cn.wanyiwige.alexcavesspellsbook.toxic;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class RaycatPurificationEntity extends ToxicEffectEntity {
    private UUID targetUUID;
    public RaycatPurificationEntity(EntityType<?> type,Level level){super(type,level,ToxicSpec.PURIFICATION);}
    @Override protected void setup(LivingEntity owner) {
        LivingEntity target=ToxicCombat.aimedFriend((ServerLevel)level(),owner,12);targetUUID=target.getUUID();value("Target",target.getId());
    }
    @Override protected void serverTick(ServerLevel level,LivingEntity owner) {
        Entity raw=level.getEntity(targetUUID);
        if(!(raw instanceof LivingEntity target)||!target.isAlive()||owner.distanceToSqr(target)>144){discard();return;}
        setPos(owner.getBoundingBox().getCenter());vector("End",target.getBoundingBox().getCenter());
        if(age!=1||!ToxicCombat.visible(level,owner.getEyePosition(),target.getBoundingBox().getCenter(),owner))return;
        if(target!=owner&&ToxicCombat.enemy(owner,target))return;
        MobEffectInstance radiation=target.getEffect(ACEffectRegistry.IRRADIATED.get());
        // Keep special high-amplifier blue radiation variants intact.
        if(radiation==null||radiation.getAmplifier()!=0)return;
        if(target.removeEffect(ACEffectRegistry.IRRADIATED.get())) {
            value("Success",1);
            long now=level.getGameTime();
            if(target.getPersistentData().getLong("WanToxicCleanseUntil")<=now) {
                ToxicProtection.grant(target,spec.cleanseProtection(rank),80,"cleanse");
                target.getPersistentData().putLong("WanToxicCleanseUntil",now+200);
            }
        }
    }
}
