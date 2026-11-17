package cn.wanyiwige.illusion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Supplier;
public final class IllusionNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(IllusionContent.id("wan_integration"),()->"1","1"::equals,"1"::equals);
    public static void register(){
        CHANNEL.registerMessage(0,State.class,State::write,State::read,State::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1,Select.class,Select::write,Select::read,Select::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void tracking(LivingEntity e,Object packet){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->e),packet);}
    public static void send(ServerPlayer p,Object packet){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet);}
    public record State(int entity,UUID uuid,CompoundTag data){
        static void write(State p,FriendlyByteBuf b){b.writeInt(p.entity);b.writeUUID(p.uuid);b.writeNbt(p.data);}
        static State read(FriendlyByteBuf b){return new State(b.readInt(),b.readUUID(),b.readNbt());}
        static void handle(State p,Supplier<NetworkEvent.Context> c){var ctx=c.get();ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->IllusionClient.receive(p)));ctx.setPacketHandled(true);}
    }
    public record Select(int slot,boolean clear){
        static void write(Select p,FriendlyByteBuf b){b.writeByte(p.slot);b.writeBoolean(p.clear);}
        static Select read(FriendlyByteBuf b){return new Select(b.readByte(),b.readBoolean());}
        static void handle(Select p,Supplier<NetworkEvent.Context> c){var ctx=c.get();ctx.enqueueWork(()->{ServerPlayer player=ctx.getSender();if(player==null||p.slot<0||p.slot>=8)return;var stack=player.getMainHandItem();if(stack.getItem()!=IllusionContent.PICTURE_BOOK.get())return;if(p.clear)PictureBook.clear(stack,p.slot);else stack.getOrCreateTag().putInt("Selected",p.slot);player.containerMenu.broadcastChanges();});ctx.setPacketHandled(true);}
    }
}
