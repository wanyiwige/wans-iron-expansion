package cn.wanyiwige.alexcavesspellsbook.candy.client;

import cn.wanyiwige.alexcavesspellsbook.candy.CandyWizardArmorItem;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Standard armor bones plus Iron's legging-torso attachment for the sash and charms. */
public final class CandyWizardArmorRenderer extends GenericCustomArmorRenderer<CandyWizardArmorItem> {
    public CandyWizardArmorRenderer(){super(new Model());}
    private static final class Model extends GeoModel<CandyWizardArmorItem> {
        private static ResourceLocation asset(String path){return ResourceLocation.fromNamespaceAndPath("alex_caves_spellsbook",path);}
        public ResourceLocation getModelResource(CandyWizardArmorItem item){return asset("geo/candy_wizard_armor.geo.json");}
        public ResourceLocation getTextureResource(CandyWizardArmorItem item){return asset("textures/models/armor/candy_wizard.png");}
        public ResourceLocation getAnimationResource(CandyWizardArmorItem item){return asset("animations/candy_wizard_armor.animation.json");}
    }
}
