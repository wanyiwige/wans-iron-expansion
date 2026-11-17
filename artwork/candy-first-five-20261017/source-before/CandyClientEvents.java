package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.candy.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import com.github.alexmodguy.alexscaves.client.render.entity.GummyBearRenderer;
import com.github.alexmodguy.alexscaves.client.render.entity.GingerbreadManRenderer;
import com.github.alexmodguy.alexscaves.client.render.entity.GumbeeperRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class CandyClientEvents {
    @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CandyRegistry.EFFECT.get(), CandyEffectRenderer::new);
        event.registerEntityRenderer(CandyRegistry.SUMMON_BEAR.get(),nativeBear());
        event.registerEntityRenderer(CandyRegistry.SUMMON_GINGER.get(),nativeGinger());
        event.registerEntityRenderer(CandyRegistry.SUMMON_SENTRY.get(),nativeSentry());
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static EntityRendererProvider nativeBear(){return c->(EntityRenderer)new GummyBearRenderer(c);}
    @SuppressWarnings({"rawtypes","unchecked"}) private static EntityRendererProvider nativeGinger(){return c->(EntityRenderer)new GingerbreadManRenderer(c);}
    @SuppressWarnings({"rawtypes","unchecked"}) private static EntityRendererProvider nativeSentry(){return c->(EntityRenderer)new GumbeeperRenderer(c);}
    @Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID, value=Dist.CLIENT)
    public static final class Ticks {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            Minecraft mc=Minecraft.getInstance();
            if(event.phase!=TickEvent.Phase.END || mc.level==null || mc.isPaused()) return;
            int remaining=64;
            for(var entity:mc.level.entitiesForRendering()) if(entity instanceof CandyEffectEntity effect) {
                remaining-=CandyVisuals.particles(effect,remaining); if(remaining<=0) break;
            }
        }
    }
}
