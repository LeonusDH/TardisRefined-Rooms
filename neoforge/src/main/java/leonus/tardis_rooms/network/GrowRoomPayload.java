package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.RoomGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Клиент → сервер: пристроить {@code pieceId} к узлу {@code pos}; {@code replaceCore} ≠ ZERO — замена помещения. */
public record GrowRoomPayload(BlockPos pos, ResourceLocation pieceId, BlockPos replaceCore) implements CustomPacketPayload {
    public static final Type<GrowRoomPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "grow_room"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GrowRoomPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GrowRoomPayload::pos,
            ResourceLocation.STREAM_CODEC, GrowRoomPayload::pieceId,
            BlockPos.STREAM_CODEC, GrowRoomPayload::replaceCore,
            GrowRoomPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Сервер, главный поток. */
    public static void handle(GrowRoomPayload payload, ServerPlayer player) {
        RoomGrowth.tryGrow(player, payload.pos(), payload.pieceId(), payload.replaceCore());
    }
}
