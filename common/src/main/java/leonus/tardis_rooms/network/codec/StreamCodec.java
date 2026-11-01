package leonus.tardis_rooms.network.codec;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Кодек для сети. На 1.20.1 его нет в игре: это упрощённая копия {@code StreamCodec} из 1.20.5+ с тем же видом
 * (параметр {@code B} оставлен ради совпадения кода с основной веткой, всегда {@link FriendlyByteBuf}).
 */
public interface StreamCodec<B extends FriendlyByteBuf, V> {
    V decode(B buf);

    void encode(B buf, V value);

    interface Encoder<B, V> { void encode(B buf, V value); }

    interface Decoder<B, V> { V decode(B buf); }

    static <B extends FriendlyByteBuf, V> StreamCodec<B, V> of(Encoder<B, V> encoder, Decoder<B, V> decoder) {
        return new StreamCodec<>() {
            @Override public V decode(B buf) { return decoder.decode(buf); }
            @Override public void encode(B buf, V value) { encoder.encode(buf, value); }
        };
    }

    default <O> O apply(Function<StreamCodec<B, V>, O> op) {
        return op.apply(this);
    }

    default <O> StreamCodec<B, O> map(Function<V, O> to, Function<O, V> from) {
        StreamCodec<B, V> self = this;
        return of((buf, o) -> self.encode(buf, from.apply(o)), buf -> to.apply(self.decode(buf)));
    }

    static <B extends FriendlyByteBuf, C, T1> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, Function<T1, C> f) {
        return of((buf, v) -> enc(c1, buf, g1.apply(v)), buf -> f.apply(dec(c1, buf)));
    }

    static <B extends FriendlyByteBuf, C, T1, T2> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            BiFunction<T1, T2, C> f) {
        return of((buf, v) -> { enc(c1, buf, g1.apply(v)); enc(c2, buf, g2.apply(v)); },
                buf -> f.apply(dec(c1, buf), dec(c2, buf)));
    }

    static <B extends FriendlyByteBuf, C, T1, T2, T3> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3, Function3<T1, T2, T3, C> f) {
        return of((buf, v) -> { enc(c1, buf, g1.apply(v)); enc(c2, buf, g2.apply(v)); enc(c3, buf, g3.apply(v)); },
                buf -> f.apply(dec(c1, buf), dec(c2, buf), dec(c3, buf)));
    }

    static <B extends FriendlyByteBuf, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            Function4<T1, T2, T3, T4, C> f) {
        return of((buf, v) -> { enc(c1, buf, g1.apply(v)); enc(c2, buf, g2.apply(v)); enc(c3, buf, g3.apply(v)); enc(c4, buf, g4.apply(v)); },
                buf -> f.apply(dec(c1, buf), dec(c2, buf), dec(c3, buf), dec(c4, buf)));
    }

    static <B extends FriendlyByteBuf, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5, Function5<T1, T2, T3, T4, T5, C> f) {
        return of((buf, v) -> { enc(c1, buf, g1.apply(v)); enc(c2, buf, g2.apply(v)); enc(c3, buf, g3.apply(v)); enc(c4, buf, g4.apply(v)); enc(c5, buf, g5.apply(v)); },
                buf -> f.apply(dec(c1, buf), dec(c2, buf), dec(c3, buf), dec(c4, buf), dec(c5, buf)));
    }

    static <B extends FriendlyByteBuf, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6,
            Function6<T1, T2, T3, T4, T5, T6, C> f) {
        return of((buf, v) -> { enc(c1, buf, g1.apply(v)); enc(c2, buf, g2.apply(v)); enc(c3, buf, g3.apply(v)); enc(c4, buf, g4.apply(v)); enc(c5, buf, g5.apply(v)); enc(c6, buf, g6.apply(v)); },
                buf -> f.apply(dec(c1, buf), dec(c2, buf), dec(c3, buf), dec(c4, buf), dec(c5, buf), dec(c6, buf)));
    }

    @SuppressWarnings("unchecked")
    private static <B extends FriendlyByteBuf, T> void enc(StreamCodec<? super B, T> c, B buf, T v) {
        ((StreamCodec<B, T>) c).encode(buf, v);
    }

    @SuppressWarnings("unchecked")
    private static <B extends FriendlyByteBuf, T> T dec(StreamCodec<? super B, T> c, B buf) {
        return ((StreamCodec<B, T>) c).decode(buf);
    }
}
