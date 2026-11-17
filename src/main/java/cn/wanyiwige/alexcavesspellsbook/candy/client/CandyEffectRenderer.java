package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import static cn.wanyiwige.alexcavesspellsbook.client.render.MagneticMeshes.*;

/** Eight reusable scene primitives, depth tested and independent of witch post-processing. */
public final class CandyEffectRenderer extends EntityRenderer<CandyEffectEntity> {
    private static final ResourceLocation WHITE = CandyRegistry.id("textures/entity/magnetic_mesh.png");
    private static final ResourceLocation HEX = ResourceLocation.fromNamespaceAndPath("alexscaves", "textures/entity/sugar_staff_hex.png");
    private static final ResourceLocation PEPPERMINT = ResourceLocation.fromNamespaceAndPath("alexscaves", "textures/block/large_peppermint_front.png");
    public CandyEffectRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius=0; }
    @Override public boolean shouldRender(CandyEffectEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.distanceToSqr(x,y,z) < 64*64;
    }
    @Override public void render(CandyEffectEntity e, float yaw, float partial, PoseStack p, MultiBufferSource buffers, int light) {
        float age=e.renderAge(partial);
        if(CandyPixelScenes.render(e,age,partial,p,buffers,light)){super.render(e,yaw,partial,p,buffers,light);return;}
        int alpha=e.stage()==CandyEffectEntity.Stage.END ? Math.max(0,(int)(220*(1-(age-e.visual().getInt("StageAge"))/8))) : 220;
        int color=CandyVisuals.COLORS[Math.floorMod(e.visual().getInt("Color"),12)];
        boolean detail=CandyVisuals.detail(e);
        if(CandyLastVisualScenes.render(e,age,p,buffers,alpha,detail)||CandyFirstVisualScenes.render(e,age,p,buffers,alpha,detail)){super.render(e,yaw,partial,p,buffers,light);return;}
        VertexConsumer b=buffers.getBuffer(RenderType.entityTranslucent(WHITE));
        double radius=Math.max(0.2,Math.min(8,e.visual().getFloat("Radius")));
        if(e.stage()==CandyEffectEntity.Stage.START) ring(p,b,Vec3.ZERO,0.2+age*0.07,0.035,0,age*0.1,CandyVisuals.CREAM,alpha,detail?24:12);
        switch(e.mode()) {
            case BALL -> {
                VertexConsumer ball=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("alexscaves",
                        "textures/entity/gumball/gumball_"+Math.floorMod(e.visual().getInt("Color"),11)+".png")));
                core(p,ball,Vec3.ZERO,0.15,color,alpha);
            }
            case MINT -> {
                VertexConsumer mint=buffers.getBuffer(RenderType.entityTranslucent(PEPPERMINT));
                p.pushPose(); p.mulPose(com.mojang.math.Axis.YP.rotation(age*0.18F));
                plane(p,mint,0.35,0,0xFFFFFF,alpha); p.popPose();
                ring(p,b,Vec3.ZERO,0.36,0.025,0,0,CandyVisuals.MINT,alpha,16);
            }
            case GLYPH -> {
                VertexConsumer hex=buffers.getBuffer(RenderType.entityTranslucentEmissive(HEX));
                for(int i=0;i<CandyVisuals.layers(e);i++) {
                    p.pushPose(); p.mulPose(com.mojang.math.Axis.YP.rotation(age*0.015F*(i%2==0?1:-1)));
                    plane(p,hex,radius*(1-i*0.12),0.02+i*0.08,CandyVisuals.BERRY,alpha/(i+1)); p.popPose();
                }
            }
            case GROUND -> {
                plane(p,b,radius,0.02,0xBD795C,alpha/2);
                ring(p,b,Vec3.ZERO,radius,0.06,0,age*0.01,CandyVisuals.CREAM,alpha,detail?32:12);
            }
            case ROCKET -> {
                box(p,b,Vec3.ZERO,0.12,0.3,0.12,CandyVisuals.BERRY,alpha);
                core(p,b,new Vec3(0,0.32,0),0.13,CandyVisuals.MINT,alpha);
                rainbow(p,b,Vec3.ZERO,new Vec3(0,-0.9,0),alpha,detail?12:4);
            }
            case PHANTOM -> {
                core(p,b,new Vec3(0,0.5,0),0.3,CandyVisuals.MINT,alpha/2);
                rainbow(p,b,Vec3.ZERO,e.anchor("Second").subtract(e.position()),alpha,detail?16:4);
            }
            case SUMMON -> {
                // Summoned mob bodies use their own native renderer; this is the translucent portal/cue.
                for(int i=0;i<CandyVisuals.layers(e);i++) ring(p,b,new Vec3(0,0.1+i*0.15,0),0.6+i*0.15,0.05,0,age*0.03,color,alpha/(i+1),detail?24:12);
                gummy(p,b,new Vec3(0,0.45,0),color,alpha,CandyVisuals.layers(e));
            }
            case ANCHOR -> rainbow(p,b,e.anchor("Anchor").subtract(e.position()),e.anchor("Second").subtract(e.position()),alpha,detail?24:6);
        }
        if(e.stage()==CandyEffectEntity.Stage.IMPACT || e.stage()==CandyEffectEntity.Stage.END)
            ring(p,b,Vec3.ZERO,0.3+(age-e.visual().getInt("StageAge"))*0.12,0.045,0,0,color,alpha,detail?24:12);
        super.render(e,yaw,partial,p,buffers,light);
    }
    private static void plane(PoseStack p, VertexConsumer b, double r, double y, int color, int alpha) {
        Vec3 a=new Vec3(-r,y,-r),c=new Vec3(r,y,r),d=new Vec3(r,y,-r),v=new Vec3(-r,y,r);
        quad(p,b,a,v,c,d,color,alpha); quad(p,b,d,c,v,a,color,alpha);
    }
    /** Transparent shell with a visible candy core; no post-processing dependency. */
    public static void gummy(PoseStack p, VertexConsumer b, Vec3 center, int color, int alpha, int layers) {
        core(p,b,center,0.12,CandyVisuals.CREAM,alpha);
        for(int i=0;i<Math.min(3,layers);i++) core(p,b,center,0.25+i*0.025,color,Math.max(0,alpha/(4+i)));
    }
    public static void rainbow(PoseStack p, VertexConsumer b, Vec3 from, Vec3 to, int alpha, int segments) {
        Vec3 last=from;
        for(int i=1;i<=segments;i++) {
            double t=(double)i/segments;
            Vec3 next=from.lerp(to,t).add(0,Math.sin(t*Math.PI)*0.4,0);
            beam(p,b,last,next,0.04,CandyVisuals.COLORS[(i-1)%6],alpha); last=next;
        }
    }
    @Override public ResourceLocation getTextureLocation(CandyEffectEntity entity) { return WHITE; }
}
