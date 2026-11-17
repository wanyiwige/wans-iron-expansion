package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.entities.SaciliteSpellProjectile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

/** References the installed Foolish resources without redistributing its model/animation. */
public final class SaciliteCrystalRenderer extends GeoEntityRenderer<SaciliteSpellProjectile> {
    public SaciliteCrystalRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<>() {
            @Override public ResourceLocation getModelResource(SaciliteSpellProjectile entity) {
                return ResourceLocation.fromNamespaceAndPath("foolish", "geo/seekingsacilite.geo.json");
            }
            @Override public ResourceLocation getTextureResource(SaciliteSpellProjectile entity) {
                return ResourceLocation.fromNamespaceAndPath("foolish", "textures/entities/seekingsacilite.png");
            }
            @Override public ResourceLocation getAnimationResource(SaciliteSpellProjectile entity) {
                return ResourceLocation.fromNamespaceAndPath("foolish", "animations/seekingsacilite.animation.json");
            }
        });
        shadowRadius = 0.15F;
    }
    @Override public RenderType getRenderType(SaciliteSpellProjectile entity, ResourceLocation texture,
                                              MultiBufferSource buffer, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
    @Override protected void applyRotations(SaciliteSpellProjectile entity, PoseStack pose,
                                            float age, float bodyYaw, float partialTick) {
        // GeoEntityRenderer's living body/head rotation does not orient this Projectile.
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-pitch));
    }
}
