package leonus.tardis_rooms.network.codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Готовые сетевые кодеки (копия нужной части {@code ByteBufCodecs} и {@code *.STREAM_CODEC} из 1.20.5+). */
public final class ByteBufCodecs {
    public static final StreamCodec<FriendlyByteBuf, Boolean> BOOL = StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean);
    public static final StreamCodec<FriendlyByteBuf, Integer> VAR_INT = StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt);
    public static final StreamCodec<FriendlyByteBuf, Integer> INT = StreamCodec.of(FriendlyByteBuf::writeInt, FriendlyByteBuf::readInt);
    public static final StreamCodec<FriendlyByteBuf, Long> VAR_LONG = StreamCodec.of(FriendlyByteBuf::writeVarLong, FriendlyByteBuf::readVarLong);
    public static final StreamCodec<FriendlyByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    public static final StreamCodec<FriendlyByteBuf, byte[]> BYTE_ARRAY = StreamCodec.of(FriendlyByteBuf::writeByteArray, FriendlyByteBuf::readByteArray);
    public static final StreamCodec<FriendlyByteBuf, BlockPos> BLOCK_POS = StreamCodec.of(FriendlyByteBuf::writeBlockPos, FriendlyByteBuf::readBlockPos);
    public static final StreamCodec<FriendlyByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of(FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::readResourceLocation);
    public static final StreamCodec<FriendlyByteBuf, Direction> DIRECTION = StreamCodec.of(FriendlyByteBuf::writeEnum, buf -> buf.readEnum(Direction.class));
    public static final StreamCodec<FriendlyByteBuf, Component> COMPONENT = StreamCodec.of(FriendlyByteBuf::writeComponent, FriendlyByteBuf::readComponent);
    public static final StreamCodec<FriendlyByteBuf, Optional<Component>> OPTIONAL_COMPONENT = StreamCodec.of(
            (buf, c) -> buf.writeOptional(c, FriendlyByteBuf::writeComponent), buf -> buf.readOptional(FriendlyByteBuf::readComponent));

    public static StreamCodec<FriendlyByteBuf, String> stringUtf8(int max) {
        return StreamCodec.of((buf, s) -> buf.writeUtf(s, max), buf -> buf.readUtf(max));
    }

    /** {@code codec.apply(ByteBufCodecs.list())} — список таких значений. */
    public static <B extends FriendlyByteBuf, V> Function<StreamCodec<B, V>, StreamCodec<B, List<V>>> list() {
        return list(Integer.MAX_VALUE);
    }

    /** То же, но не длиннее {@code max} (длиннее — ошибка при записи и чтении, как в игре на 1.21). */
    public static <B extends FriendlyByteBuf, V> Function<StreamCodec<B, V>, StreamCodec<B, List<V>>> list(int max) {
        return c -> StreamCodec.of((buf, list) -> {
            if (list.size() > max) throw new IllegalArgumentException("List too long: " + list.size() + " > " + max);
            buf.writeVarInt(list.size());
            for (V v : list) c.encode(buf, v);
        }, buf -> {
            int n = buf.readVarInt();
            if (n > max) throw new IllegalArgumentException("List too long: " + n + " > " + max);
            List<V> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(c.decode(buf));
            return out;
        });
    }

    /** {@code codec.apply(ByteBufCodecs::optional)} — значение, которого может не быть. */
    public static <B extends FriendlyByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<B, V> c) {
        return StreamCodec.of((buf, o) -> {
            buf.writeBoolean(o.isPresent());
            o.ifPresent(v -> c.encode(buf, v));
        }, buf -> buf.readBoolean() ? Optional.of(c.decode(buf)) : Optional.empty());
    }

    private ByteBufCodecs() {}
}
