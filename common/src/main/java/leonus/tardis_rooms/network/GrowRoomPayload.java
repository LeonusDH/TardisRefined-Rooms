package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.network.codec.ByteBufCodecs;
import leonus.tardis_rooms.network.codec.StreamCodec;
import leonus.tardis_rooms.room.RoomGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Клиент → сервер: пристроить {@code pieceId} к узлу {@code pos}; {@code replaceCore} ≠ ZERO — замена помещения. */
public record GrowRoomPayload(BlockPos pos, ResourceLocation pieceId, BlockPos replaceCore) implements Payload {
    public static final Type<GrowRoomPayload> TYPE = new Type<>(new ResourceLocation(TardisRooms.MODID, "grow_room"));
    public static final StreamCodec<FriendlyByteBuf, GrowRoomPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BLOCK_POS, GrowRoomPayload::pos,
            ByteBufCodecs.RESOURCE_LOCATION, GrowRoomPayload::pieceId,
            ByteBufCodecs.BLOCK_POS, GrowRoomPayload::replaceCore,
            GrowRoomPayload::new);

    @Override
    public Type<? extends Payload> type() { return TYPE; }

    /** Сервер, главный поток. */
    public static void handle(GrowRoomPayload payload, ServerPlayer player) {
        RoomGrowth.tryGrow(player, payload.pos(), payload.pieceId(), payload.replaceCore());
    }
}
