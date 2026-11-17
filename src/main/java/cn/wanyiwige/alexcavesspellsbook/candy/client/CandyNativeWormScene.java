package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyEffectEntity;
import com.github.alexmodguy.alexscaves.client.model.GumWormModel;
import com.github.alexmodguy.alexscaves.client.model.GumWormSegmentModel;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.GumWormEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.GumWormSegmentEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native head, six native segments and connecting gum follow sampled spell positions. */
final class CandyNativeWormScene {
    private static final float SCALE=.8F;
    private static final GumWormModel HEAD=new GumWormModel();
    private static final GumWormSegmentModel SEGMENT=new GumWormSegmentModel();
    private static final ResourceLocation HEAD_TEX=nativeTexture("gum_worm"),JOIN=nativeTexture("gum_worm_connection");
    private static final ResourceLocation[] SEGMENTS={nativeTexture("gum_worm_segment_0"),nativeTexture("gum_worm_segment_1"),nativeTexture("gum_worm_segment_2")};
    private static final Map<CandyEffectEntity,Trail> TRAILS=new WeakHashMap<>();
    private static ResourceLocation nativeTexture(String name){return ResourceLocation.fromNamespaceAndPath("alexscaves","textures/entity/"+name+".png");}
    private static final class HeadPose extends GumWormEntity {
        float mouth,roll;
        HeadPose(Level level){super(ACEntityRegistry.GUM_WORM.get(),level);}
        @Override public float getMouthOpenProgress(float partial){return mouth;}
        @Override public float getBodyZRot(float partial){return roll;}
    }
    private static final class SegmentPose extends GumWormSegmentEntity {
        float roll;
        SegmentPose(Level level){super(ACEntityRegistry.GUM_WORM_SEGMENT.get(),level);}
        @Override public float getBodyZRot(float partial){return roll;}
    }
    private static final class Trail {
        final ArrayDeque<Vec3> path=new ArrayDeque<>();
        final HeadPose head;
        final SegmentPose segment;
        Trail(Level level){head=new HeadPose(level);segment=new SegmentPose(level);}
    }
    private CandyNativeWormScene() { }
    static void tick(CandyEffectEntity e){
        if(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(e.position())>64*64){TRAILS.remove(e);return;}
        Trail trail=TRAILS.computeIfAbsent(e,key->new Trail(e.level()));
        if(!trail.path.isEmpty()&&trail.path.peekFirst().distanceToSqr(e.position())>16*16)trail.path.clear();
        trail.path.addFirst(e.position());while(trail.path.size()>48)trail.path.removeLast();
    }
    /** Arc-length lookup clamps to the sampled route and never extends behind its start. */
    static Vec3 sample(List<Vec3> points,double distance){
        if(points.isEmpty())return Vec3.ZERO;
        if(distance<=0)return points.get(0);
        for(int i=1;i<points.size();i++){
            Vec3 a=points.get(i-1),b=points.get(i);double length=a.distanceTo(b);
            if(length<1e-7)continue;
            if(distance<=length)return a.lerp(b,distance/length);
            distance-=length;
        }
        return points.get(points.size()-1);
    }
    static void render(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,int light,int alpha){
        Trail trail=TRAILS.get(e);if(trail==null)return;
        Vec3 origin=new Vec3(Mth.lerp(partial,e.xo,e.getX()),Mth.lerp(partial,e.yo,e.getY()),Mth.lerp(partial,e.zo,e.getZ()));
        List<Vec3> route=new ArrayList<>();route.add(origin);
        // Skip the current server sample: it can lie ahead of the interpolated head.
        int skip=0;for(Vec3 point:trail.path)if(skip++>0)route.add(point);
        double length=0;for(int i=1;i<route.size();i++)length+=route.get(i-1).distanceTo(route.get(i));
        float rise=Mth.clamp((age-4)/8F,0,1),endTicks=e.stage()==CandyEffectEntity.Stage.END?Math.max(0,age-e.visual().getInt("StageAge")):0;
        trail.head.tickCount=(int)age;trail.head.mouth=.55F+.35F*Mth.sin(age*.28F);trail.head.roll=Mth.sin(age*.2F)*7;
        Vec3 direction=new Vec3(Math.sin(e.visual().getFloat("Heading")),0,Math.cos(e.visual().getFloat("Heading")));
        if(route.size()>1){Vec3 actual=origin.subtract(sample(route,.5));if(actual.horizontalDistanceSqr()>.001)direction=actual.normalize();}
        float yaw=(float)Math.atan2(direction.x,direction.z);
        Vec3 headAt=new Vec3(0,-(1-rise)*2.4-Mth.clamp(endTicks/3.5F,0,1)*2.8,0);
        HEAD.young=false;HEAD.setupAnim(trail.head,0,0,age,0,0);
        p.pushPose();p.translate(headAt.x,headAt.y+SCALE,headAt.z);p.mulPose(Axis.YP.rotation((float)Math.PI+yaw));p.mulPose(Axis.XP.rotation((float)Math.atan2(direction.y,direction.horizontalDistance())));p.scale(-SCALE,-SCALE,SCALE);p.translate(0,-.5,0);
        HEAD.renderToBuffer(p,b.getBuffer(RenderType.entityTranslucent(HEAD_TEX)),light,OverlayTexture.NO_OVERLAY,1,1,1,alpha/255F);p.popPose();
        Vec3 previousRear=headAt.add(direction.scale(-1.7*SCALE)).add(0,SCALE,0);
        for(int i=0;i<6;i++){
            double distance=(3.1+i*2.25)*SCALE;
            float emerge=Mth.clamp((float)(length-distance+.8)/.8F,0,1)*rise;
            if(emerge<=0)break;
            Vec3 center=sample(route,distance),front=sample(route,Math.max(0,distance-.25)),back=sample(route,distance+.25);
            Vec3 tangent=front.subtract(back);if(tangent.horizontalDistanceSqr()<.001)tangent=direction;tangent=tangent.normalize();
            float segmentYaw=(float)Math.atan2(tangent.x,tangent.z);
            float pitch=(float)Math.atan2(tangent.y,tangent.horizontalDistance());
            double sink=Mth.clamp((endTicks-(i+1)*.55F)/3.5F,0,1)*2.8;
            Vec3 at=center.subtract(origin).add(0,Math.sin(age*.22-i*.65)*.12-(1-emerge)*2.2-sink,0);
            trail.segment.tickCount=(int)age;trail.segment.roll=(float)Math.sin(age*.2-i*.6)*7;
            SEGMENT.young=false;SEGMENT.setGumVisible(true,i<5);SEGMENT.setupAnim(trail.segment,0,0,age,0,0);
            p.pushPose();p.translate(at.x,at.y+SCALE,at.z);p.mulPose(Axis.YP.rotation((float)Math.PI+segmentYaw));p.mulPose(Axis.XP.rotation(pitch));p.scale(-SCALE,-SCALE,SCALE);p.translate(0,-.5,0);
            SEGMENT.renderToBuffer(p,b.getBuffer(RenderType.entityTranslucent(SEGMENTS[i%3])),light,OverlayTexture.NO_OVERLAY,1,1,1,alpha*emerge/255F);p.popPose();
            Vec3 frontSurface=at.add(tangent.scale(.9*SCALE)).add(0,SCALE,0);
            connector(p,b,previousRear,frontSurface,light,(int)(alpha*emerge));
            previousRear=at.add(tangent.scale(-.9*SCALE)).add(0,SCALE,0);
        }
        if(CandyVisuals.detail(e))for(int i=0;i<7;i++){
            float t=(age*.1F+i/7F)%1;double a=i*2.399963,r=.55+t*.85;
            CandyPixelScenes.sprite(p,b,"worm_crumbs",new Vec3(Math.cos(a)*r,.04+Math.sin(t*Math.PI)*.35,Math.sin(a)*r),.10*(1-t),0xFFFFFF,(int)(alpha*(1-t)),i+age*.06F);
        }
    }
    private static void connector(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,int light,int alpha){
        Vec3 delta=to.subtract(from);if(delta.lengthSqr()<1e-5||delta.lengthSqr()>9)return;
        Vec3 side=delta.normalize().cross(new Vec3(0,1,0));if(side.lengthSqr()<1e-5)side=new Vec3(1,0,0);side=side.normalize().scale(SCALE*.48);
        Vec3 up=delta.normalize().cross(side).normalize().scale(SCALE*.48);
        // Crossed native connector sheets bridge both the visible top and side views.
        quad(p,b,from.add(side),to.add(side),to.subtract(side),from.subtract(side),light,alpha);
        quad(p,b,from.add(up),to.add(up),to.subtract(up),from.subtract(up),light,alpha);
    }
    private static void quad(PoseStack p,MultiBufferSource b,Vec3 a,Vec3 c,Vec3 d,Vec3 f,int light,int alpha){
        VertexConsumer v=b.getBuffer(RenderType.entityTranslucent(JOIN));vertex(p,v,a,0,0,light,alpha);vertex(p,v,c,0,1,light,alpha);vertex(p,v,d,1,1,light,alpha);vertex(p,v,f,1,0,light,alpha);
    }
    private static void vertex(PoseStack p,VertexConsumer v,Vec3 at,float u,float w,int light,int alpha){
        v.vertex(p.last().pose(),(float)at.x,(float)at.y,(float)at.z).color(255,255,255,Mth.clamp(alpha,0,255)).uv(u,w).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,1,0).endVertex();
    }
}
