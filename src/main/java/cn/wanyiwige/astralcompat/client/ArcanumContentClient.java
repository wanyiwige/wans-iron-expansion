package cn.wanyiwige.astralcompat.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.astralcompat.recreation.*;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.*;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.*;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ArcanumContentClient {
    public static IClientItemExtensions itemExtensions(String name) {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null)renderer=new NativeItemRenderer<>(name);return renderer;
            }
        };
    }
    private static final class NativeItemRenderer<T extends Item & GeoItem> extends GeoItemRenderer<T> {
        private NativeItemRenderer(String name) {
            super(new GeoModel<T>() {
                public ResourceLocation getModelResource(T item) { return ArcanumContent.id("geo/"+name+".geo.json"); }
                public ResourceLocation getTextureResource(T item) { return ArcanumContent.id("textures/item/"+name+".png"); }
                public ResourceLocation getAnimationResource(T item) { return name.equals("celestial_staff")?ArcanumContent.id("animations/celestial_staff.animation.json"):new ResourceLocation("irons_spellbooks","animations/wizard_armor_animation.json"); }
            });
            // Astrolabe has no glowmask; an uninitialized emissive texture can cover it in black.
            if (name.equals("celestial_staff")) addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }
    public static GeoArmorRenderer<?> armorRenderer() {
        return new GenericCustomArmorRenderer<>(new GeoModel<ArcanumEquipment.AstralArmor>() {
            public ResourceLocation getModelResource(ArcanumEquipment.AstralArmor a) { return ArcanumContent.id("geo/astral_magic_armor.geo.json"); }
            public ResourceLocation getTextureResource(ArcanumEquipment.AstralArmor a) { return ArcanumContent.id("textures/entity/astral_armor/astral_robes_texture.png"); }
            public ResourceLocation getAnimationResource(ArcanumEquipment.AstralArmor a) { return new ResourceLocation("irons_spellbooks","animations/wizard_armor_animation.json"); }
        });
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ArcanumContent.DRAGONFLY.get(),context->new InsectRenderer(context,"dragonfly"));
        event.registerEntityRenderer(ArcanumContent.LUNAR_MOTH.get(),context->new InsectRenderer(context,"lunar_moth"));
        event.registerEntityRenderer(ArcanumContent.SUMMONED_MOTH.get(),context->new InsectRenderer(context,"big_moth"));
        event.registerEntityRenderer(ArcanumContent.THROWN_EGG.get(),ThrownItemRenderer::new);
        event.registerEntityRenderer(ArcanumContent.ASTROMANCER.get(),context->new AbstractSpellCastingMobRenderer(context,new AbstractSpellCastingMobModel() {
            @Override public ResourceLocation getTextureResource(AbstractSpellCastingMob mob) { return ArcanumContent.id("textures/entity/astromancer/astromancer.png"); }
        }) {});
    }
    private static final class InsectRenderer extends GeoEntityRenderer<ArcanumInsect> {
        private InsectRenderer(EntityRendererProvider.Context context,String name) {
            super(context,new GeoModel<ArcanumInsect>() {
                public ResourceLocation getModelResource(ArcanumInsect insect) { return ArcanumContent.id("geo/"+name+".geo.json"); }
                public ResourceLocation getTextureResource(ArcanumInsect insect) { String texture=name.equals("big_moth")?"lunar_moth":name;return ArcanumContent.id("textures/entity/"+texture+"/"+texture+"_texture.png"); }
                public ResourceLocation getAnimationResource(ArcanumInsect insect) { return ArcanumContent.id("animations/"+name+".animation.json"); }
            });addRenderLayer(new AutoGlowingGeoLayer<>(this));shadowRadius=name.equals("big_moth")?1:.5F;
        }
    }
    @SubscribeEvent public static void dimension(RegisterDimensionSpecialEffectsEvent event) { event.register(ArcanumContent.id("astral_sea"),new AstralSeaSky()); }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(ArcanumContent.BOOK.get(),io.redspace.ironsspellbooks.render.SpellBookCurioRenderer::new));
    }
    private ArcanumContentClient() {}
}
