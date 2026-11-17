package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.NebulousCataclysmEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Fullbright, translucent nebula sprites with no solid background plane. */
public final class NebulousCataclysmRenderer extends EntityRenderer<NebulousCataclysmEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlexCavesSpellsbook.MOD_ID, "textures/entity/nebulacloud.png");
    private static final ResourceLocation CORE = ResourceLocation.fromNamespaceAndPath(
            AlexCavesSpellsbook.MOD_ID, "textures/entity/nebulacloudcore.png");
    private static final ResourceLocation NOVA = ResourceLocation.fromNamespaceAndPath(
            AlexCavesSpellsbook.MOD_ID, "textures/entity/nebulanova.png");
    private static final ResourceLocation RANGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "astral_spell_compat", "textures/entity/blender_white.png");
    public NebulousCataclysmRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }
    @Override public void render(NebulousCataclysmEntity entity, float yaw, float partial,
                                  PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.visualAge(partial);
        if (entity.isNova() && (age < 0 || age >= NebulousCataclysmEntity.NOVA_DURATION)) return;
        float size;
        if (entity.isNova()) {
            float t = Mth.clamp(age / NebulousCataclysmEntity.NOVA_DURATION, 0, 1);
            size = (1.1F + 4.9F * Mth.sin(t * Mth.HALF_PI)) * (t > 0.75F ? (1 - t) * 4 : 1);
        } else {
            size = 1.9F * (1 + Mth.sin(age * 0.4F) * 0.07F);
        }
        if (entity.isNova()) renderRangeRing(age, pose, buffers);
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        // Camera.rotation maps local +Z along the camera's forward direction (away from it).
        // Keep the corrected front-facing direction and cloud/core layer order.
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.mulPose(Axis.ZP.rotationDegrees(age * (entity.isNova() ? -12 : 5) + entity.getId() * 37));
        pose.scale(size, size, size);
        if (entity.isNova()) pose.scale(1, 168.0F / 190, 1);
        if (entity.isNova()) {
            renderNova(age, pose, buffers);
        } else {
            quad(pose.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)),
                    -0.002F, 0, 0, 1, 1, 255, 255, 255, 153);
            int frame = Math.floorMod((int) (age / 2), 4);
            // Four 118-pixel slots, each containing a 116-pixel frame and a two-pixel gutter.
            float v0 = (frame * 118 + 0.5F) / 472;
            float v1 = (frame * 118 + 115.5F) / 472;
            quad(pose.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(CORE)),
                    0, 0.5F / 116, v0, 115.5F / 116, v1, 255, 255, 255, 204);
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }
    private static void renderRangeRing(float age, PoseStack pose, MultiBufferSource buffers) {
        // A world-space ring outlines the actual per-nova damage radius, independent of billboard size.
        float progress = Mth.clamp(age / NebulousCataclysmEntity.NOVA_DURATION, 0, 1);
        float radius = (float) NebulousCataclysmEntity.NOVA_RADIUS * Mth.clamp(age / 4, 0, 1);
        float inner = Math.max(0, radius - 0.065F);
        int alpha = Math.round(112 * (1 - progress));
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(RANGE_TEXTURE));
        for (int i = 0; i < 32; i++) {
            float a = i * Mth.TWO_PI / 32, b = (i + 1) * Mth.TWO_PI / 32;
            ringVertex(pose.last(), buffer, Mth.cos(a) * inner, Mth.sin(a) * inner, alpha);
            ringVertex(pose.last(), buffer, Mth.cos(a) * radius, Mth.sin(a) * radius, alpha);
            ringVertex(pose.last(), buffer, Mth.cos(b) * radius, Mth.sin(b) * radius, alpha);
            ringVertex(pose.last(), buffer, Mth.cos(b) * inner, Mth.sin(b) * inner, alpha);
        }
    }
    private static void ringVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float z, int alpha) {
        buffer.vertex(pose.pose(), x, 0.03F, z).color(198, 109, 250, alpha).uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, 1, 0).endVertex();
    }
    private static void renderNova(float age, PoseStack pose, MultiBufferSource buffers) {
        int frame = Mth.clamp((int) (age / NebulousCataclysmEntity.NOVA_DURATION * 14), 0, 13);
        int x = frame / 7, y = frame % 7;
        float progress = age / NebulousCataclysmEntity.NOVA_DURATION;
        int alpha = Math.round(191 * (1 - Mth.clamp((progress - 0.65F) / 0.35F, 0, 1)));
        quad(pose.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(NOVA)), 0,
                (x * 190 + 0.5F) / 380, (y * 168 + 0.5F) / 1176,
                (x * 190 + 189.5F) / 380, (y * 168 + 167.5F) / 1176, 255, 255, 255, alpha);
    }
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float z,
                             float u0, float v0, float u1, float v1, int r, int g, int b, int alpha) {
        vertex(pose, buffer, -0.5F, -0.5F, z, u0, v1, r, g, b, alpha);
        vertex(pose, buffer, 0.5F, -0.5F, z, u1, v1, r, g, b, alpha);
        vertex(pose, buffer, 0.5F, 0.5F, z, u1, v0, r, g, b, alpha);
        vertex(pose, buffer, -0.5F, 0.5F, z, u0, v0, r, g, b, alpha);
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                               float u, float v, int r, int g, int b, int alpha) {
        buffer.vertex(pose.pose(), x, y, z).color(r, g, b, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, 0, 1).endVertex();
    }
    @Override public ResourceLocation getTextureLocation(NebulousCataclysmEntity entity) { return TEXTURE; }
}
