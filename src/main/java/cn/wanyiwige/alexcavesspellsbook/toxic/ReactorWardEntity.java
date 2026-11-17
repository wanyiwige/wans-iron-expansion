package cn.wanyiwige.alexcavesspellsbook.toxic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

public final class ReactorWardEntity extends ToxicEffectEntity {
    private int charges;
    public ReactorWardEntity(EntityType<?> type,Level level){super(type,level,ToxicSpec.WARD);}
    @Override protected void setup(LivingEntity owner){ToxicProtection.grant(owner,Math.min(32,power*.5F),life+2,"ward");}
    @Override protected void serverTick(ServerLevel level,LivingEntity owner) {
        setPos(owner.position().add(0,owner.getBbHeight()*.5,0));
        if(visual().getInt("Released")>0)return;
        if(ToxicProtection.remaining(owner,"ward")<=0) {value("Broken",1);life=Math.min(life,age+6);return;}
        value("Charges",charges);
        if(age==120){release(owner);value("Released",1);life=128;ToxicProtection.removeWard(owner);}
    }
    @Override protected void expire(LivingEntity owner) {
        ToxicProtection.removeWard(owner);
    }
    private void release(LivingEntity owner) {
        if(visual().getInt("Broken")==0&&charges>0)
            for(LivingEntity target:ToxicCombat.sphere((ServerLevel)level(),owner,position(),3))ToxicCombat.damage(this,target,.25F*charges);
    }
    @Override public void remove(RemovalReason reason){if(!level().isClientSide&&owner()!=null&&isCurrentCast(owner()))ToxicProtection.removeWard(owner());super.remove(reason);}
    public static void charge(ServerLevel level,LivingEntity owner) {
        for(ReactorWardEntity ward:level.getEntitiesOfClass(ReactorWardEntity.class,owner.getBoundingBox().inflate(4),
                e->owner.getUUID().equals(e.ownerUUID())&&e.visual().getInt("Broken")==0&&e.visual().getInt("Released")==0))ward.charges=Math.min(3,ward.charges+1);
    }
}
