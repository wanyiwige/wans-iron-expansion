package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyEffectEntity;
import com.github.alexmodguy.alexscaves.client.model.CandicornModel;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.CandicornEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;
import static cn.wanyiwige.alexcavesspellsbook.candy.client.CandyPixelScenes.*;

/** Support scenes 16-20. Native creatures keep their original model and texture resources. */
final class CandySupportPixelScenes {
    private static final ResourceLocation BAND=ResourceLocation.fromNamespaceAndPath("alex_caves_spellsbook","textures/entity/candy/pixel/snare_band.png");
    private static final ResourceLocation NATIVE_RAINBOW=nativeTexture("particle/rainbow");
    private static final ResourceLocation[] SUGAR=new ResourceLocation[5],COOKIE=new ResourceLocation[8],CANDICORN=new ResourceLocation[5];
    private static final CandicornModel MODEL=new CandicornModel();
    private static final Map<CandyEffectEntity,ChargePose> POSES=new WeakHashMap<>();
    static {
        for(int i=0;i<5;i++){SUGAR[i]=nativeTexture("particle/sugar_flake_"+i);CANDICORN[i]=nativeTexture("entity/candicorn_"+i);}
        for(int i=0;i<8;i++)COOKIE[i]=nativeTexture("particle/witch_cookie_"+i);
    }
    private static ResourceLocation nativeTexture(String path){return ResourceLocation.fromNamespaceAndPath("alexscaves","textures/"+path+".png");}
    private CandySupportPixelScenes() { }
    // This unspawned pose adapter is never ticked. No native AI, collision or damage runs.
    private static final class ChargePose extends CandicornEntity {
        float run,charge;
        ChargePose(Level level){super(ACEntityRegistry.CANDICORN.get(),level);}
        @Override public float getRunProgress(float partial){return run;}
        @Override public float getChargeProgress(float partial){return charge;}
        @Override public float getTailYaw(float partial){return 0;}
    }
    static boolean render(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,int light){
        String id=e.visual().getString("Spell");
        if(!id.equals("sweet_overdrive")&&!id.equals("rainbow_snare")&&!id.equals("candicorn_charge")&&!id.equals("gummy_guardian")&&!id.equals("gingerbread_march"))return false;
        float end=e.stage()==CandyEffectEntity.Stage.END?Mth.clamp((age-e.visual().getInt("StageAge"))/8F,0,1):0;
        int alpha=(int)(235*(1-end));boolean near=CandyVisuals.detail(e);
        switch(id){
            case "sweet_overdrive" -> overdrive(e,age,p,b,alpha,near,end);
            case "rainbow_snare" -> snare(e,age,partial,p,b,alpha,near,end);
            case "candicorn_charge" -> charge(e,age,partial,p,b,light,alpha,near,end);
            case "gummy_guardian","gingerbread_march" -> summon(e,age,p,b,light,alpha,near,id.equals("gingerbread_march"));
        }
        return true;
    }
    private static void overdrive(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource b,int alpha,boolean near,float end){
        Minecraft mc=Minecraft.getInstance();
        boolean first=mc.player!=null&&e.visual().hasUUID("Owner")&&e.visual().getUUID("Owner").equals(mc.player.getUUID())&&mc.options.getCameraType().isFirstPerson();
        int count=first?2:near?6:3;float start=Mth.clamp(age/8F,0,1);
        for(int i=0;i<count;i++){
            double a=i*Math.PI*2/count+age*.045,r=.5+(1-start)*.45+end*.3;
            float pulse=(age*.06F+i*.19F)%1;
            Vec3 at=new Vec3(Math.cos(a)*r,first?-.75:-.6+pulse*1.35,Math.sin(a)*r);
            sprite(p,b,"overdrive_speed_"+((int)(age/3)+i)%3,at,.11,0xFFFFFF,(int)(alpha*(.3+.6*Math.sin(pulse*Math.PI))),0);
            if(!first&&i%2==0)sprite(p,b,"overdrive_bolt",at.add(0,.15,0),.10,0xFFFFFF,alpha/2,(float)Math.sin(age*.07+i)*.2F);
        }
        if(age<8&&!first)for(int i=0;i<4;i++){
            double a=i*Math.PI/2;double r=(1-start)*.85+.3;
            sprite(p,b,"crumb",new Vec3(Math.cos(a)*r,Math.sin(a)*r*.5,Math.sin(a)*r),.065,0xFFD473,(int)(alpha*(1-start)),age*.1F);
        }
    }
    private static void snare(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,int alpha,boolean near,float end){
        double radius=Math.max(.1,e.visual().getFloat("Radius"));
        float pulse=Mth.clamp((age-e.visual().getInt("SnarePulseAge"))/10F,0,1);
        float grow=Mth.clamp(age/6F,0,1);double r=radius*grow;
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3;
            Vec3 foot=new Vec3(Math.cos(a)*r,.04,Math.sin(a)*r);
            plane(p,b,"snare_knot",foot,new Vec3(1,0,0),new Vec3(0,0,-1),.18,0xFFFFFF,alpha);
            Vec3 last=foot;
            for(int j=1;j<=6;j++){
                double t=j/6.0,angle=a+t*.62;
                double rad=r*(1-.22*Math.sin(t*Math.PI)*(1-pulse));
                Vec3 next=new Vec3(Math.cos(angle)*rad,.06+Math.sin(t*Math.PI)*(.48+.18*(1-pulse)),Math.sin(angle)*rad);
                band(p,b,last,next,.065,alpha*(j==3&&end>0?0:1));last=next;
            }
        }
        if(!near||!e.visual().contains("SnarePulseAge")||age-e.visual().getInt("SnarePulseAge")>=40)return;
        Vec3 origin=new Vec3(Mth.lerp(partial,e.xo,e.getX()),Mth.lerp(partial,e.yo,e.getY()),Mth.lerp(partial,e.zo,e.getZ()));
        int shown=0;
        for(int id:e.visual().getIntArray("BoundTargets")){
            if(shown++>=8)break;
            if(!(e.level().getEntity(id) instanceof LivingEntity target)||!target.isAlive()||!target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN))continue;
            Vec3 at=new Vec3(Mth.lerp(partial,target.xo,target.getX()),Mth.lerp(partial,target.yo,target.getY()),Mth.lerp(partial,target.zo,target.getZ())).subtract(origin);
            double rr=target.getBbWidth()*.65+.1;
            Vec3 last=at.add(rr,.18,0);
            for(int j=1;j<=8;j++){
                double a=j*Math.PI/4;
                Vec3 next=at.add(Math.cos(a)*rr,.18+target.getBbHeight()*.3*j/8,Math.sin(a)*rr);
                band(p,b,last,next,.045,alpha*2/3);last=next;
            }
        }
    }
    private static void charge(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,int light,int alpha,boolean near,float end){
        ChargePose pose=POSES.computeIfAbsent(e,key->new ChargePose(e.level()));
        pose.tickCount=(int)age;pose.run=Mth.clamp(age/5F,0,1)*(1-end);pose.charge=Mth.clamp(age/7F,0,1)*(1-end);
        pose.setPos(e.getX(),e.getY(),e.getZ());
        MODEL.young=false;MODEL.riding=false;MODEL.attackTime=0;
        MODEL.setupAnim(pose,age*1.4F,.9F*(1-end),age,0,0);
        p.pushPose();p.mulPose(Axis.YP.rotation((float)Math.PI+e.visual().getFloat("Heading")));
        p.scale(-1,-1,1);p.translate(0,-1.5,0);
        MODEL.renderToBuffer(p,b.getBuffer(RenderType.entityTranslucent(CANDICORN[Math.floorMod(e.getUUID().hashCode(),5)])),light,OverlayTexture.NO_OVERLAY,1,1,1,alpha/255F);
        p.popPose();
        Vec3 last=new Vec3(0,.7,0);int segment=0;
        for(Vec3 point:trailPoints(e,partial)){
            Vec3 next=point.add(0,.7,0);double fade=1-segment++/9.0;
            if(segment>1&&last.distanceToSqr(next)>.001&&last.distanceToSqr(next)<4){
                Vec3 up=new Vec3(0,.22*fade,0);
                quad(p,b,NATIVE_RAINBOW,last.add(up),next.add(up),next.subtract(up),last.subtract(up),(int)(alpha*fade*.65),light);
            }
            last=next;
        }
        // Original sugar flakes mark hoof-level movement and the real stopping point.
        if(near){
            double heading=e.visual().getFloat("Heading");
            Vec3 back=new Vec3(-Math.sin(heading),0,-Math.cos(heading));
            for(int i=0;i<5;i++){
                float t=((age*.12F+i*.2F)%1);
                Vec3 at=back.scale(.45+t*1.5).add(Math.cos(i*2.4)*.3,.08+t*.25,Math.sin(i*2.4)*.3);
                nativeSprite(p,b,SUGAR[i],at,.06*(1-t)+.03,(int)(alpha*(1-t)),light,age*.06F+i);
            }
        }
    }
    private static void summon(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource b,int light,int alpha,boolean near,boolean ginger){
        float t=(age-e.visual().getInt("SummonCueAge"))/16F;
        if(!e.visual().contains("SummonCueAge")||t<0||t>=1)return;
        int count=Math.min(3,e.visual().getInt("SummonCues"));
        for(int i=0;i<count;i++){
            Vec3 at=new Vec3(e.visual().getDouble("SummonX"+i),e.visual().getDouble("SummonY"+i),e.visual().getDouble("SummonZ"+i)).subtract(e.position());
            int bits=near?8:3;
            for(int j=0;j<bits;j++){
                double a=j*2.399963+i,r=.15+t*.65;
                Vec3 pos=at.add(Math.cos(a)*r,.07+Math.sin(t*Math.PI)*(.3+j%3*.1),Math.sin(a)*r);
                ResourceLocation tex=ginger?COOKIE[j%8]:SUGAR[j%5];
                nativeSprite(p,b,tex,pos,(ginger?.10:.065)*(1-t*.4),(int)(alpha*(1-t)),light,j+age*.045F);
            }
        }
    }
    private static void band(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,double width,int alpha){
        if(alpha<=0)return;Vec3 dir=to.subtract(from);if(dir.lengthSqr()<1e-7)return;
        Vec3 side=dir.normalize().cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);
        side=side.normalize().scale(width);
        quad(p,b,BAND,from.add(side),from.subtract(side),to.subtract(side),to.add(side),alpha,15728880);
        Vec3 up=dir.normalize().cross(side).normalize().scale(width);
        quad(p,b,BAND,from.add(up),from.subtract(up),to.subtract(up),to.add(up),alpha,15728880);
    }
    private static void nativeSprite(PoseStack p,MultiBufferSource b,ResourceLocation tex,Vec3 at,double r,int alpha,int light,float rotation){
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());p.mulPose(Axis.ZP.rotation(rotation));
        quad(p,b,tex,new Vec3(-r,-r,0),new Vec3(r,-r,0),new Vec3(r,r,0),new Vec3(-r,r,0),alpha,light);p.popPose();
    }
    private static void quad(PoseStack p,MultiBufferSource b,ResourceLocation tex,Vec3 a,Vec3 c,Vec3 d,Vec3 f,int alpha,int light){
        VertexConsumer v=b.getBuffer(RenderType.entityTranslucent(tex));
        vertex(p,v,a,0,1,alpha,light);vertex(p,v,c,1,1,alpha,light);vertex(p,v,d,1,0,alpha,light);vertex(p,v,f,0,0,alpha,light);
    }
    private static void vertex(PoseStack p,VertexConsumer v,Vec3 at,float u,float w,int alpha,int light){
        v.vertex(p.last().pose(),(float)at.x,(float)at.y,(float)at.z).color(255,255,255,Mth.clamp(alpha,0,255)).uv(u,w).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,1,0).endVertex();
    }
}
