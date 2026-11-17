package cn.wanyiwige.astralcompat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Six-faced sky using the same installed Iron's Spellbooks textures as upstream. */
public final class AstralSeaSky extends DimensionSpecialEffects {
    public AstralSeaSky() { super(Float.NaN,false,SkyType.NONE,false,true); }
    @Override public Vec3 getBrightnessDependentFogColor(Vec3 fog,float brightness) { return new Vec3(.16,.1,.28); }
    @Override public boolean isFoggyAt(int x,int z) { return false; }
    @Override public boolean renderClouds(ClientLevel level,int ticks,float partial,PoseStack pose,double x,double y,double z,Matrix4f projection) { return true; }
    @Override public boolean renderSky(ClientLevel level,int ticks,float partial,PoseStack pose,Camera camera,Matrix4f projection,boolean foggy,Runnable setupFog) {
        RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        try {
            cube(pose,new ResourceLocation("irons_spellbooks","textures/environment/pocket_dimension_sky.png"),200,.32F,.22F,.55F,1,0);
            cube(pose,new ResourceLocation("irons_spellbooks","textures/environment/pocket_clouds.png"),190,.55F,.42F,.75F,.22F,(ticks+partial)*.001F);
        } finally { RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend(); }
        return true;
    }
    private static void cube(PoseStack pose,ResourceLocation texture,float radius,float r,float g,float b,float a,float offset) {
        RenderSystem.setShaderTexture(0,texture);
        for(int face=0;face<6;face++) {
            pose.pushPose();
            if(face<4)pose.mulPose(Axis.YP.rotationDegrees(face*90));else pose.mulPose(Axis.XP.rotationDegrees(face==4?90:-90));
            Matrix4f matrix=pose.last().pose();BufferBuilder buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.vertex(matrix,-radius,-radius,-radius).uv(offset,1).color(r,g,b,a).endVertex();
            buffer.vertex(matrix,radius,-radius,-radius).uv(offset+1,1).color(r,g,b,a).endVertex();
            buffer.vertex(matrix,radius,radius,-radius).uv(offset+1,0).color(r,g,b,a).endVertex();
            buffer.vertex(matrix,-radius,radius,-radius).uv(offset,0).color(r,g,b,a).endVertex();
            BufferUploader.drawWithShader(buffer.end());pose.popPose();
        }
    }
}
