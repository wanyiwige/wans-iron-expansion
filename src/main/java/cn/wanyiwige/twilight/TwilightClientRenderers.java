package cn.wanyiwige.twilight;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class TwilightClientRenderers {
    static void quad(PoseStack stack,VertexConsumer vertices,float width,float height){
        Matrix4f m=stack.last().pose();vertex(vertices,m,-width,-height,0,0,1);vertex(vertices,m,width,-height,0,1,1);vertex(vertices,m,width,height,0,1,0);vertex(vertices,m,-width,height,0,0,0);
    }
    private static void vertex(VertexConsumer v,Matrix4f m,float x,float y,float z,float u,float t){v.vertex(m,x,y,z).color(255,255,255,255).uv(u,t).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(0,0,1).endVertex();}
    public static final class FireballRenderer extends io.redspace.ironsspellbooks.entity.spells.fireball.FireballRenderer{
        private final boolean hydra;
        public FireballRenderer(EntityRendererProvider.Context context,boolean hydra){super(context,hydra?1.15F:1.25F);this.hydra=hydra;}
        @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.projectile.Projectile entity){return TwilightFoundation.id("textures/entity/"+(hydra?"hydra_fireball/fireball_core.png":"exanimated_fireball/comet.png"));}
        @Override public ResourceLocation getFireTextureLocation(net.minecraft.world.entity.projectile.Projectile entity){return TwilightFoundation.id("textures/entity/"+(hydra?"hydra_fireball":"exanimated_fireball")+"/fire_"+(entity.tickCount/2%4)+".png");}
    }
    public static final class ProjectileRenderer extends EntityRenderer<TwilightProjectile>{
        private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;
        public ProjectileRenderer(EntityRendererProvider.Context context){super(context);blocks=context.getBlockRenderDispatcher();}
        @Override public void render(TwilightProjectile entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light){
            stack.pushPose();
            if(entity.kind().equals("ice_chunk")||entity.kind().equals("extended_ice_bomb")){
                float scale=entity.kind().equals("ice_chunk")?1.5F:.5F;stack.scale(scale,scale,scale);stack.translate(-.5,-.5,-.5);
                blocks.renderSingleBlock(Blocks.PACKED_ICE.defaultBlockState(),stack,buffers,light,OverlayTexture.NO_OVERLAY);
            }else if(entity.kind().equals("ice_arrow")){
                Vec3 motion=entity.getDeltaMovement();if(motion.lengthSqr()<1E-6)motion=Vec3.directionFromRotation(entity.getXRot(),entity.getYRot());
                stack.mulPose(Axis.YP.rotationDegrees((float)-Math.toDegrees(Math.atan2(motion.z,motion.x))));stack.mulPose(Axis.ZP.rotationDegrees((float)Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()))));stack.scale(.13F,.13F,.13F);stack.translate(-2,0,0);
                VertexConsumer v=buffers.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity)));
                for(int side=0;side<4;side++){stack.mulPose(Axis.XP.rotationDegrees(90));Matrix4f m=stack.last().pose();vertex(v,m,-8,-2,0,0,0);vertex(v,m,8,-2,0,.5F,0);vertex(v,m,8,2,0,.5F,.15625F);vertex(v,m,-8,2,0,0,.15625F);}
            }else{
                stack.mulPose(entityRenderDispatcher.cameraOrientation());
                quad(stack,buffers.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity))),entity.kind().equals("ice_arrow")?.3F:.8F,entity.kind().equals("ice_arrow")?.3F:.8F);
            }
            stack.popPose();super.render(entity,yaw,partial,stack,buffers,light);
        }
        public ResourceLocation getTextureLocation(TwilightProjectile entity){return TwilightFoundation.id(entity.kind().equals("ice_arrow")?"textures/entity/ice_arrow.png":"textures/entity/"+(entity.kind().equals("hydra_fireball")?"hydra_fireball":"exanimated_fireball")+"/fire_"+(entity.tickCount/2%8)+".png");}
    }
    public static final class TridentRenderer extends GeoEntityRenderer<TwilightProjectile>{
        public TridentRenderer(EntityRendererProvider.Context context){super(context,new GeoModel<>(){
            public ResourceLocation getModelResource(TwilightProjectile entity){return TwilightFoundation.id("geo/entity/spells/exanimated_trident.geo.json");}
            public ResourceLocation getTextureResource(TwilightProjectile entity){return TwilightFoundation.id("textures/entity/exanimated_trident.png");}
            public ResourceLocation getAnimationResource(TwilightProjectile entity){return TwilightFoundation.id("animations/entity/spells/exanimated_trident.animation.json");}
        });}
        @Override public void render(TwilightProjectile entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light){
            Vec3 motion=entity.getDeltaMovement();if(motion.lengthSqr()<1E-6)motion=Vec3.directionFromRotation(entity.getXRot(),entity.getYRot());
            stack.pushPose();stack.mulPose(Axis.YP.rotationDegrees((float)-Math.toDegrees(Math.atan2(motion.z,motion.x))+270));stack.mulPose(Axis.XP.rotationDegrees((float)-Math.toDegrees(Math.atan2(motion.y,motion.horizontalDistance()))));super.render(entity,yaw,partial,stack,buffers,light);stack.popPose();
        }
    }
    public static final class AreaRenderer extends EntityRenderer<TwilightAreaEntity>{
        public AreaRenderer(EntityRendererProvider.Context context){super(context);}
        @Override public boolean shouldRender(TwilightAreaEntity entity,net.minecraft.client.renderer.culling.Frustum camera,double x,double y,double z){return entity.kind().equals("exanimated_ray")||super.shouldRender(entity,camera,x,y,z);}
        @Override public void render(TwilightAreaEntity entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light){
            if(entity.kind().equals("exanimated_ray")){
                Vec3 direction=entity.beamEnd().subtract(entity.position());float length=(float)direction.length();
                stack.pushPose();stack.mulPose(Axis.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(direction.x,direction.z))));stack.mulPose(Axis.XP.rotationDegrees((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()))));
                Matrix4f m=stack.last().pose();VertexConsumer v=buffers.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity)));
                for(int side=0;side<2;side++){
                    vertex(v,m,-.12F,0,0,0,0);vertex(v,m,.12F,0,0,1,0);vertex(v,m,.12F,0,length,1,1);vertex(v,m,-.12F,0,length,0,1);
                    stack.mulPose(Axis.ZP.rotationDegrees(90));m=stack.last().pose();
                }
                stack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount+partial)*5+45));float expansion=Math.max(.05F,1.2F*(1-(entity.tickCount+partial)/12F));stack.scale(expansion,expansion,1);
                VertexConsumer overlay=buffers.getBuffer(RenderType.energySwirl(TwilightFoundation.id("textures/entity/exanimated_ray/overlay.png"),0,0));
                for(int side=0;side<2;side++){m=stack.last().pose();vertex(overlay,m,-.2F,0,0,0,0);vertex(overlay,m,.2F,0,0,1,0);vertex(overlay,m,.2F,0,length,1,1);vertex(overlay,m,-.2F,0,length,0,1);stack.mulPose(Axis.ZP.rotationDegrees(90));}
                stack.popPose();
            }else if(entity.kind().equals("ice_bomb_zone")){
                stack.pushPose();stack.scale(.5F,.5F,.5F);stack.translate(-.5,-.5,-.5);net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.PACKED_ICE.defaultBlockState(),stack,buffers,light,OverlayTexture.NO_OVERLAY);stack.popPose();
            }
            super.render(entity,yaw,partial,stack,buffers,light);
        }
        public ResourceLocation getTextureLocation(TwilightAreaEntity entity){return TwilightFoundation.id("textures/entity/exanimated_ray/core.png");}
    }
    public static final class SoulRenderer extends EntityRenderer<TwilightSoul>{
        private final net.minecraft.client.model.geom.ModelPart body,crown;
        public SoulRenderer(EntityRendererProvider.Context context){super(context);
            var orbMesh=new net.minecraft.client.model.geom.builders.MeshDefinition();orbMesh.getRoot().addOrReplaceChild("body",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-4,8,8,8),net.minecraft.client.model.geom.PartPose.ZERO);body=net.minecraft.client.model.geom.builders.LayerDefinition.create(orbMesh,48,24).bakeRoot().getChild("body");
            var crownMesh=new net.minecraft.client.model.geom.builders.MeshDefinition();crownMesh.getRoot().addOrReplaceChild("crown",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,0).addBox(-5,-5,-5,10,10,10),net.minecraft.client.model.geom.PartPose.ZERO);crown=net.minecraft.client.model.geom.builders.LayerDefinition.create(crownMesh,48,48).bakeRoot().getChild("crown");
        }
        @Override public void render(TwilightSoul entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light){
            float time=entity.tickCount+partial;
            if(entity.kind().equals("lich_soul")||entity.kind().equals("snow_queen_soul")){
                stack.pushPose();stack.translate(0,.75,0);stack.scale(1.5F,-1.5F,1.5F);stack.mulPose(Axis.YP.rotationDegrees(time));stack.mulPose(Axis.XP.rotationDegrees((float)Math.sin(time/100)*6));crown.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),light,OverlayTexture.NO_OVERLAY);stack.popPose();
            }
            int color=switch(entity.kind()){case "snow_queen_soul"->0x1e1f8c;case "urghast_soul"->0xdd0000;case "hydra_soul"->0x7c002c;default->0x401e5b;};
            for(int i=0;i<3;i++){
                float pulse=(float)(.5+.08*Math.sin(time*.1+entity.getId()));float scale=(i==0?.25F:i==1?1F:1.5F)+pulse;
                stack.pushPose();stack.translate(0,.25+Math.sin(time*.05)*.08,0);stack.scale(.75F*scale,.75F*scale,.75F*scale);stack.mulPose(Axis.YP.rotationDegrees(time*(i%2==0?2:-2)));
                body.render(stack,buffers.getBuffer(RenderType.lightning()),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,i==0?1:((color>>16)&255)/255F,i==0?1:((color>>8)&255)/255F,i==0?1:(color&255)/255F,i==2?.8F:1F);stack.popPose();
            }super.render(entity,yaw,partial,stack,buffers,light);
        }
        public ResourceLocation getTextureLocation(TwilightSoul entity){return TwilightFoundation.id(entity.kind().equals("snow_queen_soul")?"textures/entity/snow_crown.png":"textures/entity/lich_crown.png");}
    }
    public static final class MinotaurRenderer extends HumanoidMobRenderer<twilightforest.entity.monster.Minotaur,twilightforest.client.model.entity.MinotaurModel>{
        public MinotaurRenderer(EntityRendererProvider.Context context){super(context,new twilightforest.client.model.entity.MinotaurModel(context.bakeLayer(twilightforest.client.model.TFModelLayers.MINOTAUR)),.5F);}
        public ResourceLocation getTextureLocation(twilightforest.entity.monster.Minotaur entity){return new ResourceLocation("twilightforest","textures/model/minotaur.png");}
    }
    public static final class CasterRenderer extends AbstractSpellCastingMobRenderer{
        public CasterRenderer(EntityRendererProvider.Context context,boolean loyal){super(context,new AbstractSpellCastingMobModel(){
            public ResourceLocation getModelResource(AbstractSpellCastingMob entity){return new ResourceLocation("irons_spellbooks",loyal?"geo/abstract_casting_mob.geo.json":"geo/skeleton_mob.geo.json");}
            public ResourceLocation getTextureResource(AbstractSpellCastingMob entity){return TwilightFoundation.id("textures/entity/"+(loyal?"advanced_loyal_zombie":entity instanceof TwilightCaster caster&&caster.boss()?"ominous_lich":"advanced_druid")+".png");}
        });}
    }
    private TwilightClientRenderers(){}
}
