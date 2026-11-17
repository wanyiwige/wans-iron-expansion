package cn.wanyiwige.alexcavesspellsbook.hazen.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Original sprite sequences and generic/leaf physics, plus the oriented four-frame slash. */
public final class HazenParticle extends TextureSheetParticle{
    private final SpriteSet sprites;
    private final boolean slash,leaf,mirrored;
    private final Vec3[] corners;
    private float bounce,targetR,targetG,targetB;
    private int colorTicks;
    private boolean touchedGround;
    public HazenParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,String kind){
        super(level,x,y,z,dx,dy,dz);
        this.sprites=sprites;slash=kind.equals("nights_edge_strike")||kind.endsWith("slash_particle")||kind.equals("secondary_soul_flaming_strike");leaf=kind.endsWith("leaf_particle");mirrored=random.nextBoolean();
        this.xd=dx;this.yd=dy;this.zd=dz;
        if(slash){
            lifetime=4;gravity=0;friction=1;quadSize=3.25f;xd=yd=zd=0;
            Vec3 forward=new Vec3(dx,dy,dz).normalize();
            Vec3 up=Math.abs(forward.y)>.999?new Vec3(1,0,0):new Vec3(0,1,0);
            Vec3 right=forward.cross(up).normalize();
            corners=new Vec3[]{forward.scale(-1).subtract(right),forward.scale(-1).add(right),forward.add(right),forward.subtract(right)};
        }else{
            corners=null;
            if(leaf){
                lifetime=20+random.nextInt(45);gravity=1.3f;friction=.985f;quadSize=.25f;
                float tint=.9f+random.nextFloat()*.1f;targetR=.23f*tint;targetG=.78f*tint;targetB=.08f*tint;
                bounce=.6f+random.nextFloat()*.2f;colorTicks=5+random.nextInt(20);pickSprite(sprites);
            }else{scale(random.nextFloat()*.5f+.5f);lifetime=10+random.nextInt(10);gravity=-.01f;}
        }
        if(!leaf)setSpriteFromAge(sprites);
    }
    @Override protected float getU0(){return mirrored&&!slash?super.getU1():super.getU0();}
    @Override protected float getU1(){return mirrored&&!slash?super.getU0():super.getU1();}
    @Override public void tick(){
        if(slash){if(age++>=lifetime)remove();else setSpriteFromAge(sprites);return;}
        if(leaf){
            if(!touchedGround&&lifetime<30)lifetime++;
            float t=Mth.clamp((float)age/colorTicks,0,1);rCol=Mth.lerp(t,1,targetR);gCol=Mth.lerp(t,1,targetG);bCol=Mth.lerp(t,1,targetB);
            if(onGround){touchedGround=true;yd*=-bounce;bounce*=.8f;quadSize*=.9f;}
            super.tick();if(new Vec3(x-xo,y-yo,z-zo).lengthSqr()<.001)remove();
        }else{
            super.tick();xd+=(random.nextFloat()/500)*(random.nextBoolean()?1:-1);yd+=random.nextFloat()/100;zd+=(random.nextFloat()/500)*(random.nextBoolean()?1:-1);
            setSpriteFromAge(sprites);
        }
    }
    @Override public void render(VertexConsumer buffer,Camera camera,float partial){
        if(!slash){super.render(buffer,camera,partial);return;}
        Vec3 origin=new Vec3(Mth.lerp(partial,xo,x),Mth.lerp(partial,yo,y),Mth.lerp(partial,zo,z)).subtract(camera.getPosition());
        float[] u={getU1(),getU1(),getU0(),getU0()},v={getV1(),getV0(),getV0(),getV1()};
        for(int i:new int[]{0,1,2,3,3,2,1,0}){
            Vec3 vertex=origin.add(corners[i].scale(quadSize));
            buffer.vertex(vertex.x,vertex.y,vertex.z).uv(u[i],v[i]).color(rCol,gCol,bCol,alpha).uv2(0xF000F0).endVertex();
        }
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
    @Override protected int getLightColor(float partial){return 0xF000F0;}
}
