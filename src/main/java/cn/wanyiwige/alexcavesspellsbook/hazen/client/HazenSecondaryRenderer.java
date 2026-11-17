package cn.wanyiwige.alexcavesspellsbook.hazen.client;

import cn.wanyiwige.alexcavesspellsbook.hazen.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class HazenSecondaryRenderer extends GeoEntityRenderer<HazenSecondaryProjectile>{
    public HazenSecondaryRenderer(EntityRendererProvider.Context context){super(context,new Model());shadowRadius=0;}
    private static final class Model extends GeoModel<HazenSecondaryProjectile>{
        public ResourceLocation getModelResource(HazenSecondaryProjectile e){return new ResourceLocation(e.visual().model()!=null?e.visual().model():"irons_spellbooks:geo/fiery_dagger.geo.json");}
        public ResourceLocation getTextureResource(HazenSecondaryProjectile e){var textures=e.visual().textures();return new ResourceLocation(textures.length>0?textures[Math.floorMod(e.tickCount/Math.max(1,e.visual().frame_ticks()),textures.length)]:"irons_spellbooks:textures/entity/fiery_dagger.png");}
        public ResourceLocation getAnimationResource(HazenSecondaryProjectile e){return new ResourceLocation(e.visual().animation()!=null?e.visual().animation():"irons_spellbooks:animations/wizard_armor_animation.json");}
        @Override public RenderType getRenderType(HazenSecondaryProjectile e,ResourceLocation texture){return switch(e.visual().render_type()){case "eyes"->RenderType.eyes(texture);case "energy"->RenderType.energySwirl(texture,0,0);case "translucent"->RenderType.entityTranslucent(texture);case "magic"->io.redspace.ironsspellbooks.render.RenderHelper.CustomerRenderType.magic(texture);default->RenderType.entityCutoutNoCull(texture);};}
    }
    @Override public void preRender(PoseStack pose,HazenSecondaryProjectile entity,BakedGeoModel model,MultiBufferSource source,VertexConsumer buffer,boolean rerender,float partial,int light,int overlay,float r,float g,float b,float a){
        var scale=entity.visual().scale();if(scale!=null)pose.scale(scale[0],scale[1],scale[2]);
        if(!entity.stationary()){
            var motion=entity.getDeltaMovement();pose.mulPose(Axis.YP.rotationDegrees((float)(90-Math.toDegrees(Math.atan2(motion.z,motion.x)))));pose.mulPose(Axis.XP.rotationDegrees((float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()))));
        }
        super.preRender(pose,entity,model,source,buffer,rerender,partial,light,overlay,r,g,b,a);
    }
    /** Original ice-arrow planes; particle-only clouds/streams have no extra solid mesh. */
    public static final class NativeSprite extends EntityRenderer<HazenSecondaryProjectile>{
        private final String kind;
        public NativeSprite(EntityRendererProvider.Context context,String kind){super(context);this.kind=kind;}
        @Override public ResourceLocation getTextureLocation(HazenSecondaryProjectile e){return HazenEquipment.id("textures/hazen/secondary/spells/ice_arrow.png");}
        @Override public void render(HazenSecondaryProjectile e,float yaw,float partial,PoseStack pose,MultiBufferSource source,int light){
            if(!kind.equals("ice_arrow"))return;
            pose.pushPose();var motion=e.getDeltaMovement();pose.mulPose(Axis.YP.rotationDegrees((float)(90-Math.toDegrees(Math.atan2(motion.z,motion.x)))));pose.mulPose(Axis.XP.rotationDegrees((float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()))));pose.scale(.13f,.13f,.13f);pose.translate(-2,0,0);
            var consumer=source.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(e)));
            for(int side=0;side<4;side++){
                pose.mulPose(Axis.XP.rotationDegrees(90));float[][] verts={{-8,-2,0,0,0},{8,-2,0,.5f,0},{8,2,0,.5f,.15625f},{-8,2,0,0,.15625f}};
                for(var v:verts)consumer.vertex(pose.last().pose(),v[0],v[1],v[2]).color(200,200,200,255).uv(v[3],v[4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(pose.last().normal(),0,1,0).endVertex();
            }pose.popPose();
        }
    }
}
