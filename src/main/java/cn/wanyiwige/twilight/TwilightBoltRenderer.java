package cn.wanyiwige.twilight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class TwilightBoltRenderer extends EntityRenderer<TwilightBoltProjectile> {
    private static final ResourceLocation TEXTURE = TwilightFoundation.id("textures/entity/twilight_bolt.png");
    public TwilightBoltRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(TwilightBoltProjectile entity, float yaw, float partial,
                                 PoseStack stack, MultiBufferSource buffers, int light) {
        stack.pushPose();
        stack.mulPose(entityRenderDispatcher.cameraOrientation());
        stack.scale(.7F, .7F, .7F);
        Matrix4f pose = stack.last().pose();
        VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        vertex.vertex(pose, -.5F, -.5F, 0).color(255,255,255,255).uv(0,1).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        vertex.vertex(pose,  .5F, -.5F, 0).color(255,255,255,255).uv(1,1).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        vertex.vertex(pose,  .5F,  .5F, 0).color(255,255,255,255).uv(1,0).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        vertex.vertex(pose, -.5F,  .5F, 0).color(255,255,255,255).uv(0,0).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        stack.popPose();
        super.render(entity, yaw, partial, stack, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(TwilightBoltProjectile entity) { return TEXTURE; }
}
