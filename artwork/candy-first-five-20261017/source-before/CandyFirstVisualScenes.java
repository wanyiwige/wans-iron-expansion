package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import static cn.wanyiwige.alexcavesspellsbook.client.render.MagneticMeshes.*;

/** Distinct staged scenes for the first batch. All geometry follows synchronized server state. */
public final class CandyFirstVisualScenes {
    private static final ResourceLocation HEX=ResourceLocation.fromNamespaceAndPath("alexscaves","textures/entity/sugar_staff_hex.png");
    private static final ResourceLocation MINT=ResourceLocation.fromNamespaceAndPath("alexscaves","textures/block/large_peppermint_front.png");
    private static final ResourceLocation WHITE=CandyRegistry.id("textures/entity/magnetic_mesh.png");
    private CandyFirstVisualScenes(){ }
    public static boolean render(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource buffers,int alpha,boolean detail){
        if(e.visual().getInt("Program")==0)return false;
        String id=e.visual().getString("Spell");int role=e.visual().getInt("Role");
        VertexConsumer b=buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        int color=CandyVisuals.COLORS[Math.floorMod(e.visual().getInt("Color"),12)];
        if(e.stage()==CandyEffectEntity.Stage.START){
            ring(p,b,Vec3.ZERO,0.2+age*0.03,0.03,0,age*0.15,CandyVisuals.CREAM,alpha,16);
        }
        if(role==CandySpellEntity.CONTROL){
            if(e.stage()==CandyEffectEntity.Stage.END)ring(p,b,Vec3.ZERO,0.25+(age-e.visual().getInt("StageAge"))*0.08,0.025,0,0,color,alpha,16);
            return true;
        }
        double radius=Math.max(0.1,e.visual().getFloat("Radius"));
        if(role==CandySpellEntity.BALL){
            core(p,b,Vec3.ZERO,0.16,color,alpha);
            if(id.equals("popping_cluster")){
                float fuse=e.visual().getInt("Fuse");
                ring(p,b,Vec3.ZERO,0.24+0.035*Math.sin(fuse*0.8),0.025,1.1,age*0.1,CandyVisuals.CREAM,alpha,16);
                core(p,b,new Vec3(0,0.2,0),0.04,CandyVisuals.BERRY,alpha);
            }else{
                Vec3 velocity=e.getDeltaMovement();
                if(detail)rainbowTrail(p,b,velocity.scale(-0.6),color,alpha);
            }
        }else if(role==CandySpellEntity.MINT){
            VertexConsumer peppermint=buffers.getBuffer(RenderType.entityTranslucent(MINT));
            p.pushPose();p.mulPose(Axis.XP.rotationDegrees(90));p.mulPose(Axis.YP.rotation(age*0.2F));
            disc(p,peppermint,0.35,0,0xFFFFFF,alpha);disc(p,peppermint,0.35,0.045,0xFFFFFF,alpha);p.popPose();
            ring(p,b,Vec3.ZERO,0.36,0.025,Math.PI/2,age*0.1,CandyVisuals.MINT,alpha,16);
            if(e.visual().getInt("Orbiting")==0&&detail)rainbowTrail(p,b,e.getDeltaMovement().scale(-0.6),CandyVisuals.MINT,alpha);
        }else if(role==CandySpellEntity.SPEAR){
            beam(p,b,new Vec3(0,-0.65,0),new Vec3(0,0.65,0),0.035,CandyVisuals.CREAM,alpha);
            for(int i=0;i<4;i++)ring(p,b,new Vec3(0,-0.4+i*0.25,0),0.08,0.028,0,age*0.2+i,CandyVisuals.MINT,alpha,8);
            // Four tapered faces make the downward spear point readable at a distance.
            Vec3 tip=new Vec3(0,-0.95,0);
            for(int i=0;i<4;i++){
                double a=i*Math.PI/2,c=(i+1)*Math.PI/2;
                Vec3 v=new Vec3(Math.cos(a)*0.16,-0.4,Math.sin(a)*0.16),w=new Vec3(Math.cos(c)*0.16,-0.4,Math.sin(c)*0.16);
                quad(p,b,tip,v,w,tip,CandyVisuals.MINT,alpha);
            }
            if(detail)ring(p,b,new Vec3(0,0.3,0),0.26,0.02,0,age*0.2,CandyVisuals.CREAM,alpha,12);
        }else if(role==CandySpellEntity.ROCKET){
            p.pushPose();Vec3 v=e.getDeltaMovement();
            float yaw=(float)Math.atan2(v.x,v.z),pitch=(float)Math.atan2(Math.sqrt(v.x*v.x+v.z*v.z),v.y);
            p.mulPose(Axis.YP.rotation(yaw));p.mulPose(Axis.XP.rotation(pitch));
            box(p,b,Vec3.ZERO,0.14,0.28,0.14,CandyVisuals.BERRY,alpha);
            ring(p,b,new Vec3(0,0.05,0),0.15,0.05,0,0,CandyVisuals.CREAM,alpha,12);
            core(p,b,new Vec3(0,0.33,0),0.11,CandyVisuals.MINT,alpha);
            rainbowTrail(p,b,new Vec3(0,-1,0),CandyVisuals.MINT,alpha);p.popPose();
        }else if(role==CandySpellEntity.AREA){
            switch(id){
                case "caramel_bog" -> {
                    disc(p,b,radius,0.01,0xBD795C,alpha/2);
                    for(int i=0;i<(detail?3:1);i++)ring(p,b,new Vec3(0,0.02+i*0.015,0),radius*(1-i*0.12),0.04,0,age*0.01,0xEAA66A,alpha/(i+1),detail?32:16);
                    if(detail)for(int i=0;i<6;i++){double a=age*0.015+i*Math.PI/3;core(p,b,new Vec3(Math.cos(a)*radius*0.7,0.05,Math.sin(a)*radius*0.7),0.05,CandyVisuals.CREAM,alpha/2);}
                }
                case "soda_frostburst" -> {
                    disc(p,b,radius,0.02,0x8D54BD,alpha/3);
                    ring(p,b,Vec3.ZERO,radius,0.06,0,age*0.03,CandyVisuals.MINT,alpha,detail?32:16);
                    for(int i=0;i<(detail?6:2);i++){double a=i*Math.PI/3;core(p,b,new Vec3(Math.cos(a)*radius*0.65,0.2+(age*0.03+i*0.2)%0.6,Math.sin(a)*radius*0.65),0.09,CandyVisuals.MINT,alpha/2);}
                }
                case "frostmint_rain" -> {
                    ring(p,b,Vec3.ZERO,radius,0.035,0,age*0.01,CandyVisuals.MINT,alpha/2,detail?32:16);
                    if(detail)for(int i=0;i<6;i++){double a=i*Math.PI/3;ring(p,b,new Vec3(Math.cos(a)*1.6,0.03,Math.sin(a)*1.6),0.3,0.02,0,age*0.05,CandyVisuals.CREAM,alpha/2,12);}
                }
                default -> {
                    VertexConsumer hex=buffers.getBuffer(RenderType.entityTranslucentEmissive(HEX));
                    double scale=radius;
                    if(id.equals("sugar_seal_bloom"))scale=radius*Math.min(1,Math.max(0.2,age/20));
                    for(int i=0;i<CandyVisuals.layers(e);i++){
                        p.pushPose();p.mulPose(Axis.YP.rotation(age*0.025F*(i%2==0?1:-1)));
                        disc(p,hex,scale*(1-i*0.12),0.04+i*(e.visual().getInt("FinalPulse")==1?0.3:0.1),CandyVisuals.BERRY,alpha/(i+1));p.popPose();
                    }
                    ring(p,b,Vec3.ZERO,scale,0.04,0,0,CandyVisuals.CREAM,alpha,detail?32:16);
                    if(id.equals("sugar_seal_bloom")&&e.visual().getInt("Bloom")>0){
                        int i=e.visual().getInt("BloomRing");double r=radius*(i+1)/3;
                        ring(p,b,new Vec3(0,0.12,0),r,0.12,0,age*0.06,CandyVisuals.MINT,alpha,detail?32:16);
                    }
                }
            }
        }
        if(e.stage()==CandyEffectEntity.Stage.IMPACT||e.stage()==CandyEffectEntity.Stage.END){
            double t=Math.max(0,age-e.visual().getInt("StageAge"));
            double burst=e.visual().contains("BurstRadius")?e.visual().getFloat("BurstRadius"):0.5;
            ring(p,b,Vec3.ZERO,burst*Math.min(1,t/6),0.055,0,age*0.03,color,alpha,detail?32:16);
            if(detail&&t<3)core(p,b,Vec3.ZERO,0.15+0.06*t,CandyVisuals.CREAM,alpha/2);
        }
        return true;
    }
    private static void rainbowTrail(PoseStack p,VertexConsumer b,Vec3 end,int color,int alpha){
        beam(p,b,Vec3.ZERO,end,0.035,color,alpha/2);
        beam(p,b,end,end.scale(1.7),0.018,CandyVisuals.MINT,alpha/3);
    }
    /** Disk triangles keep the visual boundary circular, matching the server's horizontal radius. */
    private static void disc(PoseStack p,VertexConsumer b,double radius,double y,int color,int alpha){
        Vec3 center=new Vec3(0,y,0);
        for(int i=0;i<24;i++){
            double a=i*Math.PI/12,c=(i+1)*Math.PI/12;
            Vec3 v=new Vec3(Math.cos(a)*radius,y,Math.sin(a)*radius),w=new Vec3(Math.cos(c)*radius,y,Math.sin(c)*radius);
            vertex(p,b,center,color,alpha,0.5F,0.5F);
            vertex(p,b,v,color,alpha,(float)(0.5+0.5*Math.cos(a)),(float)(0.5+0.5*Math.sin(a)));
            vertex(p,b,w,color,alpha,(float)(0.5+0.5*Math.cos(c)),(float)(0.5+0.5*Math.sin(c)));
            vertex(p,b,center,color,alpha,0.5F,0.5F);
            vertex(p,b,center,color,alpha,0.5F,0.5F);
            vertex(p,b,w,color,alpha,(float)(0.5+0.5*Math.cos(c)),(float)(0.5+0.5*Math.sin(c)));
            vertex(p,b,v,color,alpha,(float)(0.5+0.5*Math.cos(a)),(float)(0.5+0.5*Math.sin(a)));
            vertex(p,b,center,color,alpha,0.5F,0.5F);
        }
    }
    private static void vertex(PoseStack p,VertexConsumer b,Vec3 v,int color,int alpha,float u,float texV){
        b.vertex(p.last().pose(),(float)v.x,(float)v.y,(float)v.z).color((color>>16)&255,(color>>8)&255,color&255,alpha)
                .uv(u,texV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.last().normal(),0,1,0).endVertex();
    }
}
