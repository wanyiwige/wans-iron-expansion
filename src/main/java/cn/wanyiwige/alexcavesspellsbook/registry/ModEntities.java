package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.HeavyweightSpellEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.SpellMagneticWeaponEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.SummonedFerrouslimeEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.TeslaArcNodeEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.DipoleVortexEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.MagneticMageEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.LustrousGemProjectile;
import cn.wanyiwige.alexcavesspellsbook.entities.SaciliteSpellProjectile;
import cn.wanyiwige.alexcavesspellsbook.entities.PolarityCollapseEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.MagneticRailLanceEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.DipoleAnvilEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.MagnetizedFractureChainEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.PolarityReversalTrackerEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.FerrousSandstormEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFieldEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessRiteEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessWardEntity;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFinaleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AlexCavesSpellsbook.MOD_ID);

    public static final RegistryObject<EntityType<cn.wanyiwige.alexcavesspellsbook.entities.NebulousCataclysmEntity>> NEBULOUS_CATACLYSM =
            ENTITY_TYPES.register("nebulous_cataclysm", () -> EntityType.Builder
                    .<cn.wanyiwige.alexcavesspellsbook.entities.NebulousCataclysmEntity>of(
                            cn.wanyiwige.alexcavesspellsbook.entities.NebulousCataclysmEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(12).updateInterval(1).noSave().build("nebulous_cataclysm"));

    public static final RegistryObject<EntityType<SaciliteSpellProjectile>> SACILITE_RAIN_CRYSTAL =
            ENTITY_TYPES.register("sacilite_rain_crystal", () -> EntityType.Builder
                    .<SaciliteSpellProjectile>of(SaciliteSpellProjectile::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).clientTrackingRange(12).updateInterval(1).noSave().build("sacilite_rain_crystal"));
    public static final RegistryObject<EntityType<SaciliteSpellProjectile>> SEEKING_SACILITE_CRYSTAL =
            ENTITY_TYPES.register("seeking_sacilite_crystal", () -> EntityType.Builder
                    .<SaciliteSpellProjectile>of(SaciliteSpellProjectile::new, MobCategory.MISC)
                    .sized(0.45F, 0.7F).clientTrackingRange(12).updateInterval(1).noSave().build("seeking_sacilite_crystal"));

    public static final RegistryObject<EntityType<LustrousGemProjectile>> LUSTROUS_GEM =
            ENTITY_TYPES.register("lustrous_gem_projectile", () -> EntityType.Builder
                    .<LustrousGemProjectile>of(LustrousGemProjectile::new, MobCategory.MISC)
                    .sized(0.28F, 0.28F).clientTrackingRange(12).updateInterval(1)
                    .build("lustrous_gem_projectile"));

    public static final RegistryObject<EntityType<cn.wanyiwige.alexcavesspellsbook.entities.DesolateBloodbladeEntity>> DESOLATE_BLOODBLADE =
            ENTITY_TYPES.register("pure_darkness_desolate_bloodblade_entity", () -> EntityType.Builder
                    .of(cn.wanyiwige.alexcavesspellsbook.entities.DesolateBloodbladeEntity::new, MobCategory.MISC)
                    .sized(.23F, .23F).clientTrackingRange(8).updateInterval(1).noSave()
                    .build("pure_darkness_desolate_bloodblade_entity"));

    public static final RegistryObject<EntityType<PureDarknessFieldEntity>> PURE_DARKNESS_FIELD =
            ENTITY_TYPES.register("pure_darkness_field", () -> EntityType.Builder
                    .of(PureDarknessFieldEntity::new, MobCategory.MISC)
                    .sized(.25F, .25F).clientTrackingRange(12).updateInterval(1).noSave()
                    .build("pure_darkness_field"));

    public static final RegistryObject<EntityType<PureDarknessRiteEntity>> PURE_DARKNESS_RITE =
            ENTITY_TYPES.register("pure_darkness_rite", () -> EntityType.Builder
                    .of(PureDarknessRiteEntity::new, MobCategory.MISC)
                    .sized(.68F, .95F).clientTrackingRange(12).updateInterval(1).noSave()
                    .build("pure_darkness_rite"));

    public static final RegistryObject<EntityType<PureDarknessWardEntity>> PURE_DARKNESS_WARD =
            ENTITY_TYPES.register("pure_darkness_ward", () -> EntityType.Builder
                    .of(PureDarknessWardEntity::new, MobCategory.MISC)
                    .sized(.82F, 1.13F).clientTrackingRange(12).updateInterval(1).noSave()
                    .build("pure_darkness_ward"));

    public static final RegistryObject<EntityType<PureDarknessFinaleEntity>> PURE_DARKNESS_FINALE =
            ENTITY_TYPES.register("pure_darkness_finale", () -> EntityType.Builder
                    .of(PureDarknessFinaleEntity::new, MobCategory.MISC)
                    .sized(.33F, .45F).clientTrackingRange(14).updateInterval(1).noSave()
                    .build("pure_darkness_finale"));

    public static final RegistryObject<EntityType<MagneticMageEntity>> MAGNETIC_MAGE =
            ENTITY_TYPES.register("magnetic_mage", () -> EntityType.Builder
                    .of(MagneticMageEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3)
                    .build("magnetic_mage"));

    public static final RegistryObject<EntityType<SpellMagneticWeaponEntity>> SPELL_MAGNETIC_WEAPON =
            ENTITY_TYPES.register("spell_magnetic_weapon", () -> EntityType.Builder
                    .of(SpellMagneticWeaponEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("spell_magnetic_weapon"));

    public static final RegistryObject<EntityType<HeavyweightSpellEntity>> HEAVYWEIGHT_SPELL =
            ENTITY_TYPES.register("heavyweight_spell", () -> EntityType.Builder
                    .of(HeavyweightSpellEntity::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("heavyweight_spell"));

    public static final RegistryObject<EntityType<SummonedFerrouslimeEntity>> SUMMONED_FERROUSLIME =
            ENTITY_TYPES.register("summoned_ferrouslime", () -> EntityType.Builder
                    .of(SummonedFerrouslimeEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("summoned_ferrouslime"));

    public static final RegistryObject<EntityType<TeslaArcNodeEntity>> TESLA_ARC_NODE =
            ENTITY_TYPES.register("tesla_arc_node", () -> EntityType.Builder
                    .of(TeslaArcNodeEntity::new, MobCategory.MISC)
                    .sized(0.8F, 1.1F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("tesla_arc_node"));

    public static final RegistryObject<EntityType<DipoleVortexEntity>> DIPOLE_VORTEX =
            ENTITY_TYPES.register("dipole_vortex", () -> EntityType.Builder
                    .of(DipoleVortexEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("dipole_vortex"));

    public static final RegistryObject<EntityType<PolarityCollapseEntity>> POLARITY_COLLAPSE =
            ENTITY_TYPES.register("polarity_collapse", () -> EntityType.Builder
                    .<PolarityCollapseEntity>of(PolarityCollapseEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1).build("polarity_collapse"));

    public static final RegistryObject<EntityType<MagneticRailLanceEntity>> MAGNETIC_RAIL_LANCE =
            ENTITY_TYPES.register("magnetic_rail_lance", () -> EntityType.Builder
                    .<MagneticRailLanceEntity>of(MagneticRailLanceEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1).build("magnetic_rail_lance"));

    public static final RegistryObject<EntityType<DipoleAnvilEntity>> DIPOLE_ANVIL =
            ENTITY_TYPES.register("dipole_anvil", () -> EntityType.Builder
                    .<DipoleAnvilEntity>of(DipoleAnvilEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1).build("dipole_anvil"));

    public static final RegistryObject<EntityType<MagnetizedFractureChainEntity>> MAGNETIZED_FRACTURE_CHAIN =
            ENTITY_TYPES.register("magnetized_fracture_chain", () -> EntityType.Builder
                    .<MagnetizedFractureChainEntity>of(MagnetizedFractureChainEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1).build("magnetized_fracture_chain"));

    public static final RegistryObject<EntityType<PolarityReversalTrackerEntity>> POLARITY_REVERSAL_TRACKER =
            ENTITY_TYPES.register("polarity_reversal_tracker", () -> EntityType.Builder
                    .<PolarityReversalTrackerEntity>of(PolarityReversalTrackerEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1).build("polarity_reversal_tracker"));

    public static final RegistryObject<EntityType<FerrousSandstormEntity>> FERROUS_SANDSTORM =
            ENTITY_TYPES.register("ferrous_sandstorm", () -> EntityType.Builder
                    .<FerrousSandstormEntity>of(FerrousSandstormEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(1).build("ferrous_sandstorm"));

    private ModEntities() {
    }
}
