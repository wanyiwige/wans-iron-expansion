package cn.wanyiwige.illusion;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.authlib.GameProfile;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import java.util.*;

@Mod.EventBusSubscriber(modid="alex_caves_spellsbook",value=Dist.CLIENT)
public final class IllusionClient {
    private static final Minecraft MC=Minecraft.getInstance();
    private static final Map<UUID,IllusionNetwork.State> PENDING=new HashMap<>();
    private static final Map<UUID,LivingEntity> DISGUISES=new HashMap<>();
    private static final Map<UUID,CompoundTag> APPEARANCES=new HashMap<>();
    private static final Set<UUID> PUSHED=new HashSet<>();
    private static boolean replacement;
    private static RemotePlayer mirror;
    private static Object lastLevel;
    public static final KeyMapping MENU=new KeyMapping("key.illusionist_grimoire.open_disguise_gui",GLFW.GLFW_KEY_J,"key.categories.illusionist_grimoire");
    public static void receive(IllusionNetwork.State packet){if(packet.data()==null)return;if(MC.level==null)return;if(lastLevel!=MC.level){PENDING.clear();DISGUISES.clear();APPEARANCES.clear();lastLevel=MC.level;}var e=MC.level.getEntity(packet.entity());if(e==null||!e.getUUID().equals(packet.uuid())){if(PENDING.size()<1024)PENDING.put(packet.uuid(),packet);return;}apply(e,packet.data());}
    private static void apply(Entity e,CompoundTag data){e.getPersistentData().put("WanIllusion",data.copy());DISGUISES.remove(e.getUUID());APPEARANCES.remove(e.getUUID());e.refreshDimensions();}
    public static boolean has(LivingEntity entity,String effect){return entity.hasEffect(IllusionContent.effect(effect))||entity.getPersistentData().getCompound("WanIllusion").getCompound("Effects").getInt(effect)>0;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        if(lastLevel!=MC.level){PENDING.clear();DISGUISES.clear();APPEARANCES.clear();PUSHED.clear();mirror=null;lastLevel=MC.level;}
        if(MC.level==null||MC.player==null)return;
        for(var it=PENDING.entrySet().iterator();it.hasNext();){var p=it.next().getValue();var e=MC.level.getEntity(p.entity());if(e!=null&&e.getUUID().equals(p.uuid())){apply(e,p.data());it.remove();}}
        if(!MC.isPaused())for(Entity e:MC.level.entitiesForRendering())if(e instanceof LivingEntity living){
            var d=e.getPersistentData().getCompound("WanIllusion");var effects=d.getCompound("Effects");for(String key:effects.getAllKeys()){int time=effects.getInt(key);if(time>0){effects.putInt(key,time-1);if(time==1){if(key.equals("squish")){d.remove("Axis");d.remove("Amount");living.refreshDimensions();}if(key.equals("disguised"))DISGUISES.remove(e.getUUID());}}}
            var fake=DISGUISES.get(e.getUUID());if(fake!=null)fake.walkAnimation.update(living.walkAnimation.speed(),1);
        }
        if(!MC.isPaused())disguiseMovement();
        while(MENU.consumeClick())if(MC.player.getMainHandItem().is(IllusionContent.PICTURE_BOOK.get()))MC.setScreen(new SelectionScreen(MC.player.getMainHandItem()));
        if(has(MC.player,"paranoia")&&MC.player.tickCount%35==0){MC.level.playLocalSound(MC.player.getX()+(MC.level.random.nextDouble()-.5)*12,MC.player.getY(),MC.player.getZ()+(MC.level.random.nextDouble()-.5)*12,net.minecraft.sounds.SoundEvents.CREEPER_PRIMED,net.minecraft.sounds.SoundSource.AMBIENT,.4F,.7F,false);}
    }
    private static void disguiseMovement(){
        if(!has(MC.player,"disguised"))return;var disguise=appearance(MC.player);if(disguise==null)return;
        MobCategory category=disguise.getType().getCategory();boolean aquatic=category==MobCategory.WATER_CREATURE||category==MobCategory.WATER_AMBIENT||category==MobCategory.UNDERGROUND_WATER_CREATURE;
        if(aquatic&&MC.player.isInWater()){
            var forward=MC.player.getLookAngle();var right=new net.minecraft.world.phys.Vec3(0,1,0).cross(forward).normalize();var input=forward.scale(MC.player.input.forwardImpulse).add(right.scale(MC.player.input.leftImpulse));if(MC.player.input.jumping)input=input.add(0,1,0);if(MC.player.input.shiftKeyDown)input=input.add(0,-1,0);
            if(input.lengthSqr()>.0001)MC.player.setDeltaMovement(input.normalize().scale(disguise.getType()==EntityType.DOLPHIN?.4:.25));
        }
        if(disguise instanceof net.minecraft.world.entity.GlowSquid)MC.level.addParticle(net.minecraft.core.particles.ParticleTypes.GLOW,MC.player.getRandomX(.6),MC.player.getRandomY(),MC.player.getRandomZ(.6),0,0,0);
        if(disguise instanceof net.minecraft.world.entity.monster.Blaze)MC.level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,MC.player.getRandomX(.5),MC.player.getRandomY(),MC.player.getRandomZ(.5),0,0,0);
    }
    private static LivingEntity appearance(LivingEntity original){
        CompoundTag tag=IllusionState.data(original).getCompound("Disguise");if(tag.isEmpty()||MC.level==null)return null;
        LivingEntity cached=DISGUISES.get(original.getUUID());if(cached!=null&&tag.equals(APPEARANCES.get(original.getUUID())))return cached;
        Entity created;
        if(tag.contains("Profile",10)){GameProfile profile=NbtUtils.readGameProfile(tag.getCompound("Profile"));if(profile==null)return null;created=new DisguisedPlayer(profile);}
        else{var type=ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(tag.getString("Type")));if(type==null)return null;created=type.create(MC.level);if(created instanceof LivingEntity l)l.load(tag.getCompound("Appearance"));}
        if(!(created instanceof LivingEntity living))return null;DISGUISES.put(original.getUUID(),living);APPEARANCES.put(original.getUUID(),tag.copy());return living;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void render(RenderLivingEvent.Pre<?,?> event){
        if(replacement||event.isCanceled()||MC.player==null)return;LivingEntity e=event.getEntity();
        if(has(MC.player,"emptiness")&&e!=MC.player){event.setCanceled(true);return;}
        if(e!=MC.player&&!IllusionState.mobImmune(MC.player)&&hiddenByWall(e,event.getPartialTick())){event.setCanceled(true);return;}
        if(has(e,"disguised")){
            LivingEntity mob=appearance(e);
            if(mob!=null){
                mob.setPos(e.position());mob.xo=e.xo;mob.yo=e.yo;mob.zo=e.zo;mob.setYRot(e.getYRot());mob.yRotO=e.yRotO;mob.setXRot(e.getXRot());mob.xRotO=e.xRotO;mob.yBodyRot=e.yBodyRot;mob.yBodyRotO=e.yBodyRotO;mob.yHeadRot=e.yHeadRot;mob.yHeadRotO=e.yHeadRotO;mob.tickCount=e.tickCount;mob.walkAnimation.setSpeed(e.walkAnimation.speed());mob.setPose(e.getPose());mob.hurtTime=e.hurtTime;
                var stack=event.getPoseStack();stack.pushPose();replacement=true;
                try{applyVisual(e,stack,event.getPartialTick());renderReplacement(mob,event);}finally{replacement=false;stack.popPose();}
                event.setCanceled(true);return;
            }
        }
        event.getPoseStack().pushPose();PUSHED.add(e.getUUID());applyVisual(e,event.getPoseStack(),event.getPartialTick());
    }
    private static boolean hiddenByWall(LivingEntity target,float partial){
        var start=MC.player.getEyePosition(partial);var end=target.getPosition(partial).add(0,target.getBbHeight()*.5,0);var delta=end.subtract(start);double distance=delta.length();if(distance>64)return false;
        for(double d=0;d<distance;d+=.25){var pos=net.minecraft.core.BlockPos.containing(start.add(delta.normalize().scale(d)));if(MC.level.getBlockEntity(pos) instanceof IllusionBlockEntity b&&b.getBlockState().is(IllusionContent.WALL.get())&&!MC.player.getUUID().equals(b.owner)&&end.distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=5){double resist=1-IllusionState.attribute(MC.player,io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_RESIST.get());if(!(resist>Math.max(0,b.ownerPower-1)/1.5))return true;}}
        return false;
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static void renderReplacement(LivingEntity mob,RenderLivingEvent.Pre<?,?> event){EntityRenderer renderer=MC.getEntityRenderDispatcher().getRenderer(mob);renderer.render(mob,mob.getYRot(),event.getPartialTick(),event.getPoseStack(),event.getMultiBufferSource(),event.getPackedLight());}
    private static void applyVisual(LivingEntity e,PoseStack pose,float partial){
        if(has(e,"squish")){float[] scales=IllusionState.scales(e);pose.scale(scales[0],scales[1],scales[2]);}
        if(MC.player!=null&&has(MC.player,"depth_trick")&&e!=MC.player){var delta=e.getPosition(partial).subtract(MC.player.getEyePosition(partial)).normalize().scale(-7);pose.translate(delta.x,delta.y,delta.z);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void post(RenderLivingEvent.Post<?,?> event){if(PUSHED.remove(event.getEntity().getUUID()))event.getPoseStack().popPose();}
    @SubscribeEvent public static void name(RenderNameTagEvent event){if(event.getEntity() instanceof LivingEntity e&&has(e,"disguised")){var tag=IllusionState.data(e).getCompound("Disguise");event.setContent(tag.contains("Profile")?Component.literal(tag.getString("Name")):Component.empty());}}
    @SubscribeEvent public static void mirror(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||MC.player==null||MC.level==null||!has(MC.player,"paranoia")||has(MC.player,"emptiness"))return;
        if(mirror==null||mirror.level()!=MC.level)mirror=new DisguisedPlayer(MC.player.getGameProfile());
        var player=MC.player;double distance=1+Math.floorMod(player.tickCount/100*37+player.getId(),51)/10.0;var look=player.getLookAngle();var pos=player.getPosition(event.getPartialTick()).add(look.x*distance,0,look.z*distance);var camera=event.getCamera().getPosition();
        float yaw=(float)(Math.toDegrees(Math.atan2(player.getZ()-pos.z,player.getX()-pos.x))-90);mirror.setYRot(yaw);mirror.yRotO=yaw;mirror.yBodyRot=yaw;mirror.yBodyRotO=yaw;mirror.yHeadRot=yaw;mirror.yHeadRotO=yaw;mirror.tickCount=player.tickCount;mirror.setPos(pos);mirror.setItemSlot(EquipmentSlot.MAINHAND,player.getMainHandItem().copy());
        var pose=event.getPoseStack();pose.pushPose();pose.translate(pos.x-camera.x,pos.y-camera.y,pos.z-camera.z);replacement=true;
        try{MC.getEntityRenderDispatcher().getRenderer(mirror).render(mirror,yaw,event.getPartialTick(),pose,MC.renderBuffers().bufferSource(),MC.getEntityRenderDispatcher().getPackedLightCoords(player,event.getPartialTick()));}finally{replacement=false;pose.popPose();}
    }
    @SubscribeEvent public static void overlay(RenderGuiEvent.Post event){
        if(MC.player==null)return;GuiGraphics g=event.getGuiGraphics();int w=g.guiWidth(),h=g.guiHeight();
        if(has(MC.player,"feared")){var fx=MC.player.getEffect(IllusionContent.effect("feared"));int duration=fx==null?IllusionState.data(MC.player).getCompound("Effects").getInt("feared"):fx.getDuration();if(duration<=90){int rw=(int)(h*229F/300);g.blit(IllusionContent.id("textures/gui/job_application_screen.png"),(w-rw)/2,0,rw,h,0,0,229,300,229,300);}}
        if(has(MC.player,"paranoia")){
            float alpha=.5F+.05F*(float)Math.sin(MC.player.tickCount*.1F);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();g.setColor(.35F,.5F,.53F,alpha);
            g.blit(new ResourceLocation("minecraft","textures/misc/vignette.png"),0,0,0,0,w,h,w,h);g.setColor(1,1,1,1);RenderSystem.disableBlend();
        }
    }
    @Mod.EventBusSubscriber(modid="alex_caves_spellsbook",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(MENU);}
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
            e.registerEntityRenderer(IllusionContent.FEAR_BOLT.get(),EmptyRenderer::new);e.registerEntityRenderer(IllusionContent.TRAP_CASTER.get(),EmptyRenderer::new);
            e.registerBlockEntityRenderer(IllusionContent.BLOCK_ENTITY.get(),TrapRenderer::new);
        }
    }
    private static final class EmptyRenderer<T extends Entity> extends EntityRenderer<T>{EmptyRenderer(EntityRendererProvider.Context c){super(c);}public ResourceLocation getTextureLocation(T e){return IllusionContent.id("textures/item/scroll_illusion.png");}}
    private static final class DisguisedPlayer extends RemotePlayer {
        private ResourceLocation skin;private String model="default";
        DisguisedPlayer(GameProfile profile){super(MC.level,profile);skin=net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(profile.getId());MC.getInstance().getSkinManager().registerSkins(profile,(type,location,texture)->{if(type==com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN){skin=location;model="slim".equals(texture.getMetadata("model"))?"slim":"default";}},false);}
        @Override public ResourceLocation getSkinTextureLocation(){return skin;}
        @Override public String getModelName(){return model;}
        @Override public boolean isSkinLoaded(){return true;}
    }
    public static final class TrapRenderer implements BlockEntityRenderer<IllusionBlockEntity>{
        public TrapRenderer(BlockEntityRendererProvider.Context c){}
        @Override public void render(IllusionBlockEntity b,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(!b.getBlockState().is(IllusionContent.TRAP.get())||MC.player==null)return;
            boolean visible=MC.player.getUUID().equals(b.owner)||!IllusionState.immune(MC.player,MC.player);if(!visible)return;
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(b.getBlockState().getValue(IllusionBlock.FACING).getRotation());pose.translate(0,-.495,0);pose.mulPose(Axis.YP.rotationDegrees((b.getLevel().getGameTime()+partial)*2));
            VertexConsumer v=buffers.getBuffer(RenderType.entityTranslucentEmissive(IllusionContent.id("textures/block/magic_circle.png")));var m=pose.last();float alpha=b.cooldown>0?.35F:.8F;
            v.vertex(m.pose(),-.48F,0,-.48F).color(1F,1F,1F,alpha).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(m.normal(),0,1,0).endVertex();
            v.vertex(m.pose(),-.48F,0,.48F).color(1F,1F,1F,alpha).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(m.normal(),0,1,0).endVertex();
            v.vertex(m.pose(),.48F,0,.48F).color(1F,1F,1F,alpha).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(m.normal(),0,1,0).endVertex();
            v.vertex(m.pose(),.48F,0,-.48F).color(1F,1F,1F,alpha).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(m.normal(),0,1,0).endVertex();pose.popPose();
        }
    }
    private static final class SelectionScreen extends Screen {
        private final ItemStack book;SelectionScreen(ItemStack book){super(Component.translatable("gui.illusionist_grimoire.disguise_selection"));this.book=book;}
        @Override protected void init(){for(int i=0;i<8;i++){final int slot=i;var data=PictureBook.slot(book,i);String label=(i+1)+": "+(data.isEmpty()?"—":data.getString("Name"));addRenderableWidget(Button.builder(Component.literal(label),b->{book.getOrCreateTag().putInt("Selected",slot);IllusionNetwork.CHANNEL.sendToServer(new IllusionNetwork.Select(slot,false));onClose();}).bounds(width/2-150+(i%2)*155,height/2-70+(i/2)*30,150,24).build());}}
        @Override public boolean mouseClicked(double x,double y,int button){if(button==1){for(int i=0;i<8;i++){int bx=width/2-150+(i%2)*155,by=height/2-70+(i/2)*30;if(x>=bx&&x<bx+150&&y>=by&&y<by+24){PictureBook.clear(book,i);IllusionNetwork.CHANNEL.sendToServer(new IllusionNetwork.Select(i,true));rebuildWidgets();return true;}}}return super.mouseClicked(x,y,button);}
        @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);g.drawCenteredString(font,title,width/2,height/2-105,0xffffff);g.drawCenteredString(font,Component.translatable("gui.illusionist_grimoire.disguise_selection.instructions"),width/2,height/2+65,0xcccccc);super.render(g,x,y,partial);}
        @Override public boolean isPauseScreen(){return false;}
    }
}
