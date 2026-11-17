package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import static cn.wanyiwige.alexcavesspellsbook.client.render.MagneticMeshes.*;

/** Distinct geometry for each late-school effect; native summons render their own bodies. */
public final class CandyLastVisualScenes {
    private static final ResourceLocation WHITE=CandyRegistry.id("textures/entity/magnetic_mesh.png");
    private CandyLastVisualScenes(){}
    public static boolean render(CandyEffectEntity e,float age,PoseStack p,MultiBufferSource buffers,int alpha,boolean detail){
        if(e.visual().getInt("LastProgram")!=1)return false;
        VertexConsumer b=buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        String id=e.visual().getString("Spell");int role=e.visual().getInt("LastRole");
        double r=e.visual().getFloat("Radius");
        if(e.stage()==CandyEffectEntity.Stage.END)alpha=Math.max(0,alpha);
        if(role!=0){
            if(role==1){
                p.pushPose();p.mulPose(Axis.YP.rotation(age*0.22F));
                core(p,b,Vec3.ZERO,0.2,0xE5FCFF,alpha);
                for(int i=0;i<4;i++){double a=i*Math.PI/2;beam(p,b,Vec3.ZERO,new Vec3(Math.cos(a)*0.36,0.18,Math.sin(a)*0.36),0.035,CandyVisuals.COLORS[i],alpha);}p.popPose();
            }else if(role==2){core(p,b,Vec3.ZERO,0.18,CandyVisuals.COLORS[Math.floorMod(e.visual().getInt("Index"),6)],alpha);ring(p,b,Vec3.ZERO,0.23,0.025,0,age*0.15,CandyVisuals.CREAM,alpha,12);}
            else {core(p,b,Vec3.ZERO,0.32,0xFFE4E5,alpha);core(p,b,new Vec3(0,0.16,0),0.22,0xFFF3C3,alpha);ring(p,b,Vec3.ZERO,0.36,0.03,0,age*0.08,0x98EAF2,alpha,12);}
            return true;
        }
        switch(id){
            case "sugar_glass_stars" -> {
                ring(p,b,Vec3.ZERO,0.42,0.035,Math.PI/2,age*0.12,0xB8F4FA,alpha,16);
                for(int i=0;i<6;i++){double a=i*Math.PI/3+age*0.08;core(p,b,new Vec3(Math.cos(a)*0.3,Math.sin(a)*0.3,0),0.045,CandyVisuals.COLORS[i],alpha);}
            }
            case "rainbow_leap" -> {
                CandyEffectRenderer.rainbow(p,b,Vec3.ZERO,e.anchor("Second").subtract(e.position()),alpha,detail?18:8);
                ring(p,b,Vec3.ZERO,0.5+age*0.04,0.04,0,age*0.1,CandyVisuals.CREAM,alpha,16);
            }
            case "sweet_overdrive" -> {
                for(int i=0;i<3;i++){double a=age*0.16+i*Math.PI*2/3;
                    ring(p,b,new Vec3(0,Math.sin(a)*0.5,0),0.62,0.025,Math.PI/2,a,CandyVisuals.COLORS[i*2],alpha,16);}
                core(p,b,new Vec3(0,0.65,0),0.14,0xFFE78A,alpha/2);
            }
            case "rainbow_snare" -> {
                ring(p,b,Vec3.ZERO,r,0.08,0,age*0.025,0xED83CE,alpha,detail?32:16);
                for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 foot=new Vec3(Math.cos(a)*r,0,Math.sin(a)*r);
                    beam(p,b,foot,foot.add(0,0.65+0.2*Math.sin(age*0.15+i),0),0.035,CandyVisuals.COLORS[i],alpha);}
            }
            case "candicorn_charge" -> {
                // Short-lived candy unicorn silhouette: body, legs, neck, head and a luminous horn.
                p.pushPose();p.mulPose(Axis.YP.rotation(e.visual().getFloat("Heading")));
                box(p,b,new Vec3(0,0.75,0),0.58,0.36,0.82,0xFFDEEE,alpha);
                for(int side=-1;side<=1;side+=2)for(int end=-1;end<=1;end+=2)
                    box(p,b,new Vec3(side*0.39,0.35,end*0.52),0.12,0.36,0.12,0xFFE7F2,alpha);
                box(p,b,new Vec3(0,1.12,0.5),0.22,0.55,0.2,0xFFDEEE,alpha);
                core(p,b,new Vec3(0,1.53,0.65),0.28,0xFFF2D5,alpha);
                beam(p,b,new Vec3(0,1.7,0.8),new Vec3(0,2.23,1.1),0.09,0xFFE985,alpha);
                CandyEffectRenderer.rainbow(p,b,new Vec3(0,0.8,-0.5),new Vec3(0,0.8,-2),alpha/2,8);p.popPose();
            }
            case "gummy_guardian","gingerbread_march","gumball_sentry" -> {
                int count=e.visual().getInt("Summons");
                for(int i=0;i<Math.max(1,Math.min(3,count));i++)ring(p,b,new Vec3(0,0.03+i*0.12,0),0.65+i*0.2,0.05,0,age*0.03,CandyVisuals.COLORS[(i*3)%6],alpha/(i+1),detail?24:12);
                if(id.equals("gumball_sentry"))for(int i=0;i<4;i++){double a=i*Math.PI/2+age*0.03;core(p,b,new Vec3(Math.cos(a)*0.7,0.6,Math.sin(a)*0.7),0.07,CandyVisuals.COLORS[i],alpha);}
            }
            case "candy_cane_whirl" -> {
                for(int i=0;i<2;i++){double a=age*0.38+i*Math.PI;Vec3 tip=new Vec3(Math.cos(a)*r,0.35,Math.sin(a)*r);
                    beam(p,b,Vec3.ZERO,tip,0.1,i==0?0xFF496D:0xFFFFFF,alpha);
                    core(p,b,tip,0.18,CandyVisuals.CREAM,alpha);}
                ring(p,b,Vec3.ZERO,r,0.025,0,age*0.22,0xFF7599,alpha/2,detail?32:16);
            }
            case "gum_worm_breach" -> {
                p.pushPose();p.mulPose(Axis.YP.rotation(e.visual().getFloat("Heading")));
                for(int i=0;i<7;i++){double z=-i*0.52,y=0.5+Math.sin(age*0.35-i*0.55)*0.22;
                    core(p,b,new Vec3(0,y,z),Math.max(0.22,0.65-i*0.06),i%2==0?0xDE7EC2:0xF6B7D9,alpha);}
                beam(p,b,new Vec3(0,1.15,0),new Vec3(0,1.65,0.25),0.07,0xFFF0AA,alpha);
                p.popPose();
            }
            case "candy_banquet" -> {
                ring(p,b,Vec3.ZERO,r,0.1,0,age*0.012,0xFFCE83,alpha,detail?40:20);
                ring(p,b,new Vec3(0,0.06,0),r*0.7,0.045,0,-age*0.025,0xEE8DBB,alpha,detail?32:16);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 pos=new Vec3(Math.cos(a)*r*0.82,0.18,Math.sin(a)*r*0.82);
                    core(p,b,pos,0.13,CandyVisuals.COLORS[i],alpha);}
            }
            case "ice_cream_storm" -> {
                ring(p,b,Vec3.ZERO,r,0.05,0,age*0.012,0xAEDDF2,alpha,detail?32:16);
                for(int i=0;i<7;i++){double a=i*2.3999632297,rad=Math.sqrt((i+0.5)/7.0)*r;
                    core(p,b,new Vec3(Math.cos(a)*rad,0.2+0.12*Math.sin(age*0.08+i),Math.sin(a)*rad),0.08,0xFFEEF3,alpha/2);}
            }
            case "licorice_grand_feast" -> {
                ring(p,b,Vec3.ZERO,r,0.12,0,age*0.02,0x8A4B9A,alpha,detail?40:20);
                ring(p,b,new Vec3(0,0.08,0),r*0.66,0.055,0,-age*0.03,0xF2A1D4,alpha,detail?32:16);
                for(int i=0;i<6;i++){double a=i*Math.PI/3+age*0.01;Vec3 foot=new Vec3(Math.cos(a)*r*0.85,0,Math.sin(a)*r*0.85);
                    beam(p,b,foot,foot.add(0,0.9+0.2*Math.sin(age*0.07+i),0),0.04,0xBE72C8,alpha/2);}
                core(p,b,new Vec3(0,0.7,0),0.28,0xEC8DCB,alpha/2);
            }
        }
        if(e.stage()==CandyEffectEntity.Stage.IMPACT)
            ring(p,b,Vec3.ZERO,Math.max(0.4,Math.min(r,2.5)),0.05,0,age*0.08,CandyVisuals.CREAM,alpha,detail?24:12);
        return true;
    }
}
