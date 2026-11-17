package cn.wanyiwige.illusion.mixin;
import cn.wanyiwige.illusion.IllusionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=LevelRenderer.class,remap=false)
public abstract class EmptyWorldMixin {
    @Inject(method={"renderChunkLayer","m_172993_"},remap=false,at=@At("HEAD"),cancellable=true)
    private void wan$empty(RenderType type,com.mojang.blaze3d.vertex.PoseStack poses,double x,double y,double z,Matrix4f projection,CallbackInfo ci){var player=Minecraft.getInstance().player;if(player!=null&&IllusionClient.has(player,"emptiness"))ci.cancel();}
}
