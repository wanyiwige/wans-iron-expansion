package cn.wanyiwige.twilight;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TwilightEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TwilightFoundation.NAMESPACE);
    public static final RegistryObject<EntityType<TwilightBoltProjectile>> TWILIGHT_BOLT =
            ENTITIES.register("twilight_bolt", () -> EntityType.Builder
                    .<TwilightBoltProjectile>of(TwilightBoltProjectile::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(64).updateInterval(1)
                    .build(TwilightFoundation.NAMESPACE + ":twilight_bolt"));
    public static final RegistryObject<EntityType<DruidBoltProjectile>> DRUID_BOLT =
            ENTITIES.register("extended_nature_bolt", () -> EntityType.Builder
                    .<DruidBoltProjectile>of(DruidBoltProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(64).updateInterval(1)
                    .build(TwilightFoundation.NAMESPACE + ":extended_nature_bolt"));
    public static final RegistryObject<EntityType<AuroraMissileProjectile>> AURORA_MISSILE =
            ENTITIES.register("aurora_missile", () -> EntityType.Builder
                    .<AuroraMissileProjectile>of(AuroraMissileProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(64).updateInterval(1)
                    .build(TwilightFoundation.NAMESPACE + ":aurora_missile"));

    public static final java.util.Map<String, RegistryObject<EntityType<TwilightProjectile>>> PROJECTILES=new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, RegistryObject<EntityType<TwilightAreaEntity>>> AREAS=new java.util.LinkedHashMap<>();
    static {
        for(String id:java.util.List.of("exanimated_fireball","exanimated_trident","hydra_fireball","ice_chunk","extended_ice_bomb","ice_arrow"))
            PROJECTILES.put(id,ENTITIES.register(id,()->EntityType.Builder.<TwilightProjectile>of(TwilightProjectile::new,MobCategory.MISC).sized(.75F,.75F).clientTrackingRange(64).updateInterval(1).build(TwilightFoundation.NAMESPACE+":"+id)));
        for(String id:java.util.List.of("exanimated_ray","carminite_pull","mosquito_swarm","hydra_fire_field","jet_eruption","ice_bomb_zone"))
            AREAS.put(id,ENTITIES.register(id,()->EntityType.Builder.<TwilightAreaEntity>of(TwilightAreaEntity::new,MobCategory.MISC).sized(1,1).clientTrackingRange(64).updateInterval(1).build(TwilightFoundation.NAMESPACE+":"+id)));
    }
    public static final RegistryObject<EntityType<TwilightWinterWolf>> WINTER_WOLF=ENTITIES.register("summoned_winter_wolf",()->EntityType.Builder.<TwilightWinterWolf>of(TwilightWinterWolf::new,MobCategory.MONSTER).sized(1.4F,1.9F).clientTrackingRange(64).build(TwilightFoundation.NAMESPACE+":summoned_winter_wolf"));
    public static final RegistryObject<EntityType<TwilightMinotaur>> MINOTAUR=ENTITIES.register("summoned_minotaur",()->EntityType.Builder.<TwilightMinotaur>of(TwilightMinotaur::new,MobCategory.MONSTER).sized(.6F,2.1F).clientTrackingRange(64).build(TwilightFoundation.NAMESPACE+":summoned_minotaur"));
    public static final RegistryObject<EntityType<TwilightCarminiteGolem>> CARMINITE_GOLEM=ENTITIES.register("summoned_carminite_golem",()->EntityType.Builder.<TwilightCarminiteGolem>of(TwilightCarminiteGolem::new,MobCategory.MONSTER).sized(1.4F,2.9F).clientTrackingRange(64).build(TwilightFoundation.NAMESPACE+":summoned_carminite_golem"));
    public static final RegistryObject<EntityType<TwilightDeathTome>> DEATH_TOME=ENTITIES.register("summoned_death_tome",()->EntityType.Builder.<TwilightDeathTome>of(TwilightDeathTome::new,MobCategory.MONSTER).sized(1.4F,1.9F).clientTrackingRange(64).build(TwilightFoundation.NAMESPACE+":summoned_death_tome"));
    public static final RegistryObject<EntityType<TwilightLoyalZombie>> LOYAL_ZOMBIE=ENTITIES.register("advanced_loyal_zombie",()->EntityType.Builder.<TwilightLoyalZombie>of(TwilightLoyalZombie::new,MobCategory.MONSTER).sized(.6F,1.8F).clientTrackingRange(64).build(TwilightFoundation.NAMESPACE+":advanced_loyal_zombie"));
    public static final RegistryObject<EntityType<TwilightCaster>> OMINOUS_LICH=ENTITIES.register("ominous_lich",()->EntityType.Builder.<TwilightCaster>of(TwilightCaster::new,MobCategory.MONSTER).sized(.6F,1.8F).clientTrackingRange(64).build(TwilightFoundation.NAMESPACE+":ominous_lich"));
    public static final RegistryObject<EntityType<TwilightCaster>> ADVANCED_DRUID=ENTITIES.register("advanced_druid",()->EntityType.Builder.<TwilightCaster>of(TwilightCaster::new,MobCategory.MONSTER).sized(.6F,1.8F).clientTrackingRange(64).build(TwilightFoundation.NAMESPACE+":advanced_druid"));
    public static final java.util.Map<String,RegistryObject<EntityType<TwilightSoul>>> SOULS=new java.util.LinkedHashMap<>();
    static {
        for(String name:java.util.List.of("lich_soul","snow_queen_soul","urghast_soul","hydra_soul"))SOULS.put(name,ENTITIES.register(name,()->EntityType.Builder.<TwilightSoul>of(TwilightSoul::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(64).updateInterval(1).build(TwilightFoundation.NAMESPACE+":"+name)));
    }

    private TwilightEntities() {}
}
