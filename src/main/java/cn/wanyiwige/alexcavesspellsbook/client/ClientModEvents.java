package cn.wanyiwige.alexcavesspellsbook.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.client.render.HeavyweightSpellRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.SpellMagneticWeaponRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.TeslaArcNodeRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.DipoleVortexRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.PolarityCollapseRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.MagneticRailLanceRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.DipoleAnvilRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.MagnetizedFractureChainRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.PolarityReversalRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.FerrousSandstormRenderer;
import cn.wanyiwige.alexcavesspellsbook.registry.ModEntities;
import cn.wanyiwige.alexcavesspellsbook.registry.ModItems;
import cn.wanyiwige.alexcavesspellsbook.registry.PureDarknessParticles;
import cn.wanyiwige.alexcavesspellsbook.client.render.DesolateBloodbladeRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.PureDarknessFieldRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.PureDarknessRiteRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.PureDarknessWardRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.PureDarknessFinaleRenderer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import cn.wanyiwige.alexcavesspellsbook.client.render.LandOfTheLustrousCurioRenderer;
import cn.wanyiwige.alexcavesspellsbook.client.render.LustrousGemRenderer;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import com.github.alexmodguy.alexscaves.client.render.entity.FerrouslimeRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CuriosRendererRegistry.register(
                ModItems.CANDY_STARCATCHER.get(), io.redspace.ironsspellbooks.render.SpellBookCurioRenderer::new));
        event.enqueueWork(() -> CuriosRendererRegistry.register(
                ModItems.LAND_OF_THE_LUSTROUS.get(), LandOfTheLustrousCurioRenderer::new));
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new LustrousGemModels());
    }

    @SubscribeEvent
    public static void registerPureDarknessParticles(RegisterParticleProvidersEvent event) {
        registerDarknessParticle(event, PureDarknessParticles.DREAD_ARROW_TAIL.get(), .105F, 0, 9);
        registerDarknessParticle(event, PureDarknessParticles.SONIC_WAVEFRONT.get(), .19F, 0, 10);
        registerDarknessParticle(event, PureDarknessParticles.THORN_SPLINTER.get(), .11F, .18F, 12);
        registerDarknessParticle(event, PureDarknessParticles.BLOOD_MIST.get(), .43F, .005F, 16);
        registerDarknessParticle(event, PureDarknessParticles.MOTH_SCALE.get(), .085F, .07F, 14);
        registerDarknessParticle(event, PureDarknessParticles.GLOOM_MOTH.get(), .16F, -.015F, 16);
        registerDarknessParticle(event, PureDarknessParticles.SOIL_CLAST.get(), .1F, .13F, 11);
        registerDarknessParticle(event, PureDarknessParticles.SCARLET_FILAMENT.get(), .13F, 0, 10);
        registerDarknessParticle(event, PureDarknessParticles.SHADOW_SHARD.get(), .14F, .05F, 11);
        registerDarknessParticle(event, PureDarknessParticles.IRIS_FRAGMENT.get(), .15F, .065F, 10);
        event.registerSpriteSet(PureDarknessParticles.SCARLET_DROP.get(), sprites ->
                (type, level, x, y, z, dx, dy, dz) ->
                        new PureDarknessParticle(level, x, y, z, dx, dy, dz, sprites, false));
        event.registerSpriteSet(PureDarknessParticles.BLOOD_CUT.get(), sprites ->
                (type, level, x, y, z, dx, dy, dz) ->
                        new PureDarknessParticle(level, x, y, z, dx, dy, dz, sprites, true));
    }

    private static void registerDarknessParticle(RegisterParticleProvidersEvent event, SimpleParticleType type,
                                                 float size, float gravity, int duration) {
        event.registerSpriteSet(type, sprites -> (particle, level, x, y, z, dx, dy, dz) ->
                new PureDarknessParticle(level, x, y, z, dx, dy, dz, sprites, size, gravity, duration));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PURE_DARKNESS_FINALE.get(), PureDarknessFinaleRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_DARKNESS_WARD.get(), PureDarknessWardRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_DARKNESS_RITE.get(), PureDarknessRiteRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_DARKNESS_FIELD.get(), PureDarknessFieldRenderer::new);
        event.registerEntityRenderer(ModEntities.DESOLATE_BLOODBLADE.get(), DesolateBloodbladeRenderer::new);
        event.registerEntityRenderer(ModEntities.NEBULOUS_CATACLYSM.get(),
                cn.wanyiwige.alexcavesspellsbook.client.render.NebulousCataclysmRenderer::new);
        event.registerEntityRenderer(cn.wanyiwige.alexcavesspellsbook.foolish.FoolishSpells.EFFECT.get(),
                cn.wanyiwige.alexcavesspellsbook.foolish.client.BossEffectRenderer::new);
        event.registerEntityRenderer(cn.wanyiwige.alexcavesspellsbook.foolish.FoolishSpells.COMPANION.get(),
                cn.wanyiwige.alexcavesspellsbook.foolish.client.BossCompanionRenderer::new);
        event.registerEntityRenderer(ModEntities.SACILITE_RAIN_CRYSTAL.get(),
                cn.wanyiwige.alexcavesspellsbook.client.render.SaciliteRainRenderer::new);
        event.registerEntityRenderer(ModEntities.SEEKING_SACILITE_CRYSTAL.get(),
                cn.wanyiwige.alexcavesspellsbook.client.render.SaciliteCrystalRenderer::new);
        event.registerEntityRenderer(ModEntities.POLARITY_COLLAPSE.get(), PolarityCollapseRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGNETIC_RAIL_LANCE.get(), MagneticRailLanceRenderer::new);
        event.registerEntityRenderer(ModEntities.DIPOLE_ANVIL.get(), DipoleAnvilRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGNETIZED_FRACTURE_CHAIN.get(), MagnetizedFractureChainRenderer::new);
        event.registerEntityRenderer(ModEntities.POLARITY_REVERSAL_TRACKER.get(), PolarityReversalRenderer::new);
        event.registerEntityRenderer(ModEntities.FERROUS_SANDSTORM.get(), FerrousSandstormRenderer::new);
        event.registerEntityRenderer(ModEntities.LUSTROUS_GEM.get(), LustrousGemRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGNETIC_MAGE.get(),
                cn.wanyiwige.alexcavesspellsbook.client.render.MagneticMageRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_MAGNETIC_WEAPON.get(), SpellMagneticWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.HEAVYWEIGHT_SPELL.get(), HeavyweightSpellRenderer::new);
        event.registerEntityRenderer(ModEntities.SUMMONED_FERROUSLIME.get(), ferrouslimeRenderer());
        event.registerEntityRenderer(ModEntities.TESLA_ARC_NODE.get(), TeslaArcNodeRenderer::new);
        event.registerEntityRenderer(ModEntities.DIPOLE_VORTEX.get(), DipoleVortexRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EntityRendererProvider ferrouslimeRenderer() {
        return context -> (EntityRenderer) new FerrouslimeRenderer(context);
    }
}
