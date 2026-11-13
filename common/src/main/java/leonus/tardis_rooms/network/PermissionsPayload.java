package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервер → клиент: что игроку запрещено ({@link leonus.tardis_rooms.room.RoomPermissionSync}). Измерение ТАРДИС →
 * биты запрещённых действий ({@code 1 << Action.ordinal()}); ТАРДИС, где можно всё, нет. Целиком заменяет прежнее.
 */
public record PermissionsPayload(Map<ResourceLocation, Integer> denied) implements Payload {
    public static final Type<PermissionsPayload> TYPE = new Type<>(new ResourceLocation(TardisRooms.MODID, "permissions"));
    public static final StreamCodec<FriendlyByteBuf, PermissionsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.denied.size());
                p.denied.forEach((tardis, mask) -> {
                    buf.writeResourceLocation(tardis);
                    buf.writeVarInt(mask);
                });
            },
            buf -> {
                int n = buf.readVarInt();
                Map<ResourceLocation, Integer> denied = new HashMap<>(n);
                for (int k = 0; k < n; k++) denied.put(buf.readResourceLocation(), buf.readVarInt());
                return new PermissionsPayload(Map.copyOf(denied));
            });

    @Override
    public Type<? extends Payload> type() { return TYPE; }

    /** Клиент, главный поток. */
    public static void handle(PermissionsPayload p) {
        leonus.tardis_rooms.client.ClientPermissions.accept(p.denied);
    }
}
