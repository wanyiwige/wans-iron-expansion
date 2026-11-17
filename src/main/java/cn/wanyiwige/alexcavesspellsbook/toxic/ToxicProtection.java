package cn.wanyiwige.alexcavesspellsbook.toxic;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Separate finite ward pool; never overwrites another mod's absorption hearts or resistance effects. */
@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID)
public final class ToxicProtection {
    private static final String KEY="WanToxicProtection";
    public static void grant(LivingEntity target,float points,int duration,String source) {
        CompoundTag all=target.getPersistentData().getCompound(KEY);
        float own=remaining(target,source),other=remaining(target)-own;
        points=Math.min(40-other,Math.max(0,points));
        if(own>points)return;
        CompoundTag tag=new CompoundTag();tag.putFloat("Points",points);tag.putLong("Until",target.level().getGameTime()+duration);
        all.put(source,tag);target.getPersistentData().put(KEY,all);
    }
    public static float remaining(LivingEntity target) {
        return remaining(target,"ward")+remaining(target,"cleanse");
    }
    public static float remaining(LivingEntity target,String source) {
        CompoundTag tag=target.getPersistentData().getCompound(KEY).getCompound(source);
        return tag.getLong("Until")>target.level().getGameTime()?Math.max(0,tag.getFloat("Points")):0;
    }
    public static void removeWard(LivingEntity owner) {
        CompoundTag all=owner.getPersistentData().getCompound(KEY);all.remove("ward");owner.getPersistentData().put(KEY,all);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide||event.getAmount()<=0||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        LivingEntity target=event.getEntity();CompoundTag all=target.getPersistentData().getCompound(KEY);
        float damage=event.getAmount();
        for(String source:new String[]{"ward","cleanse"}) {
            float amount=remaining(target,source),absorbed=Math.min(amount,damage);if(absorbed<=0)continue;
            CompoundTag tag=all.getCompound(source);tag.putFloat("Points",amount-absorbed);all.put(source,tag);damage-=absorbed;
        }
        target.getPersistentData().put(KEY,all);event.setAmount(damage);
    }
    private ToxicProtection(){}
}
