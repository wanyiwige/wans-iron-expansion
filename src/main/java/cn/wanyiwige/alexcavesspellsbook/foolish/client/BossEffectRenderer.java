package cn.wanyiwige.alexcavesspellsbook.foolish.client;

import cn.wanyiwige.alexcavesspellsbook.foolish.BossEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Original Foolish item renderers for bolts, original Gecko models for animated attacks. */
public final class BossEffectRenderer extends EntityRenderer<BossEffect> {
    private final ItemRenderer items;
    private final GeoEntityRenderer<BossEffect> geo;
    public static ResourceLocation asset(String path){return ResourceLocation.fromNamespaceAndPath("foolish",path);}
    public static String model(BossEffect.Kind kind){return switch(kind){case MINE->"stormmine";case SEED->"seedbomb";
        case METEOR->"meteor";case BLADE,SLASH->"newstarblade";case STARBOMB->"starbomb";default->null;};}
    public static String texture(BossEffect.Kind kind){return switch(kind){case MINE,STARBOMB->"blanker";default->model(kind);};}
    public BossEffectRenderer(EntityRendererProvider.Context context){
        super(context);items=context.getItemRenderer();
        geo=new GeoEntityRenderer<>(context,new GeoModel<BossEffect>(){
            @Override public ResourceLocation getModelResource(BossEffect entity){return asset("geo/"+model(entity.kind())+".geo.json");}
            @Override public ResourceLocation getTextureResource(BossEffect entity){return asset("textures/entities/"+texture(entity.kind())+".png");}
            @Override public ResourceLocation getAnimationResource(BossEffect entity){return asset("animations/"+model(entity.kind())+".animation.json");}
        }){
            @Override protected void applyRotations(BossEffect e,PoseStack pose,float age,float yaw,float partial){
                if(e.kind()==BossEffect.Kind.BLADE||e.kind()==BossEffect.Kind.SLASH){
                    pose.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
                    pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial,e.xRotO,e.getXRot())));
                }
            }
            @Override public RenderType getRenderType(BossEffect e,ResourceLocation tex,MultiBufferSource buffer,float partial){return RenderType.entityTranslucent(tex);}
        };
    }
    @Override public void render(BossEffect e,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
        if(model(e.kind())!=null){geo.render(e,yaw,partial,pose,buffer,light);return;}
        if(e.kind()==BossEffect.Kind.SMALL_FIREBALL){
            renderItem(e,new ItemStack(Items.FIRE_CHARGE),pose,buffer,light);return;
        }
        String item=switch(e.kind()){case FIREBALL,THUNDER->"incinerator_fire_ball";case RADIUM->"radium_charge_item";
            case GAS_BOLT->"corrosive_gas_charge_base_item";case POLLEN->"pollen_burst";case CAPSULE->"scarlant_capsule";
            case SHOCK->"shock_charge_launcher";default->null;};
        if(item==null)return;
        Item visual=ForgeRegistries.ITEMS.getValue(asset(item));if(visual==null)return;
        renderItem(e,new ItemStack(visual),pose,buffer,light);
    }
    private void renderItem(BossEffect e,ItemStack stack,PoseStack pose,MultiBufferSource buffer,int light){
        pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.mulPose(Axis.YP.rotationDegrees(180));
        items.renderStatic(stack,ItemDisplayContext.GROUND,light,OverlayTexture.NO_OVERLAY,
                pose,buffer,e.level(),e.getId());pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(BossEffect e){return TextureAtlas.LOCATION_BLOCKS;}
}
