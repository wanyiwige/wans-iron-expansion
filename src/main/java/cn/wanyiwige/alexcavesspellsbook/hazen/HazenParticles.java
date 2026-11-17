package cn.wanyiwige.alexcavesspellsbook.hazen;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HazenParticles {
    private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,"alex_caves_spellsbook");
    public static final Map<String,RegistryObject<SimpleParticleType>> ENTRIES=new LinkedHashMap<>();
    static {
        for (String name:new String[]{"star_impact_particle","cosmic_ember_particle","nights_edge_particle","rod_of_discord_particle","leaf_particle","nights_edge_strike"})
            ENTRIES.put(name,REGISTRY.register("hazen_"+name,()->new SimpleParticleType(false)));
        try(var reader=new java.io.InputStreamReader(HazenParticles.class.getResourceAsStream("/hazen-secondary-particles.json"),java.nio.charset.StandardCharsets.UTF_8)){
            for(String id:new com.google.gson.Gson().fromJson(reader,String[].class)){
                String name="secondary_"+id;
                ENTRIES.put(name,REGISTRY.register("hazen_"+name,()->new SimpleParticleType(false)));
            }
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    public static SimpleParticleType get(String name){return ENTRIES.get(name).get();}
    public static void register(IEventBus bus){REGISTRY.register(bus);}
    public static void spawn(Level level,String name,Vec3 pos,int count,double x,double y,double z,double speed){
        if(level instanceof ServerLevel server)server.sendParticles(get(name),pos.x,pos.y,pos.z,count,x,y,z,speed);
    }
    public static void slash(Level level,Vec3 pos,Vec3 forward){
        // A count of zero sends the normalized direction as a single particle's velocity.
        spawn(level,"nights_edge_strike",pos,0,forward.x,forward.y,forward.z,1);
    }
    public static void ring(Level level,Vec3 center,double radius,double speed){
        for(int i=0;i<40;i++){
            double angle=Math.PI*2*i/40,x=Math.cos(angle),z=Math.sin(angle);
            spawn(level,"rod_of_discord_particle",center.add(x*radius,.2,z*radius),0,x*speed,.05,z*speed,1);
        }
    }
    private HazenParticles(){}
}
