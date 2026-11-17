package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessWardEntity;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessWardSpell.Kind;
import com.github.alexmodguy.alexscaves.client.model.WatcherModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.mojang.blaze3d.vertex.PoseStack;
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
import static cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessWardEntity.*;

public final class PureDarknessWardRenderer extends EntityRenderer<PureDarknessWardEntity> {
    private static final ResourceLocation FISSURE = texture("burrow_fissure");
    private static final ResourceLocation MAW = texture("maw_warning");
    private static final ResourceLocation WATCHER = nativeTexture("watcher_appearance");
    private static final ResourceLocation FORSAKEN_EYES = nativeTexture("forsaken_eyes");
    private static final ResourceLocation[] SEAMS = {
            texture("vestment_seam_0"), texture("vestment_seam_1"), texture("vestment_seam_2"),
            texture("vestment_seam_3"), texture("vestment_seam_4")
    };
    private final WatcherModel watcher = new WatcherModel();
    // Alex's Caves 2.0.2 exports the head, including both horns, at this position.
    private final AdvancedModelBox watcherHead = StreamSupport.stream(watcher.getAllParts().spliterator(), false)
            .skip(10).findFirst().orElse(null);
    private List<Part> vestment, maw;

    public PureDarknessWardRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public boolean shouldRender(PureDarknessWardEntity ward, Frustum camera, double x, double y, double z) {
        if (!ward.ready() || ward.distanceToSqr(x, y, z) > 64 * 64) return false;
        return switch (ward.kind()) {
            case WATCHER_DREAD -> ward.age(0) < 14 && camera.isVisible(ward.getBoundingBox().inflate(3));
            case MARTYR_VESTMENT -> ward.owner() != null && camera.isVisible(ward.owner().getBoundingBox().inflate(1.2));
            case FORSAKEN_MAW -> camera.isVisible(ward.getBoundingBox().inflate(ward.kind().radius(ward.rank()), 2.75,
                    ward.kind().radius(ward.rank())));
        };
    }

    @Override public Vec3 getRenderOffset(PureDarknessWardEntity ward, float partial) {
        if (ward.ready() && ward.kind() == Kind.MARTYR_VESTMENT && ward.owner() != null) {
            return position(ward.owner(), partial).subtract(position(ward, partial));
        }
        return Vec3.ZERO;
    }

    @Override public void render(PureDarknessWardEntity ward, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        if (!ward.ready()) return;
        float age = Math.max(0, ward.age(partial));
        pose.pushPose();
        switch (ward.kind()) {
            case WATCHER_DREAD -> dread(ward, age, pose, buffers);
            case MARTYR_VESTMENT -> vestment(ward, age, partial, pose, buffers, light);
            case FORSAKEN_MAW -> maw(ward, age, pose, buffers, light);
        }
        pose.popPose();
        super.render(ward, yaw, partial, pose, buffers, light);
    }

    private void dread(PureDarknessWardEntity ward, float age, PoseStack pose, MultiBufferSource buffers) {
        if (watcherHead == null || age >= 14) return;
        watcher.positionForParticle(age, 0);
        watcherHead.rotationPointX = 0;
        watcherHead.rotationPointY = 0;
        watcherHead.rotationPointZ = 0;
        float pulse = ramp(age, 5, DREAD_WAVE) * (1 - ramp(age, DREAD_WAVE, 12));
        float alpha = ramp(age, 0, 4) * (1 - ramp(age, 9, 14));
        float scale = .82F + pulse * .25F;
        pose.translate(0, -.06 + .1 * pulse, 0);
        pose.mulPose(Axis.YP.rotation(yaw(ward.heading()) + Mth.PI));
        pose.scale(-scale, -scale, scale);
        watcherHead.render(pose, buffers.getBuffer(RenderType.entityTranslucent(WATCHER)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1, .56F + .32F * pulse, .61F + .25F * pulse, alpha);
    }

    private void vestment(PureDarknessWardEntity ward, float age, float partial, PoseStack pose,
                          MultiBufferSource buffers, int light) {
        if (vestment == null) vestment = loadModel("martyr_vestment");
        if (vestment.size() != 2) return;
        var owner = ward.owner();
        if (owner == null) return;
        float health = ward.remaining() / ward.kind().budget(ward.rank());
        int seam = Mth.clamp(Mth.ceil((1 - health) * 4), 0, 4);
        float impact = ward.lastHit() < 0 ? 0 : 1 - ramp(age, ward.lastHit(), ward.lastHit() + 5);
        float ending = ward.ending() >= 0 ? ramp(age, ward.ending(), ward.ending() + 8) : 0;
        float appear = ramp(age, 0, 8);
        float alpha = appear * (1 - ending);
        float heading = Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180 - heading));
        float scale = owner.getBbHeight() / 1.8F;
        pose.scale(scale, scale, scale);
        Part cape = vestment.get(0);
        Part hem = vestment.get(1);
        translate(pose, cape.pivot().add(0, -.11, 0));
        pose.mulPose(Axis.XP.rotationDegrees(70 - 46 * (1 - appear) - 10 * impact
                + 5 * Mth.sin(age * .26F) - (ward.shattered() ? 32 * ending : 0)));
        pose.mulPose(Axis.ZP.rotationDegrees(-20 * ending));
        cloth(cape, pose, buffers, light, seam, alpha, impact);
        translate(pose, hem.pivot().subtract(cape.pivot()));
        pose.mulPose(Axis.XP.rotationDegrees(-25 + 20 * (1 - appear) + 8 * Mth.sin(age * .26F + .42F)));
        pose.mulPose(Axis.ZP.rotationDegrees(37 * ending));
        cloth(hem, pose, buffers, light, seam, alpha, impact);
    }

    private static void cloth(Part cloth, PoseStack pose, MultiBufferSource buffers, int light,
                              int seam, float alpha, float impact) {
        draw(cloth, pose, buffers, null, light, alpha);
        if (seam >= 4) return;
        pose.pushPose();
        pose.translate(0, -.0025, 0);
        draw(cloth, pose, buffers, SEAMS[seam], LightTexture.FULL_BRIGHT, alpha * (.79F + .2F * impact));
        pose.popPose();
    }

    private void maw(PureDarknessWardEntity ward, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        if (maw == null) maw = loadModel("forsaken_maw");
        float radius = (float) ward.kind().radius(ward.rank());
        float yaw = yaw(ward.heading());
        float fade = 1 - ramp(age, 22, 32);
        ground(pose, buffers, MAW, new Vec3(0, .036, 0), radius * 1.19F, radius * 1.19F, yaw,
                Mth.clamp((int) age / 4, 0, 5), fade * (.7F + .28F * ramp(age, 2, 16)));
        if (age < 5) return;
        float appear = ramp(age, 5, 13);
        float gape = ramp(age, 6, 13) * (1 - ramp(age, 14, MAW_BITE));
        float recoil = ramp(age, 16, MAW_BITE) * (1 - ramp(age, MAW_BITE + 1, 24));
        pose.mulPose(Axis.YP.rotation(yaw + Mth.PI));
        pose.translate(0, -1.53 + 2.08 * appear - .93 * (1 - fade), .59 - .29 * recoil);
        float scale = radius * .56F;
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(-7 * gape + 9 * recoil));
        for (int i = 0; i < maw.size(); i++) {
            Part part = maw.get(i);
            pose.pushPose();
            translate(pose, part.pivot());
            if (i == maw.size() - 1) pose.mulPose(Axis.XP.rotationDegrees(-46 * gape + 4 * recoil));
            draw(part, pose, buffers, null, light, fade);
            if (i == 0) {
                pose.scale(1.004F, 1.004F, 1.004F);
                draw(part, pose, buffers, FORSAKEN_EYES, LightTexture.FULL_BRIGHT, fade * .92F);
            }
            pose.popPose();
        }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID,
                "textures/entity/pure_darkness/" + name + ".png");
    }

    private static ResourceLocation nativeTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath("alexscaves", "textures/entity/" + name + ".png");
    }

    @Override public ResourceLocation getTextureLocation(PureDarknessWardEntity ward) { return FISSURE; }
}

