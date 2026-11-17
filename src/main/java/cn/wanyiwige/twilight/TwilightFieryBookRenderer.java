package cn.wanyiwige.twilight;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
public final class TwilightFieryBookRenderer extends GeoItemRenderer<TwilightEquipment.Book>{
    public TwilightFieryBookRenderer(){super(new GeoModel<>(){
        public ResourceLocation getModelResource(TwilightEquipment.Book item){return TwilightFoundation.id("geo/item/fiery_spell_book.geo.json");}
        public ResourceLocation getTextureResource(TwilightEquipment.Book item){return TwilightFoundation.id("textures/item/spell_book_models/fiery_spell_book.png");}
        public ResourceLocation getAnimationResource(TwilightEquipment.Book item){return TwilightFoundation.id("animations/item/empty.animation.json");}
    });addRenderLayer(new AutoGlowingGeoLayer<>(this));}
}
