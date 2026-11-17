package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFieldEntity;
import com.github.alexmodguy.alexscaves.client.model.CorrodentModel;
import com.github.alexmodguy.alexscaves.client.model.VesperModel;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.CorrodentEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.VesperEntity;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFieldEntity.*;

public final class PureDarknessFieldRenderer extends EntityRenderer<PureDarknessFieldEntity> {
    private static final ResourceLocation EYE = texture(AlexCavesSpellsbook.MOD_ID, "pure_darkness/eye_front");
    private static final ResourceLocation EYE_SIDE = texture(AlexCavesSpellsbook.MOD_ID, "pure_darkness/eye_side");
    private static final ResourceLocation EYE_BACK = texture(AlexCavesSpellsbook.MOD_ID, "pure_darkness/eye_back");
    private static final ResourceLocation CLAMP = texture(AlexCavesSpellsbook.MOD_ID, "pure_darkness/closing_plane");
    private static final ResourceLocation FISSURE = texture(AlexCavesSpellsbook.MOD_ID, "pure_darkness/burrow_fissure");
    private static final ResourceLocation CORRODENT = texture("alexscaves", "corrodent");
    private static final ResourceLocation CORRODENT_EYES = texture("alexscaves", "corrodent_eyes");
    private static final ResourceLocation VESPER = texture("alexscaves", "vesper");

    private final CorrodentModel corrodent = new CorrodentModel();
    private final VesperModel vesper = new VesperModel();
    private CorrodentPose corrodentPose;
    private VesperPose vesperPose;

    public PureDarknessFieldRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public boolean shouldRender(PureDarknessFieldEntity field, Frustum camera, double x, double y, double z) {
        return field.ready() && field.distanceToSqr(x, y, z) < 64 * 64
                && switch (field.kind()) {
                    case MOTHSCALE_MIST -> false;
                    case CLOSING_EYE -> camera.isVisible(field.getBoundingBox().inflate(2));
                    case CORRODENT_BURROW -> camera.isVisible(field.getBoundingBox().inflate(1)
                            .expandTowards(field.heading().scale(field.kind().range(field.rank()))).inflate(0, 4, 0));
                    case VESPER_PERCH -> {
                        double radius = field.kind().triggerRadius(field.rank()) + 1;
                        yield camera.isVisible(field.getBoundingBox().inflate(radius, BAT_TRIGGER_DEPTH + 1, radius));
                    }
                };
    }

    @Override public void render(PureDarknessFieldEntity field, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        if (!field.ready()) return;
        float age = field.age(partial);
        if (age < 0) return;
        pose.pushPose();
        switch (field.kind()) {
            case CLOSING_EYE -> eye(field, age, pose, buffers);
            case CORRODENT_BURROW -> burrow(field, age, pose, buffers, light);
            case VESPER_PERCH -> perch(field, age, pose, buffers, light);
            case MOTHSCALE_MIST -> { }
        }
        pose.popPose();
        super.render(field, yaw, partial, pose, buffers, light);
    }

    private static void eye(PureDarknessFieldEntity field, float age, PoseStack pose, MultiBufferSource buffers) {
        pose.mulPose(Axis.YP.rotation(yaw(field.heading()) + Mth.PI));
        float opacity = Mth.clamp((20 - age) / 3, 0, 1);
        if (age >= 4) {
            float width = (float) field.kind().eyeWidth(field.rank());
            int frame = age >= EYE_HIT ? Math.min(5, 3 + (int) (age - EYE_HIT) / 2) : Math.min(3, (int) (age - 4) / 3);
            float alpha = age < EYE_HIT ? .74F : opacity;
            sheet(pose, buffers, CLAMP, width / 2, (float) EYE_HEIGHT / 2, -.015F, frame, 6, alpha);
        }

        // The 12-pixel cube sits above the center of the fixed contact plane.
        pose.translate(0, EYE_RISE, .015);
        float scale = 1 - .42F * (1 - opacity);
        pose.scale(scale, scale, scale);
        final float half = .375F;
        for (int side = 0; side < 4; side++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(side * 90));
            ResourceLocation texture = side == 0 ? EYE : side == 2 ? EYE_BACK : EYE_SIDE;
            int frame = side == 0 ? Math.min(9, (int) age / 2) : 0;
            sheet(pose, buffers, texture, half, half, half, frame, side == 0 ? 10 : 1, opacity);
            pose.popPose();
        }
        for (int pitch : new int[]{90, -90}) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            sheet(pose, buffers, EYE_SIDE, half, half, half, 0, 1, opacity);
            pose.popPose();
        }
    }

    private void burrow(PureDarknessFieldEntity field, float age, PoseStack pose,
                        MultiBufferSource buffers, int light) {
        for (int delay = 0; delay < 6; delay += 2) {
            if (age + FISSURE_LEAD - delay >= 36 || age < delay) continue;
            pose.pushPose();
            offset(pose, field.burrowPoint(age + FISSURE_LEAD - delay).subtract(field.position()).add(0, .027, 0));
            pose.mulPose(Axis.YP.rotation(yaw(field.heading())));
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            sheet(pose, buffers, FISSURE, .38F, .43F, 0, (delay + (int) age) % 6, 6, 1F - delay * .12F);
            pose.popPose();
        }
        if (age < FISSURE_LEAD) return;
        if (corrodentPose == null || corrodentPose.level() != field.level()) corrodentPose = new CorrodentPose(field.level());
        float travel = age - FISSURE_LEAD;
        float rise = PureDarknessRiteRenderer.ramp(travel, 0, 5);
        rise = rise * rise * (3 - 2 * rise);
        float sink = PureDarknessRiteRenderer.ramp(age, field.kind().duration(field.rank()) - 5,
                field.kind().duration(field.rank()));
        sink = sink * sink * (3 - 2 * sink);
        float emerge = rise * (1 - sink);
        corrodentPose.tickCount = (int) age;
        corrodentPose.dig = 1 - emerge;
        corrodentPose.setAnimation(IAnimatedEntity.NO_ANIMATION);
        corrodent.setupAnim(corrodentPose, travel * 1.35F, .9F * emerge, age, 0, 0);
        offset(pose, field.burrowPoint(age).subtract(field.position()).add(0, -.74 * (1 - emerge), 0));
        orientNativeModel(pose, field.heading(), .75F);
        corrodent.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CORRODENT)),
                light, OverlayTexture.NO_OVERLAY, .82F, .64F, .64F, 1);
        corrodent.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(CORRODENT_EYES)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, .38F, .37F, 1);
    }

    private void perch(PureDarknessFieldEntity field, float age, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (vesperPose == null || vesperPose.level() != field.level()) vesperPose = new VesperPose(field.level());
        var phase = field.dive();
        float warning = phase.isEmpty() ? 0 : age - phase.getInt("Lock");
        boolean falling = phase.contains("Impact");
        boolean flying = !phase.isEmpty() && warning >= BAT_WINDUP;
        Vec3 at = field.batPoint(age);
        Vec3 direction = field.heading();
        if (flying && !falling) {
            Vec3 tangent = at.subtract(field.batPoint(age - 1));
            if (tangent.lengthSqr() > .0001) direction = tangent;
        }
        vesperPose.tickCount = (int) age;
        vesperPose.hang = 1 - Mth.clamp(warning / BAT_WINDUP, 0, 1);
        vesperPose.fly = falling ? .4F : 1 - vesperPose.hang;
        vesperPose.pitch = flying && !falling ? (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())) : 0;
        vesper.setupAnim(vesperPose, 0, 0, age, 0, 0);
        offset(pose, at.subtract(field.position()).add(0, -.43, 0));
        if (falling) pose.mulPose(Axis.ZP.rotationDegrees((age - phase.getInt("Impact")) * 44));
        orientNativeModel(pose, direction, .71F);
        float alpha = phase.isEmpty() ? Mth.clamp((field.kind().duration(field.rank()) - age) / 8, 0, 1) : 1;
        vesper.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(VESPER)), light,
                OverlayTexture.NO_OVERLAY, .72F, .48F, .55F, alpha);
    }

    private static void orientNativeModel(PoseStack pose, Vec3 direction, float scale) {
        pose.mulPose(Axis.YP.rotation(Mth.PI + yaw(direction)));
        pose.scale(-scale, -scale, scale);
        pose.translate(0, -1.501, 0);
    }

    private static float yaw(Vec3 direction) { return (float) Math.atan2(direction.x, direction.z); }
    private static void offset(PoseStack pose, Vec3 delta) { pose.translate(delta.x, delta.y, delta.z); }

    private static void sheet(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, float x,
                              float y, float z, int frame, int frames, float alpha) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(texture));
        float top = (frame + .001F) / frames;
        float bottom = (frame + .999F) / frames;
        vertex(out, pose.last(), -x, -y, z, 0, bottom, alpha);
        vertex(out, pose.last(), x, -y, z, 1, bottom, alpha);
        vertex(out, pose.last(), x, y, z, 1, top, alpha);
        vertex(out, pose.last(), -x, y, z, 0, top, alpha);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float alpha) {
        out.vertex(pose.pose(), x, y, z).color(1F, 1F, 1F, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, 0, 1).endVertex();
    }

    private static ResourceLocation texture(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, "textures/entity/" + path + ".png");
    }

    @Override public ResourceLocation getTextureLocation(PureDarknessFieldEntity field) { return EYE; }

    private static final class CorrodentPose extends CorrodentEntity {
        float dig;
        CorrodentPose(Level level) { super(ACEntityRegistry.CORRODENT.get(), level); }
        @Override public float getDigAmount(float partial) { return dig; }
    }

    private static final class VesperPose extends VesperEntity {
        float hang, fly, pitch;
        VesperPose(Level level) { super(ACEntityRegistry.VESPER.get(), level); }
        @Override public float getSleepProgress(float partial) { return hang; }
        @Override public float getFlyProgress(float partial) { return fly; }
        @Override public float getFlightPitch(float partial) { return pitch; }
    }
}

