package cn.wanyiwige.alexcavesspellsbook.toxic.client;
import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.toxic.ToxicRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ToxicClientEvents {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ToxicRegistry.GAMMA.get(),ToxicEffectRenderer::new);
        event.registerEntityRenderer(ToxicRegistry.DRUM.get(),ToxicEffectRenderer::new);
        event.registerEntityRenderer(ToxicRegistry.PURIFICATION.get(),ToxicEffectRenderer::new);
        event.registerEntityRenderer(ToxicRegistry.LANCE.get(),ToxicEffectRenderer::new);
        event.registerEntityRenderer(ToxicRegistry.FISSION.get(),ToxicEffectRenderer::new);
        event.registerEntityRenderer(ToxicRegistry.WARD.get(),ToxicEffectRenderer::new);
        for(var type:ToxicRegistry.NEW_EFFECTS.values())event.registerEntityRenderer(type.get(),RadiationEffectRenderer::new);
    }
    private ToxicClientEvents(){}
}
