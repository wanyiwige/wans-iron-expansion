package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessRiteEntity;
import cn.wanyiwige.alexcavesspellsbook.spells.PureDarknessRiteSpell.Kind;
import com.github.alexmodguy.alexscaves.client.model.UnderzealotModel;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.UnderzealotEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessRiteEntity.*;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, value = Dist.CLIENT)
public final class PureDarknessRiteRenderer extends EntityRenderer<PureDarknessRiteEntity> {
    private static final ResourceLocation FISSURE = texture("burrow_fissure");
    private static final ResourceLocation NODES = texture("chorus_nodes");
    private static final ResourceLocation UNDERZEALOT =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "textures/entity/underzealot.png");
    private static final ResourceLocation[] WINGS = {
            texture("wing_eye_stage_0"), texture("wing_eye_stage_1"), texture("wing_eye_stage_2"),
            texture("wing_eye_stage_3"), texture("wing_eye_stage_4")
    };

    private final UnderzealotModel zealot = new UnderzealotModel();
    private ZealotPose zealotPose;
    private List<Part> hooks, wings;

    public PureDarknessRiteRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public boolean shouldRender(PureDarknessRiteEntity rite, Frustum camera, double x, double y, double z) {
        if (!rite.ready() || rite.distanceToSqr(x, y, z) > 64 * 64) return false;
        double padding = switch (rite.kind()) {
            case ZEALOT_CHORUS -> rite.slot() < 0 ? rite.kind().chorusRadius(rite.rank()) + 2 : 2;
            default -> 2;
        };
        return camera.isVisible(rite.getBoundingBox().inflate(padding, 2.5, padding));
    }

    @Override public Vec3 getRenderOffset(PureDarknessRiteEntity rite, float partial) {
        if (rite.ready() && (rite.kind() == Kind.MOTHWING_GUARD || rite.kind() == Kind.BURROWING_PRAYER)
                && rite.owner() != null) {
            return position(rite.owner(), partial).subtract(position(rite, partial));
        }
        return Vec3.ZERO;
    }

    @Override public void render(PureDarknessRiteEntity rite, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        if (!rite.ready()) return;
        float age = Math.max(0, rite.age(partial));
        pose.pushPose();
        switch (rite.kind()) {
            case THORN_GIBBET -> gibbet(rite, age, pose, buffers, light);
            case ZEALOT_CHORUS -> chorus(rite, age, pose, buffers, light);
            case MOTHWING_GUARD -> guard(rite, age, partial, pose, buffers, light);
            case BURROWING_PRAYER -> shadow(rite, age, pose, buffers);
        }
        pose.popPose();
        super.render(rite, yaw, partial, pose, buffers, light);
    }

    private void gibbet(PureDarknessRiteEntity rite, float age, PoseStack pose,
                        MultiBufferSource buffers, int light) {
        if (hooks == null) hooks = loadModel("thorn_gibbet");
        pose.mulPose(Axis.YP.rotation(yaw(rite.forward())));
        float rise = ramp(age, 2, 8) * (1 - ramp(age, 22, 28));
        float pinch = ramp(age, GIBBET_HOLD, GIBBET_HIT) * (1 - ramp(age, 20, 24));
        for (int i = 0; i < hooks.size(); i++) {
            Part hook = hooks.get(i);
            double side = Math.signum(hook.pivot().x);
            pose.pushPose();
            translate(pose, hook.pivot().add(-side * .25 * pinch, -1.9 * (1 - rise), 0));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (side * (15 * pinch - 12 * (1 - rise)))));
            pose.scale(1.13F, 1.35F, 1.2F);
            draw(hook, pose, buffers, null, light, 1);
            pose.popPose();
            ground(pose, buffers, FISSURE, new Vec3(side * .8, .023, 0), .31F, .54F, 0,
                    Math.min(5, (int) age / 4), .85F * (1 - ramp(age, 22, 28)));
        }
    }

    private void chorus(PureDarknessRiteEntity rite, float age, PoseStack pose,
                        MultiBufferSource buffers, int light) {
        if (rite.slot() >= 0) {
            if (zealotPose == null || zealotPose.level() != rite.level()) zealotPose = new ZealotPose(rite.level());
            float poseIn = ramp(age, rite.slot() * 2, 8 + rite.slot() * 2);
            zealotPose.tickCount = (int) age;
            float dissolve = rite.ending() < 0 ? 0 : ramp(age, rite.ending(), rite.ending() + 8);
            zealotPose.pray = poseIn * (1 - ramp(age, CHORUS_BURST - 4, CHORUS_BURST + 3));
            zealotPose.buried = Math.max(1 - poseIn, dissolve);
            zealot.setupAnim(zealotPose, 0, 0, age, 0, 0);
            pose.mulPose(Axis.YP.rotation(Mth.PI + yaw(rite.origin().subtract(rite.position()))));
            pose.scale(-.85F, -.85F, .85F);
            pose.translate(0, -1.501, 0);
            zealot.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(UNDERZEALOT)),
                    light, OverlayTexture.NO_OVERLAY, .83F, .52F, .56F,
                    poseIn * (1 - Math.max(dissolve, ramp(age, CHORUS_BURST + 4, rite.kind().duration(rite.rank())))));
            return;
        }
        if (rite.ending() >= 0) return;
        int mask = rite.status();
        for (int i = 0; i < 3; i++) {
            Vec3 point = rite.point(i);
            if ((mask & (1 << i)) == 0 || point == null) continue;
            Vec3 end = point.subtract(rite.position());
            int beat = chorusBeat(i);
            float pulse = ramp(age, beat - 4, beat) * (1 - ramp(age, beat + 1, beat + 5));
            float tail = 1 - ramp(age, CHORUS_BURST + 3, rite.kind().duration(rite.rank()));
            ground(pose, buffers, NODES, end.add(0, .035, 0), .58F, .58F, 0,
                    (int) Math.min(5, pulse * 5), (.48F + pulse * .47F) * tail);
            if (age >= beat - 2 && age < CHORUS_BURST + 3) {
                line(pose, buffers, NODES, end, Vec3.ZERO, .13F + pulse * .11F, (.46F + pulse * .47F) * tail);
            }
            if (age >= CHORUS_BURST - 5) {
                float radius = (float) rite.kind().chorusRadius(rite.rank());
                float spread = .42F + .58F * ramp(age, CHORUS_BURST - 5, CHORUS_BURST);
                float alpha = (.24F + .40F * ramp(age, CHORUS_BURST - 3, CHORUS_BURST)) * tail;
                ground(pose, buffers, NODES, end.add(0, .047 + i * .004, 0), radius * spread, radius * spread, 0,
                        Math.min(5, (int) (age - CHORUS_BURST + 5) / 3), alpha);
            }
        }
    }

    private void guard(PureDarknessRiteEntity rite, float age, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (wings == null) wings = loadModel("mothwing_guard");
        var owner = rite.owner();
        if (owner == null) return;
        float heading = Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180 - heading));
        float scale = owner.getBbHeight() / 1.8F;
        pose.scale(scale, scale, scale);
        // The wing mesh uses positive local Z for the wearer’s back.
        pose.translate(0, 0, .22);
        float end = rite.ending() < 0 ? 0 : ramp(age, rite.ending(), rite.ending() + 8);
        float open = ramp(age, 0, 6);
        int loss = Mth.clamp(rite.kind().charges(rite.rank()) - rite.status(), 0, 4);
        int impact = rite.guardImpact();
        float recoil = impact == 0 ? 0 : 1 - ramp(age, Math.abs(impact) - 1, Math.abs(impact) + 5);
        float sweep = Mth.lerp(end, Mth.lerp(open, -62F, 22F), -62F);
        for (Part wing : wings) {
            double side = Math.signum(wing.pivot().x);
            pose.pushPose();
            translate(pose, wing.pivot());
            float kick = side == Math.signum(impact) ? 30 * recoil * (1 - end) : 0;
            pose.mulPose(Axis.YP.rotationDegrees((float) (side * (sweep + kick))));
            pose.mulPose(Axis.ZP.rotationDegrees((float) (side * (Mth.sin(age * .24F) * 3 + 16 * end))));
            draw(wing, pose, buffers, WINGS[loss], light, 1 - end);
            pose.popPose();
        }
    }

    private static void shadow(PureDarknessRiteEntity rite, float age, PoseStack pose, MultiBufferSource buffers) {
        ground(pose, buffers, FISSURE, new Vec3(0, .032, 0), .49F, .66F, yaw(rite.forward()),
                Math.min(5, (int) age / 2), .95F);
        ground(pose, buffers, FISSURE, rite.forward().scale(-.55).add(0, .024, 0),
                .33F, .54F, yaw(rite.forward()), Math.min(5, (int) age / 2), .63F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderBurrowingPlayer(RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        boolean buried = !player.level().getEntitiesOfClass(PureDarknessRiteEntity.class,
                player.getBoundingBox().inflate(2.25), rite -> rite.ready()
                        && rite.kind() == Kind.BURROWING_PRAYER && rite.owner() == player
                        && rite.age(event.getPartialTick()) >= 2 && rite.age(event.getPartialTick()) < BURROW_TRAVEL_TICKS - 1).isEmpty();
        if (buried) event.setCanceled(true);
    }

    static void ground(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, Vec3 at,
                               float x, float z, float yaw, int frame, float alpha) {
        if (alpha <= 0) return;
        pose.pushPose();
        translate(pose, at);
        pose.mulPose(Axis.YP.rotation(yaw));
        var out = buffers.getBuffer(RenderType.entityTranslucent(texture));
        float v = (frame + .005F) / 6F, w = (frame + .995F) / 6F;
        vertex(out, pose.last(), new Vec3(-x, 0, z), 0, w, new Vec3(0, 1, 0), LightTexture.FULL_BRIGHT, alpha);
        vertex(out, pose.last(), new Vec3(x, 0, z), 1, w, new Vec3(0, 1, 0), LightTexture.FULL_BRIGHT, alpha);
        vertex(out, pose.last(), new Vec3(x, 0, -z), 1, v, new Vec3(0, 1, 0), LightTexture.FULL_BRIGHT, alpha);
        vertex(out, pose.last(), new Vec3(-x, 0, -z), 0, v, new Vec3(0, 1, 0), LightTexture.FULL_BRIGHT, alpha);
        pose.popPose();
    }

    private static void line(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture,
                             Vec3 from, Vec3 to, float width, float alpha) {
        Vec3 direction = to.subtract(from);
        if (direction.horizontalDistanceSqr() < .01) return;
        Vec3 side = new Vec3(direction.z, 0, -direction.x).normalize().scale(width);
        from = from.add(0, .055, 0);
        to = to.add(0, .055, 0);
        Vec3 normal = new Vec3(0, 1, 0);
        var out = buffers.getBuffer(RenderType.entityTranslucent(texture));
        vertex(out, pose.last(), from.subtract(side), 0, .825F, normal, LightTexture.FULL_BRIGHT, alpha);
        vertex(out, pose.last(), from.add(side), 1, .825F, normal, LightTexture.FULL_BRIGHT, alpha);
        vertex(out, pose.last(), to.add(side), 1, .672F, normal, LightTexture.FULL_BRIGHT, alpha);
        vertex(out, pose.last(), to.subtract(side), 0, .672F, normal, LightTexture.FULL_BRIGHT, alpha);
    }

    private record Point(Vec3 pos, float u, float v) {}
    private record Face(ResourceLocation texture, List<Point> points, Vec3 normal) {}
    record Part(Vec3 pivot, List<Face> faces) {}

    static List<Part> loadModel(String name) {
        var id = ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "ritual_models/" + name + ".json");
        try (var reader = new InputStreamReader(Minecraft.getInstance().getResourceManager().open(id),
                StandardCharsets.UTF_8)) {
            List<Part> parts = new ArrayList<>();
            for (var item : JsonParser.parseReader(reader).getAsJsonArray()) {
                var part = item.getAsJsonObject();
                List<Face> faces = new ArrayList<>();
                for (var faceValue : part.getAsJsonArray("faces")) {
                    JsonArray face = faceValue.getAsJsonArray();
                    List<Point> points = new ArrayList<>(4);
                    for (var point : face.get(1).getAsJsonArray()) {
                        JsonArray p = point.getAsJsonArray();
                        points.add(new Point(readVec(p), p.get(3).getAsFloat(), p.get(4).getAsFloat()));
                    }
                    Vec3 normal = points.get(1).pos().subtract(points.get(0).pos())
                            .cross(points.get(2).pos().subtract(points.get(0).pos())).normalize();
                    ResourceLocation material = ResourceLocation.tryParse(face.get(0).getAsString());
                    if (material != null) faces.add(new Face(material, points, normal));
                }
                parts.add(new Part(readVec(part.getAsJsonArray("pivot")), faces));
            }
            return parts;
        } catch (IOException | RuntimeException error) {
            com.mojang.logging.LogUtils.getLogger().warn("Could not load ritual model {}", id, error);
            return List.of();
        }
    }

    private static Vec3 readVec(JsonArray values) {
        return new Vec3(values.get(0).getAsDouble(), values.get(1).getAsDouble(), values.get(2).getAsDouble());
    }

    static void draw(Part part, PoseStack pose, MultiBufferSource buffers, ResourceLocation override,
                             int light, float alpha) {
        for (Face face : part.faces()) {
            ResourceLocation texture = override == null ? face.texture() : override;
            var out = buffers.getBuffer(alpha >= .999F ? RenderType.entityCutoutNoCull(texture)
                    : RenderType.entityTranslucent(texture));
            for (Point point : face.points()) {
                vertex(out, pose.last(), point.pos(), point.u(), point.v(), face.normal(), light, alpha);
            }
        }
    }

    static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 point, float u, float v,
                               Vec3 normal, int light, float alpha) {
        out.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z).color(1F, 1F, 1F, alpha)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }

    static float ramp(float age, float start, float end) { return Mth.clamp((age - start) / (end - start), 0, 1); }
    static float yaw(Vec3 direction) { return (float) Math.atan2(direction.x, direction.z); }
    static void translate(PoseStack pose, Vec3 at) { pose.translate(at.x, at.y, at.z); }
    static Vec3 position(Entity entity, float partial) {
        return new Vec3(Mth.lerp(partial, entity.xo, entity.getX()), Mth.lerp(partial, entity.yo, entity.getY()),
                Mth.lerp(partial, entity.zo, entity.getZ()));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "textures/entity/pure_darkness/" + name + ".png");
    }

    @Override public ResourceLocation getTextureLocation(PureDarknessRiteEntity rite) { return FISSURE; }

    private static final class ZealotPose extends UnderzealotEntity {
        float pray, buried;
        ZealotPose(Level level) { super(ACEntityRegistry.UNDERZEALOT.get(), level); }
        @Override public float getPrayingProgress(float partial) { return pray; }
        @Override public float getBuriedProgress(float partial) { return buried; }
    }
}

