package cn.wanyiwige.alexcavesspellsbook.compat;

import cn.wanyiwige.alexcavesspellsbook.mixin.brutality.BrutalityThrowingProjectileAccessor;
import net.minecraft.world.entity.Entity;

/** Persistent marker scopes both Brutality damage hooks to this spell's reactors. */
public final class SupernovaReactorSupport {
    public static final String MARKER = "alex_caves_spellsbook:supernova_reactor";
    public static final float DAMAGE_MULTIPLIER = 100.0F;
    public static final float AREA_DAMAGE_RATIO = 0.8F;

    private SupernovaReactorSupport() {
    }

    public static void mark(Entity reactor) {
        reactor.getPersistentData().putBoolean(MARKER, true);
    }

    public static float scaleDirectDamage(Entity reactor, float original) {
        return reactor.getPersistentData().getBoolean(MARKER) ? original * DAMAGE_MULTIPLIER : original;
    }

    public static float getAreaDamage(Entity reactor, float original) {
        if (!reactor.getPersistentData().getBoolean(MARKER)) {
            return original;
        }
        if (reactor instanceof BrutalityThrowingProjectileAccessor accessor) {
            Float baseContactDamage = accessor.wan$getBaseContactDamage();
            if (baseContactDamage != null) {
                return baseContactDamage * DAMAGE_MULTIPLIER * AREA_DAMAGE_RATIO;
            }
        }
        // The accessor is required and normal reactors initialize damage in onAddedToWorld.
        // Keep a deterministic fallback if another mod changes that lifecycle.
        return original * DAMAGE_MULTIPLIER * AREA_DAMAGE_RATIO;
    }
}
