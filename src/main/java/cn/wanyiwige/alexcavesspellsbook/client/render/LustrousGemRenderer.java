package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.client.LustrousGemModels;
import cn.wanyiwige.alexcavesspellsbook.entities.LustrousGemProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public final class LustrousGemRenderer extends EntityRenderer<LustrousGemProjectile> {
    private static final ResourceLocation FALLBACK = new ResourceLocation(AlexCavesSpellsbook.MOD_ID,
            "textures/entity/lustrous_gems/ars_nouveau_source_gem.png");
    public LustrousGemRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(LustrousGemProjectile gem, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        LustrousGemModels.Mesh mesh = LustrousGemModels.get(gem.getGemKind());
        if (mesh != null) {
            renderTrail(gem, partialTick, poses, buffers, mesh.trailColor());
            poses.pushPose();
            float age = gem.tickCount + partialTick;
            poses.mulPose(Axis.YP.rotationDegrees(age * 8 + gem.getId() * 37));
            poses.mulPose(Axis.ZP.rotationDegrees(age * 5));
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(mesh.texture()));
            PoseStack.Pose pose = poses.last();
            for (LustrousGemModels.Triangle triangle : mesh.triangles()) {
                float u = triangle.swatch() * 4f;
                vertex(buffer, pose, triangle.a(), triangle.normal(), (u + .5f) / 64f, .5f / 16f, light);
                vertex(buffer, pose, triangle.b(), triangle.normal(), (u + 3.3f) / 64f, .5f / 16f, light);
                vertex(buffer, pose, triangle.c(), triangle.normal(), (u + 1.9f) / 64f, 3.3f / 16f, light);
                // The entity pipeline uses quads: repeat the last vertex to preserve a triangle.
                vertex(buffer, pose, triangle.c(), triangle.normal(), (u + 1.9f) / 64f, 3.3f / 16f, light);
            }
            poses.popPose();
        }
        super.render(gem, yaw, partialTick, poses, buffers, light);
    }

    private static void renderTrail(LustrousGemProjectile gem, float partialTick, PoseStack poses,
                                    MultiBufferSource buffers, int color) {
        Vec3 origin = gem.getPosition(partialTick);
        List<Vec3> points = new ArrayList<>();
        points.add(origin);
        points.addAll(gem.getTrail());
        if (points.size() < 2) return;
        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i).subtract(origin), b = points.get(i + 1).subtract(origin);
            Vec3 direction = b.subtract(a);
            if (direction.lengthSqr() < 1.0E-8) continue;
            direction = direction.normalize();
            Vec3 side = direction.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0E-6) side = direction.cross(new Vec3(1, 0, 0));
            side = side.normalize();
            Vec3 other = direction.cross(side).normalize();
            float head = 1f - (float) i / (points.size() - 1);
            float tail = 1f - (float) (i + 1) / (points.size() - 1);
            for (Vec3 axis : List.of(side, other)) {
                Vec3 wa = axis.scale(.04 * head), wb = axis.scale(.04 * tail);
                trailVertex(buffer, poses.last(), a.add(wa), red, green, blue, (int) (150 * head));
                trailVertex(buffer, poses.last(), a.subtract(wa), red, green, blue, (int) (150 * head));
                trailVertex(buffer, poses.last(), b.subtract(wb), red, green, blue, (int) (150 * tail));
                trailVertex(buffer, poses.last(), b.add(wb), red, green, blue, (int) (150 * tail));
            }
        }
    }

    private static void trailVertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 point,
                                     int red, int green, int blue, int alpha) {
        buffer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).endVertex();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3f point,
                               Vector3f normal, float u, float v, int light) {
        buffer.vertex(pose.pose(), point.x, point.y, point.z).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), normal.x, normal.y, normal.z).endVertex();
    }

    @Override public ResourceLocation getTextureLocation(LustrousGemProjectile gem) {
        LustrousGemModels.Mesh mesh = LustrousGemModels.get(gem.getGemKind());
        return mesh == null ? FALLBACK : mesh.texture();
    }
}
