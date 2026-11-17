package cn.wanyiwige.astralcompat.registry;

import cn.wanyiwige.astralcompat.AstralSpellCompat;
import cn.wanyiwige.astralcompat.entity.AstralAnchorEntity;
import cn.wanyiwige.astralcompat.entity.AstralBoltEntity;
import cn.wanyiwige.astralcompat.entity.PiercingStarEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AstralSpellCompat.MOD_ID);

    public static final RegistryObject<EntityType<AstralBoltEntity>> ASTRAL_BOLT = ENTITY_TYPES.register(
            "astral_bolt",
            () -> EntityType.Builder.<AstralBoltEntity>of(AstralBoltEntity::new, MobCategory.MISC)
                    .sized(0.45F, 0.45F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(AstralSpellCompat.MOD_ID + ":astral_bolt")
    );

    public static final RegistryObject<EntityType<AstralAnchorEntity>> ASTRAL_ANCHOR = ENTITY_TYPES.register(
            "astral_anchor",
            () -> EntityType.Builder.<AstralAnchorEntity>of(AstralAnchorEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(96)
                    .updateInterval(1)
                    .build(AstralSpellCompat.MOD_ID + ":astral_anchor")
    );

    public static final RegistryObject<EntityType<PiercingStarEntity>> PIERCING_STAR = ENTITY_TYPES.register(
            "piercing_star", () -> EntityType.Builder.<PiercingStarEntity>of(PiercingStarEntity::new, MobCategory.MISC)
                    .sized(0.32F, 0.32F).clientTrackingRange(64).updateInterval(1)
                    .build(AstralSpellCompat.MOD_ID + ":piercing_star"));

    public static final RegistryObject<EntityType<cn.wanyiwige.astralcompat.recreation.ArcanumProjectile>> ARCANUM_PROJECTILE = ENTITY_TYPES.register(
            "arcanum_projectile", () -> EntityType.Builder.<cn.wanyiwige.astralcompat.recreation.ArcanumProjectile>of(
                    cn.wanyiwige.astralcompat.recreation.ArcanumProjectile::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(64).updateInterval(1)
                    .build(AstralSpellCompat.MOD_ID + ":arcanum_projectile"));
    public static final RegistryObject<EntityType<cn.wanyiwige.astralcompat.recreation.ArcanumEffect>> ARCANUM_EFFECT = ENTITY_TYPES.register(
            "arcanum_effect", () -> EntityType.Builder.<cn.wanyiwige.astralcompat.recreation.ArcanumEffect>of(
                    cn.wanyiwige.astralcompat.recreation.ArcanumEffect::new, MobCategory.MISC)
                    .sized(.9F, 1.8F).clientTrackingRange(64).updateInterval(1)
                    .build(AstralSpellCompat.MOD_ID + ":arcanum_effect"));

    private ModEntities() {
    }
}
