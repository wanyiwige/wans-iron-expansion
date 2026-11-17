package cn.wanyiwige.twilight;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TwilightClient {
    @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event){event.register(TwilightFoundation.id("item/scroll_exanimated"));}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TwilightEntities.TWILIGHT_BOLT.get(), TwilightBoltRenderer::new);
        event.registerEntityRenderer(TwilightEntities.DRUID_BOLT.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(TwilightEntities.AURORA_MISSILE.get(), AuroraMissileRenderer::new);
        TwilightEntities.PROJECTILES.forEach((id,type)->{
            if(id.equals("exanimated_trident"))event.registerEntityRenderer(type.get(),TwilightClientRenderers.TridentRenderer::new);
            else if(id.equals("hydra_fireball")||id.equals("exanimated_fireball"))event.registerEntityRenderer(type.get(),context->new TwilightClientRenderers.FireballRenderer(context,id.equals("hydra_fireball")));
            else event.registerEntityRenderer(type.get(),TwilightClientRenderers.ProjectileRenderer::new);
        });
        TwilightEntities.AREAS.values().forEach(type->event.registerEntityRenderer(type.get(),TwilightClientRenderers.AreaRenderer::new));
        TwilightEntities.SOULS.values().forEach(type->event.registerEntityRenderer(type.get(),TwilightClientRenderers.SoulRenderer::new));
        event.registerEntityRenderer(TwilightEntities.OMINOUS_LICH.get(),context->new TwilightClientRenderers.CasterRenderer(context,false));
        event.registerEntityRenderer(TwilightEntities.ADVANCED_DRUID.get(),context->new TwilightClientRenderers.CasterRenderer(context,false));
        event.registerEntityRenderer(TwilightEntities.LOYAL_ZOMBIE.get(),context->new TwilightClientRenderers.CasterRenderer(context,true));
        event.registerEntityRenderer(TwilightEntities.WINTER_WOLF.get(),twilightforest.client.renderer.entity.WinterWolfRenderer::new);
        event.registerEntityRenderer(TwilightEntities.MINOTAUR.get(),TwilightClientRenderers.MinotaurRenderer::new);
        event.registerEntityRenderer(TwilightEntities.DEATH_TOME.get(),context->new twilightforest.client.renderer.entity.TFGenericMobRenderer(context,new twilightforest.client.model.entity.DeathTomeModel(context.bakeLayer(twilightforest.client.model.TFModelLayers.DEATH_TOME)),.5F,"deathtome.png"));
        event.registerEntityRenderer(TwilightEntities.CARMINITE_GOLEM.get(),context->new twilightforest.client.renderer.entity.CarminiteGolemRenderer(context,new twilightforest.client.model.entity.CarminiteGolemModel(context.bakeLayer(twilightforest.client.model.TFModelLayers.CARMINITE_GOLEM)),.5F));
    }
    @SubscribeEvent public static void particles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event){TwilightParticles.PARTICLES.getEntries().forEach(type->event.registerSpriteSet((net.minecraft.core.particles.SimpleParticleType)type.get(),sprites->TwilightClientParticle.provider(sprites,type.getId().getPath())));}
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
        net.minecraft.client.renderer.item.ItemProperties.register(TwilightEquipment.get("boreal_bow"),new net.minecraft.resources.ResourceLocation("pull"),(stack,world,entity,seed)->entity!=null&&entity.getUseItem()==stack?(stack.getUseDuration()-entity.getUseItemRemainingTicks())/20F:0);
        net.minecraft.client.renderer.item.ItemProperties.register(TwilightEquipment.get("boreal_bow"),new net.minecraft.resources.ResourceLocation("pulling"),(stack,world,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1:0);
    });}
    private TwilightClient() {}
}
