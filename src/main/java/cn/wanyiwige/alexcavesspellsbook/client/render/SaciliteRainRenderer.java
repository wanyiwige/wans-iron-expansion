package cn.wanyiwige.alexcavesspellsbook.client.render;

import cn.wanyiwige.alexcavesspellsbook.entities.SaciliteSpellProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public final class SaciliteRainRenderer extends EntityRenderer<SaciliteSpellProjectile> {
    private final BlockRenderDispatcher blocks;
    public SaciliteRainRenderer(EntityRendererProvider.Context context) {
        super(context); blocks = context.getBlockRenderDispatcher(); shadowRadius = 0.3F;
    }
    @Override public void render(SaciliteSpellProjectile entity, float yaw, float partialTick,
                                 PoseStack pose, MultiBufferSource buffer, int light) {
        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("foolish", "sacilite_bomb"));
        if (block != null) {
            pose.pushPose(); pose.translate(-0.5, 0, -0.5);
            blocks.renderSingleBlock(block.defaultBlockState(), pose, buffer, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }
    @Override public ResourceLocation getTextureLocation(SaciliteSpellProjectile entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
