package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyEffectEntity;
import cn.wanyiwige.alexcavesspellsbook.mixin.client.CaniacModelAccessor;
import com.github.alexmodguy.alexscaves.client.model.CaniacModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import static cn.wanyiwige.alexcavesspellsbook.candy.client.CandyPixelScenes.*;

/** Last six spells: native arms/worm/summons, pixel domains, and modeled cream. */
final class CandyFinalPixelScenes {
    private static final CaniacModel CANIAC=new CaniacModel();
    private static final ResourceLocation ARM=ResourceLocation.fromNamespaceAndPath("alexscaves","textures/entity/caniac.png");
    private static final String[] FLAVORS={"vanilla","strawberry","chocolate","mint","blueberry","grape"};
    private static final int[] COLORS={0xFFEABD,0xF5A6B9,0xB4805A,0x9CDFBD,0x98BFE9,0xC59FDF};
    private static final int[] SHOT_COLORS={0xFF729B,0xFFB253,0xFFF1A1,0x8CDFAB,0x72C9F2,0xAE8BE4};
    private CandyFinalPixelScenes() { }
    static boolean render(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,int light){
        String id=e.visual().getString("Spell");
        if(!id.equals("candy_cane_whirl")&&!id.equals("gumball_sentry")&&!id.equals("gum_worm_breach")&&!id.equals("candy_banquet")&&!id.equals("ice_cream_storm")&&!id.equals("licorice_grand_feast"))return false;
        float end=e.stage()==CandyEffectEntity.Stage.END?Mth.clamp((age-e.visual().getInt("StageAge"))/8F,0,1):0;
        int alpha=(int)(240*(1-end));boolean near=CandyVisuals.detail(e);
        int role=e.visual().getInt("LastRole");
        if(role==2||role==3){projectile(e,age,partial,p,b,light,alpha,near,role==3);return true;}
        switch(id){
            case "candy_cane_whirl" -> whirl(e,age,p,b,light,alpha,near,end);
            case "gumball_sentry" -> {summons(e,age,p,b,alpha,near);muzzle(e,age,p,b,alpha);}
            case "gum_worm_breach" -> CandyNativeWormScene.render(e,age,partial,p,b,light,alpha);
            case "candy_banquet" -> {domain(e,age,partial,p,b,alpha,near,end,false);}
            case "ice_cream_storm" -> {
                // The falling actors show actual spawn sites; no fictional second rain layer.
                if(age<10)sprite(p,b,"frost_snow",new Vec3(0,.15,0),.15,0xE4F5FF,(int)(alpha*(1-age/10)),0);
            }
            case "licorice_grand_feast" -> {domain(e,age,partial,p,b,alpha,near,end,true);summons(e,age,p,b,alpha,near);muzzle(e,age,p,b,alpha);}
        }
        return true;
    }
    private static float hit(CandyEffectEntity e,float age,float duration){
        return e.visual().getInt("LastHitSequence")>0?Mth.clamp(1-(age-e.visual().getInt("LastHitAge"))/duration,0,1):0;
    }
    private static void whirl(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource b,int light,int alpha,boolean near,float end){
        double radius=Math.max(.3,e.visual().getFloat("Radius"));
        CANIAC.resetToDefaultPose();
        CaniacModelAccessor nativeParts=(CaniacModelAccessor)(Object)CANIAC;
        AdvancedModelBox[] arms={nativeParts.wan$getLeftArm(),nativeParts.wan$getRightArm()};
        for(int i=0;i<2;i++){
            AdvancedModelBox arm=arms[i];arm.setRotationPoint(0,0,0);arm.rotateAngleX=0;arm.rotateAngleY=0;arm.rotateAngleZ=0;
            for(int ghost=near?2:0;ghost>=0;ghost--){
                float angle=age*.38F+i*(float)Math.PI-ghost*.25F;
                p.pushPose();p.mulPose(Axis.YP.rotation(-angle));p.translate(.25,0,0);p.mulPose(Axis.ZP.rotation((float)Math.PI/2));
                float size=(float)((radius-.25)/1.98)*(1-end*.15F);p.scale(size,-size,size);
                arm.render(p,b.getBuffer(RenderType.entityTranslucent(ARM)),light,OverlayTexture.NO_OVERLAY,1,1,1,alpha/255F/(ghost==0?1:ghost*5F));p.popPose();
            }
            double a=age*.38+i*Math.PI;
            if(near)for(int j=0;j<3;j++){
                double t=a-j*.22;Vec3 at=new Vec3(Math.cos(t)*radius,0,Math.sin(t)*radius);
                plane(p,b,"cane_swipe",at,new Vec3(Math.cos(t),0,Math.sin(t)),new Vec3(-Math.sin(t),0,Math.cos(t)),.30,0xFFFFFF,alpha/(j+2));
            }
            if(e.visual().contains("WhirlPulseAge")){
                float pulse=Mth.clamp(1-(age-e.visual().getInt("WhirlPulseAge"))/4F,0,1);
                if(pulse>0)sprite(p,b,"mint_slash",new Vec3(Math.cos(a)*radius,0,Math.sin(a)*radius),.22,0xFFE3E5,(int)(alpha*pulse),age*.1F);
            }
        }
    }
    private static void projectile(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,int light,int alpha,boolean near,boolean ice){
        int color=Math.floorMod(e.visual().getInt("LastPalette"),6);float impact=hit(e,age,ice?10:7);
        boolean active=e.stage()==CandyEffectEntity.Stage.START||e.stage()==CandyEffectEntity.Stage.ACTIVE;
        if(active){
            p.pushPose();p.mulPose(Axis.YP.rotation(age*.11F+color));p.mulPose(Axis.ZP.rotation(age*.07F));
            float scale=ice?.66F:.42F;p.scale(scale,scale,scale);p.translate(-.5,-.5,-.5);
            var mc=Minecraft.getInstance();var model=mc.getModelManager().getModel((ice?ICE_MODELS:RAINBOW_MODELS).get(color));
            mc.getBlockRenderer().getModelRenderer().renderModel(p.last(),b.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),null,model,1,1,1,light,OverlayTexture.NO_OVERLAY);p.popPose();
            if(near){
                pathTrail(e,partial,p,b,ice?COLORS[color]:SHOT_COLORS[color],alpha/2);
                if(ice)for(int j=0;j<3;j++){
                    float t=(age*.1F+j/3F)%1;
                    sprite(p,b,"icecream_drop_"+FLAVORS[color],new Vec3(Math.sin(j*2.4)*.12,t*.65,Math.cos(j*2.4)*.12),.045,0xFFFFFF,(int)(alpha*(1-t)),j);
                }
            }
        }
        if(impact<=0)return;
        float t=1-impact;
        if(ice){
            if(e.visual().getInt("LastFloorImpact")==1)
                plane(p,b,"icecream_splat_"+FLAVORS[color],new Vec3(0,.025,0),new Vec3(1,0,0),new Vec3(0,0,-1),.25+t*.8,0xFFFFFF,(int)(alpha*impact));
            else sprite(p,b,"icecream_splat_"+FLAVORS[color],Vec3.ZERO,.22+t*.32,0xFFFFFF,(int)(alpha*impact),0);
            for(int j=0;j<(near?7:3);j++){
                double a=j*2.399963,r=t*.95;
                sprite(p,b,"icecream_drop_"+FLAVORS[color],new Vec3(Math.cos(a)*r,Math.sin(t*Math.PI)*.45,Math.sin(a)*r),.06*impact,0xFFFFFF,(int)(alpha*impact),j+age*.1F);
            }
            if(near)sprite(p,b,"frost_snow",new Vec3(0,.12,0),.25+t*.35,0xE9F9FF,(int)(alpha*impact*.4),-age*.025F);
        }else{
            sprite(p,b,"burst_"+Math.min(3,(int)(t*4)),Vec3.ZERO,.12+t*.35,SHOT_COLORS[color],(int)(alpha*impact),0);
            for(int i=0;i<(near?5:2);i++){
                double a=i*2.399963;sprite(p,b,"crumb",new Vec3(Math.cos(a)*t*.5,Math.sin(a)*t*.3,Math.sin(a)*t*.5),.045*impact,SHOT_COLORS[color],(int)(alpha*impact),i+age*.12F);
            }
        }
    }
    private static void domain(CandyEffectEntity e,float age,float partial,PoseStack p,MultiBufferSource b,int alpha,boolean near,float end,boolean dark){
        double radius=Math.max(.2,e.visual().getFloat("Radius"));float grow=Mth.clamp(age/14F,0,1);
        float pulse=hit(e,age,8);int count=near?12:8;
        for(int i=0;i<count;i++){
            double a=i*Math.PI*2/count,r=radius*grow;
            if(dark){
                Vec3 at=new Vec3(Math.cos(a)*r*.85,.04,Math.sin(a)*r*.85);
                plane(p,b,"licorice_knot",at,new Vec3(Math.cos(a),0,Math.sin(a)),new Vec3(-Math.sin(a),0,Math.cos(a)),.34,0xFFFFFF,alpha);
                Vec3 prev=at;
                for(int j=1;j<=4;j++){
                    double t=j/4.0,rad=r*(.85-.22*t),angle=a+Math.sin(t*Math.PI)*.16;
                    Vec3 next=new Vec3(Math.cos(angle)*rad,.06+Math.sin(t*Math.PI)*(.2+pulse*.35),Math.sin(angle)*rad);
                    tendril(p,b,prev,next,.10,(int)(alpha*(.65+pulse*.35)));prev=next;
                }
                if(near&&pulse>0)sprite(p,b,"spark",at.add(0,.15,0),.08,0xE8B0DC,(int)(alpha*pulse),a>0?(float)a:0);
            }else{
                // A 32px wedge is repeated around the true radius with an open center.
                p.pushPose();p.mulPose(Axis.YP.rotation((float)-a));
                plane(p,b,"banquet_frosting",new Vec3(0,.028,0),new Vec3(1,0,0),new Vec3(0,0,-1),r,0xFFFFFF,alpha/2);p.popPose();
                if(near){Vec3 at=new Vec3(Math.cos(a)*r*.82,.12,Math.sin(a)*r*.82);
                    sprite(p,b,"snare_knot",at,.10,0xFFFFFF,alpha/2,(float)a+age*.03F);}
            }
            if(pulse>0){
                double rad=radius*(.6+(1-pulse)*.38);
                sprite(p,b,dark?"licorice_knot":"spark",new Vec3(Math.cos(a)*rad,.10+pulse*.2,Math.sin(a)*rad),dark?.07:.06,dark?0xEAA4D5:0xFFE7A8,(int)(alpha*pulse),i+age*.04F);
            }
        }
        if(!dark&&near&&pulse>0){
            Vec3 origin=interpolated(e,partial);
            for(int id:e.visual().getIntArray("FeastAllies")){
                if(!(e.level().getEntity(id) instanceof LivingEntity ally)||!ally.isAlive()||!ally.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION))continue;
                Vec3 at=new Vec3(Mth.lerp(partial,ally.xo,ally.getX()),Mth.lerp(partial,ally.yo,ally.getY()),Mth.lerp(partial,ally.zo,ally.getZ())).subtract(origin).add(0,ally.getBbHeight()*.7+(1-pulse)*.3,0);
                sprite(p,b,"feast_heal",at,.10,0xFFFFFF,(int)(alpha*pulse),0);
            }
        }
    }
    private static Vec3 interpolated(CandyEffectEntity e,float partial){return new Vec3(Mth.lerp(partial,e.xo,e.getX()),Mth.lerp(partial,e.yo,e.getY()),Mth.lerp(partial,e.zo,e.getZ()));}
    private static void summons(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource b,int alpha,boolean near){
        if(!e.visual().contains("SummonCueAge"))return;
        float t=(age-e.visual().getInt("SummonCueAge"))/16F;if(t<0||t>=1)return;
        for(int i=0;i<Math.min(4,e.visual().getInt("SummonCues"));i++){
            Vec3 at=new Vec3(e.visual().getDouble("SummonX"+i),e.visual().getDouble("SummonY"+i),e.visual().getDouble("SummonZ"+i)).subtract(e.position());
            int kind=e.visual().getInt("SummonKind"+i);
            for(int j=0;j<(near?7:3);j++){
                double a=j*2.399963,r=.15+t*.6;
                sprite(p,b,kind==1?"worm_crumbs":"crumb",at.add(Math.cos(a)*r,.08+Math.sin(t*Math.PI)*.35,Math.sin(a)*r),.06,kind==0?0xF0A3A7:kind==1?0xDAB07F:0xDFA8E9,(int)(alpha*(1-t)),j+age*.08F);
            }
        }
    }
    private static void muzzle(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource b,int alpha){
        if(!e.visual().contains("SentryShotAge"))return;float t=(age-e.visual().getInt("SentryShotAge"))/5F;
        if(t<0||t>=1)return;
        Vec3 at=new Vec3(e.visual().getDouble("MuzzleX"),e.visual().getDouble("MuzzleY"),e.visual().getDouble("MuzzleZ")).subtract(e.position());
        sprite(p,b,"burst_"+Math.min(3,(int)(t*4)),at,.10+t*.13,0xFFE9C6,(int)(alpha*(1-t)),0);
    }
    private static void tendril(PoseStack p,MultiBufferSource b,Vec3 a,Vec3 c,double width,int alpha){
        Vec3 d=c.subtract(a);if(d.lengthSqr()<1e-7)return;
        Vec3 side=d.normalize().cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);side=side.normalize().scale(width);
        plane(p,b,"licorice_tendril",a.add(c).scale(.5),d.scale(.5),side,1,0xFFFFFF,alpha);
        plane(p,b,"licorice_tendril",a.add(c).scale(.5),d.scale(.5),d.normalize().cross(side).normalize().scale(width),1,0xFFFFFF,alpha);
    }
}
