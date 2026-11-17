package cn.wanyiwige.alexcavesspellsbook.hazen.client;

import cn.wanyiwige.alexcavesspellsbook.hazen.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid="alex_caves_spellsbook",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HazenClient {
    private record Visual(String model,String[] textures,String animation,String animation_name,String[] glow_textures){}
    private static final Map<String,Visual> VISUALS;
    static {
        try(var reader=new InputStreamReader(HazenClient.class.getResourceAsStream("/hazen-spell-visuals.json"),StandardCharsets.UTF_8)){
            VISUALS=new Gson().fromJson(reader,new TypeToken<Map<String,Visual>>(){}.getType());
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        HazenEntities.BOLTS.forEach((id,type)->event.registerEntityRenderer(type.get(),context->new Renderer<HazenProjectile>(context,p->p.kind)));
        event.registerEntityRenderer(HazenEntities.SUMMON.get(),context->new Renderer<HazenSummonedTerraprisma>(context,p->"terraprisma"));
        HazenSecondaryRegistry.TYPES.forEach((kind,type)->event.registerEntityRenderer(type.get(),context->
                HazenSecondaryRegistry.VISUALS.get(kind).model()!=null||kind.equals("fiery_dagger_magic_projectile")
                ?new HazenSecondaryRenderer(context):new HazenSecondaryRenderer.NativeSprite(context,kind)));
    }
    @SubscribeEvent public static void particles(RegisterParticleProvidersEvent event){
        HazenParticles.ENTRIES.forEach((name,type)->event.registerSpriteSet(type.get(),sprites->
                (particle,level,x,y,z,dx,dy,dz)->new HazenParticle(level,x,y,z,dx,dy,dz,sprites,name)));
    }
    @SubscribeEvent public static void scrollModels(ModelEvent.RegisterAdditional event){
        for(String school:new String[]{"cosmic","radiance","shadow","hydro"})
            event.register(HazenEquipment.id("item/scroll_hazen_"+school));
    }
    private static final class Model<T extends Entity & GeoAnimatable> extends GeoModel<T>{
        private final Function<T,String> kind;
        private Model(Function<T,String> kind){this.kind=kind;}
        private Visual visual(T object){return VISUALS.get(kind.apply(object));}
        public ResourceLocation getModelResource(T object){return new ResourceLocation(visual(object).model);}
        public ResourceLocation getTextureResource(T object){var textures=visual(object).textures;return new ResourceLocation(textures[object.tickCount%textures.length]);}
        public ResourceLocation getAnimationResource(T object){return new ResourceLocation(visual(object).animation);}
        @Override public RenderType getRenderType(T object,ResourceLocation texture){
            return switch(kind.apply(object)){
                case "shooting_star","nights_edge_after_slash"->RenderType.eyes(texture);
                case "spectral_axe","vampire_knife"->RenderType.energySwirl(texture,0,0);
                default->RenderType.entityCutoutNoCull(texture);
            };
        }
    }
    private static final class Renderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T>{
        private Renderer(EntityRendererProvider.Context context,Function<T,String> kind){
            super(context,new Model<>(kind));shadowRadius=0;
            addRenderLayer(new GeoRenderLayer<>(this){
                @Override public void render(PoseStack pose,T entity,BakedGeoModel model,RenderType type,MultiBufferSource source,VertexConsumer buffer,float partial,int light,int overlay){
                    var glow=VISUALS.get(kind.apply(entity)).glow_textures;
                    if(glow==null||entity.isInvisible())return;
                    var glowType=RenderType.entityCutoutNoCull(new ResourceLocation(glow[entity.tickCount%glow.length]));
                    getRenderer().reRender(model,pose,source,entity,glowType,source.getBuffer(glowType),partial,0xF000F0,overlay,1,1,1,1);
                }
            });
        }
        @Override public void preRender(PoseStack pose,T entity,BakedGeoModel model,MultiBufferSource source,VertexConsumer buffer,boolean rerender,float partial,int light,int overlay,float r,float g,float b,float a){
            if(entity instanceof HazenProjectile){
                var motion=entity.getDeltaMovement();
                float yaw=(float)(90-Math.toDegrees(Math.atan2(motion.z,motion.x)));
                float pitch=(float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()));
                pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.mulPose(Axis.XP.rotationDegrees(pitch));
            }
            super.preRender(pose,entity,model,source,buffer,rerender,partial,light,overlay,r,g,b,a);
        }
    }
    private HazenClient(){}
}
