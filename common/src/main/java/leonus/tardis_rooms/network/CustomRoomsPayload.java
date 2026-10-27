package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.custom.CustomRoomInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Сервер → клиент: помещения из датапаков (при входе и после /reload). Целиком заменяет прежний список. */
public record CustomRoomsPayload(List<CustomRoomInfo> rooms) implements CustomPacketPayload {
    public static final Type<CustomRoomsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "custom_rooms"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomRoomsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.rooms.size());
                for (CustomRoomInfo i : p.rooms) CustomRoomInfo.STREAM_CODEC.encode(buf, i);
            },
            buf -> {
                int n = buf.readVarInt();
                List<CustomRoomInfo> list = new ArrayList<>(n);
                for (int k = 0; k < n; k++) list.add(CustomRoomInfo.STREAM_CODEC.decode(buf));
                return new CustomRoomsPayload(List.copyOf(list));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Клиент, главный поток. */
    public static void handle(CustomRoomsPayload p) {
        leonus.tardis_rooms.client.ClientCustomRooms.accept(p.rooms);
    }
}
