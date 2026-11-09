package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Сервер → клиент: обвести эти рамки (в текущем измерении игрока) на {@code seconds} секунд. */
public record HighlightPayload(List<AABB> boxes, int seconds) implements CustomPacketPayload {
    public static final Type<HighlightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "highlight"));
    private static final StreamCodec<ByteBuf, AABB> BOX = StreamCodec.of(
            (buf, b) -> {
                buf.writeDouble(b.minX); buf.writeDouble(b.minY); buf.writeDouble(b.minZ);
                buf.writeDouble(b.maxX); buf.writeDouble(b.maxY); buf.writeDouble(b.maxZ);
            },
            buf -> new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()));
    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightPayload> STREAM_CODEC = StreamCodec.composite(
            BOX.apply(ByteBufCodecs.list(256)), HighlightPayload::boxes,
            ByteBufCodecs.VAR_INT, HighlightPayload::seconds,
            HighlightPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Клиент, главный поток. */
    public static void handle(HighlightPayload p) {
        leonus.tardis_rooms.client.BlockHighlights.show(p.boxes(), p.seconds());
    }
}
