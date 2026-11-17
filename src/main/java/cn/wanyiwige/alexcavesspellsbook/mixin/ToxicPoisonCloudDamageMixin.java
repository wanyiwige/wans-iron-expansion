package cn.wanyiwige.alexcavesspellsbook.mixin;
import cn.wanyiwige.alexcavesspellsbook.toxic.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonCloud;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PoisonCloud.class,remap=false)
public abstract class ToxicPoisonCloudDamageMixin {
    @Inject(method="applyEffect",at=@At("HEAD"),cancellable=true,remap=false)
    private void wan$toxicCloudDamage(LivingEntity target,CallbackInfo callback) {
        PoisonCloud cloud=(PoisonCloud)(Object)this;String origin=cloud.getPersistentData().getString("WanToxicOrigin");
        if(!origin.equals("poison_arrow")&&!origin.equals("poison_splash"))return;
        callback.cancel();
        if(!(cloud.getOwner() instanceof LivingEntity owner)||cloud.level().isClientSide||!ToxicCombat.enemy(owner,target))return;
        var spell=origin.equals("poison_arrow")?SpellRegistry.POISON_ARROW_SPELL.get():SpellRegistry.POISON_SPLASH_SPELL.get();
        DamageSources.ignoreNextKnockback(target);
        DamageSources.applyDamage(target,cloud.getDamage(),spell.getDamageSource(cloud,owner));
        target.addEffect(new MobEffectInstance(MobEffects.POISON,120,Math.max(0,(int)cloud.getDamage())),owner);
    }
}
