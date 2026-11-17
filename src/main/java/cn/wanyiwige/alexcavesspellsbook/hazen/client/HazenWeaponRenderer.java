package cn.wanyiwige.alexcavesspellsbook.hazen.client;

import cn.wanyiwige.alexcavesspellsbook.hazen.HazenWeaponItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import java.util.function.Consumer;

/** Exact upstream geometry, texture, glow mask and item-display transforms. */
public final class HazenWeaponRenderer extends GeoItemRenderer<HazenWeaponItem> {
    private HazenWeaponRenderer(HazenWeaponItem item) {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(HazenWeaponItem object) { return new ResourceLocation(object.spec.model); }
            @Override public ResourceLocation getTextureResource(HazenWeaponItem object) { return new ResourceLocation(object.spec.texture); }
            @Override public ResourceLocation getAnimationResource(HazenWeaponItem object) {
                return new ResourceLocation(object.spec.animation.isEmpty() ? "irons_spellbooks:animations/wizard_armor_animation.json" : object.spec.animation);
            }
        });
        if (item.spec.glowing) addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    public static void register(HazenWeaponItem item, Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HazenWeaponRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new HazenWeaponRenderer(item);
                return renderer;
            }
        });
    }
}
