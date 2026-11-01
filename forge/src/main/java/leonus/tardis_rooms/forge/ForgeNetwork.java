package leonus.tardis_rooms.forge;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.network.Payload;
import leonus.tardis_rooms.network.Payloads;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** Пакеты на Forge 1.20.1: один SimpleChannel, сообщения — по списку {@link Payloads}. */
public final class ForgeNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(TardisRooms.MODID, "main"), () -> Payloads.VERSION, Payloads.VERSION::equals, Payloads.VERSION::equals);

    public static void register() {
        int index = 0;
        for (Payloads.Entry<?> e : Payloads.ALL) register(index++, e);
    }

    private static <T extends Payload> void register(int index, Payloads.Entry<T> e) {
        CHANNEL.registerMessage(index, e.cls(), Payloads::encode, e::decode,
                (T msg, Supplier<NetworkEvent.Context> sup) -> {
                    NetworkEvent.Context ctx = sup.get();
                    if (e.toServer()) {
                        ServerPlayer player = ctx.getSender();
                        if (player != null) ctx.enqueueWork(() -> e.server().accept(msg, player));
                    } else {
                        ctx.enqueueWork(() -> e.client().accept(msg));
                    }
                    ctx.setPacketHandled(true);
                },
                Optional.of(e.toServer() ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToServer(Payload payload) {
        CHANNEL.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    private ForgeNetwork() {}
}
