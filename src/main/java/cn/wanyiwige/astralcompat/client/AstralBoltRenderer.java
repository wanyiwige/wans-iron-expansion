package cn.wanyiwige.astralcompat.client;

import cn.wanyiwige.astralcompat.entity.AstralBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class AstralBoltRenderer extends EntityRenderer<AstralBoltEntity> {
    private static final ResourceLocation STARFIRE_TEXTURE =
            new ResourceLocation("asterismarcanum", "textures/entity/starfire/starfire.png");
    private static final ResourceLocation NEEDLE_TEXTURE =
            new ResourceLocation("irons_spellbooks", "textures/entity/blood_needle/needle_5.png");
    private static final ResourceLocation STAR_SWARM_TEXTURE =
            new ResourceLocation("astral_spell_compat", "textures/entity/star_swarm_crystal.png");

    public AstralBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AstralBoltEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        orientToMotion(entity, poseStack);
        if (entity.getKind() == AstralBoltEntity.STARFIRE) {
            renderStarfire(entity, partialTick, poseStack, buffers);
        } else if (entity.getKind() == AstralBoltEntity.STAR_SWARM) {
            renderStarSwarm(entity, partialTick, poseStack, buffers);
        } else {
            renderNeedle(entity, partialTick, poseStack, buffers);
        }
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void orientToMotion(AstralBoltEntity entity, PoseStack poseStack) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6) {
            return;
        }
        float xRot = -((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * Mth.RAD_TO_DEG) - 90.0F);
        float yRot = -((float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
    }

    private static void renderStarfire(AstralBoltEntity entity, float partialTick, PoseStack poseStack,
                                       MultiBufferSource buffers) {
        float time = entity.tickCount + partialTick;
        float pulse = 0.96F + Mth.sin(time * 0.55F) * 0.08F;
        float modelScale = 0.78F * (entity.getScale() / 0.35F);
        poseStack.scale(modelScale, modelScale, modelScale);

        VertexConsumer energy = buffers.getBuffer(RenderType.lightning());
        drawCometTail(poseStack.last(), energy, pulse);

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 24.0F));
        drawHalo(poseStack.last(), energy, 0.53F * pulse, 0.065F, time);
        drawOctahedron(poseStack.last(), energy, 0.32F * pulse, 0.92F,
                235, 250, 255, 245, 70, 185, 255, 230);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 42.0F));
        for (int i = 0; i < 4; i++) {
            float angle = i * 90.0F * ((float) Math.PI / 180.0F);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * 0.58F, Mth.sin(angle) * 0.58F, -0.08F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            drawOctahedron(poseStack.last(), energy, 0.105F, 0.42F,
                    255, 230, 145, 220, 185, 100, 255, 205);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderNeedle(AstralBoltEntity entity, float partialTick, PoseStack poseStack,
                                     MultiBufferSource buffers) {
        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * 35.0F));
        poseStack.scale(entity.getScale(), entity.getScale(), entity.getScale());
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(NEEDLE_TEXTURE));
        drawCrossedQuad(poseStack.last(), consumer, 2.5F);
    }

    private static void renderStarSwarm(AstralBoltEntity entity, float partialTick, PoseStack poseStack,
                                        MultiBufferSource buffers) {
        float time = entity.tickCount + partialTick;
        float pulse = 0.96F + Mth.sin(time * 0.8F + entity.getId()) * 0.07F;
        float scale = entity.getScale() / 0.42F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 46.0F + entity.getId() * 29.0F));

        // The generated sprite provides the detailed crystalline facets while
        // two solid octahedra give the projectile a genuine 3D silhouette.
        VertexConsumer texture = buffers.getBuffer(RenderType.entityTranslucentEmissive(STAR_SWARM_TEXTURE));
        drawCrossedQuad(poseStack.last(), texture, 0.86F * pulse, 1.72F * pulse);

        VertexConsumer energy = buffers.getBuffer(RenderType.lightning());
        drawOctahedron(poseStack.last(), energy, 0.23F * pulse, 1.46F,
                235, 252, 255, 245, 55, 185, 255, 220);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        drawOctahedron(poseStack.last(), energy, 0.15F * pulse, 1.72F,
                255, 244, 185, 215, 135, 90, 255, 185);
        poseStack.popPose();
        drawTaperedPrism(poseStack.last(), energy, -0.55F, -1.55F,
                0.16F * pulse, 0.012F, 70, 175, 255, 120);
    }

    private static void drawOctahedron(PoseStack.Pose pose, VertexConsumer consumer,
                                       float radius, float length,
                                       int coreR, int coreG, int coreB, int coreA,
                                       int edgeR, int edgeG, int edgeB, int edgeA) {
        float front = length * 0.5F;
        float back = -length * 0.5F;
        float[][] ring = {
                {radius, 0}, {0, radius}, {-radius, 0}, {0, -radius}
        };
        for (int i = 0; i < 4; i++) {
            float[] a = ring[i];
            float[] b = ring[(i + 1) & 3];
            colorQuad(pose, consumer,
                    0, 0, front, a[0], a[1], 0, b[0], b[1], 0, 0, 0, front,
                    coreR, coreG, coreB, coreA);
            colorQuad(pose, consumer,
                    0, 0, back, b[0], b[1], 0, a[0], a[1], 0, 0, 0, back,
                    edgeR, edgeG, edgeB, edgeA);
        }
    }

    private static void drawCometTail(PoseStack.Pose pose, VertexConsumer consumer, float pulse) {
        drawTaperedPrism(pose, consumer, -0.18F, -1.85F,
                0.23F * pulse, 0.018F, 70, 170, 255, 145);
        drawTaperedPrism(pose, consumer, -0.12F, -1.25F,
                0.09F * pulse, 0.006F, 245, 250, 255, 205);
    }

    private static void drawTaperedPrism(PoseStack.Pose pose, VertexConsumer consumer,
                                         float startZ, float endZ, float startRadius, float endRadius,
                                         int red, int green, int blue, int alpha) {
        colorQuad(pose, consumer,
                -startRadius, -startRadius, startZ, startRadius, -startRadius, startZ,
                endRadius, -endRadius, endZ, -endRadius, -endRadius, endZ,
                red, green, blue, alpha);
        colorQuad(pose, consumer,
                startRadius, startRadius, startZ, -startRadius, startRadius, startZ,
                -endRadius, endRadius, endZ, endRadius, endRadius, endZ,
                red, green, blue, alpha);
        colorQuad(pose, consumer,
                -startRadius, startRadius, startZ, -startRadius, -startRadius, startZ,
                -endRadius, -endRadius, endZ, -endRadius, endRadius, endZ,
                red, green, blue, alpha);
        colorQuad(pose, consumer,
                startRadius, -startRadius, startZ, startRadius, startRadius, startZ,
                endRadius, endRadius, endZ, endRadius, -endRadius, endZ,
                red, green, blue, alpha);
    }

    private static void drawHalo(PoseStack.Pose pose, VertexConsumer consumer,
                                 float radius, float thickness, float time) {
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2.0 * i / segments);
            float a1 = (float) (Math.PI * 2.0 * (i + 1) / segments);
            float inner = radius - thickness;
            int red = (i & 1) == 0 ? 255 : 150;
            int green = (i & 1) == 0 ? 205 : 115;
            int blue = (i & 1) == 0 ? 90 : 255;
            int alpha = 155 + (int) (Mth.sin(time * 0.7F + i) * 30.0F);
            colorQuad(pose, consumer,
                    Mth.cos(a0) * inner, Mth.sin(a0) * inner, 0,
                    Mth.cos(a0) * radius, Mth.sin(a0) * radius, 0,
                    Mth.cos(a1) * radius, Mth.sin(a1) * radius, 0,
                    Mth.cos(a1) * inner, Mth.sin(a1) * inner, 0,
                    red, green, blue, alpha);
        }
    }

    private static void colorQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  float x3, float y3, float z3, float x4, float y4, float z4,
                                  int red, int green, int blue, int alpha) {
        Matrix4f matrix = pose.pose();
        colorVertex(consumer, matrix, x1, y1, z1, red, green, blue, alpha);
        colorVertex(consumer, matrix, x2, y2, z2, red, green, blue, alpha);
        colorVertex(consumer, matrix, x3, y3, z3, red, green, blue, alpha);
        colorVertex(consumer, matrix, x4, y4, z4, red, green, blue, alpha);
    }

    private static void colorVertex(VertexConsumer consumer, Matrix4f matrix,
                                    float x, float y, float z,
                                    int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static void drawCrossedQuad(PoseStack.Pose pose, VertexConsumer consumer, float size) {
        drawCrossedQuad(pose, consumer, size, size);
    }

    private static void drawCrossedQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                        float width, float length) {
        float halfWidth = width * 0.5F;
        float halfLength = length * 0.5F;
        quad(pose, consumer,
                0, -halfWidth, -halfLength, 0, halfWidth, -halfLength,
                0, halfWidth, halfLength, 0, -halfWidth, halfLength);
        quad(pose, consumer,
                -halfWidth, 0, -halfLength, halfWidth, 0, -halfLength,
                halfWidth, 0, halfLength, -halfWidth, 0, halfLength);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, x1, y1, z1, 0, 1);
        vertex(consumer, matrix, normal, x2, y2, z2, 1, 1);
        vertex(consumer, matrix, normal, x3, y3, z3, 1, 0);
        vertex(consumer, matrix, normal, x4, y4, z4, 0, 0);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z).color(255, 255, 235, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(AstralBoltEntity entity) {
        return switch (entity.getKind()) {
            case AstralBoltEntity.STARFIRE -> STARFIRE_TEXTURE;
            case AstralBoltEntity.STAR_SWARM -> STAR_SWARM_TEXTURE;
            default -> NEEDLE_TEXTURE;
        };
    }
}
