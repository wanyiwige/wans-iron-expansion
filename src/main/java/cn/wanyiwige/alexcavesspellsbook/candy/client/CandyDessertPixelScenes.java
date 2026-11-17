package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyEffectEntity;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyFirstRules;
import cn.wanyiwige.alexcavesspellsbook.candy.CandySpellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import static cn.wanyiwige.alexcavesspellsbook.candy.client.CandyPixelScenes.*;

/** Spells 11-15 use purpose-drawn pixel sheets and existing native spear resources. */
final class CandyDessertPixelScenes {
    private static final ResourceLocation[][] CARAMEL=new ResourceLocation[6][3];
    static {
        for(int shape=0;shape<CARAMEL.length;shape++)for(int frame=0;frame<3;frame++)
            CARAMEL[shape][frame]=ResourceLocation.fromNamespaceAndPath("alex_caves_spellsbook",
                "textures/entity/candy/pixel/caramel_pool_"+(shape==0?"":("shape"+shape+"_"))+frame+".png");
    }
    private static final ResourceLocation RIBBON=ResourceLocation.fromNamespaceAndPath("alex_caves_spellsbook","textures/entity/candy/pixel/rainbow_ribbon.png");
    private static final int[] GLASS={0xE8FCFF,0xF3C8FF,0xAFEADB,0xC4DAFF,0xFFD5E8,0xFFF0C0};
    private CandyDessertPixelScenes() { }
    static boolean render(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,int light){
        String id=e.visual().getString("Spell");
        if(!id.equals("soda_meteor")&&!id.equals("soda_frostburst")&&!id.equals("caramel_bog")&&!id.equals("sugar_glass_stars")&&!id.equals("rainbow_leap"))return false;
        float fade=e.stage()==CandyEffectEntity.Stage.END?Mth.clamp(1-(age-e.visual().getInt("StageAge"))/8F,0,1):1;
        int alpha=(int)(240*fade);boolean near=CandyVisuals.detail(e);
        switch(id){
            case "soda_meteor" -> {
                if(e.visual().getInt("PixelDetonated")==0){
                    Vec3 motion=e.getDeltaMovement().lengthSqr()>.001?e.getDeltaMovement().normalize():new Vec3(0,1,0);
                    Vec3 side=motion.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);side=side.normalize();
                    plane(p,b,"soda_rocket",Vec3.ZERO,side.scale(.20),motion.scale(.40),1,0xFFFFFF,alpha);
                    plane(p,b,"soda_rocket",Vec3.ZERO,motion.cross(side).scale(.20),motion.scale(.40),1,0xFFFFFF,alpha);
                    if(near)pathTrail(e,partial,p,b,0xC882F2,alpha/2);
                    for(int i=0;i<(near?4:1);i++){
                        float t=((age*.12F+i*.25F)%1);Vec3 at=motion.scale(-.35-i*.18).add(Math.sin(age*.2+i)*.05,0,Math.cos(age*.2+i)*.05);
                        sprite(p,b,"soda_bubble",at,.035+t*.065,0xFFFFFF,(int)(alpha*(1-t)),i*.4F);
                    }
                }
                sodaImpact(e,age,p,b,alpha,near,false);
            }
            case "soda_frostburst" -> {
                if(e.visual().getInt("Role")==CandySpellEntity.SPEAR){
                    if(e.stage()==CandyEffectEntity.Stage.START||e.stage()==CandyEffectEntity.Stage.ACTIVE)
                        CandyFieldPixelScenes.nativeSpear(e,p,b,light,fade);
                }else{
                    double radius=Math.max(.1,e.visual().getFloat("Radius"));
                    float grow=Mth.clamp((age-4)/18F,.18F,1);
                    floor(p,b,"soda_reaction_pool",new Vec3(0,.035,0),radius*grow,0xFFFFFF,alpha/2);
                    for(int i=0;i<(near?6:2);i++){
                        double a=i*2.399963,r=radius*.45;float lift=(age*.025F+i*.17F)%1;
                        sprite(p,b,"soda_bubble",new Vec3(Math.cos(a)*r,.08+lift*.5,Math.sin(a)*r),.05+lift*.06,0xFFFFFF,(int)(alpha*(1-lift)),i);
                    }
                    sodaImpact(e,age,p,b,alpha,near,true);
                }
            }
            case "caramel_bog" -> {
                double maximum=e.visual().getFloat("CaramelMaxRadius");
                double radius=CandyFirstRules.caramelRadius(maximum,age);
                int poolAlpha=(int)(alpha*CandyFirstRules.caramelOpacity(age));
                if(poolAlpha<=0||radius<=0)return true;
                int frame=(int)(age/8)%3;
                int shape=Math.floorMod(e.visual().getInt("CaramelShape"),CARAMEL.length);
                floorLit(p,b,CARAMEL[shape][frame],new Vec3(0,.032,0),radius,poolAlpha,light);
                if(near)for(int i=0;i<3;i++){
                    float t=(age*.035F+i/3F)%1;double a=i*2.399963;
                    Vec3 at=new Vec3(Math.cos(a)*radius*.36,.05+Math.sin(t*Math.PI)*.12,Math.sin(a)*radius*.36);
                    sprite(p,b,"caramel_drip",at,.035,0xFFFFFF,(int)(poolAlpha*.65),i*.3F);
                }
            }
            case "sugar_glass_stars" -> {
                if(e.visual().getInt("LastRole")==1){
                    int color=GLASS[Math.floorMod(e.visual().getInt("Index"),GLASS.length)];
                    if(e.stage()==CandyEffectEntity.Stage.START||e.stage()==CandyEffectEntity.Stage.ACTIVE){
                        sprite(p,b,"sugar_glass_star",Vec3.ZERO,.23, color,alpha,age*.15F+e.getId());
                        if(near)pathTrail(e,partial,p,b,color,alpha/2);
                    }
                    float hit=hitFade(e,age,8);
                    if(hit>0){
                        sprite(p,b,"burst_"+Math.min(3,(int)((1-hit)*4)),Vec3.ZERO,.12+(1-hit)*.4,color,(int)(alpha*hit),age*.05F);
                        for(int i=0;i<(near?5:2);i++){
                            double a=i*2.399963,r=(1-hit)*.6;
                            sprite(p,b,"crumb",new Vec3(Math.cos(a)*r,Math.sin(a)*r,0),.04*hit,color,(int)(alpha*hit),i+age*.2F);
                        }
                    }
                }else if(age<9)sprite(p,b,"spark",Vec3.ZERO,.16,0xDDF5FF,(int)(alpha*(1-age/9)),age*.1F);
            }
            case "rainbow_leap" -> leap(e,age,p,b,alpha,near);
        }
        return true;
    }
    private static void leap(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource b,int alpha,boolean near){
        Vec3 start=e.anchor("Anchor").subtract(e.position()).add(0,-.4,0);
        Vec3 end=e.anchor("Second").subtract(e.position()).add(0,1,0);
        float head=Mth.clamp(age/4F,0,1),tail=Mth.clamp((age-3)/11F,0,1);
        int count=Math.min(24,Math.max(2,(int)Math.ceil(start.distanceTo(end)*2)));
        for(int i=0;i<count;i++){
            float from=Math.max(tail,(float)i/count),to=Math.min(head,(float)(i+1)/count);
            if(to<=from)continue;
            Vec3 a=start.lerp(end,from),c=start.lerp(end,to);
            rainbowBeam(p,b,a,c,.13*(.4+.6*(1-from)),(int)(alpha*(1-age/20F)));
            if(near&&i%4==0)sprite(p,b,"rainbow_motes",a,.09,0xFFFFFF,(int)(alpha*(1-age/20F)),i*.3F);
        }
        if(age<8)sprite(p,b,"leap_flash",start,.18+age*.04,0xFFFFFF,(int)(alpha*(1-age/8)),age*.03F);
        if(age>=3&&age<16)sprite(p,b,"leap_flash",end,.25+(age-3)*.025,0xFFFFFF,(int)(alpha*(1-(age-3)/13F)),-age*.03F);
    }
    private static void sodaImpact(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource b,int alpha,boolean near,boolean frost){
        float hit=hitFade(e,age,10);if(hit<=0)return;
        int frame=Math.min(3,(int)((1-hit)*4));
        sprite(p,b,"soda_burst_"+frame,Vec3.ZERO,.22+(1-hit)*1.25,0xFFFFFF,(int)(alpha*hit),0);
        for(int i=0;i<(near?8:3);i++){
            double a=i*2.399963,r=(1-hit)*1.5;
            sprite(p,b,"soda_bubble",new Vec3(Math.cos(a)*r,.1+(1-hit)*.8,Math.sin(a)*r),.04+hit*.05,0xFFFFFF,(int)(alpha*hit),i);
        }
        if(frost){
            floor(p,b,"frost_crack",new Vec3(0,.055,0),e.visual().getFloat("Radius"),0xFFFFFF,(int)(alpha*hit));
            if(near)for(int i=0;i<4;i++)sprite(p,b,"frost_snow",new Vec3(Math.cos(i)*.6,(1-hit)*.5,Math.sin(i)*.6),.08*hit,0xD7FFF4,(int)(alpha*hit),i);
        }
    }
    private static float hitFade(CandyEffectEntity e,float age,float ticks){
        return e.visual().getInt("PixelHitSequence")>0?Mth.clamp(1-(age-e.visual().getInt("PixelHitAge"))/ticks,0,1):0;
    }
    private static void floor(PoseStack p,MultiBufferSource b,String tex,Vec3 at,double size,int color,int alpha){
        plane(p,b,tex,at,new Vec3(1,0,0),new Vec3(0,0,-1),size,color,alpha);
    }
    private static void floorLit(PoseStack p,MultiBufferSource b,ResourceLocation tex,Vec3 c,double r,int alpha,int light){
        VertexConsumer v=b.getBuffer(RenderType.entityTranslucent(tex));
        vertex(p,v,c.add(-r,0,r),0,1,alpha,light);vertex(p,v,c.add(r,0,r),1,1,alpha,light);
        vertex(p,v,c.add(r,0,-r),1,0,alpha,light);vertex(p,v,c.add(-r,0,-r),0,0,alpha,light);
    }
    private static void rainbowBeam(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,double width,int alpha){
        Vec3 dir=to.subtract(from);if(dir.lengthSqr()<1e-8)return;dir=dir.normalize();
        Vec3 side=dir.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=dir.cross(new Vec3(1,0,0));side=side.normalize().scale(width);
        Vec3 up=dir.cross(side).normalize().scale(width);VertexConsumer v=b.getBuffer(RenderType.entityTranslucentEmissive(RIBBON));
        strip(p,v,from,to,side,alpha);strip(p,v,from,to,up,alpha);
    }
    private static void strip(PoseStack p,VertexConsumer v,Vec3 from,Vec3 to,Vec3 side,int alpha){
        vertex(p,v,from.add(side),0,0,alpha,15728880);vertex(p,v,from.subtract(side),0,1,alpha,15728880);
        vertex(p,v,to.subtract(side),1,1,alpha,15728880);vertex(p,v,to.add(side),1,0,alpha,15728880);
    }
    private static void vertex(PoseStack p,VertexConsumer v,Vec3 at,float u,float w,int alpha,int light){
        v.vertex(p.last().pose(),(float)at.x,(float)at.y,(float)at.z).color(255,255,255,Mth.clamp(alpha,0,255)).uv(u,w)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,1,0).endVertex();
    }
}
