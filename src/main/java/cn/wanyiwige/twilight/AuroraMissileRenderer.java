package cn.wanyiwige.twilight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class AuroraMissileRenderer extends EntityRenderer<AuroraMissileProjectile> {
    private static final ResourceLocation TEXTURE = TwilightFoundation.id("textures/entity/aurora_missile.png");
    public AuroraMissileRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(AuroraMissileProjectile entity, float yaw, float partial,
                                 PoseStack stack, MultiBufferSource buffers, int light) {
        stack.pushPose();
        stack.mulPose(entityRenderDispatcher.cameraOrientation());
        stack.scale(.7F, .7F, .7F);
        Matrix4f pose = stack.last().pose();
        VertexConsumer vertex = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        vertex.vertex(pose, -.5F, -.5F, 0).color(255,255,255,255).uv(0,1).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        vertex.vertex(pose,  .5F, -.5F, 0).color(255,255,255,255).uv(1,1).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        vertex.vertex(pose,  .5F,  .5F, 0).color(255,255,255,255).uv(1,0).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        vertex.vertex(pose, -.5F,  .5F, 0).color(255,255,255,255).uv(0,0).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();
        stack.popPose();
        renderRibbon(entity,stack,buffers);
        super.render(entity, yaw, partial, stack, buffers, light);
    }
    private void renderRibbon(AuroraMissileProjectile entity,PoseStack stack,MultiBufferSource buffers){
        var points=entity.ribbon();if(points.size()<2)return;
        int[] colors={7601996,5111662,4325317,4325374,5022207,5003263,5985791,14238207};
        var camera=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        VertexConsumer vertices=buffers.getBuffer(RenderType.lightning());
        for(int i=0;i<points.size()-1;i++){
            var a=points.get(i);var b=points.get(i+1);var side=b.subtract(a).cross(camera.subtract(a)).normalize().scale(.18*Math.pow(1-i/24D,2));
            int color=colors[Math.min(7,i*8/24)],alpha=(int)(220*(1-i/24D));
            for(var point:java.util.List.of(a.add(side),b.add(side),b.subtract(side),a.subtract(side))){var p=point.subtract(entity.position());vertices.vertex(stack.last().pose(),(float)p.x,(float)p.y,(float)p.z).color((color>>16)&255,(color>>8)&255,color&255,alpha).endVertex();}
        }
    }
    @Override public ResourceLocation getTextureLocation(AuroraMissileProjectile entity) { return TEXTURE; }
}
