package cn.wanyiwige.alexcavesspellsbook.traveller.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.traveller.MapTravelNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Optional Xaero integration. No Xaero classes are linked on a dedicated server. */
@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, value = Dist.CLIENT)
public final class MapTravelClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static UUID token;
    private static ResourceLocation dimension;
    private static long expires;
    private static boolean pending;

    public static void grant(UUID newToken, ResourceLocation newDimension) {
        token = newToken;
        dimension = newDimension;
        expires = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        pending = false;
        Minecraft mc = Minecraft.getInstance();
        try {
            Object session = Class.forName("xaero.map.WorldMapSession").getMethod("getCurrentSession").invoke(null);
            if (session == null) throw new IllegalStateException("No active Xaero world map session");
            Object processor = call(session, "getMapProcessor");
            Screen map = (Screen)Class.forName("xaero.map.gui.GuiMap")
                    .getConstructor(Screen.class, Screen.class, Class.forName("xaero.map.MapProcessor"), Entity.class)
                    .newInstance(null, null, processor, mc.getCameraEntity());
            mc.setScreen(map);
            message("map_travel.ready");
        } catch (ReflectiveOperationException | RuntimeException e) {
            clear();
            LOGGER.warn("Unable to open Xaero world map for Map Travel", e);
            message("map_travel.map_missing");
        }
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return token != null && System.nanoTime() < expires && mc.player != null && mc.player.isAlive()
                && mc.level != null && mc.level.dimension().location().equals(dimension);
    }

    public static void mapTeleport(Object mapWorld, int x, int y, int z, ResourceKey<Level> targetDimension) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Object session = Class.forName("xaero.map.WorldMapSession").getMethod("getCurrentSession").invoke(null);
            if (session == null || call(call(session, "getMapProcessor"), "getMapWorld") != mapWorld) {
                message("map_travel.wrong_world");
                return;
            }
            if (mc.gameMode != null && mc.gameMode.canHurtPlayer()) {
                Object destination = call(mapWorld, "getDimension", targetDimension == null ? mc.level.dimension() : targetDimension);
                Object playerMapKey = call(mapWorld, "getPlayerMapKey");
                Object destinationMapKey = destination == null ? null : call(destination, "getSelectedMapKeyUnsynced");
                if (playerMapKey == null || destinationMapKey == null
                        || !(Boolean)call(call(mapWorld, "getMapConnections"), "isConnected", playerMapKey, destinationMapKey)) {
                    message("map_travel.wrong_world");
                    return;
                }
            }
            request(targetDimension == null ? mc.level.dimension().location() : targetDimension.location(),
                    x, y, z, y == 32767, false, 0);
        } catch (ReflectiveOperationException | RuntimeException e) { integrationError(e); }
    }

    public static boolean waypointAllowed(Object teleporter, Object world) {
        if (!active() || world == null) return false;
        try {
            Object session = minimapSession(teleporter);
            Object currentWorld = call(call(session, "getWorldManager"), "getAutoWorld");
            if (currentWorld == null || call(currentWorld, "getContainer") != call(world, "getContainer")) return false;
            return (Boolean)call(teleporter, "isWorldTeleportable", world)
                    && (Boolean)call(teleporter, "isTeleportationSafe", world);
        } catch (ReflectiveOperationException | RuntimeException e) { return false; }
    }

    public static boolean waypointMenuAllowed(Object menu) {
        try {
            Class<?> type = Class.forName("xaero.common.gui.GuiWaypoints");
            Field selected = type.getDeclaredField("selectedListSet");
            Field world = type.getDeclaredField("displayedWorld");
            Field session = type.getDeclaredField("session");
            selected.setAccessible(true); world.setAccessible(true); session.setAccessible(true);
            if (((java.util.Set<?>)selected.get(menu)).size() != 1) return false;
            Object teleporter = call(call(session.get(menu), "getWaypointSession"), "getTeleport");
            return waypointAllowed(teleporter, world.get(menu));
        } catch (ReflectiveOperationException | RuntimeException e) { return false; }
    }

    public static void waypointTeleport(Object teleporter, Object waypoint, Object world) {
        try {
            Object session = minimapSession(teleporter);
            call(call(session, "getWorldStateUpdater"), "update");
            if (waypoint == null || !waypointAllowed(teleporter, world)) {
                message("map_travel.wrong_world");
                return;
            }
            // Match Xaero's coordinate scaling while restricting the world to the current dimension container.
            double division = ((Number)call(call(session, "getDimensionHelper"), "getDimensionDivision", world)).doubleValue();
            if (!Double.isFinite(division) || division <= 0) throw new IllegalStateException("Invalid coordinate scale");
            int x = (int)Math.floor(((Number)call(waypoint, "getX")).doubleValue() / division);
            int z = (int)Math.floor(((Number)call(waypoint, "getZ")).doubleValue() / division);
            int y = ((Number)call(waypoint, "getY")).intValue();
            boolean surface = !(Boolean)call(waypoint, "isYIncluded");
            boolean rotate = (Boolean)call(waypoint, "isRotation");
            float yaw = rotate ? ((Number)call(waypoint, "getYaw")).floatValue() : 0;
            request(dimension, x, y, z, surface, rotate, yaw);
        } catch (ReflectiveOperationException | RuntimeException e) { integrationError(e); }
    }

    private static void request(ResourceLocation target, int x, int y, int z, boolean surface, boolean rotate, float yaw) {
        if (!active() || pending) return;
        if (!dimension.equals(target)) { message("map_travel.wrong_dimension"); return; }
        pending = true;
        MapTravelNetwork.request(new MapTravelNetwork.Request(token, target, x, y, z, surface, rotate, yaw));
        // Closing the map also unpauses an integrated server so it can process the request.
        Minecraft.getInstance().setScreen(null);
    }

    public static void result(UUID responseToken, boolean revoke, String key) {
        if (!responseToken.equals(token)) return;
        pending = false;
        if (revoke) clear();
        message(key);
    }

    private static Object minimapSession(Object teleporter) throws ReflectiveOperationException {
        Field field = Class.forName("xaero.hud.minimap.waypoint.WaypointTeleport").getDeclaredField("minimapSession");
        field.setAccessible(true);
        return field.get(teleporter);
    }

    private static Object call(Object receiver, String name, Object... args) throws ReflectiveOperationException {
        return XaeroReflection.call(receiver, name, args);
    }

    private static void integrationError(Exception e) {
        LOGGER.warn("Xaero Map Travel integration failed", e);
        message("map_travel.integration_error");
    }
    private static void message(String key) {
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(key), false);
    }
    private static void clear() { token = null; dimension = null; pending = false; }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && token != null && !active()) {
            clear();
            message("map_travel.expired");
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    private MapTravelClient() { }
}
