package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFinaleEntity;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessFinaleSpell.Kind;
import com.github.alexmodguy.alexscaves.client.model.DreadbowModel;
import com.github.alexmodguy.alexscaves.client.model.ForsakenModel;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.ForsakenEntity;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.StreamSupport;

import static cn.wanyiwige.alexcavesspellsbook.client.render.PureDarknessRiteRenderer.*;
import static cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFinaleEntity.*;

public final class PureDarknessFinaleRenderer extends EntityRenderer<PureDarknessFinaleEntity> {
    private static final ResourceLocation FORSAKEN = nativeTexture("forsaken");
    private static final ResourceLocation EYES = nativeTexture("forsaken_eyes");
    private static final ResourceLocation BOW = nativeTexture("dreadbow");
    private static final ResourceLocation BOW_EYE = nativeTexture("dreadbow_eye");
    private static final ResourceLocation BOW_FULL_EYE = nativeTexture("dreadbow_eye_perfect");
    private static final ResourceLocation STRING = texture("dreadbow_string");
    private static final ResourceLocation WAVE = particle("sonic_wavefront_01");
    private static final ResourceLocation BLOOD = particle("scarlet_filament_01");
    private static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/projectiles/arrow.png");

    private final DreadbowModel bow = new DreadbowModel();
    private final ForsakenModel forsaken = new ForsakenModel();
    private final List<AdvancedModelBox> parts = StreamSupport.stream(forsaken.getAllParts().spliterator(), false).toList();
    private ForsakenEntity poseEntity;

    public PureDarknessFinaleRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public boolean shouldRender(PureDarknessFinaleEntity field, Frustum camera, double x, double y, double z) {
        if (!field.ready() || field.distanceToSqr(x, y, z) > 80 * 80) return false;
        if (field.kind() == Kind.HOLLOWS_SCREECH) {
            return camera.isVisible(field.getBoundingBox().inflate(2.4, 4, 2.4)
                    .expandTowards(field.aim().scale(field.kind().range(field.rank()))));
        }
        if (field.kind() == Kind.DREADBOW_GALLOWS && !field.arrow()) {
            return camera.isVisible(field.getBoundingBox().inflate(3.25, 4.5, 3.25));
        }
        return camera.isVisible(field.getBoundingBox().inflate(2, 3.2, 2));
    }

    @Override public Vec3 getRenderOffset(PureDarknessFinaleEntity field, float partial) {
        if (field.ready() && field.kind() == Kind.HOLLOWS_SCREECH && !field.opened() && field.owner() != null) {
            var caster = field.owner();
            Vec3 mouth = position(caster, partial).add(0, caster.getEyeHeight(), 0)
                    .add(Vec3.directionFromRotation(0, caster.getYRot()).scale(.65));
            return mouth.subtract(position(field, partial));
        }
        if (field.ready() && field.kind() == Kind.DARKBORN_RECOVERY
                && field.owner() != null) {
            return position(field.owner(), partial).subtract(position(field, partial));
        }
        return Vec3.ZERO;
    }

    @Override public void render(PureDarknessFinaleEntity field, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        if (!field.ready()) return;
        float age = Math.max(0, field.age(partial));
        float phase = Math.max(0, field.phaseAge(partial));
        float fade = field.ending() < 0 ? 1 : 1 - ramp(age, field.ending(), field.ending() + 8);
        pose.pushPose();
        switch (field.kind()) {
            case HOLLOWS_SCREECH -> screech(field, age, phase, fade, pose, buffers, light);
            case DREADBOW_GALLOWS -> {
                if (field.arrow()) arrow(field, pose, buffers, light);
                else {
                    for (var shot : field.bows()) {
                        pose.pushPose();
                        translate(pose, shot.origin().subtract(position(field, partial)));
                        bow(field, shot.aim(), age, fade, pose, buffers, light);
                        pose.popPose();
                    }
                }
            }
            case DARKBORN_RECOVERY -> recovery(field, age, phase, fade, pose, buffers);
        }
        pose.popPose();
        super.render(field, yaw, partial, pose, buffers, light);
    }

    private void screech(PureDarknessFinaleEntity field, float age, float phase, float fade, PoseStack pose,
                         MultiBufferSource buffers, int light) {
        if (field.opened() && field.action() == 0 && phase >= 1 && field.travelled() > 0) {
            float distance = field.travelled();
            for (int i = 0; i < 3; i++) {
                if (distance - i * .87 < .04) break;
                pose.pushPose();
                translate(pose, field.aim().scale(distance - i * .87));
                orient(pose, field.aim());
                float radius = .64F + Math.min(.28F, distance * .026F);
                sheet(pose, buffers, WAVE, radius, radius * .86F, 0, 1, .86F - i * .22F, 1, .48F, .52F);
                pose.popPose();
            }
        }
        float opacity = fade * ramp(age, 0, 7) * (field.opened() ? 1 - ramp(phase, 6, 17) : 1);
        if (opacity <= .01F) return;
        float frame = field.opened() ? 10 + phase : Math.min(9.9F, age * .49F);
        upper(field, ForsakenEntity.ANIMATION_SONIC_ATTACK, frame, opacity, .64F,
                new Vec3(0, -.52, -1.24), pose, buffers, light);
    }

    private void bow(PureDarknessFinaleEntity field, Vec3 aim, float age, float fade, PoseStack pose,
                     MultiBufferSource buffers, int light) {
        orient(pose, aim);
        float draw = Mth.clamp(age / BOW_FULL, 0, 1);
        if (field.action() == 1) draw *= fade;
        float alpha = fade * ramp(age, 0, 4);
        pose.pushPose();
        pose.translate(0, -.14 * (1 - fade), 0);
        pose.mulPose(Axis.ZP.rotationDegrees(82 + 21 * (1 - fade)));
        pose.scale(.9F, -.9F, -.9F);
        bow.setupAnim(field, draw, 0, age, 0, 0);
        bow.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(BOW)), light,
                OverlayTexture.NO_OVERLAY, .87F, .72F, .74F, alpha);
        bow.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(
                age >= BOW_FULL ? BOW_FULL_EYE : BOW_EYE)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1, .42F, .49F, alpha);
        pose.popPose();
        pose.translate(0, 0, -.24 - draw * .26);
        pose.mulPose(Axis.ZP.rotationDegrees(82));
        sheet(pose, buffers, STRING, .17F + draw * .11F, .59F, Math.min(5, (int) (draw * 5)), 6,
                alpha * .83F, 1, .74F, .76F);
    }

    private static void arrow(PureDarknessFinaleEntity field, PoseStack pose, MultiBufferSource buffers, int light) {
        orient(pose, field.aim());
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale(.058F, .058F, .058F);
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(ARROW));
        for (int side = 0; side < 4; side++) {
            pose.mulPose(Axis.XP.rotationDegrees(90));
            arrowVertex(out, pose.last(), -10, -2, 0, 5 / 32F, light);
            arrowVertex(out, pose.last(), 9, -2, .5F, 5 / 32F, light);
            arrowVertex(out, pose.last(), 9, 2, .5F, 0, light);
            arrowVertex(out, pose.last(), -10, 2, 0, 0, light);
        }
    }

    private static void arrowVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y,
                                    float u, float v, int light) {
        out.vertex(pose.pose(), x, y, 0).color(.37F, .15F, .17F, 1).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), 0, 0, 1).endVertex();
    }

    private static void recovery(PureDarknessFinaleEntity field, float age, float phase, float fade,
                                 PoseStack pose, MultiBufferSource buffers) {
        float pulse = field.opened() ? phase % 20 / 20F : Mth.clamp(age / 8F, 0, 1);
        float alpha = fade * ramp(age, 0, 6);
        for (int i = 0; i < 7; i++) {
            double angle = Math.PI * 2 * i / 7 + age * .12;
            double radius = .94 - .57 * pulse;
            pose.pushPose();
            pose.translate(Math.sin(angle) * radius, .18 + .93 * pulse + i % 3 * .21, Math.cos(angle) * radius);
            pose.mulPose(Axis.YP.rotation((float) angle));
            pose.mulPose(Axis.ZP.rotationDegrees(20 + 42 * pulse));
            sheet(pose, buffers, BLOOD, .089F, .16F + .11F * pulse, 0, 1,
                    alpha * (.36F + .59F * pulse), .94F, .64F, .67F);
            pose.popPose();
        }
    }

    private void upper(PureDarknessFinaleEntity field, Animation motion, float tick, float alpha, float scale,
                       Vec3 offset, PoseStack pose, MultiBufferSource buffers, int light) {
        if (alpha <= .01F || parts.size() < 62) return;
        prepare(field, motion, tick, field.age(0));
        // Use the original rig and animations while hiding the lower torso and smaller arms.
        parts.get(4).showModel = false;
        parts.get(12).showModel = false;
        parts.get(36).showModel = false;
        pose.pushPose();
        Vec3 heading = field.kind() == Kind.HOLLOWS_SCREECH && !field.opened() && field.owner() != null
                ? field.owner().getLookAngle() : field.aim();
        pose.mulPose(Axis.YP.rotation(yaw(heading) + Mth.PI));
        pose.translate(offset.x, offset.y, -offset.z);
        pose.scale(-scale, -scale, scale);
        pose.translate(0, -1.501, 0);
        forsaken.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(FORSAKEN)), light,
                OverlayTexture.NO_OVERLAY, .87F, .66F, .67F, alpha);
        forsaken.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(EYES)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1, .48F, .47F, alpha);
        pose.popPose();
    }

    private void prepare(PureDarknessFinaleEntity field, Animation animation, float frame, float age) {
        if (poseEntity == null || poseEntity.level() != field.level()) {
            poseEntity = new ForsakenEntity(ACEntityRegistry.FORSAKEN.get(), field.level());
        }
        int tick = animation == IAnimatedEntity.NO_ANIMATION ? 0 : Mth.clamp((int) frame, 0, animation.getDuration() - 1);
        poseEntity.setAnimation(animation);
        poseEntity.setAnimationTick(tick);
        poseEntity.tickCount = (int) age;
        for (AdvancedModelBox box : parts) box.showModel = true;
        forsaken.setupAnim(poseEntity, 0, 0, age, 0, 0);
    }

    private static void orient(PoseStack pose, Vec3 aim) {
        pose.mulPose(Axis.YP.rotation(yaw(aim)));
        pose.mulPose(Axis.XP.rotation((float) -Math.atan2(aim.y, aim.horizontalDistance())));
    }

    private static void sheet(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, float x, float y,
                              int frame, int frames, float alpha, float r, float g, float b) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(texture));
        float top = (frame + .005F) / frames;
        float bottom = (frame + .995F) / frames;
        sheetVertex(out, pose.last(), -x, -y, 0, bottom, alpha, r, g, b);
        sheetVertex(out, pose.last(), x, -y, 1, bottom, alpha, r, g, b);
        sheetVertex(out, pose.last(), x, y, 1, top, alpha, r, g, b);
        sheetVertex(out, pose.last(), -x, y, 0, top, alpha, r, g, b);
    }

    private static void sheetVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float u, float v,
                                    float alpha, float r, float g, float b) {
        out.vertex(pose.pose(), x, y, 0).color(r, g, b, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, 0, 1).endVertex();
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "textures/entity/pure_darkness/" + name + ".png");
    }

    private static ResourceLocation particle(String name) {
        return ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "textures/particle/pure_darkness/" + name + ".png");
    }

    private static ResourceLocation nativeTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath("alexscaves", "textures/entity/" + name + ".png");
    }

    @Override public ResourceLocation getTextureLocation(PureDarknessFinaleEntity field) { return FORSAKEN; }
}

