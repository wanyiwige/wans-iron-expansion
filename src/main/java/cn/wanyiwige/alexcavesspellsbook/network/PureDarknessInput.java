package cn.wanyiwige.alexcavesspellsbook.network;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.entities.PureDarknessFinaleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class PureDarknessInput {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(AlexCavesSpellsbook.MOD_ID, "pure_darkness_input"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private PureDarknessInput() {}

    public static void register() {
        CHANNEL.registerMessage(0, Signal.class, Signal::encode, Signal::decode, Signal::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void send(PureDarknessFinaleEntity entity, int state) {
        CHANNEL.sendToServer(new Signal(entity.getId(), entity.getUUID(), state));
    }

    private record Signal(int id, UUID token, int state) {
        private void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(id);
            buf.writeUUID(token);
            buf.writeByte(state);
        }

        private static Signal decode(FriendlyByteBuf buf) {
            return new Signal(buf.readVarInt(), buf.readUUID(), buf.readUnsignedByte());
        }

        private void handle(Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            var sender = context.getSender();
            if (sender != null && state >= 0 && state <= PureDarknessFinaleEntity.CANCEL) {
                context.enqueueWork(() -> PureDarknessFinaleEntity.input(sender, id, token, state));
            }
            context.setPacketHandled(true);
        }
    }
}

