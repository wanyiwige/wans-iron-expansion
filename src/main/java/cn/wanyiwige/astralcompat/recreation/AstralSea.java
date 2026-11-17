package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native flight and safe saved return locations replace the upstream external swim hook. */
@Mod.EventBusSubscriber(modid=AlexCavesSpellsbook.MOD_ID)
public final class AstralSea {
    public static final ResourceKey<Level> SEA=ResourceKey.create(Registries.DIMENSION,ArcanumContent.id("astral_sea"));
    private static final String FLIGHT="WanAstralSeaFlight",ORIGIN="WanAstralSeaOrigin";
    public static void travelGroup(ServerPlayer caster) {
        boolean returning=caster.level().dimension().equals(SEA);
        var group=caster.serverLevel().getEntitiesOfClass(ServerPlayer.class,caster.getBoundingBox().inflate(8),p->p.distanceToSqr(caster)<=64&&!p.isSpectator());
        for(ServerPlayer player:group) {
            if(!returning) {
                ServerLevel sea=player.getServer().getLevel(SEA);if(sea==null)continue;
                Vec3 target=findSafe(sea,player,new Vec3(player.getX(),64,player.getZ()));if(target==null)continue;
                CompoundTag saved=new CompoundTag();saved.putString("Dimension",player.level().dimension().location().toString());
                saved.putDouble("X",player.getX());saved.putDouble("Y",player.getY());saved.putDouble("Z",player.getZ());saved.putFloat("Yaw",player.getYRot());saved.putFloat("Pitch",player.getXRot());
                player.getPersistentData().put(ORIGIN,saved);if(player.isPassenger())player.stopRiding();
                player.teleportTo(sea,target.x,target.y,target.z,player.getYRot(),player.getXRot());grantFlight(player);
            } else {
                CompoundTag saved=player.getPersistentData().getCompound(ORIGIN);
                ResourceLocation dimension=ResourceLocation.tryParse(saved.getString("Dimension"));
                ServerLevel dest=dimension==null?null:player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
                if(dest==null||dest.dimension().equals(SEA))dest=player.getServer().overworld();
                Vec3 preferred=saved.contains("X")?new Vec3(saved.getDouble("X"),saved.getDouble("Y"),saved.getDouble("Z")):Vec3.atBottomCenterOf(dest.getSharedSpawnPos());
                Vec3 target=findSafe(dest,player,preferred);
                if(target==null) { player.displayClientMessage(net.minecraft.network.chat.Component.literal("原处没有安全的返回空间。"),true);continue; }
                if(player.isPassenger())player.stopRiding();
                player.teleportTo(dest,target.x,target.y,target.z,saved.getFloat("Yaw"),saved.getFloat("Pitch"));
                restoreFlight(player);player.getPersistentData().remove(ORIGIN);
            }
            player.resetFallDistance();
        }
    }
    private static Vec3 findSafe(ServerLevel level,ServerPlayer player,Vec3 target) {
        if(!level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(target)))return null;
        level.getChunk(net.minecraft.core.BlockPos.containing(target));
        for(int offset=0;offset<24;offset++) {
            int dy=offset%2==0?offset/2:-(offset+1)/2;
            Vec3 candidate=target.add(0,dy,0);
            AABB box=player.getBoundingBox().move(candidate.subtract(player.position()));
            if(box.minY<=level.getMinBuildHeight()||box.maxY>=level.getMaxBuildHeight())continue;
            if(level.noCollision(player,box)&&!level.containsAnyLiquid(box))return candidate;
        }
        return null;
    }
    private static void grantFlight(ServerPlayer player) {
        if(!player.getPersistentData().contains(FLIGHT)) {
            CompoundTag previous=new CompoundTag();previous.putBoolean("Mayfly",player.getAbilities().mayfly);previous.putBoolean("Flying",player.getAbilities().flying);
            player.getPersistentData().put(FLIGHT,previous);
        }
        if(!player.getAbilities().mayfly) { player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities(); }
    }
    private static void restoreFlight(ServerPlayer player) {
        CompoundTag saved=player.getPersistentData().getCompound(FLIGHT);
        if(!player.getPersistentData().contains(FLIGHT))return;
        if(!player.isCreative()&&!player.isSpectator()) { player.getAbilities().mayfly=saved.getBoolean("Mayfly");player.getAbilities().flying=saved.getBoolean("Flying");player.onUpdateAbilities(); }
        player.getPersistentData().remove(FLIGHT);
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player))return;
        if(player.level().dimension().equals(SEA)) { grantFlight(player);player.resetFallDistance(); }
        else restoreFlight(player);
    }
    @SubscribeEvent public static void entityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        Entity entity=event.getEntity();if(event.getLevel().isClientSide||entity instanceof net.minecraft.world.entity.player.Player)return;
        if(event.getLevel().dimension().equals(SEA)) {
            if(!entity.isNoGravity()) { entity.getPersistentData().putBoolean("WanAstralSeaGravity",true);entity.setNoGravity(true); }
        } else if(entity.getPersistentData().getBoolean("WanAstralSeaGravity")) { entity.setNoGravity(false);entity.getPersistentData().remove("WanAstralSeaGravity"); }
    }
    private AstralSea() {}
}
