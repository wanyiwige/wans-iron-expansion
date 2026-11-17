package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyEffectEntity;
import cn.wanyiwige.alexcavesspellsbook.candy.CandyFirstRules;
import cn.wanyiwige.alexcavesspellsbook.candy.CandySpellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.github.alexmodguy.alexscaves.client.model.FrostmintSpearModel;
import static cn.wanyiwige.alexcavesspellsbook.candy.client.CandyPixelScenes.*;

/** Pixel fields and the original Alex's Caves frostmint spear model; no decorative server entities. */
final class CandyFieldPixelScenes {
    private static final FrostmintSpearModel NATIVE_SPEAR=new FrostmintSpearModel();
    private static final ResourceLocation SPEAR_TEXTURE=ResourceLocation.fromNamespaceAndPath("alexscaves","textures/entity/frostmint_spear.png");
    private CandyFieldPixelScenes() { }
    static boolean handles(String id){return id.equals("licorice_hex_wave")||id.equals("sugar_seal_bloom")||id.equals("frostmint_rain");}
    static boolean render(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource b,int light){
        String id=e.visual().getString("Spell");if(!handles(id))return false;
        float active=Math.max(0,age-4),fade=e.stage()==CandyEffectEntity.Stage.END?Mth.clamp(1-(age-e.visual().getInt("StageAge"))/8F,0,1):1;
        boolean near=CandyVisuals.detail(e);int alpha=(int)(225*fade);
        double radius=Math.max(.1,e.visual().getFloat("Radius"));
        if(id.equals("licorice_hex_wave")){
            float pulse=eventFade(e,age,8);
            p.pushPose();p.mulPose(Axis.YP.rotation(e.visual().getFloat("PixelHeading")));
            floor(p,b,"licorice_wave_"+((int)(active/4)%3),new Vec3(0,.035,0),radius,0xFFFFFF,(int)(alpha*(.48+.42*pulse)));
            if(near)for(int i=0;i<6;i++){
                double a=i*Math.PI/3+active*.013;
                Vec3 at=new Vec3(Math.cos(a)*radius*.80,.16+.09*Math.sin(active*.13+i),Math.sin(a)*radius*.80);
                sprite(p,b,"smoke",at,.13+.10*pulse,0xA276B3,(int)(alpha*.3),active*.025F+i);
                if(pulse>.1)sprite(p,b,"spark",at,.045,0xF3C4D9,(int)(alpha*pulse),i);
            }
            p.popPose();
        }else if(id.equals("sugar_seal_bloom")){
            float grow=Mth.clamp(active/20F,.18F,1);
            floor(p,b,"sugar_flower",new Vec3(0,.04,0),radius*grow,0xFFFFFF,(int)(alpha*(active<20?.35+.4*grow:.30)));
            for(int band=0;band<3;band++){
                float t=active-CandyFirstRules.bloomTick(band);
                if(t>=0&&t<9){
                    int a=(int)(alpha*(1-t/9));
                    floor(p,b,"seal_band_"+band,new Vec3(0,.065+band*.008,0),radius,0xFFFFFF,a);
                    if(near)for(int i=0;i<8;i++){
                        double angle=i*Math.PI/4;double r=radius*(band+1)/3;
                        sprite(p,b,"crumb",new Vec3(Math.cos(angle)*r,.10+t*.035,Math.sin(angle)*r),.045,0xFFCBDF,a,i+t*.1F);
                    }
                }
            }
        }else if(e.visual().getInt("Role")==CandySpellEntity.SPEAR){
            if(e.visual().getInt("PixelDetonated")==0){
                // Match the native projectile renderer transform, without adding its combat entity.
                Vec3 v=e.getDeltaMovement();
                float yaw=(float)Math.toDegrees(Math.atan2(v.x,v.z));
                float pitch=(float)Math.toDegrees(Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z)));
                p.pushPose();p.translate(0,.25,0);
                p.mulPose(Axis.YP.rotationDegrees(yaw-180));p.mulPose(Axis.XP.rotationDegrees(pitch+90));
                p.translate(0,.25,0);
                // Native setupAnim only resets this model's pose; no dummy entity or unsafe cast needed.
                NATIVE_SPEAR.resetToDefaultPose();
                NATIVE_SPEAR.renderToBuffer(p,b.getBuffer(RenderType.entityCutoutNoCull(SPEAR_TEXTURE)),light,OverlayTexture.NO_OVERLAY,1,1,1,fade);
                p.popPose();
                if(near)for(int i=0;i<3;i++)sprite(p,b,"frost_snow",new Vec3(Math.sin(age*.2+i)*.12,.5+i*.2,Math.cos(age*.2+i)*.12),.045,0xD6FFF5,alpha/2,age*.03F+i);
            }
            float hit=eventFade(e,age,9);
            if(hit>0){
                Vec3 at=new Vec3(e.visual().getDouble("PixelHitX")-e.getX(),e.visual().getDouble("PixelHitY")-e.getY(),e.visual().getDouble("PixelHitZ")-e.getZ());
                double burst=e.visual().contains("BurstRadius")?e.visual().getFloat("BurstRadius"):2.4;
                floor(p,b,"frost_crack",at.add(0,.035,0),burst*(1-hit*.4),0xFFFFFF,(int)(alpha*hit));
                sprite(p,b,"burst_1",at,.25+(1-hit)*.4,0xD3FFF8,(int)(alpha*hit),0);
                if(near)for(int i=0;i<6;i++){
                    double a=i*Math.PI/3,r=(1-hit)*1.1;
                    sprite(p,b,"frost_snow",at.add(Math.cos(a)*r,(1-hit)*.5,Math.sin(a)*r),.07*hit,0xD5FFF8,(int)(alpha*hit),i+age*.05F);
                }
            }
        }else{
            // No ground circle: actual falling spears provide the spatial cue.
            if(near&&active<12)sprite(p,b,"frost_snow",new Vec3(0,.2,0),.2,0xDEFFFA,(int)(alpha*(1-active/12)),age*.025F);
        }
        return true;
    }
    private static float eventFade(CandyEffectEntity e,float age,float duration){
        if(e.visual().getInt("PixelHitSequence")<=0)return 0;
        return Mth.clamp(1-(age-e.visual().getInt("PixelHitAge"))/duration,0,1);
    }
    private static void floor(PoseStack p,MultiBufferSource b,String tex,Vec3 centre,double radius,int color,int alpha){
        // Face normal points up; no camera-facing billboard for the affected ground area.
        plane(p,b,tex,centre,new Vec3(1,0,0),new Vec3(0,0,-1),radius,color,alpha);
    }
}
