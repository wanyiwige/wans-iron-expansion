package cn.wanyiwige.twilight;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.*;

public final class TwilightParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,TwilightFoundation.NAMESPACE);
    public static final RegistryObject<SimpleParticleType> EXANIMATED_FIRE = type("exanimated_fire");
    public static final RegistryObject<SimpleParticleType> EXANIMATED_SMOKE = type("exanimated_smoke");
    public static final RegistryObject<SimpleParticleType> EXANIMATED_EMBERS = type("exanimated_embers");
    public static final RegistryObject<SimpleParticleType> HYDRA_FIRE = type("hydra_fire");
    public static final RegistryObject<SimpleParticleType> HYDRA_EMBERS = type("hydra_embers");
    public static final RegistryObject<SimpleParticleType> MOSQUITO = type("mosquito");
    private static RegistryObject<SimpleParticleType> type(String name) { return PARTICLES.register(name,()->new SimpleParticleType(false)); }
    private TwilightParticles() {}
}
