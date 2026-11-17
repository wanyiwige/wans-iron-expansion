package cn.wanyiwige.astralcompat.client.item;

import cn.wanyiwige.astralcompat.AstralSpellCompat;
import cn.wanyiwige.astralcompat.item.AstralLanceItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AstralLanceModel extends GeoModel<AstralLanceItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(AstralSpellCompat.MOD_ID, "geo/item/astral_lance.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(AstralSpellCompat.MOD_ID, "textures/item/astral_lance.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(AstralSpellCompat.MOD_ID, "animations/item/astral_lance.animation.json");

    @Override
    public ResourceLocation getModelResource(AstralLanceItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AstralLanceItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AstralLanceItem animatable) {
        return ANIMATION;
    }
}
