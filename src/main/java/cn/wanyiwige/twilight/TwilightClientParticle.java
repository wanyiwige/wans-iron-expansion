package cn.wanyiwige.twilight;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public final class TwilightClientParticle extends TextureSheetParticle {
    private final SpriteSet sprites;private final boolean glowing;
    TwilightClientParticle(ClientLevel world,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,String name){
        super(world,x,y,z,dx,dy,dz);this.sprites=sprites;glowing=!name.contains("smoke")&&!name.equals("mosquito");
        xd=dx;yd=dy+(name.contains("smoke")?.02:0);zd=dz;
        lifetime=name.equals("mosquito")?16:name.contains("embers")?25:20;quadSize=name.equals("mosquito")?.08F:name.contains("smoke")?.45F:.2F;
        hasPhysics=false;setSpriteFromAge(sprites);
    }
    @Override public void tick(){super.tick();setSpriteFromAge(sprites);alpha=Math.max(0,1-age/(float)lifetime);}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    @Override public int getLightColor(float partial){return glowing?0xF000F0:super.getLightColor(partial);}
    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites,String name){return(type,world,x,y,z,dx,dy,dz)->new TwilightClientParticle(world,x,y,z,dx,dy,dz,sprites,name);}
}
