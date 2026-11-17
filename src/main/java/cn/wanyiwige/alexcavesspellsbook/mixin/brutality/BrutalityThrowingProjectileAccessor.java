package cn.wanyiwige.alexcavesspellsbook.mixin.brutality;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads Brutality's per-projectile raw contact damage after onAddedToWorld initializes it. */
@Pseudo
@Mixin(targets = "net.goo.brutality.entity.base.BrutalityAbstractThrowingProjectile", remap = false)
public interface BrutalityThrowingProjectileAccessor {
    @Accessor(value = "damage", remap = false)
    Float wan$getBaseContactDamage();
}
