package cn.wanyiwige.illusion;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public final class IllusionEvents {
    @SubscribeEvent public static void illusionerDrop(LivingDropsEvent event){
        LivingEntity entity=event.getEntity();
        if(entity.level().isClientSide||entity.getType()!=EntityType.ILLUSIONER||!(event.getSource().getEntity() instanceof Player)||entity.getPersistentData().hasUUID(OWNER_KEY))return;
        event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(entity.level(),entity.getX(),entity.getY()+.25,entity.getZ(),IllusionContent.MIRAGE_PRISM.get().getDefaultInstance()));
    }
    public static final ThreadLocal<java.util.UUID> TRAP_OWNER=new ThreadLocal<>();
    public static final String OWNER_KEY="WanTrapOwner";
    @SubscribeEvent public static void join(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide)return;Entity e=event.getEntity();java.util.UUID owner=TRAP_OWNER.get();
        if(owner==null&&e instanceof net.minecraft.world.entity.projectile.Projectile p){Entity source=p.getOwner();if(source!=null&&source.getPersistentData().hasUUID(OWNER_KEY))owner=source.getPersistentData().getUUID(OWNER_KEY);}
        if(owner!=null)e.getPersistentData().putUUID(OWNER_KEY,owner);
    }
    public static boolean trapAlly(Entity source,Entity target){
        if(source==null||target==null||!source.getPersistentData().hasUUID(OWNER_KEY))return false;var id=source.getPersistentData().getUUID(OWNER_KEY);
        if(id.equals(target.getUUID())||target instanceof OwnableEntity pet&&id.equals(pet.getOwnerUUID()))return true;
        if(source.level() instanceof net.minecraft.server.level.ServerLevel server){var owner=server.getServer().getPlayerList().getPlayer(id);return owner!=null&&(owner.isAlliedTo(target)||target.isAlliedTo(owner));}return false;
    }
    @SubscribeEvent public static void attack(LivingAttackEvent event){if(trapAlly(event.getSource().getDirectEntity(),event.getEntity())||trapAlly(event.getSource().getEntity(),event.getEntity()))event.setCanceled(true);}
    @SubscribeEvent public static void size(net.minecraftforge.event.entity.EntityEvent.Size event){
        if(!(event.getEntity() instanceof LivingEntity e))return;
        if(e instanceof TrapCaster){event.setNewEyeHeight(0);return;}
        // Forge also fires Size from Entity's constructor, before LivingEntity's effect map exists.
        if(!e.getPersistentData().contains("WanIllusion",10))return;
        var d=IllusionState.data(e);if(!d.contains("Axis"))return;
        boolean active=e.hasEffect(IllusionContent.effect("squish"))||e.level().isClientSide&&d.getCompound("Effects").getInt("squish")>0;
        if(!active)return;float[] scales=IllusionState.scales(e);var old=event.getNewSize();int axis=d.contains("Axis")?d.getInt("Axis"):-1;
        if(axis<0)return;event.setNewSize(EntityDimensions.scalable(Math.max(.125F,old.width*(axis==1?1:Math.min(scales[0],scales[2]))),Math.max(.125F,old.height*(axis==1?scales[1]:1))));event.setNewEyeHeight(Math.max(.2F,event.getNewEyeHeight()*(axis==1?scales[1]:1)));
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){
        LivingEntity e=event.getEntity();if(e.level().isClientSide)return;
        int mask=IllusionState.mask(e);var d=e.getPersistentData().contains("WanIllusion",10)?IllusionState.data(e):mask==0?null:IllusionState.data(e);
        if(d!=null&&mask!=d.getInt("LastMask")){
            d.putInt("LastMask",mask);
            if(!e.hasEffect(IllusionContent.effect("squish"))){d.remove("Axis");d.remove("Amount");e.refreshDimensions();}
            if(!e.hasEffect(IllusionContent.effect("disguised")))d.remove("Disguise");
            IllusionState.sync(e);
        }
        if(e instanceof Mob mob){
            if(e.hasEffect(IllusionContent.effect("emptiness"))){mob.setTarget(null);mob.getNavigation().stop();mob.setDeltaMovement(0,mob.getDeltaMovement().y,0);}
            else if(e.hasEffect(IllusionContent.effect("paranoia"))&&e.tickCount%20==0){mob.setTarget(null);mob.getNavigation().stop();Vec3 p=e.position().add((e.getRandom().nextDouble()-.5)*16,0,(e.getRandom().nextDouble()-.5)*16);if(e.getLastHurtByMob()!=null)p=e.position().add(e.position().subtract(e.getLastHurtByMob().position()).normalize().scale(10));mob.getNavigation().moveTo(p.x,p.y,p.z,1.5);}
            if(e.tickCount%10==0&&mob.getTarget()!=null&&!IllusionState.mobImmune(mob)&&wallBetween(mob,mob.getTarget()))mob.setTarget(null);
        }
    }
    public static boolean wallBetween(LivingEntity a,LivingEntity b){Vec3 start=a.getEyePosition(),end=b.getEyePosition(),delta=end.subtract(start);double distance=delta.length();if(distance>64)return false;Vec3 step=delta.normalize().scale(.5);for(int i=0;i<=distance*2;i++)if(a.level().getBlockState(net.minecraft.core.BlockPos.containing(start.add(step.scale(i)))).is(IllusionContent.WALL.get()))return true;return false;}
    @SubscribeEvent public static void target(LivingChangeTargetEvent event){if(event.getNewTarget()==null)return;var e=event.getEntity();if(e.hasEffect(IllusionContent.effect("paranoia"))||e.hasEffect(IllusionContent.effect("emptiness"))||(!IllusionState.mobImmune(e)&&wallBetween(e,event.getNewTarget())))event.setCanceled(true);}
    @SubscribeEvent public static void track(PlayerEvent.StartTracking event){if(event.getEntity() instanceof ServerPlayer p&&event.getTarget() instanceof LivingEntity e&&IllusionState.mask(e)!=0)IllusionNetwork.send(p,new IllusionNetwork.State(e.getId(),e.getUUID(),IllusionState.snapshot(e)));}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p)IllusionNetwork.send(p,new IllusionNetwork.State(p.getId(),p.getUUID(),IllusionState.snapshot(p)));}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p)IllusionState.sync(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer p)IllusionState.sync(p);}
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        LivingEntity e=event.getEntity();if(!e.hasEffect(IllusionContent.effect("squish"))||event.getAmount()<=0)return;
        int axis;float amount;float damage=event.getAmount();var source=event.getSource();
        if(source.is(DamageTypes.FALL)||source.is(DamageTypes.FALLING_BLOCK)||source.is(DamageTypes.FALLING_ANVIL)){axis=1;amount=damage>=15?.125F:damage>=10?.2F:damage>=5?.35F:.5F;}
        else{Vec3 direction=source.is(DamageTypes.FLY_INTO_WALL)?e.getDeltaMovement():source.getSourcePosition()==null?null:e.position().subtract(source.getSourcePosition());if(direction==null)return;double x=Math.abs(direction.x),y=Math.abs(direction.y),z=Math.abs(direction.z);axis=x>y&&x>z?0:y>z?1:2;amount=damage>=e.getMaxHealth()*.66F?.25F:damage>=e.getMaxHealth()*.33F?.4F:.6F;}
        var d=IllusionState.data(e);if(d.contains("Axis")&&d.getInt("Axis")==axis)amount*=d.getFloat("Amount");d.putInt("Axis",axis);d.putFloat("Amount",Math.max(.125F,amount));e.refreshDimensions();IllusionState.sync(e);
    }
}
