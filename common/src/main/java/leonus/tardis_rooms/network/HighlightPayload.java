package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.BlockerHighlight;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Сервер → клиент: обвести эти блоки (в текущем мире игрока) на {@code seconds} секунд; пусто — снять обводку.
 * Блоков не больше {@link #MAX}: по две половины на каждый из {@link BlockerHighlight#MAX_BLOCKS} предметов.
 */
public record HighlightPayload(List<BlockPos> blocks, int seconds) implements CustomPacketPayload {
    public static final int MAX = BlockerHighlight.MAX_BLOCKS * 2;
    public static final Type<HighlightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "highlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), HighlightPayload::blocks,
            ByteBufCodecs.VAR_INT, HighlightPayload::seconds,
            HighlightPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Клиент, главный поток. */
    public static void handle(HighlightPayload p) {
        leonus.tardis_rooms.client.BlockHighlights.show(p.blocks(), p.seconds());
    }
}
