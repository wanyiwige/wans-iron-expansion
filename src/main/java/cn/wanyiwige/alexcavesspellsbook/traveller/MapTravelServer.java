package cn.wanyiwige.alexcavesspellsbook.traveller;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID)
public final class MapTravelServer {
    private record Permit(UUID token, ResourceKey<Level> dimension, long expires) { }
    private static final Map<UUID, Permit> PERMITS = new HashMap<>();
    private static final long DURATION = TimeUnit.SECONDS.toNanos(60);

    public static void grant(ServerPlayer player) {
        Permit permit = new Permit(UUID.randomUUID(), player.level().dimension(), System.nanoTime() + DURATION);
        PERMITS.put(player.getUUID(), permit);
        MapTravelNetwork.send(player, new MapTravelNetwork.Grant(permit.token, permit.dimension.location()));
    }

    public static void teleport(ServerPlayer player, MapTravelNetwork.Request request) {
        Permit permit = PERMITS.get(player.getUUID());
        if (permit == null || !permit.token.equals(request.token())) {
            reply(player, request, true, "no_permit");
            return;
        }
        if (System.nanoTime() >= permit.expires || !player.isAlive()) {
            PERMITS.remove(player.getUUID());
            reply(player, request, true, "expired");
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(permit.dimension) || !permit.dimension.location().equals(request.dimension())) {
            reply(player, request, false, "wrong_dimension");
            return;
        }
        BlockPos horizontal = new BlockPos(request.x(), level.getMinBuildHeight(), request.z());
        if (!level.getWorldBorder().isWithinBounds(horizontal) || player.isPassenger()
                || (request.rotate() && !Float.isFinite(request.yaw()))) {
            reply(player, request, false, "invalid_target");
            return;
        }
        // getChunkNow never generates or loads a destination chunk.
        LevelChunk chunk = level.getChunkSource().getChunkNow(request.x() >> 4, request.z() >> 4);
        if (chunk == null) {
            reply(player, request, false, "unloaded");
            return;
        }
        int startY = request.surface()
                ? chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, request.x() & 15, request.z() & 15) + 1
                : request.y();
        BlockPos landing = findLanding(level, chunk, player, request.x(), startY, request.z());
        if (landing == null) {
            reply(player, request, false, "unsafe");
            return;
        }
        // Consume before moving: duplicate or delayed packets cannot teleport a second time.
        PERMITS.remove(player.getUUID());
        player.teleportTo(level, landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D,
                request.rotate() ? request.yaw() : player.getYRot(), player.getXRot());
        player.fallDistance = 0;
        player.setDeltaMovement(0, 0, 0);
        reply(player, request, true, "success");
    }

    private static BlockPos findLanding(ServerLevel level, LevelChunk chunk, ServerPlayer player, int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return null;
        for (int offset = 0; offset <= 8; offset++) {
            BlockPos above = new BlockPos(x, y + offset, z);
            if (safe(level, chunk, player, above)) return above;
            if (offset > 0) {
                BlockPos below = new BlockPos(x, y - offset, z);
                if (safe(level, chunk, player, below)) return below;
            }
        }
        return null;
    }

    private static boolean safe(ServerLevel level, LevelChunk chunk, ServerPlayer player, BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight() || feet.getY() + player.getBbHeight() >= level.getMaxBuildHeight()) return false;
        BlockState ground = chunk.getBlockState(feet.below());
        if (!ground.isSolidRender(level, feet.below()) || !ground.getFluidState().isEmpty()) return false;
        AABB box = player.getBoundingBox().move(feet.getX() + 0.5 - player.getX(), feet.getY() - player.getY(),
                feet.getZ() + 0.5 - player.getZ());
        // A scaled player can overlap adjacent chunks; require those to be loaded too.
        int minX = (int)Math.floor(box.minX) >> 4, maxX = (int)Math.floor(box.maxX) >> 4;
        int minZ = (int)Math.floor(box.minZ) >> 4, maxZ = (int)Math.floor(box.maxZ) >> 4;
        for (int cx = minX; cx <= maxX; cx++) for (int cz = minZ; cz <= maxZ; cz++) {
            if (level.getChunkSource().getChunkNow(cx, cz) == null) return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed((int)Math.floor(box.minX), feet.getY(), (int)Math.floor(box.minZ),
                (int)Math.floor(box.maxX), (int)Math.floor(box.maxY), (int)Math.floor(box.maxZ))) {
            if (!level.getFluidState(pos).isEmpty()) return false;
        }
        return level.getWorldBorder().isWithinBounds(box) && level.noCollision(player, box);
    }

    private static void reply(ServerPlayer player, MapTravelNetwork.Request request, boolean clear, String key) {
        MapTravelNetwork.send(player, new MapTravelNetwork.Result(request.token(), clear, "map_travel." + key));
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) PERMITS.values().removeIf(p -> System.nanoTime() >= p.expires);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { PERMITS.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event) { PERMITS.clear(); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { revoke(event.getEntity()); }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) revoke(player);
    }
    private static void revoke(net.minecraft.world.entity.Entity entity) {
        Permit permit = PERMITS.remove(entity.getUUID());
        if (permit != null && entity instanceof ServerPlayer player)
            MapTravelNetwork.send(player, new MapTravelNetwork.Result(permit.token, true, "map_travel.expired"));
    }
    private MapTravelServer() { }
}
