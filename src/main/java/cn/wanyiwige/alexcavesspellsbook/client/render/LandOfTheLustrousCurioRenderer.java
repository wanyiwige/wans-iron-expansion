package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.compat.Curios;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Rendered in the wearer's body frame, with no separate entity or position packets. */
public final class LandOfTheLustrousCurioRenderer implements ICurioRenderer {
    // Model-space X points left, Y down and Z back in the parent humanoid layer.
    private static final double SHOULDER_X = -0.78;
    private static final double SHOULDER_Y = -0.10;
    private static final double SHOULDER_Z = 0.10;
    private static final float MODEL_SCALE = 0.48F;
    private static final float BOB_HEIGHT = 0.025F;
    private static final ResourceLocation TEXTURE = asset("textures/curios/land_of_the_lustrous_caster.png");
    private final Caster caster = new Caster();
    private final GeoObjectRenderer<Caster> renderer = new GeoObjectRenderer<>(new CasterModel());

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext context, PoseStack poseStack,
            RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        LivingEntity wearer = context.entity();
        if (!Curios.SPELLBOOK_SLOT.equals(context.identifier()) || !context.visible()
                || wearer.isInvisible()) {
            return;
        }
        float time = wearer.tickCount + partialTicks;
        float phase = (wearer.getId() % 31) * 0.2F;
        poseStack.pushPose();
        try {
            // The outer layer already contains the interpolated body yaw, riding and flight pose.
            // Applying the torso part also follows its crouch translation and bend.
            if (parent.getModel() instanceof HumanoidModel<?> humanoid) {
                humanoid.body.translateAndRotate(poseStack);
            } else {
                ICurioRenderer.translateIfSneaking(poseStack, wearer);
            }
            poseStack.translate(SHOULDER_X,
                    SHOULDER_Y - Mth.sin(time * 0.09F + phase) * BOB_HEIGHT, SHOULDER_Z);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(time * 0.045F + phase) * 5.0F));
            // Convert GeckoLib's upright/forward model into the humanoid layer's axes.
            poseStack.scale(MODEL_SCALE, -MODEL_SCALE, -MODEL_SCALE);
            // GeoObjectRenderer adds a block-center offset in preRender; this accessory is centered.
            poseStack.translate(-0.5, -0.51, -0.5);
            caster.tick = time;
            RenderType type = RenderType.entityCutoutNoCull(TEXTURE);
            renderer.render(poseStack, caster, buffers, type, buffers.getBuffer(type), light);
        } finally {
            poseStack.popPose();
        }
    }

    private static ResourceLocation asset(String path) {
        return new ResourceLocation(AlexCavesSpellsbook.MOD_ID, path);
    }

    private static final class Caster implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);
        private double tick;

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object relatedObject) {
            return tick;
        }
    }

    private static final class CasterModel extends GeoModel<Caster> {
        @Override
        public ResourceLocation getModelResource(Caster caster) {
            return asset("geo/land_of_the_lustrous_caster.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(Caster caster) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(Caster caster) {
            return asset("animations/land_of_the_lustrous_caster.animation.json");
        }
    }
}
