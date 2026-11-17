package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class TwilightArmorRenderer extends GenericCustomArmorRenderer<TwilightEquipment.Armor> {
    public TwilightArmorRenderer(boolean glowing){super(new Model());if(glowing)addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));}
    private static final class Model extends GeoModel<TwilightEquipment.Armor>{
        public ResourceLocation getModelResource(TwilightEquipment.Armor item){return item.set.startsWith("tarnished")?new ResourceLocation("irons_spellbooks","geo/tarnished_armor.geo.json"):TwilightFoundation.id("geo/armor/"+item.set+"_armor.geo.json");}
        public ResourceLocation getTextureResource(TwilightEquipment.Armor item){return TwilightFoundation.id("textures/armor/"+item.set+(item.set.startsWith("tarnished")?"":"_armor")+".png");}
        public ResourceLocation getAnimationResource(TwilightEquipment.Armor item){return new ResourceLocation("irons_spellbooks","animations/wizard_armor_animation.json");}
    }
}
