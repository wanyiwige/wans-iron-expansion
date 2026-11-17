package cn.wanyiwige.alexcavesspellsbook.mixin.brutality;

import cn.wanyiwige.alexcavesspellsbook.compat.SupernovaReactorSupport;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "net.goo.brutality.entity.projectile.trident.physics_projectile.BiomechReactor", remap = false)
public abstract class SupernovaExplosionDamageMixin {
    // These are the production SRG names verified in Brutality 0.8.5's bytecode.
    @ModifyArg(method = "m_6532_(Lnet/minecraft/world/phys/HitResult;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    remap = false), index = 1, remap = false)
    private float wan$supernovaExplosionDamage(float original) {
        return SupernovaReactorSupport.getAreaDamage((Entity) (Object) this, original);
    }
}
