package cn.wanyiwige.alexcavesspellsbook.toxic.client;

import cn.wanyiwige.alexcavesspellsbook.toxic.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Model props have real depth; translucent elements are purpose-drawn native pixel sprites. */
public final class ToxicEffectRenderer extends EntityRenderer<ToxicEffectEntity> {
    private final ItemRenderer items;
    private static final int GREEN=0xCBEA26,PALE=0xF3FFD1;
    public ToxicEffectRenderer(EntityRendererProvider.Context context){super(context);items=context.getItemRenderer();shadowRadius=0;}
    private static ResourceLocation texture(String name){return ToxicRegistry.id("textures/entity/toxic/"+name+".png");}
    @Override public boolean shouldRender(ToxicEffectEntity e,Frustum frustum,double x,double y,double z){return e.distanceToSqr(x,y,z)<96*96;}
    @Override public ResourceLocation getTextureLocation(ToxicEffectEntity entity){return texture("spark");}
    @Override public void render(ToxicEffectEntity e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        float age=e.visualAge()+partial;
        boolean near=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(e.position())<32*32;
        switch(e.spec()) {
            case GAMMA->gamma(e,age,pose,buffers,near);
            case DRUM->drum(e,age,pose,buffers,light);
            case PURIFICATION->purification(e,age,pose,buffers);
            case LANCE->lance(e,age,pose,buffers,near);
            case FISSION->fission(e,age,pose,buffers);
            case WARD->ward(e,age,pose,buffers,light);
            default -> {}
        }
    }
    private void gamma(ToxicEffectEntity e,float age,PoseStack p,MultiBufferSource b,boolean near) {
        Vec3 forward=e.vector("Direction").normalize();if(forward.lengthSqr()<.1)return;
        Vec3 side=forward.cross(new Vec3(0,1,0)).normalize();
        float progress=(age%8)/8F;
        int rows=near?5:3;
        for(int row=0;row<rows;row++)for(int wing=-1;wing<=1;wing++) {
            double t=Mth.clamp(progress+row*.12,0,1),distance=.5+t*7;
            Vec3 at=forward.scale(distance).add(side.scale(wing*t*1.8)).add(0,Math.sin(age*.2+row)*.10,0);
            sprite(p,b,"smoke",at,.35+t*.9,220-(int)(t*95),row*.5+wing*.25);
            if(near&&row%2==0)sprite(p,b,"splat",at.add(0,.15,0),.18,200,age*.08);
        }
    }
    private void drum(ToxicEffectEntity e,float age,PoseStack p,MultiBufferSource b,int light) {
        int impact=e.visual().getInt("Impact");
        if(impact<=0){prop(p,b,ToxicRegistry.WASTE_PROP.get(),Vec3.ZERO,.65F,age*17,age*11,light);return;}
        float t=age-impact,fade=Mth.clamp((120-t)/12F,0,1);
        floorSprite(p,b,"puddle",Vec3.ZERO.add(0,.04,0),6,(int)(205*fade));
        if(t<10)sprite(p,b,"splat",new Vec3(0,.25,0),.6+t*.08,(int)(255*(1-t/10)),0);
        for(int i=0;i<5;i++) {
            double a=i*Math.PI*2/5,r=1.1+Math.sin(i*2)*.6;
            Vec3 at=new Vec3(Math.cos(a)*r,.18+((age+i*6)%24)*.018,Math.sin(a)*r);
            sprite(p,b,"spark",at,.1,(int)(180*fade),i);
        }
    }
    private void purification(ToxicEffectEntity e,float age,PoseStack p,MultiBufferSource b) {
        if(e.visual().getInt("Success")==0){sprite(p,b,"cleanse",Vec3.ZERO,.35,100,age*.08);return;}
        Vec3 target=e.vector("End").subtract(e.position());
        for(int i=0;i<9;i++) {
            double t=(age/24F+i/9.0)%1,angle=t*8+i;
            Vec3 at=target.scale(1-t).add(Math.cos(angle)*.12,Math.sin(angle)*.12,0);
            sprite(p,b,"spark",at,.07+t*.06,220,angle);
        }
        sprite(p,b,"cleanse",target,.5,Math.max(0,230-(int)age*8),age*.06);
    }
    private void lance(ToxicEffectEntity e,float age,PoseStack p,MultiBufferSource b,boolean near) {
        Vec3 end=e.vector("End").subtract(e.position());if(end.lengthSqr()<.1)return;
        float fade=Mth.clamp((24-age)/6F,0,1);
        // A continuous core remains visible at every view angle; scrolling shells add pixel modulation.
        solidBeam(p,b,Vec3.ZERO,end,.025,PALE,(int)(255*fade));
        solidBeam(p,b,Vec3.ZERO,end,.075,GREEN,(int)(110*fade));
        int sections=Math.min(24,Math.max(1,(int)Math.ceil(end.length())));
        for(int i=0;i<sections;i++) {
            double a=(double)i/sections,c=(double)(i+1)/sections;
            texturedBeam(p,b,Vec3.ZERO.add(end.scale(a)),end.scale(c),.13,"beam_segment",(int)((130+((i+(int)age/2)&1)*65)*fade));
            if(near&&i%3==0)sprite(p,b,"spark",end.scale(a),.08,(int)(200*fade),age*.12+i);
        }
        sprite(p,b,"pulse",Vec3.ZERO,.35,(int)(230*fade),age*.1);
        sprite(p,b,"burst",end,.55,(int)(240*fade),0);
        VanillaDIPixelLight.render(e,e.vector("End"),p,b,(int)(110*fade));
    }
    private void fission(ToxicEffectEntity e,float age,PoseStack p,MultiBufferSource b) {
        if(e.visual().getInt("Detonated")==0)return;
        float t=Mth.clamp(age/16F,0,1);int alpha=(int)(255*(1-t));
        sprite(p,b,"burst",Vec3.ZERO,.3+t*1.4,alpha,0);
        sprite(p,b,"pulse",Vec3.ZERO,.4+t*3,alpha,0);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            sprite(p,b,"spark",new Vec3(Math.cos(a)*t*1.7,Math.sin(a)*t*1.7,0),.13,alpha,a);
        }
    }
    private void ward(ToxicEffectEntity e,float age,PoseStack p,MultiBufferSource b,int light) {
        int broken=e.visual().getInt("Broken"),released=e.visual().getInt("Released");
        if(released>0){sprite(p,b,"pulse",Vec3.ZERO,.4+(age-120)*.7,Math.max(0,230-(int)(age-120)*28),0);return;}
        if(broken>0){sprite(p,b,"splat",Vec3.ZERO,1.1,120,age*.12);return;}
        int charges=e.visual().getInt("Charges");
        for(int i=0;i<3;i++) {
            double angle=age*.035+i*Math.PI*2/3;
            Vec3 offset=new Vec3(Math.cos(angle)*1.1,.1,Math.sin(angle)*1.1);
            prop(p,b,ToxicRegistry.REACTOR_PROP.get(),offset,.35F,0,(float)(-angle*180/Math.PI),light);
            if(i<charges)sprite(p,b,"spark",offset.add(0,.25,0),.16,240,age*.1);
        }
        for(int i=0;i<6;i++) {
            double angle=i*Math.PI/3+age*.01;
            Vec3 center=new Vec3(Math.cos(angle)*1.35,0,Math.sin(angle)*1.35);
            verticalPanel(p,b,center,angle,"shield_panel",.95,charges>0?95:65);
        }
    }
    private void prop(PoseStack p,MultiBufferSource b,Item item,Vec3 at,float scale,float pitch,float yaw,int light) {
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(Axis.YP.rotationDegrees(yaw));p.mulPose(Axis.XP.rotationDegrees(pitch));p.scale(scale,scale,scale);
        items.renderStatic(new ItemStack(item),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,p,b,Minecraft.getInstance().level,0);p.popPose();
    }
    private void sprite(PoseStack p,MultiBufferSource b,String tex,Vec3 at,double size,int alpha,double rotation) {
        if(alpha<=0)return;p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(entityRenderDispatcher.cameraOrientation());p.mulPose(Axis.ZP.rotation((float)rotation));
        quad(p,b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex))),new Vec3(-size,-size,0),new Vec3(size,-size,0),new Vec3(size,size,0),new Vec3(-size,size,0),RadiationVisualStyle.tint(tex),alpha,false);p.popPose();
    }
    private void floorSprite(PoseStack p,MultiBufferSource b,String tex,Vec3 center,double width,int alpha) {
        double r=width*.5;quad(p,b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex))),center.add(-r,0,-r),center.add(-r,0,r),center.add(r,0,r),center.add(r,0,-r),RadiationVisualStyle.tint(tex),alpha,false);
    }
    private void verticalPanel(PoseStack p,MultiBufferSource b,Vec3 center,double angle,String tex,double width,int alpha) {
        Vec3 right=new Vec3(-Math.sin(angle)*width*.5,0,Math.cos(angle)*width*.5),up=new Vec3(0,.65,0);
        quad(p,b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex))),center.subtract(right).subtract(up),center.add(right).subtract(up),center.add(right).add(up),center.subtract(right).add(up),RadiationVisualStyle.tint(tex),alpha,false);
    }
    private void solidBeam(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,double width,int color,int alpha){beam(p,b,from,to,width,"spark",color,alpha,true);}
    private void texturedBeam(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,double width,String tex,int alpha){beam(p,b,from,to,width,tex,RadiationVisualStyle.tint(tex),alpha,false);}
    private void beam(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,double width,String tex,int color,int alpha,boolean solid) {
        Vec3 direction=to.subtract(from).normalize(),side=direction.cross(new Vec3(0,1,0));
        if(side.lengthSqr()<.001)side=direction.cross(new Vec3(1,0,0));side=side.normalize().scale(width);
        Vec3 up=direction.cross(side).normalize().scale(width);VertexConsumer v=b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex)));
        quad(p,v,from.add(side),from.subtract(side),to.subtract(side),to.add(side),color,alpha,solid);
        quad(p,v,from.add(up),from.subtract(up),to.subtract(up),to.add(up),color,alpha,solid);
    }
    static void quad(PoseStack p,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,int alpha,boolean solid) {
        vertex(p,v,a,color,alpha,solid?.5F:0,solid?.5F:1);vertex(p,v,b,color,alpha,solid?.5F:0,solid?.5F:0);
        vertex(p,v,c,color,alpha,solid?.5F:1,solid?.5F:0);vertex(p,v,d,color,alpha,solid?.5F:1,solid?.5F:1);
    }
    static void frameQuad(PoseStack p,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,int alpha,int frame){
        float top=(frame&3)*.25F,bottom=top+.25F;
        vertex(p,v,a,color,alpha,0,bottom);vertex(p,v,b,color,alpha,1,bottom);
        vertex(p,v,c,color,alpha,1,top);vertex(p,v,d,color,alpha,0,top);
    }
    private static void vertex(PoseStack p,VertexConsumer v,Vec3 at,int color,int alpha,float u,float t) {
        v.vertex(p.last().pose(),(float)at.x,(float)at.y,(float)at.z).color(color>>16&255,color>>8&255,color&255,RadiationVisualStyle.opacity(alpha)).uv(u,t)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(p.last().normal(),0,1,0).endVertex();
    }
}
