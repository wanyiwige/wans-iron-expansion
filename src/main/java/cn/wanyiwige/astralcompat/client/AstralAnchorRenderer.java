package cn.wanyiwige.astralcompat.client;

import cn.wanyiwige.astralcompat.entity.AstralAnchorEntity;
import cn.wanyiwige.alexcavesspellsbook.spells.DarkSparkSpell;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class AstralAnchorRenderer extends EntityRenderer<AstralAnchorEntity> {
    private static final ResourceLocation PIERCING_CIRCLE_TEXTURE =
            new ResourceLocation("astral_spell_compat", "textures/entity/piercing_light_circle.png");
    private static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation("astral_spell_compat", "textures/entity/luminous_beam.png");
    private static final ResourceLocation BEAM_GLOW_TEXTURE =
            new ResourceLocation("astral_spell_compat", "textures/entity/luminous_beam_glow.png");
    private static final ResourceLocation BRIGHTBURST_WAVE_TEXTURE =
            new ResourceLocation("astral_spell_compat", "textures/entity/brightburst_wave.png");
    private static final ResourceLocation CELESTIAL_TETHER_SEAL_TEXTURE =
            new ResourceLocation("astral_spell_compat", "textures/entity/celestial_tether_seal.png");
    private static final int[][] PRISM_COLORS = {
            {255, 80, 185}, {255, 170, 45}, {245, 245, 95}, {90, 255, 140},
            {70, 240, 255}, {80, 140, 255}, {155, 95, 255}, {245, 100, 255}
    };
    private static final int[][] DARK_SPARK_COLORS = {
            {255, 65, 100}, {255, 177, 58}, {250, 235, 88},
            {75, 235, 175}, {65, 180, 255}, {190, 95, 255}
    };
    private static final ResourceLocation STARFIRE_TEXTURE =
            new ResourceLocation("asterismarcanum", "textures/entity/starfire/starfire.png");
    private static final ResourceLocation STARCUTTER_TEXTURE =
            new ResourceLocation("astral_spell_compat", "textures/entity/starcutter_slash.png");

    public AstralAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AstralAnchorEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        switch (entity.getMode()) {
            case AstralAnchorEntity.PIERCING_LIGHT_CIRCLE -> {
                float age = entity.tickCount + partialTick;
                float fade = Math.min(Math.min(age / 4.0F, (entity.getDuration() - age) / 12.0F), 1.0F);
                // The source atlas has transparent margins; the inner round boundary is radius 3.
                drawWaveLayer(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(PIERCING_CIRCLE_TEXTURE)),
                        5.5F, age * 0.8F, 255, 255, 255, (int) (240 * Math.max(0, fade)));
            }
            case AstralAnchorEntity.LUMINOUS_BEAM -> renderBeam(entity, partialTick, poseStack, buffers);
            case AstralAnchorEntity.DARK_SPARK -> renderDarkSpark(entity, partialTick, poseStack, buffers);
            case AstralAnchorEntity.STAR_SWARM -> renderOrbit(entity, partialTick, poseStack, buffers, 5, 2.0F);
            case AstralAnchorEntity.STARCUTTER -> renderStarcutterSlash(entity, partialTick, poseStack, buffers);
            case AstralAnchorEntity.CELESTIAL_TETHER -> renderTether(entity, partialTick, poseStack, buffers);
            case AstralAnchorEntity.BRIGHTBURST -> renderBrightburst(entity, partialTick, poseStack, buffers);
            default -> {
            }
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private void renderBeam(AstralAnchorEntity entity, float partialTick, PoseStack poseStack,
                            MultiBufferSource buffers) {
        Entity owner = entity.getSyncedOwner();
        if (owner == null) {
            return;
        }

        float length = entity.getBeamLength();
        if (length <= 0.05F) {
            return;
        }
        float time = (entity.tickCount + partialTick) / 20.0F;
        // Last Prism begins with every ray at the casting point, then fans the
        // far end outward.  During the whole channel the far ends draw back to
        // the axis, so the angled rays visibly converge instead of becoming a
        // bundle of parallel beams.
        float convergence = Mth.clamp(time / 4.75F, 0.0F, 1.0F);
        float terminalRadius = Mth.lerp(convergence, 2.45F, 0.04F);
        float rotation = time < 10.0F ? time * time * 90.0F : time * 900.0F;
        float pulse = 0.94F + Mth.cos(time * 100.0F) * 0.06F;

        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-owner.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(owner.getXRot()));

        VertexConsumer base = buffers.getBuffer(RenderType.entityTranslucentEmissive(BEAM_TEXTURE));
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(BEAM_GLOW_TEXTURE));

        drawPrismAperture(poseStack.last(), glow, 1.14F * pulse);

        // Keep a very faint core so the prism aperture remains readable without
        // hiding the eight angled rays.
        float coreWidth = 0.055F * pulse;
        drawPrismTube(poseStack.last(), glow, 0, 0, 0.03F, 0, 0, length,
                coreWidth * 1.65F, 205, 235, 255, 40,
                32.0F / 48.0F, 0, 1.0F, 40.0F / 64.0F);
        drawPrismTube(poseStack.last(), base, 0, 0, 0.03F, 0, 0, length,
                coreWidth, 232, 246, 255, 75,
                32.0F / 48.0F, 0, 1.0F, 40.0F / 64.0F);

        int colorShift = ((int) (time * 10.0F)) & 7;
        for (int i = 0; i < 8; i++) {
            float angle = (rotation + i * 45.0F) * ((float) Math.PI / 180.0F);
            float startX = 0.0F;
            float startY = 0.0F;
            float endX = Mth.cos(angle) * terminalRadius;
            float endY = Mth.sin(angle) * terminalRadius;
            int[] color = PRISM_COLORS[(i + colorShift) & 7];
            float rayWidth = Mth.lerp(convergence, 0.20F, 0.28F) * pulse;
            // The shared source and displaced far end create the requested
            // angle to the central axis; the far end is animated inward.
            drawPrismTube(poseStack.last(), glow, startX, startY, 0.05F,
                    endX, endY, length, rayWidth * 1.55F,
                    color[0], color[1], color[2], 90,
                    32.0F / 48.0F, 0, 1.0F, 40.0F / 64.0F);
            drawPrismTube(poseStack.last(), base, startX, startY, 0.05F,
                    endX, endY, length, rayWidth, color[0], color[1], color[2], 235,
                    32.0F / 48.0F, 0, 1.0F, 40.0F / 64.0F);
        }
        poseStack.popPose();
    }

    private void renderDarkSpark(AstralAnchorEntity entity, float partialTick, PoseStack poseStack,
                                 MultiBufferSource buffers) {
        if (!(entity.getSyncedOwner() instanceof LivingEntity owner)) return;
        float age = entity.tickCount + partialTick;
        int count = age < DarkSparkSpell.SPLIT_TICKS ? 1 : 6;
        Vec3 look = entity.getDarkSparkLook(partialTick);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 0.001 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 start = entity.getDarkSparkStart(partialTick);
        VertexConsumer base = buffers.getBuffer(RenderType.entityTranslucentEmissive(BEAM_TEXTURE));
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(BEAM_GLOW_TEXTURE));

        poseStack.pushPose();
        poseStack.translate(0, 0.35, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getDarkSparkYaw(partialTick)));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getDarkSparkPitch(partialTick)));
        drawPrismAperture(poseStack.last(), glow, age < DarkSparkSpell.SPLIT_TICKS ? 0.52F : 0.60F);

        for (int ray = 0; ray < count; ray++) {
            Vec3 rayStart = entity.getDarkSparkRayStart(ray, age, partialTick);
            Vec3 intendedEnd = entity.getDarkSparkIntendedEnd(ray, age, partialTick);
            HitResult blockHit = entity.level().clip(new ClipContext(rayStart, intendedEnd,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            Vec3 end = blockHit.getType() == HitResult.Type.MISS ? intendedEnd : blockHit.getLocation();
            Vec3 relativeStart = rayStart.subtract(start);
            Vec3 relative = end.subtract(start);
            float startX = (float) -relativeStart.dot(right);
            float startY = (float) relativeStart.dot(up);
            float startZ = (float) relativeStart.dot(look);
            float x = (float) -relative.dot(right);
            float y = (float) relative.dot(up);
            float z = (float) relative.dot(look);
            if (z <= 0.05F) continue;

            int[] finalColor = DARK_SPARK_COLORS[ray];
            int red, green, blue;
            if (age < 70) {
                red = 6; green = 4; blue = 12;
            } else if (age < 100) {
                float whitening = (age - 70) / 30.0F;
                red = (int) Mth.lerp(whitening, 6, 245);
                green = (int) Mth.lerp(whitening, 4, 245);
                blue = (int) Mth.lerp(whitening, 12, 245);
            } else if (age < 140) {
                red = green = blue = 245;
            } else if (age < 200) {
                float coloring = (age - 140) / 60.0F;
                red = (int) Mth.lerp(coloring, 245, finalColor[0]);
                green = (int) Mth.lerp(coloring, 245, finalColor[1]);
                blue = (int) Mth.lerp(coloring, 245, finalColor[2]);
            } else {
                red = finalColor[0]; green = finalColor[1]; blue = finalColor[2];
            }
            float width = count == 1 ? 0.64F : 0.30F;
            int haloRed = age < 70 ? 80 : red;
            int haloGreen = age < 70 ? 80 : green;
            int haloBlue = age < 70 ? 92 : blue;
            drawPrismTube(poseStack.last(), glow, startX, startY, startZ + 0.05F, x, y, z,
                    width * 1.25F, haloRed, haloGreen, haloBlue, age < 70 ? 105 : 120,
                    32.0F / 48.0F, 0, 1.0F, 40.0F / 64.0F);
            drawPrismTube(poseStack.last(), base, startX, startY, startZ + 0.06F, x, y, z,
                    width, red, green, blue, 245,
                    32.0F / 48.0F, 0, 1.0F, 40.0F / 64.0F);
        }
        poseStack.popPose();
    }

    private static void drawPrismAperture(PoseStack.Pose pose, VertexConsumer consumer, float radius) {
        // The white aperture occupies x=0..31 and y=16..47 in the 48x64 glow atlas.
        // Draw both windings so the luminous disk remains visible from either side.
        float maxU = 32.0F / 48.0F;
        float minV = 16.0F / 64.0F;
        float maxV = 48.0F / 64.0F;
        float z = 0.025F;
        beamQuad(pose, consumer,
                -radius, -radius, z, radius, -radius, z,
                radius, radius, z, -radius, radius, z,
                255, 255, 255, 255, 0, minV, maxU, maxV);
        beamQuad(pose, consumer,
                -radius, radius, z, radius, radius, z,
                radius, -radius, z, -radius, -radius, z,
                255, 255, 255, 255, 0, minV, maxU, maxV);
    }

    private void renderOrbit(AstralAnchorEntity entity, float partialTick, PoseStack poseStack,
                             MultiBufferSource buffers, int count, float radius) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(STARFIRE_TEXTURE));
        float time = entity.tickCount + partialTick;
        for (int i = 0; i < count; i++) {
            double angle = time * 0.08 + Math.PI * 2.0 * i / count;
            poseStack.pushPose();
            poseStack.translate(Math.cos(angle) * radius, 0.8 + Math.sin(angle * 1.7) * 0.55, Math.sin(angle) * radius);
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            poseStack.scale(0.38F, 0.38F, 0.38F);
            drawBillboard(poseStack.last(), consumer, 1.0F);
            poseStack.popPose();
        }
    }

    private void renderTether(AstralAnchorEntity entity, float partialTick, PoseStack poseStack,
                              MultiBufferSource buffers) {
        float time = entity.tickCount + partialTick;
        float pulse = 1.0F + Mth.sin(time * 0.16F) * 0.075F;

        // A dense inner constellation and a counter-rotating outer constellation
        // make the restraint readable from both first and third person.
        renderTetherOrbit(entity, poseStack, buffers, Math.max(7, entity.getSpellLevel() + 4),
                1.12F * pulse, time * 0.105F, 0.82F, 0.46F);
        renderTetherOrbit(entity, poseStack, buffers, Math.max(9, entity.getSpellLevel() + 6),
                1.72F * pulse, -time * 0.065F, 1.02F, 0.32F);

        // The tether uses its own fixed celestial seal instead of the expanding
        // Brightburst shockwave asset, so the two spells remain visually distinct.
        VertexConsumer seal = buffers.getBuffer(RenderType.entityTranslucentEmissive(CELESTIAL_TETHER_SEAL_TEXTURE));
        poseStack.pushPose();
        poseStack.translate(0.0, 0.95, 0.0);
        drawWaveLayer(poseStack, seal, 1.78F * pulse, time * 0.45F,
                190, 215, 255, 215);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        drawWaveLayer(poseStack, seal, 1.46F * pulse, -time * 0.35F,
                165, 192, 255, 165);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        drawWaveLayer(poseStack, seal, 1.46F * pulse, time * 0.35F + 30.0F,
                205, 225, 255, 155);
        poseStack.popPose();
        poseStack.popPose();

        VertexConsumer stars = buffers.getBuffer(RenderType.entityTranslucentEmissive(STARFIRE_TEXTURE));
        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.translate(0, i == 0 ? -0.08 : 2.0, 0);
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            float scale = (i == 0 ? 0.72F : 0.56F) * pulse;
            poseStack.scale(scale, scale, scale);
            drawBillboard(poseStack.last(), stars, 1.0F);
            poseStack.popPose();
        }
    }

    private void renderTetherOrbit(AstralAnchorEntity entity, PoseStack poseStack,
                                   MultiBufferSource buffers, int count, float radius,
                                   float phase, float centerY, float size) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(STARFIRE_TEXTURE));
        for (int i = 0; i < count; i++) {
            double angle = phase + Math.PI * 2.0 * i / count;
            poseStack.pushPose();
            poseStack.translate(Math.cos(angle) * radius,
                    centerY + Math.sin(angle * 2.0) * 0.72,
                    Math.sin(angle) * radius);
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            poseStack.scale(size, size, size);
            drawBillboard(poseStack.last(), consumer, 1.0F);
            poseStack.popPose();
        }
    }

    private void renderOrb(AstralAnchorEntity entity, float partialTick, PoseStack poseStack,
                           MultiBufferSource buffers, float scale) {
        poseStack.pushPose();
        poseStack.translate(0, 0.5, 0);
        float rotation = (entity.tickCount + partialTick) * 7.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 0.7F));
        poseStack.scale(scale, scale, scale);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(STARCUTTER_TEXTURE));
        drawCrossedPlanes(poseStack.last(), consumer, 1.3F);
        poseStack.popPose();
    }

    /** Crescent plus seven temporary white star nodes joined as a constellation. */
    private void renderStarcutterSlash(AstralAnchorEntity entity, float partialTick, PoseStack poseStack,
                                       MultiBufferSource buffers) {
        float age = entity.tickCount + partialTick;
        float progress = Mth.clamp(age / 8.0F, 0, 1);
        float fade = Mth.clamp((1.0F - progress) * 2.0F, 0, 1);
        poseStack.pushPose();
        // Local XZ is the forward/right sweep plane, as in Flaming Strike and
        // Fantasy Ending's RGB strike; it never faces the observer's camera.
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(-35.0F + 70.0F * progress));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.scale(3.9F, 3.9F, 3.9F);
        quad(poseStack.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(STARCUTTER_TEXTURE)),
                -.5F,-.5F,0, .5F,-.5F,0, .5F,.5F,0, -.5F,.5F,0, (int)(220 * fade));
        VertexConsumer links = buffers.getBuffer(RenderType.lightning());
        float[][] stars = {{-1.38F,.20F},{-.82F,.78F},{-.18F,.46F},{.36F,1.06F},{.91F,.72F},{1.34F,1.30F},{1.63F,.85F}};
        poseStack.scale(1 / 3.9F, 1 / 3.9F, 1 / 3.9F);
        for (int i=0;i<stars.length-1;i++) constellationLink(poseStack, links, stars[i][0],stars[i][1],stars[i+1][0],stars[i+1][1],(int)(190*fade));
        for (float[] star:stars) constellationStar(poseStack, links, star[0], star[1], (int)(255*fade));
        poseStack.popPose();
    }

    private static void constellationLink(PoseStack pose, VertexConsumer out,float x1,float y1,float x2,float y2,int alpha) {
        float dx=x2-x1, dy=y2-y1, length=Mth.sqrt(dx*dx+dy*dy), w=.018F;
        float ox=-dy/length*w, oy=dx/length*w; Matrix4f m=pose.last().pose();
        lightningVertex(out,m,x1+ox,y1+oy,0,alpha); lightningVertex(out,m,x1-ox,y1-oy,0,alpha);
        lightningVertex(out,m,x2-ox,y2-oy,0,alpha); lightningVertex(out,m,x2+ox,y2+oy,0,alpha);
    }
    private static void constellationStar(PoseStack pose, VertexConsumer out,float x,float y,int alpha) {
        Matrix4f m=pose.last().pose(); float s=.075F;
        lightningVertex(out,m,x-s,y,0,alpha);lightningVertex(out,m,x,y+s,0,alpha);lightningVertex(out,m,x+s,y,0,alpha);lightningVertex(out,m,x,y-s,0,alpha);
    }
    private static void lightningVertex(VertexConsumer out, Matrix4f matrix,float x,float y,float z,int alpha) {
        out.vertex(matrix,x,y,z).color(255,255,255,alpha).endVertex();
    }

    private void renderBrightburst(AstralAnchorEntity entity, float partialTick, PoseStack poseStack,
                                   MultiBufferSource buffers) {
        float age = entity.tickCount + partialTick;
        float progress = Mth.clamp(age / 18.0F, 0.0F, 1.0F);
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0);
        float targetRadius = 10.0F + entity.getSpellLevel() * 2.0F;
        float radius = targetRadius * eased;
        float fade = Mth.clamp((1.0F - progress) * 1.65F, 0.0F, 1.0F);
        if (radius <= 0.05F || fade <= 0.01F) {
            return;
        }

        VertexConsumer wave = buffers.getBuffer(RenderType.entityTranslucentEmissive(BRIGHTBURST_WAVE_TEXTURE));
        poseStack.pushPose();
        poseStack.translate(0.0, 0.08, 0.0);

        drawWaveLayer(poseStack, wave, radius * 1.03F, age * 1.8F,
                225, 246, 255, (int) (190.0F * fade));
        poseStack.translate(0.0, 0.035, 0.0);
        drawWaveLayer(poseStack, wave, radius * 0.96F, -age * 2.6F + 11.25F,
                255, 239, 178, (int) (230.0F * fade));
        poseStack.translate(0.0, 0.035, 0.0);
        drawWaveLayer(poseStack, wave, radius * 0.82F, age * 3.4F + 22.5F,
                207, 224, 255, (int) (135.0F * fade));

        poseStack.popPose();
    }

    private static void drawWaveLayer(PoseStack poseStack, VertexConsumer consumer, float radius,
                                      float rotation, int red, int green, int blue, int alpha) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        PoseStack.Pose pose = poseStack.last();
        beamQuad(pose, consumer,
                -radius, 0, -radius, -radius, 0, radius,
                radius, 0, radius, radius, 0, -radius,
                red, green, blue, alpha, 0, 0, 1, 1);
        beamQuad(pose, consumer,
                radius, 0, -radius, radius, 0, radius,
                -radius, 0, radius, -radius, 0, -radius,
                red, green, blue, alpha, 0, 0, 1, 1);
        poseStack.popPose();
    }

    private static void drawPrismTube(PoseStack.Pose pose, VertexConsumer consumer,
                                      float startX, float startY, float startZ,
                                      float endX, float endY, float endZ, float width,
                                      int red, int green, int blue, int alpha,
                                      float minU, float minV, float maxU, float maxV) {
        float h = width * 0.5F;
        beamQuad(pose, consumer,
                startX - h, startY - h, startZ, startX + h, startY - h, startZ,
                endX + h, endY - h, endZ, endX - h, endY - h, endZ,
                red, green, blue, alpha, minU, minV, maxU, maxV);
        beamQuad(pose, consumer,
                startX + h, startY + h, startZ, startX - h, startY + h, startZ,
                endX - h, endY + h, endZ, endX + h, endY + h, endZ,
                red, green, blue, alpha, minU, minV, maxU, maxV);
        beamQuad(pose, consumer,
                startX - h, startY + h, startZ, startX - h, startY - h, startZ,
                endX - h, endY - h, endZ, endX - h, endY + h, endZ,
                red, green, blue, alpha, minU, minV, maxU, maxV);
        beamQuad(pose, consumer,
                startX + h, startY - h, startZ, startX + h, startY + h, startZ,
                endX + h, endY + h, endZ, endX + h, endY - h, endZ,
                red, green, blue, alpha, minU, minV, maxU, maxV);
    }

    private static void beamQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float x3, float y3, float z3, float x4, float y4, float z4,
                                 int red, int green, int blue, int alpha,
                                 float minU, float minV, float maxU, float maxV) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        beamVertex(consumer, matrix, normal, x1, y1, z1, minU, maxV, red, green, blue, alpha);
        beamVertex(consumer, matrix, normal, x2, y2, z2, maxU, maxV, red, green, blue, alpha);
        beamVertex(consumer, matrix, normal, x3, y3, z3, maxU, minV, red, green, blue, alpha);
        beamVertex(consumer, matrix, normal, x4, y4, z4, minU, minV, red, green, blue, alpha);
    }

    private static void beamVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                   float x, float y, float z, float u, float v,
                                   int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, x, y, z).color(red, green, blue, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0, 1, 0).endVertex();
    }

    private static void drawCrossedPlanes(PoseStack.Pose pose, VertexConsumer consumer, float size) {
        float h = size * 0.5F;
        quad(pose, consumer, -h, -h, 0, h, -h, 0, h, h, 0, -h, h, 0, 255);
        quad(pose, consumer, 0, -h, -h, 0, -h, h, 0, h, h, 0, h, -h, 255);
        quad(pose, consumer, -h, 0, -h, h, 0, -h, h, 0, h, -h, 0, h, 255);
    }

    private static void drawBillboard(PoseStack.Pose pose, VertexConsumer consumer, float size) {
        float h = size * 0.5F;
        quad(pose, consumer, -h, -h, 0, h, -h, 0, h, h, 0, -h, h, 0, 220);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4, int alpha) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, x1, y1, z1, 0, 1, alpha);
        vertex(consumer, matrix, normal, x2, y2, z2, 1, 1, alpha);
        vertex(consumer, matrix, normal, x3, y3, z3, 1, 0, alpha);
        vertex(consumer, matrix, normal, x4, y4, z4, 0, 0, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v, int alpha) {
        consumer.vertex(matrix, x, y, z).color(230, 225, 255, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(AstralAnchorEntity entity) {
        return switch (entity.getMode()) {
            case AstralAnchorEntity.LUMINOUS_BEAM, AstralAnchorEntity.DARK_SPARK -> BEAM_TEXTURE;
            case AstralAnchorEntity.BRIGHTBURST -> BRIGHTBURST_WAVE_TEXTURE;
            case AstralAnchorEntity.CELESTIAL_TETHER -> CELESTIAL_TETHER_SEAL_TEXTURE;
            case AstralAnchorEntity.STARCUTTER -> STARCUTTER_TEXTURE;
            case AstralAnchorEntity.PIERCING_LIGHT_CIRCLE -> PIERCING_CIRCLE_TEXTURE;
            default -> STARFIRE_TEXTURE;
        };
    }
}
