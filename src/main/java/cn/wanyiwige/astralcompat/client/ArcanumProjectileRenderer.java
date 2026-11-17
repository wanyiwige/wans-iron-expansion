package cn.wanyiwige.astralcompat.client;

import cn.wanyiwige.astralcompat.recreation.ArcanumProjectile;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.entity.spells.fireball.FireballRenderer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class ArcanumProjectileRenderer extends EntityRenderer<ArcanumProjectile> {
    private static final ResourceLocation FIRE=new ResourceLocation("asterismarcanum","recreation/textures/entity/starfire/starfire.png");
    private static final ResourceLocation NEEDLE=new ResourceLocation("irons_spellbooks","textures/entity/blood_needle/needle_5.png");
    private final ModelPart body;
    public ArcanumProjectileRenderer(EntityRendererProvider.Context context) {
        super(context); body=context.bakeLayer(FireballRenderer.MODEL_LAYER_LOCATION).getChild("body");
    }
    @Override public void render(ArcanumProjectile entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light) {
        stack.pushPose();
        Vec3 motion=entity.getDeltaMovement();
        stack.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(motion.x,motion.z))));
        stack.mulPose(Axis.XP.rotationDegrees((float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()))));
        stack.scale(entity.scale(),entity.scale(),entity.scale());
        if(entity.isStarfire()) body.render(stack,buffers.getBuffer(RenderType.energySwirl(FIRE,0,0)),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        else {
            stack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount+partial)*40));
            VertexConsumer v=buffers.getBuffer(RenderType.entityTranslucentEmissive(NEEDLE));
            ArcanumEffectRenderer.quad(stack,v,1.25F,1.25F,230,230,220,255);
            stack.mulPose(Axis.YP.rotationDegrees(90));
            ArcanumEffectRenderer.quad(stack,v,1.25F,1.25F,230,230,220,255);
        }
        stack.popPose(); super.render(entity,yaw,partial,stack,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(ArcanumProjectile entity) { return entity.isStarfire()?FIRE:NEEDLE; }
}
