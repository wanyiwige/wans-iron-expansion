package cn.wanyiwige.alexcavesspellsbook.registry;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PureDarknessParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, AlexCavesSpellsbook.MOD_ID);
    public static final RegistryObject<SimpleParticleType> SCARLET_DROP =
            TYPES.register("pure_darkness/scarlet_drop", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_CUT =
            TYPES.register("pure_darkness/blood_cut", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> BLOOD_MIST = register("blood_mist");
    public static final RegistryObject<SimpleParticleType> MOTH_SCALE = register("moth_scale");
    public static final RegistryObject<SimpleParticleType> GLOOM_MOTH = register("gloom_moth");
    public static final RegistryObject<SimpleParticleType> SOIL_CLAST = register("soil_clast");
    public static final RegistryObject<SimpleParticleType> SCARLET_FILAMENT = register("scarlet_filament");
    public static final RegistryObject<SimpleParticleType> SHADOW_SHARD = register("shadow_shard");
    public static final RegistryObject<SimpleParticleType> IRIS_FRAGMENT = register("iris_fragment");

    public static final RegistryObject<SimpleParticleType> THORN_SPLINTER = register("thorn_splinter");

    public static final RegistryObject<SimpleParticleType> SONIC_WAVEFRONT = register("sonic_wavefront");

    public static final RegistryObject<SimpleParticleType> DREAD_ARROW_TAIL = register("dread_arrow_tail");

    private static RegistryObject<SimpleParticleType> register(String path) {
        return TYPES.register("pure_darkness/" + path, () -> new SimpleParticleType(false));
    }

    private PureDarknessParticles() {}
}
