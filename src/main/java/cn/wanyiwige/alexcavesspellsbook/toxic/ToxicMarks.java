package cn.wanyiwige.alexcavesspellsbook.toxic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.UUID;

/** Bounded per-target, per-caster marks. Dead/restarted effects never recreate active sessions. */
public final class ToxicMarks {
    private static final String KEY="WanToxicMarks";
    public static boolean hasStoredMarks(LivingEntity target){return !target.getPersistentData().getCompound(KEY).isEmpty();}
    public static int count(LivingEntity target,UUID owner,long now) {
        if(owner==null)return 0;
        CompoundTag all=target.getPersistentData().getCompound(KEY);prune(all,now);
        CompoundTag own=all.getCompound(owner.toString());
        return own.getLong("Until")>now?Math.min(4,Math.max(0,own.getInt("Count"))):0;
    }
    public static void add(LivingEntity target,UUID owner,long now) {
        CompoundTag all=target.getPersistentData().getCompound(KEY);prune(all,now);
        String key=owner.toString();if(!all.contains(key)&&all.size()>=16)return;
        CompoundTag own=all.getCompound(key);own.putInt("Count",Math.min(4,own.getInt("Count")+1));own.putLong("Until",now+160);
        all.put(key,own);target.getPersistentData().put(KEY,all);
        refreshVisual(target,now);
    }
    public static int consume(LivingEntity target,UUID owner,long now) {
        int n=count(target,owner,now);CompoundTag all=target.getPersistentData().getCompound(KEY);all.remove(owner.toString());
        target.getPersistentData().put(KEY,all);refreshVisual(target,now);return n;
    }
    public static boolean consumeOne(LivingEntity target,UUID owner,long now) {
        int n=count(target,owner,now);if(n<=0)return false;
        CompoundTag all=target.getPersistentData().getCompound(KEY);
        CompoundTag own=all.getCompound(owner.toString());
        if(n==1)all.remove(owner.toString());else {own.putInt("Count",n-1);all.put(owner.toString(),own);}
        target.getPersistentData().put(KEY,all);refreshVisual(target,now);return true;
    }
    public static void refreshVisual(LivingEntity target,long now){
        if(target.level().isClientSide)return;
        CompoundTag all=target.getPersistentData().getCompound(KEY);prune(all,now);
        int total=0;long until=now;
        for(String key:all.getAllKeys()){CompoundTag own=all.getCompound(key);total+=own.getInt("Count");until=Math.max(until,own.getLong("Until"));}
        target.getPersistentData().put(KEY,all);
        if(total<=0){target.removeEffect(ToxicRegistry.MARK_VISUAL.get());return;}
        int count=Math.min(4,total),duration=(int)Math.min(160,until-now);
        MobEffectInstance current=target.getEffect(ToxicRegistry.MARK_VISUAL.get());
        if(current==null||current.getAmplifier()!=count-1||Math.abs(current.getDuration()-duration)>20){
            // Removing first permits decreasing the synchronized stack count.
            if(current!=null)target.removeEffect(ToxicRegistry.MARK_VISUAL.get());
            target.addEffect(new MobEffectInstance(ToxicRegistry.MARK_VISUAL.get(),duration,count-1,false,false,false));
        }
    }
    private static void prune(CompoundTag all,long now) {
        for(String key:all.getAllKeys().toArray(String[]::new))if(all.getCompound(key).getLong("Until")<=now)all.remove(key);
    }
    private ToxicMarks(){}
}
