package leonus.tardis_rooms.network;

import leonus.tardis_rooms.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Все пакеты аддона в одном месте: тип, класс, кодек и обработчик. Платформы регистрируют их по этому списку
 * (на 1.21.1 это делает сама игра через {@code CustomPacketPayload}, здесь — по-старому, каналами загрузчика).
 */
public final class Payloads {
    /** Версия протокола: у сервера и клиента должна совпадать. */
    public static final String VERSION = "11";

    /** Пакет: {@code toServer} — с обработчиком на сервере, иначе — на клиенте. */
    public record Entry<T extends Payload>(Payload.Type<T> type, Class<T> cls, StreamCodec<FriendlyByteBuf, T> codec,
                                           BiConsumer<T, ServerPlayer> server, Consumer<T> client) {
        public boolean toServer() { return server != null; }

        public ResourceLocation id() { return type.id(); }

        public T decode(FriendlyByteBuf buf) { return codec.decode(buf); }
    }

    public static final List<Entry<?>> ALL = List.of(
            server(GrowRoomPayload.TYPE, GrowRoomPayload.class, GrowRoomPayload.STREAM_CODEC, GrowRoomPayload::handle),
            server(FitRequestPayload.TYPE, FitRequestPayload.class, FitRequestPayload.STREAM_CODEC, FitRequestPayload::handle),
            client(FitResultPayload.TYPE, FitResultPayload.class, FitResultPayload.STREAM_CODEC, FitResultPayload::handle),
            server(ManagePayloads.Request.TYPE, ManagePayloads.Request.class, ManagePayloads.Request.STREAM_CODEC, ManagePayloads.Request::handle),
            client(ManagePayloads.Info.TYPE, ManagePayloads.Info.class, ManagePayloads.Info.STREAM_CODEC, ManagePayloads.Info::handle),
            server(ManagePayloads.Remove.TYPE, ManagePayloads.Remove.class, ManagePayloads.Remove.STREAM_CODEC, ManagePayloads.Remove::handle),
            server(ManagePayloads.ShowBlockers.TYPE, ManagePayloads.ShowBlockers.class, ManagePayloads.ShowBlockers.STREAM_CODEC, ManagePayloads.ShowBlockers::handle),
            client(HighlightPayload.TYPE, HighlightPayload.class, HighlightPayload.STREAM_CODEC, HighlightPayload::handle),
            server(ManagePayloads.Rename.TYPE, ManagePayloads.Rename.class, ManagePayloads.Rename.STREAM_CODEC, ManagePayloads.Rename::handle),
            server(MapPayloads.Request.TYPE, MapPayloads.Request.class, MapPayloads.Request.STREAM_CODEC, MapPayloads.Request::handle),
            client(MapPayloads.Info.TYPE, MapPayloads.Info.class, MapPayloads.Info.STREAM_CODEC, MapPayloads.Info::handle),
            client(CustomRoomsPayload.TYPE, CustomRoomsPayload.class, CustomRoomsPayload.STREAM_CODEC, CustomRoomsPayload::handle),
            server(InterfacePayload.TYPE, InterfacePayload.class, InterfacePayload.STREAM_CODEC, InterfacePayload::handle),
            server(ScrewdriverModePayload.TYPE, ScrewdriverModePayload.class, ScrewdriverModePayload.STREAM_CODEC, ScrewdriverModePayload::handle),
            client(PermissionsPayload.TYPE, PermissionsPayload.class, PermissionsPayload.STREAM_CODEC, PermissionsPayload::handle));

    private static final Map<ResourceLocation, Entry<?>> BY_ID = new HashMap<>();

    static {
        for (Entry<?> e : ALL) BY_ID.put(e.id(), e);
    }

    /** Записать пакет его кодеком. */
    @SuppressWarnings("unchecked")
    public static <T extends Payload> void encode(T payload, FriendlyByteBuf buf) {
        Entry<T> e = (Entry<T>) BY_ID.get(payload.type().id());
        if (e == null) throw new IllegalArgumentException("Unregistered payload " + payload.type().id());
        e.codec().encode(buf, payload);
    }

    private static <T extends Payload> Entry<T> server(Payload.Type<T> type, Class<T> cls, StreamCodec<FriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        return new Entry<>(type, cls, codec, handler, null);
    }

    private static <T extends Payload> Entry<T> client(Payload.Type<T> type, Class<T> cls, StreamCodec<FriendlyByteBuf, T> codec, Consumer<T> handler) {
        return new Entry<>(type, cls, codec, null, handler);
    }

    private Payloads() {}
}
