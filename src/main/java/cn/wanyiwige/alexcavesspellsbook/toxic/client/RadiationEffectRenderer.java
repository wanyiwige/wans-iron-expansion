package cn.wanyiwige.alexcavesspellsbook.toxic.client;

import cn.wanyiwige.alexcavesspellsbook.toxic.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Dedicated world sprites, finite animation phases, and the original full-depth Blockbench prop. */
public final class RadiationEffectRenderer extends EntityRenderer<RadiationNewEffectEntity> {
    private final ItemRenderer items;
    public RadiationEffectRenderer(EntityRendererProvider.Context context){super(context);items=context.getItemRenderer();shadowRadius=0;}
    private static ResourceLocation texture(String name){return ToxicRegistry.id("textures/entity/toxic/rework/"+name+".png");}
    @Override public ResourceLocation getTextureLocation(RadiationNewEffectEntity e){return texture("pixel");}
    @Override public boolean shouldRender(RadiationNewEffectEntity e,Frustum f,double x,double y,double z){return e.distanceToSqr(x,y,z)<80*80;}
    @Override public void render(RadiationNewEffectEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        float age=e.visualAge()+partial,fade=fade(e,age);int frame=((int)age/4)&3;
        switch(e.spec()){
            case ISOTOPE -> isotope(e,age,partial,p,b,fade);
            case NEUTRON -> neutron(e,age,partial,p,b,fade,frame);
            case MODERATOR -> moderator(e,age,p,b,fade,frame);
            case LEAD -> lead(e,age,p,b,light,fade,frame);
            case CHAIN -> chain(e,age,partial,p,b,fade);
            case FILM -> film(e,age,partial,p,b,fade,frame);
            case CLOUD -> cloud(e,age,partial,p,b,fade,frame);
            case CHERENKOV -> wake(e,age,partial,p,b,fade,frame);
            case DECOY -> decoy(e,age,partial,p,b,fade,frame);
            case MATRIX -> matrix(e,age,p,b,fade);
            default -> {}
        }
    }
    private static float fade(RadiationNewEffectEntity e,float age){return Mth.clamp(Math.min(age/4F,(e.visual().getInt("Life")-age)/7F),0,1);}
    private static List<Vec3> points(CompoundTag state,String key,int limit){
        List<Vec3> result=new ArrayList<>();ListTag list=state.getList(key,Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(limit,list.size());i++){CompoundTag point=list.getCompound(i);result.add(new Vec3(point.getDouble("X"),point.getDouble("Y"),point.getDouble("Z")));}return result;
    }
    private static Vec3 local(RadiationNewEffectEntity e,Vec3 world,float partial){return world.subtract(e.getPosition(partial));}
    private void isotope(RadiationNewEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,float fade){
        Vec3 floor=local(e,e.vector("AreaFloor"),partial);
        ring(p,b,floor,RadiationRules.AREA_RADIUS,0xA6FF35,(int)(145*fade));
        float flash=age-e.visual().getInt("Flash");
        if(flash>=0&&flash<7)ring(p,b,floor.add(0,.02,0),RadiationRules.AREA_RADIUS*flash/7,0xDFFF35,(int)(150*(1-flash/7)*fade));
    }
    private void neutron(RadiationNewEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,float fade,int frame){
        List<Vec3> trail=points(e.visual(),"Trail",7);
        for(int i=1;i<trail.size();i++)ribbon(p,b,local(e,trail.get(i-1),partial),local(e,trail.get(i),partial),.10,"chain",(int)(105*fade*i/trail.size()),frame);
        if(e.visual().getInt("FinishedAt")<0)sprite(p,b,"neutron",Vec3.ZERO,.23,.23,(int)(255*fade),frame);
        float flash=age-e.visual().getInt("Flash");
        if(flash>=0&&flash<7){
            Vec3 at=e.visual().contains("ImpactX")?local(e,e.vector("Impact"),partial):Vec3.ZERO;
            Vec3 normal=e.vector("Normal");
            if(normal.lengthSqr()>.5)oriented(p,b,"impact",at.add(normal.scale(.025)),normal,.32+flash*.025,(int)(220*(1-flash/7)),Math.min(3,(int)flash/2));
            else sprite(p,b,"impact",at,.32+flash*.025,.32+flash*.025,(int)(220*(1-flash/7)),Math.min(3,(int)flash/2));
        }
    }
    private void moderator(RadiationNewEffectEntity e,float age,PoseStack p,MultiBufferSource b,float fade,int frame){
        floor(p,b,"field",new Vec3(0,.01,0),6,6,0,(int)(155*fade),frame);
        for(int i=0;i<12;i++){
            double angle=i*Math.PI/6,r=i%3==0?1.2:2.5,height=.08+(1-((age+i*5)%32)/32)*.65;
            pixel(p,b,new Vec3(Math.cos(angle)*r,height,Math.sin(angle)*r),.035,0xC5EF5A,(int)(120*fade));
        }
    }
    private void lead(RadiationNewEffectEntity e,float age,PoseStack p,MultiBufferSource b,int light,float fade,int frame){
        Vec3 normal=e.vector("Direction"),right=new Vec3(normal.z,0,-normal.x);
        float appear=Mth.clamp(age/6,0,1),collapse=e.visual().getInt("FinishedAt")>=0?Mth.clamp((e.visual().getInt("Life")-age)/8,0,1):1;
        float scale=RadiationRules.LEAD_SCALE,size=RadiationRules.LEAD_MODEL_SIZE;
        p.pushPose();p.mulPose(Axis.YP.rotation((float)Math.atan2(normal.x,normal.z)));
        p.scale(size,size*Math.max(.02F,appear*collapse),size);
        int metalLight=LightTexture.pack(Math.max(8,LightTexture.block(light)),LightTexture.sky(light));
        items.renderStatic(new ItemStack(ToxicRegistry.LEAD_PROP.get()),ItemDisplayContext.FIXED,metalLight,OverlayTexture.NO_OVERLAY,p,b,e.level(),0);p.popPose();
        panel(p,b,"curtain",Vec3.ZERO,normal,1.75*scale,1.86*scale*appear*collapse,0xA6FF35,(int)(210*fade),frame);
        int count=e.visual().getInt("Charges"),capacity=e.visual().getInt("Capacity");
        for(int i=0;i<capacity;i++){
            Vec3 at=right.scale((i-(capacity-1)*.5)*.17*scale).add(0,.99*scale,.0).add(normal.scale(.17*scale));
            panelPixel(p,b,at,normal,.07*scale,.045*scale,i<count?0xDDF253:0x344548,i<count?245:230);
        }
        float flash=age-e.visual().getInt("Flash");
        if(flash>=0&&flash<8){
            double diameter=(.26+flash*.04)*scale*2;
            panel(p,b,"impact",e.vector("Hit").subtract(e.position()).add(normal.scale(.03*scale)),normal,diameter,diameter,0xC0FF40,(int)(255*(1-flash/8)),Math.min(3,(int)flash/2));
        }
    }
    private void chain(RadiationNewEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,float fade){
        if(e.visual().getInt("Step")<=0)return;
        float flash=age-e.visual().getInt("Flash");if(flash>7)return;
        Vec3 from=local(e,e.vector("From"),partial),to=local(e,e.vector("End"),partial);
        Vec3 side=to.subtract(from).cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
        Vec3 previous=from;
        for(int i=1;i<=6;i++){
            Vec3 next=from.lerp(to,i/6.0).add(side.scale(i==6?0:(i%2==0?1:-1)*.12));
            ribbon(p,b,previous,next,.15,"chain",(int)(245*fade*(1-flash/8)),((int)age/2)&3);previous=next;
        }
        sprite(p,b,"impact",to,.35,.35,(int)(235*(1-flash/8)),Math.min(3,(int)flash/2));
        if(flash<4)VanillaDIPixelLight.render(e,e.vector("From"),p,b,(int)(80*fade));
    }
    private void film(RadiationNewEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,float fade,int frame){
        ring(p,b,local(e,e.vector("AreaFloor"),partial),RadiationRules.AREA_RADIUS,0x9EFF55,(int)(145*fade));
        ListTag records=e.visual().getList("FilmTargets",Tag.TAG_COMPOUND);
        for(int n=0;n<Math.min(RadiationRules.AREA_TARGET_LIMIT,records.size());n++){
            CompoundTag record=records.getCompound(n);List<Vec3> snapshots=points(record,"Snapshots",4);int fired=record.getInt("Step");
            double height=Mth.clamp(record.getInt("Height")/1000.0,1,3.2),width=Math.max(.65,Math.min(1.8,height*.52));
            for(int i=0;i<snapshots.size();i++){
                int alpha=(int)((i<fired?75:190)*fade);Vec3 at=local(e,snapshots.get(i),partial);
                sprite(p,b,"film",at,width*.5,height*.5,alpha,frame);
                pixel(p,b,at.add(0,height*.5+.13,0),.045,0xAFFF45,(int)(230*fade));
            }
            float flash=age-record.getInt("Flash");
            if(fired>0&&flash>=0&&flash<6){
                Vec3 from=local(e,vector(record,"From"),partial),end=local(e,vector(record,"End"),partial);
                ribbon(p,b,from,end,.15,"chain",(int)(245*(1-flash/6)),((int)age/2)&3);
                sprite(p,b,"impact",end,.3+flash*.035,.3+flash*.035,(int)(235*(1-flash/6)),Math.min(3,(int)flash/2));
            }
        }
    }
    private static Vec3 vector(CompoundTag state,String key){return new Vec3(state.getDouble(key+"X"),state.getDouble(key+"Y"),state.getDouble(key+"Z"));}
    private void cloud(RadiationNewEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,float fade,int frame){
        float radius=e.visual().getInt("Rank")>=5?4:3;
        floor(p,b,"field",new Vec3(0,.01,0),radius*2,radius*2,0,(int)(135*fade),frame);
        ring(p,b,new Vec3(0,.035,0),radius,0xADFF45,(int)(180*fade));
        for(int i=0;i<5;i++){
            double angle=i*Math.PI*2/5;
            sprite(p,b,"cloud",new Vec3(Math.cos(angle)*radius*.6,.35+Math.sin(age*.03+i)*.12,Math.sin(angle)*radius*.6),.55,.4,(int)(125*fade),(frame+i)&3);
        }
        ListTag paths=e.visual().getList("CloudPaths",Tag.TAG_COMPOUND);boolean active=e.visual().getInt("Triggered")>0;
        float elapsed=age-e.visual().getInt("ResolvedAt");
        for(int n=0;n<Math.min(8,paths.size());n++){
            List<Vec3> path=points(paths.getCompound(n),"Points",12);
            for(int i=1;i<path.size();i++)ribbon(p,b,local(e,path.get(i-1),partial),local(e,path.get(i),partial),.09,"chain",(int)(180*fade),frame);
            if(path.isEmpty())continue;
            float cursor=active?Math.min(path.size()-1,Math.max(0,(elapsed-1)/2)):path.size()-1;
            int index=(int)cursor;Vec3 at=path.get(index).lerp(path.get(Math.min(path.size()-1,index+1)),cursor-index);
            sprite(p,b,"neutron",local(e,at,partial),.18,.18,(int)(255*fade),frame);
            float burst=elapsed-((path.size()-1)*2+1);
            if(active&&burst>=0&&burst<8)sprite(p,b,"impact",local(e,path.get(path.size()-1),partial),.3+burst*.04,.3+burst*.04,(int)(220*(1-burst/8)),Math.min(3,(int)burst/2));
        }
    }
    private void wake(RadiationNewEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,float fade,int frame){
        List<Vec3> path=points(e.visual(),"Path",16);int wave=e.visual().getInt("Wave")-1;
        for(int i=0;i<path.size();i++){
            Vec3 before=path.get(Math.max(0,i-1)),after=path.get(Math.min(path.size()-1,i+1)),d=after.subtract(before);
            double angle=Math.atan2(d.x,d.z);Vec3 at=local(e,path.get(i),partial).add(0,.055,0);
            floor(p,b,"wake",at,.65,.7,angle,(int)((i<=wave?170:105)*fade),frame);
            if(i==wave){
                Vec3 side=new Vec3(-d.z,0,d.x).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
                for(int sign:new int[]{-1,1})floor(p,b,"wake",at.add(side.scale(sign*1.2)),1.5,1,angle,(int)(230*fade),frame);
            }
        }
        int crossed=e.visual().getInt("IntersectionAt");float flash=age-crossed;
        if(crossed>0&&flash<8)sprite(p,b,"impact",new Vec3(0,.2,0),.4+flash*.05,.4+flash*.05,(int)(180*(1-flash/8)),Math.min(3,(int)flash/2));
    }
    private void decoy(RadiationNewEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,float fade,int frame){
        boolean active=e.visual().getInt("Triggered")>0;
        Vec3 shell=local(e,e.vector("Shell"),partial);
        if(!active){
            sprite(p,b,"shell",shell.add(0,.95,0),.5,.95,(int)(180*fade),frame);
            floor(p,b,"field",shell.add(0,.02,0),1.1,1.1,0,(int)(115*fade),frame);
            for(int i=0;i<3;i++)pixel(p,b,shell.add((i-1)*.24,.2+((age+i*7)%35)*.045,0),.035,0xC6EE70,(int)(125*fade));
            return;
        }
        float elapsed=age-e.visual().getInt("ResolvedAt");
        sprite(p,b,"impact",shell.add(0,.9,0),.5+elapsed*.035,.8,(int)(210*fade),Math.min(3,(int)elapsed/3));
        if(e.visual().getInt("Swapped")>0){
            Vec3 departure=local(e,e.vector("Departure"),partial);
            sprite(p,b,"shell",departure.add(0,.95,0),.5,.95,(int)(145*fade),3);
            ribbon(p,b,departure.add(0,.8,0),shell.add(0,.8,0),.06,"chain",(int)(130*fade),frame);
        }
    }
    private void matrix(RadiationNewEffectEntity e,float age,PoseStack p,MultiBufferSource b,float fade){
        int cells=e.visual().getInt("Cells"),pattern=e.visual().getInt("Pattern"),line=RadiationRules.patternCells(pattern);
        ring(p,b,new Vec3(0,.035,0),RadiationRules.MATRIX_RADIUS,0xC5FF35,(int)(180*fade));
        p.pushPose();p.translate(0,3.2,0);p.mulPose(entityRenderDispatcher.cameraOrientation());
        for(int i=0;i<9;i++){
            Vec3 at=new Vec3((i%3-1)*.62,(1-i/3)*.62,0);boolean lit=(cells&(1<<i))!=0;
            int frame=(line&(1<<i))!=0?2:lit?1:0;
            plane(p,b,"matrix",at,.28,.28,(int)((lit?255:190)*fade),frame);
            if(i==e.visual().getInt("LastCell")&&age-e.visual().getInt("Flash")<6)plane(p,b,"matrix",at,.32,.32,(int)(210*fade),2);
        }
        p.popPose();
        if(e.visual().getInt("Triggered")==0)return;
        float elapsed=age-e.visual().getInt("ResolvedAt");int alpha=(int)(225*fade*Mth.clamp(1-elapsed/18,0,1));
        if(pattern==0){floor(p,b,"field",new Vec3(0,.03,0),1+elapsed*.35,1+elapsed*.35,0,alpha,3);return;}
        if(pattern<=3){
            double z=(pattern-2)*1.2;floor(p,b,"field",new Vec3(0,.03,z),8,1.8,0,alpha,2);
            ribbon(p,b,new Vec3(-4,.8,z),new Vec3(4,.8,z),.23,"chain",alpha,2);
        }else if(pattern<=6){
            double x=(pattern-5)*1.2;
            for(int z=-1;z<=1;z++)ribbon(p,b,new Vec3(x,0,z*1.2),new Vec3(x,4,z*1.2),.22,"chain",alpha,2);
            for(int i=0;i<3;i++){float fall=elapsed-i*4;if(fall>=0&&fall<5)sprite(p,b,"neutron",new Vec3(x,4-fall*.75,(i-1)*1.2),.19,.19,alpha,2);}
        }else{
            for(int sign:new int[]{-1,1})ribbon(p,b,new Vec3(-3,.9,-3*sign),new Vec3(3,.9,3*sign),.24,"chain",alpha,2);
        }
        sprite(p,b,"impact",new Vec3(0,3.2,0),.75+elapsed*.035,.75+elapsed*.035,alpha,Math.min(3,(int)elapsed/4));
        if(elapsed<5)VanillaDIPixelLight.render(e,e.position().add(0,3.2,0),p,b,100);
    }
    private static final Map<Integer,List<Vec3>> RINGS=new HashMap<>();
    private static void ring(PoseStack p,MultiBufferSource b,Vec3 at,double radius,int color,int alpha){
        if(alpha<=0||radius<.25)return;
        int cells=(int)Math.round(radius*4);
        List<Vec3> pixels=RINGS.computeIfAbsent(cells,r->{
            List<Vec3> result=new ArrayList<>();
            for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)
                if(Math.abs(Math.hypot(x,z)-r)<.5)result.add(new Vec3(x*.25,0,z*.25));
            return result;
        });
        var buffer=b.getBuffer(RenderType.entityTranslucentEmissive(texture("pixel")));
        for(Vec3 offset:pixels){
            Vec3 c=at.add(offset);
            ToxicEffectRenderer.quad(p,buffer,c.add(-.125,0,-.125),c.add(-.125,0,.125),c.add(.125,0,.125),c.add(.125,0,-.125),color,alpha,true);
        }
    }
    private void pixel(PoseStack p,MultiBufferSource b,Vec3 at,double half,int color,int alpha){
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(entityRenderDispatcher.cameraOrientation());
        ToxicEffectRenderer.quad(p,b.getBuffer(RenderType.entityTranslucentEmissive(texture("pixel"))),new Vec3(-half,-half,0),new Vec3(half,-half,0),new Vec3(half,half,0),new Vec3(-half,half,0),color,alpha,true);p.popPose();
    }
    private void sprite(PoseStack p,MultiBufferSource b,String tex,Vec3 at,double x,double y,int alpha,int frame){
        if(alpha<=0)return;p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(entityRenderDispatcher.cameraOrientation());plane(p,b,tex,Vec3.ZERO,x,y,alpha,frame);p.popPose();
    }
    private static void plane(PoseStack p,MultiBufferSource b,String tex,Vec3 at,double x,double y,int alpha,int frame){
        ToxicEffectRenderer.frameQuad(p,b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex))),at.add(-x,-y,0),at.add(x,-y,0),at.add(x,y,0),at.add(-x,y,0),RadiationVisualStyle.tint(tex),alpha,frame);
    }
    private static void floor(PoseStack p,MultiBufferSource b,String tex,Vec3 at,double width,double length,double angle,int alpha,int frame){
        Vec3 right=new Vec3(Math.cos(angle),0,-Math.sin(angle)).scale(width*.5),forward=new Vec3(Math.sin(angle),0,Math.cos(angle)).scale(length*.5);
        ToxicEffectRenderer.frameQuad(p,b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex))),at.subtract(right).add(forward),at.add(right).add(forward),at.add(right).subtract(forward),at.subtract(right).subtract(forward),RadiationVisualStyle.tint(tex),alpha,frame);
    }
    private static void panel(PoseStack p,MultiBufferSource b,String tex,Vec3 at,Vec3 normal,double width,double height,int alpha,int frame){
        panel(p,b,tex,at,normal,width,height,RadiationVisualStyle.tint(tex),alpha,frame);
    }
    private static void panel(PoseStack p,MultiBufferSource b,String tex,Vec3 at,Vec3 normal,double width,double height,int color,int alpha,int frame){
        Vec3 right=new Vec3(normal.z,0,-normal.x).scale(width*.5),up=new Vec3(0,height*.5,0);
        ToxicEffectRenderer.frameQuad(p,b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex))),at.subtract(right).subtract(up),at.add(right).subtract(up),at.add(right).add(up),at.subtract(right).add(up),color,alpha,frame);
    }
    private static void panelPixel(PoseStack p,MultiBufferSource b,Vec3 at,Vec3 normal,double x,double y,int color,int alpha){
        Vec3 right=new Vec3(normal.z,0,-normal.x).scale(x),up=new Vec3(0,y,0);
        ToxicEffectRenderer.quad(p,b.getBuffer(RenderType.entityTranslucentEmissive(texture("pixel"))),at.subtract(right).subtract(up),at.add(right).subtract(up),at.add(right).add(up),at.subtract(right).add(up),color,alpha,true);
    }
    private void oriented(PoseStack p,MultiBufferSource b,String tex,Vec3 at,Vec3 normal,double half,int alpha,int frame){
        if(Math.abs(normal.y)>.8)floor(p,b,tex,at,half*2,half*2,0,alpha,frame);else panel(p,b,tex,at,normal,half*2,half*2,alpha,frame);
    }
    private void ribbon(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,double half,String tex,int alpha,int frame){
        if(alpha<=0||from.distanceToSqr(to)<.0001)return;
        Vec3 d=to.subtract(from).normalize(),side=d.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=d.cross(new Vec3(1,0,0));
        side=side.normalize().scale(half);Vec3 up=d.cross(side).normalize().scale(half);
        int count=Math.min(32,Math.max(1,(int)Math.ceil(from.distanceTo(to)/.55)));
        var buffer=b.getBuffer(RenderType.entityTranslucentEmissive(texture(tex)));
        for(int i=0;i<count;i++){
            Vec3 a=from.lerp(to,(double)i/count),c=from.lerp(to,(double)(i+1)/count);
            ToxicEffectRenderer.frameQuad(p,buffer,a.subtract(side),c.subtract(side),c.add(side),a.add(side),RadiationVisualStyle.tint(tex),alpha,frame);
            ToxicEffectRenderer.frameQuad(p,buffer,a.subtract(up),c.subtract(up),c.add(up),a.add(up),RadiationVisualStyle.tint(tex),alpha,frame);
        }
    }
}
