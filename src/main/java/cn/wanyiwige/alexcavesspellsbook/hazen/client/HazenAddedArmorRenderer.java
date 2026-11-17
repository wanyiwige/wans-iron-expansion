package cn.wanyiwige.alexcavesspellsbook.hazen.client;

import cn.wanyiwige.alexcavesspellsbook.hazen.HazenAddedArmor;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HazenAddedArmorRenderer extends AddedArmorAttachmentRenderer<HazenAddedArmor.Armor> {
    public HazenAddedArmorRenderer(boolean glowing){super(new Model());if(glowing)addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));}
    private static final class Model extends GeoModel<HazenAddedArmor.Armor> {
        public ResourceLocation getModelResource(HazenAddedArmor.Armor a){return new ResourceLocation(a.spec.model());}
        public ResourceLocation getTextureResource(HazenAddedArmor.Armor a){return new ResourceLocation(a.spec.texture());}
        public ResourceLocation getAnimationResource(HazenAddedArmor.Armor a){return new ResourceLocation(a.spec.animation()==null?"alex_caves_spellsbook:animations/hazen_added/empty.json":a.spec.animation());}
    }
}
