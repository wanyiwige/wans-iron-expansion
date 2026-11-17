package cn.wanyiwige.astralcompat.client;

import cn.wanyiwige.astralcompat.entity.PiercingStarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Small six-point, three-dimensional white stars with fading world-space ribbons. */
public final class PiercingStarRenderer extends EntityRenderer<PiercingStarEntity> {
    public PiercingStarRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(PiercingStarEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        VertexConsumer out = buffers.getBuffer(RenderType.lightning());
        Vec3 origin = new Vec3(entity.xOld, entity.yOld, entity.zOld).lerp(entity.position(), partialTick);
        Vec3 previous = origin;
        int count = entity.getTrail().size();
        for (int i = 0; i < count; i++) {
            Vec3 next = entity.getTrail().get(i);
            Vec3 tangent = previous.subtract(next);
            Vec3 side = tangent.cross(entityRenderDispatcher.camera.getPosition().subtract(previous)).normalize();
            if (side.lengthSqr() > 1.0E-8 && tangent.lengthSqr() < 16) {
                float fade = 1.0F - (float) i / Math.max(1, count);
                Vec3 width = side.scale(0.09 * fade);
                Vec3 a = previous.subtract(origin), b = next.subtract(origin);
                Matrix4f matrix = pose.last().pose();
                vertex(out, matrix, a.add(width), (int) (180 * fade));
                vertex(out, matrix, a.subtract(width), (int) (180 * fade));
                vertex(out, matrix, b.subtract(width.scale(0.85)), (int) (140 * fade));
                vertex(out, matrix, b.add(width.scale(0.85)), (int) (140 * fade));
            }
            previous = next;
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 5 + entity.getId() * 31));
        pose.mulPose(Axis.ZP.rotationDegrees(25));
        // Three intersecting elongated octahedra create six sharp points from every view.
        for (int axis = 0; axis < 3; axis++) {
            pose.pushPose();
            if (axis == 1) pose.mulPose(Axis.ZP.rotationDegrees(90));
            if (axis == 2) pose.mulPose(Axis.XP.rotationDegrees(90));
            Vec3[] ring = {new Vec3(0.07, 0, 0), new Vec3(0, 0, 0.07),
                    new Vec3(-0.07, 0, 0), new Vec3(0, 0, -0.07)};
            for (int i = 0; i < 4; i++) {
                triangle(out, pose.last().pose(), new Vec3(0, 0.28, 0), ring[i], ring[(i + 1) % 4]);
                triangle(out, pose.last().pose(), new Vec3(0, -0.28, 0), ring[(i + 1) % 4], ring[i]);
            }
            pose.popPose();
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private static void triangle(VertexConsumer out, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c) {
        vertex(out, matrix, a, 255); vertex(out, matrix, b, 235);
        vertex(out, matrix, c, 235); vertex(out, matrix, c, 235);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Vec3 p, int alpha) {
        out.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(255, 255, 255, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(PiercingStarEntity entity) { return InventoryMenu.BLOCK_ATLAS; }
}
