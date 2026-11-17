package cn.wanyiwige.alexcavesspellsbook.traveller;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.traveller.client.MapTravelClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class MapTravelNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "map_travel"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, Grant.class, Grant::encode, Grant::decode, Grant::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Request.class, Request::encode, Request::decode, Request::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, Result.class, Result::encode, Result::decode, Result::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void request(Request packet) { CHANNEL.sendToServer(packet); }

    public record Grant(UUID token, ResourceLocation dimension) {
        static void encode(Grant p, FriendlyByteBuf b) { b.writeUUID(p.token); b.writeResourceLocation(p.dimension); }
        static Grant decode(FriendlyByteBuf b) { return new Grant(b.readUUID(), b.readResourceLocation()); }
        static void handle(Grant p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> MapTravelClient.grant(p.token, p.dimension)));
            context.setPacketHandled(true);
        }
    }

    public record Request(UUID token, ResourceLocation dimension, int x, int y, int z, boolean surface,
                          boolean rotate, float yaw) {
        static void encode(Request p, FriendlyByteBuf b) {
            b.writeUUID(p.token); b.writeResourceLocation(p.dimension);
            b.writeInt(p.x); b.writeInt(p.y); b.writeInt(p.z);
            b.writeBoolean(p.surface); b.writeBoolean(p.rotate); b.writeFloat(p.yaw);
        }
        static Request decode(FriendlyByteBuf b) {
            return new Request(b.readUUID(), b.readResourceLocation(), b.readInt(), b.readInt(), b.readInt(),
                    b.readBoolean(), b.readBoolean(), b.readFloat());
        }
        static void handle(Request p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) context.enqueueWork(() -> MapTravelServer.teleport(player, p));
            context.setPacketHandled(true);
        }
    }

    public record Result(UUID token, boolean clear, String message) {
        static void encode(Result p, FriendlyByteBuf b) { b.writeUUID(p.token); b.writeBoolean(p.clear); b.writeUtf(p.message, 128); }
        static Result decode(FriendlyByteBuf b) { return new Result(b.readUUID(), b.readBoolean(), b.readUtf(128)); }
        static void handle(Result p, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> MapTravelClient.result(p.token, p.clear, p.message)));
            context.setPacketHandled(true);
        }
    }

    private MapTravelNetwork() { }
}
