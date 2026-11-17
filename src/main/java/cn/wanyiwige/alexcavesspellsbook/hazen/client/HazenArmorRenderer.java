package cn.wanyiwige.alexcavesspellsbook.hazen.client;

import cn.wanyiwige.alexcavesspellsbook.hazen.HazenArmorItem;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Original model bones and UVs, fitted by the native Iron's Spellbooks armor renderer. */
public final class HazenArmorRenderer extends GenericCustomArmorRenderer<HazenArmorItem> {
    public HazenArmorRenderer() { super(new Model()); }
    private static final class Model extends GeoModel<HazenArmorItem> {
        public ResourceLocation getModelResource(HazenArmorItem item) { return new ResourceLocation(item.spec.model()); }
        public ResourceLocation getTextureResource(HazenArmorItem item) { return new ResourceLocation(item.spec.texture()); }
        public ResourceLocation getAnimationResource(HazenArmorItem item) { return new ResourceLocation(item.spec.animation()); }
    }
}
