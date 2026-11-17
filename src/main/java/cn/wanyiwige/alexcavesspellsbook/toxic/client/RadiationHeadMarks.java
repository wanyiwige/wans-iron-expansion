package cn.wanyiwige.alexcavesspellsbook.toxic.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.toxic.ToxicRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A world pass covers both vanilla living renderers and GeckoLib/custom boss renderers. */
@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID,value=Dist.CLIENT)
public final class RadiationHeadMarks {
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        PoseStack pose=event.getPoseStack();Vec3 camera=event.getCamera().getPosition();
        var buffers=mc.renderBuffers().bufferSource();
        var sigilType=RenderType.entityTranslucentEmissive(ToxicRegistry.id("textures/entity/toxic/rework/isotope.png"));
        var dotType=RenderType.entityTranslucentEmissive(ToxicRegistry.id("textures/entity/toxic/rework/pixel.png"));
        boolean drawn=false;
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof LivingEntity living)||entity.distanceToSqr(camera)>48*48||entity.isInvisibleTo(mc.player))continue;
            var mark=living.getEffect(ToxicRegistry.MARK_VISUAL.get());if(mark==null)continue;
            Vec3 at=entity.getPosition(event.getPartialTick()).subtract(camera).add(0,entity.getBbHeight()+.48,0);
            pose.pushPose();pose.translate(at.x,at.y,at.z);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            int frame=(entity.tickCount/6)&3;
            ToxicEffectRenderer.frameQuad(pose,buffers.getBuffer(sigilType),new Vec3(-.31,-.31,0),new Vec3(.31,-.31,0),new Vec3(.31,.31,0),new Vec3(-.31,.31,0),RadiationVisualStyle.tint("isotope"),255,frame);
            int count=Math.min(4,mark.getAmplifier()+1);
            var dots=buffers.getBuffer(dotType);
            for(int i=0;i<count;i++){
                double x=(i-(count-1)*.5)*.11;
                ToxicEffectRenderer.quad(pose,dots,new Vec3(x-.035,-.40,0),new Vec3(x+.035,-.40,0),new Vec3(x+.035,-.33,0),new Vec3(x-.035,-.33,0),0xE9F65A,240,true);
            }
            pose.popPose();drawn=true;
        }
        if(drawn){buffers.endBatch(sigilType);buffers.endBatch(dotType);}
    }
    private RadiationHeadMarks(){}
}
