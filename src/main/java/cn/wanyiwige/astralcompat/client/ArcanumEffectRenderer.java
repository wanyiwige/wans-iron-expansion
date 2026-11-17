package cn.wanyiwige.astralcompat.client;

import cn.wanyiwige.astralcompat.recreation.ArcanumEffect;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class ArcanumEffectRenderer extends EntityRenderer<ArcanumEffect> {
    private static final ResourceLocation ORB=new ResourceLocation("asterismarcanum","recreation/textures/entity/starcutter/starcutter.png");
    private static final ResourceLocation ECHO=new ResourceLocation("asterismarcanum","recreation/textures/entity/astral_echo/astral_echo.png");
    private final ModelPart orb;
    public ArcanumEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        orb=context.bakeLayer(new ModelLayerLocation(new ResourceLocation("irons_spellbooks","ball_lightning_model"),"main")).getChild("orb");
    }
    @Override public boolean shouldRender(ArcanumEffect e,net.minecraft.client.renderer.culling.Frustum f,double x,double y,double z) {
        return e.mode()==ArcanumEffect.BEAM || super.shouldRender(e,f,x,y,z);
    }
    @Override public void render(ArcanumEffect e,float yaw,float partial,PoseStack s,MultiBufferSource b,int light) {
        s.pushPose(); float time=e.tickCount+partial;
        if(e.mode()==ArcanumEffect.CUTTER) {
            s.translate(0,.4,0);
            for(int i=0;i<3;i++) {
                s.pushPose(); float scale=1-i*.2F; s.scale(scale,scale,scale);
                s.mulPose(Axis.XP.rotationDegrees(time*(4+i))); s.mulPose(Axis.YP.rotationDegrees(time*(6+i)));
                orb.render(s,b.getBuffer(RenderType.energySwirl(ORB,0,0)),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,.9F,.85F,.55F,1);
                s.popPose();
            }
        } else if(e.mode()==ArcanumEffect.ECHO) {
            s.translate(0,.8,0); s.mulPose(entityRenderDispatcher.cameraOrientation());
            // The upstream 64x640 strip selects frame 6 in its mcmeta. Entity
            // textures do not use the block atlas animation, so select its UVs.
            VertexConsumer v=b.getBuffer(RenderType.entityTranslucentEmissive(ECHO));
            textured(s,v,-.65F,-.65F,0,0,.7F,220,210,255,190);
            textured(s,v,.65F,-.65F,0,1,.7F,220,210,255,190);
            textured(s,v,.65F,.65F,0,1,.6F,220,210,255,190);
            textured(s,v,-.65F,.65F,0,0,.6F,220,210,255,190);
        } else if(e.mode()==ArcanumEffect.BEAM) {
            s.mulPose(Axis.YP.rotationDegrees(-e.getYRot())); s.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
            VertexConsumer v=b.getBuffer(RenderType.lightning());
            beam(s,v,e.length(),.16F,135,120,255,150);
            beam(s,v,e.length(),.045F,250,245,210,245);
        } else if(e.mode()==ArcanumEffect.SWARM) {
            s.mulPose(Axis.YP.rotationDegrees(-e.getYRot())); s.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
            for(int i=0;i<5;i++) {
                s.pushPose(); s.translate(Math.cos(i*.8)*2,Math.sin(i*.8)*2,0);
                ring(s,b.getBuffer(RenderType.lightning()),.28F,.05F,time*2,160,140,255,200);s.popPose();
            }
        } else {
            s.translate(0,e.mode()==ArcanumEffect.TETHER?1:0,0);
            s.mulPose(Axis.XP.rotationDegrees(90));
            float radius=e.mode()==ArcanumEffect.BURST?Math.min(1,time/12)*e.size():.9F;
            ring(s,b.getBuffer(RenderType.lightning()),radius,.065F,time*2,190,165,255,175);
            if(e.mode()==ArcanumEffect.TETHER) {
                s.mulPose(Axis.XP.rotationDegrees(90)); ring(s,b.getBuffer(RenderType.lightning()),1.15F,.035F,-time,245,225,160,160);
            }
        }
        s.popPose(); super.render(e,yaw,partial,s,b,light);
    }
    private static void beam(PoseStack s,VertexConsumer v,float length,float width,int r,int g,int b,int a) {
        Matrix4f m=s.last().pose();
        vertex(v,m,-width,0,0,r,g,b,a);vertex(v,m,width,0,0,r,g,b,a);vertex(v,m,width,0,length,r,g,b,a);vertex(v,m,-width,0,length,r,g,b,a);
        vertex(v,m,0,-width,0,r,g,b,a);vertex(v,m,0,width,0,r,g,b,a);vertex(v,m,0,width,length,r,g,b,a);vertex(v,m,0,-width,length,r,g,b,a);
    }
    private static void ring(PoseStack s,VertexConsumer v,float radius,float width,float rotation,int r,int g,int b,int a) {
        Matrix4f m=s.last().pose();
        for(int i=0;i<48;i++) {
            double x=(i/48.0)*Math.PI*2+Math.toRadians(rotation),y=((i+1)/48.0)*Math.PI*2+Math.toRadians(rotation);
            vertex(v,m,(float)Math.cos(x)*(radius-width),(float)Math.sin(x)*(radius-width),0,r,g,b,a);
            vertex(v,m,(float)Math.cos(x)*radius,(float)Math.sin(x)*radius,0,r,g,b,a);
            vertex(v,m,(float)Math.cos(y)*radius,(float)Math.sin(y)*radius,0,r,g,b,a);
            vertex(v,m,(float)Math.cos(y)*(radius-width),(float)Math.sin(y)*(radius-width),0,r,g,b,a);
        }
    }
    private static void vertex(VertexConsumer v,Matrix4f m,float x,float y,float z,int r,int g,int b,int a) { v.vertex(m,x,y,z).color(r,g,b,a).endVertex(); }
    static void quad(PoseStack s,VertexConsumer v,float w,float h,int r,int g,int b,int a) {
        textured(s,v,-w,-h,0,0,1,r,g,b,a);textured(s,v,w,-h,0,1,1,r,g,b,a);
        textured(s,v,w,h,0,1,0,r,g,b,a);textured(s,v,-w,h,0,0,0,r,g,b,a);
    }
    private static void textured(PoseStack s,VertexConsumer v,float x,float y,float z,float u,float t,int r,int g,int b,int a) {
        v.vertex(s.last().pose(),x,y,z).color(r,g,b,a).uv(u,t).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(s.last().normal(),0,0,1).endVertex();
    }
    @Override public ResourceLocation getTextureLocation(ArcanumEffect e) { return e.mode()==ArcanumEffect.ECHO?ECHO:ORB; }
}
