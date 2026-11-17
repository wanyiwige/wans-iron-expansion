package cn.wanyiwige.alexcavesspellsbook.mixin.brutality;

import cn.wanyiwige.alexcavesspellsbook.compat.SupernovaReactorSupport;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.goo.brutality.entity.base.BrutalityAbstractThrowingProjectile", remap = false)
public abstract class SupernovaDirectDamageMixin {
    @Inject(method = "getDamageMultiplier()F", at = @At("RETURN"), cancellable = true, remap = false)
    private void wan$supernovaDirectDamage(CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(SupernovaReactorSupport.scaleDirectDamage((Entity) (Object) this,
                callback.getReturnValue()));
    }
}
