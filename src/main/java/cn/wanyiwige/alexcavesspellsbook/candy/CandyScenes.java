package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Entry point for future spell implementations and isolated visual samples. No debug command is registered. */
public final class CandyScenes {
    private CandyScenes() { }
    public static CandyEffectEntity start(ServerLevel level, LivingEntity owner, CandySpecs.Spec spec,
                                          int spellLevel, float power, CandyEffectEntity.Mode mode,
                                          Vec3 origin, Vec3 anchor, Vec3 second, Vec3 velocity) {
        if (!owner.isAlive() || owner.level()!=level || !finite(origin) || !finite(anchor) || !finite(second)
                || !finite(velocity) || origin.distanceToSqr(owner.position())>32*32
                || anchor.distanceToSqr(origin)>32*32 || second.distanceToSqr(origin)>32*32) return null;
        CandyEffectEntity entity=CandyRegistry.EFFECT.get().create(level);
        if(entity==null) return null;
        entity.configure(spec,spellLevel,power,mode,anchor,second); entity.setPos(origin);
        entity.setDeltaMovement(velocity.lengthSqr()>4 ? velocity.normalize().scale(2) : velocity);
        CandySession session=new CandySession(level,owner,spec);
        return session.spawn(entity,null) ? entity : null;
    }
    /** A visible, bounded sample for each mode. Used only when a caller explicitly spawns it. */
    public static CandyEffectEntity sample(ServerLevel level, LivingEntity owner, CandyEffectEntity.Mode mode) {
        CandySpecs.Spec spec=CandySpecs.byId("candy_banquet");
        Vec3 from=owner.position().add(0,1,0), to=CandyCombat.aim(level,owner,8);
        return start(level,owner,spec,1,1,mode,from,from,to,Vec3.ZERO);
    }
    private static boolean finite(Vec3 vector) { return Double.isFinite(vector.x)&&Double.isFinite(vector.y)&&Double.isFinite(vector.z); }
}
