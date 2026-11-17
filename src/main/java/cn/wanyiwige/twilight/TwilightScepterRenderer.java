package cn.wanyiwige.twilight;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class TwilightScepterRenderer extends GeoItemRenderer<TwilightEquipment.Weapon>{
    public TwilightScepterRenderer(){super(new GeoModel<>(){
        public ResourceLocation getModelResource(TwilightEquipment.Weapon item){return TwilightFoundation.id("geo/item/staffs/ultimate_scepter.geo.json");}
        public ResourceLocation getTextureResource(TwilightEquipment.Weapon item){return TwilightFoundation.id("textures/item/staffs/ultimate_scepter.png");}
        public ResourceLocation getAnimationResource(TwilightEquipment.Weapon item){return TwilightFoundation.id("animations/item/ultimate_scepter.animation.json");}
    });addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));}
}
