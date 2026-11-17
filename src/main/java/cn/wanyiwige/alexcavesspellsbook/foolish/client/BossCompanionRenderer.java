package cn.wanyiwige.alexcavesspellsbook.foolish.client;

import cn.wanyiwige.alexcavesspellsbook.foolish.BossCompanion;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BossCompanionRenderer extends GeoEntityRenderer<BossCompanion>{
    public static String model(BossCompanion.Kind k){return switch(k){case BUG->"scarlant";case ASSISTANT->"scarlantassistant";
        case SENTINEL->"scarlantsentinel";case LEAF->"terrorfloraleaf";case VINE->"newterrorfloravine";};}
    public static String texture(BossCompanion.Kind k){return switch(k){case BUG->"ants_ants_were_the_ants";
        case ASSISTANT->"scarlantassistant";case SENTINEL->"antguard";case LEAF->"leaf";case VINE->"vine";};}
    public BossCompanionRenderer(EntityRendererProvider.Context context){super(context,new GeoModel<BossCompanion>(){
        @Override public ResourceLocation getModelResource(BossCompanion e){return BossEffectRenderer.asset("geo/"+model(e.kind())+".geo.json");}
        @Override public ResourceLocation getTextureResource(BossCompanion e){return BossEffectRenderer.asset("textures/entities/"+texture(e.kind())+".png");}
        @Override public ResourceLocation getAnimationResource(BossCompanion e){return BossEffectRenderer.asset("animations/"+model(e.kind())+".animation.json");}
    });shadowRadius=0.4F;}
}
