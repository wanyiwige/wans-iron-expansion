package cn.wanyiwige.alexcavesspellsbook.toxic.client;

import cn.wanyiwige.alexcavesspellsbook.toxic.CriticalWardenArmorItem;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Native slot visibility, limb poses, and leg-following coat attachments. */
public final class CriticalWardenArmorRenderer extends GenericCustomArmorRenderer<CriticalWardenArmorItem> {
    public CriticalWardenArmorRenderer() { super(new Model()); }

    private static final class Model extends GeoModel<CriticalWardenArmorItem> {
        private static ResourceLocation asset(String path) { return ResourceLocation.fromNamespaceAndPath("alex_caves_spellsbook", path); }
        public ResourceLocation getModelResource(CriticalWardenArmorItem item) { return asset("geo/critical_warden_armor.geo.json"); }
        public ResourceLocation getTextureResource(CriticalWardenArmorItem item) { return asset("textures/models/armor/critical_warden.png"); }
        public ResourceLocation getAnimationResource(CriticalWardenArmorItem item) { return asset("animations/critical_warden_armor.animation.json"); }
    }
}
