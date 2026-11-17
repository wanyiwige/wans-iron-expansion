package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.entities.DesolateBloodbladeEntity;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class DesolateBloodbladeRenderer extends EntityRenderer<DesolateBloodbladeEntity> {
    private static final ResourceLocation DAGGER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("alexscaves", "textures/item/desolate_dagger.png");
    private final ItemRenderer items;
    private final ItemStack dagger;

    public DesolateBloodbladeRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemRenderer();
        dagger = new ItemStack(ACItemRegistry.DESOLATE_DAGGER.get());
    }

    @Override
    public void render(DesolateBloodbladeEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        Vec3 direction = entity.getDeltaMovement().normalize();
        if (direction.lengthSqr() < 1.0E-8) direction = new Vec3(0, 0, 1);
        pose.mulPose(new Quaternionf().rotationTo(0, 1, 0,
                (float) direction.x, (float) direction.y, (float) direction.z));
        pose.mulPose(Axis.ZP.rotationDegrees(45));
        pose.scale(.85F, .85F, .85F);
        items.renderStatic(dagger, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(DesolateBloodbladeEntity entity) { return DAGGER_TEXTURE; }
}

