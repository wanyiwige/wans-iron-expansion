package cn.wanyiwige.astralcompat.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.astralcompat.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent
    public static void registerScrollModel(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(new net.minecraft.resources.ResourceLocation("asterismarcanum", "item/arcanum_scroll_astral"));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ARCANUM_PROJECTILE.get(), ArcanumProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.ARCANUM_EFFECT.get(), ArcanumEffectRenderer::new);
        event.registerEntityRenderer(ModEntities.ASTRAL_BOLT.get(), AstralBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.PIERCING_STAR.get(), PiercingStarRenderer::new);
        event.registerEntityRenderer(ModEntities.ASTRAL_ANCHOR.get(), AstralAnchorRenderer::new);
    }

    private ClientRegistration() {
    }
}
