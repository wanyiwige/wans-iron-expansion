package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> MAGNETIC_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "magnetic_magic"));

    public static final ResourceKey<DamageType> CLOUD_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "cloud_magic"));

    public static final ResourceKey<DamageType> PURE_DARKNESS_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "pure_darkness_magic"));

    private ModDamageTypes() {
    }
}
