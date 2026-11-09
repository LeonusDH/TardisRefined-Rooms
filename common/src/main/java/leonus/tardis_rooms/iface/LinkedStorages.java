package leonus.tardis_rooms.iface;

import com.google.common.math.LongMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Хранилища одного вида у соседей связанных интерфейсов — общее для всех платформ. Каждый интерфейс сам помнит своих
 * соседей ({@link Sides}, обычно {@link SideCaches}), а здесь обходятся связанные интерфейсы и отбираются нужные стороны.
 * Списки запоминаются на тик: трубы спрашивают помногу раз (на каждый слот или бак), а соседи за тик не меняются.
 */
public final class LinkedStorages<T> {
    /** Какие соседи нужны. */
    public enum Pick {
        /** У интерфейсов с выходом — куда отдавать. Грани «забирать» пропускаются: они сами вынимают из соседа (гоняли бы по кругу). */
        TO,
        /** У интерфейсов со входом — откуда забирать. Грани «вставлять» пропускаются: они сами кладут в соседа. */
        FROM,
        /** Все, каждый один раз (у интерфейса «вход и выход» сосед попал бы и в TO, и в FROM) — для подсказки. */
        ALL
    }

    /** Соседи интерфейса одного вида: хранилище соседа с данной стороны, или null. */
    @FunctionalInterface
    public interface Sides<T> {
        @Nullable T at(Direction side);
    }

    private final InterfaceBlockEntity be;
    private final TransferType type;
    private final Function<InterfaceBlockEntity, Sides<T>> sidesOf;
    private final Map<Pick, List<T>> memo = new EnumMap<>(Pick.class);
    private long memoTick = Long.MIN_VALUE;

    /**
     * @param type    вид передачи: связанные интерфейсы, у которых он выключен, пропускаются
     * @param sidesOf соседи этого вида у связанного интерфейса (null — вид у него сейчас недоступен)
     */
    public LinkedStorages(InterfaceBlockEntity be, TransferType type, Function<InterfaceBlockEntity, Sides<T>> sidesOf) {
        this.be = be;
        this.type = type;
        this.sidesOf = sidesOf;
    }

    /** Хранилища у соседей связанных интерфейсов. Список только для чтения. */
    public List<T> get(Pick pick) {
        Level level = be.getLevel();
        if (level == null || !be.isConnected()) return List.of();
        long now = level.getGameTime();
        if (now != memoTick) {
            memo.clear();
            memoTick = now;
        }
        List<T> l = memo.get(pick);
        if (l == null) {
            l = collect(pick, now);
            memo.put(pick, l);
        }
        return l;
    }

    private List<T> collect(Pick pick, long now) {
        List<T> out = new ArrayList<>();
        for (InterfaceBlockEntity peer : InterfaceNetwork.peers(be)) {
            if (!peer.enabled(type) || (pick == Pick.TO && !peer.flow().out) || (pick == Pick.FROM && !peer.flow().in)) continue;
            Level pl = peer.getLevel();
            Sides<T> sides = sidesOf.apply(peer);
            if (pl == null || sides == null) continue;
            for (Direction d : Direction.values()) {
                FaceMode fm = peer.face(d);
                if (fm == FaceMode.DISABLED || (pick == Pick.TO && fm == FaceMode.PULL) || (pick == Pick.FROM && fm == FaceMode.PUSH)) continue;
                BlockPos np = peer.getBlockPos().relative(d);
                // Соседний интерфейс пропускаем: передача из интерфейса в интерфейс шла бы по кругу.
                if (!pl.isLoaded(np) || pl.getBlockEntity(np) instanceof InterfaceBlockEntity) continue;
                T h = sides.at(d);
                if (h != null) out.add(h);
            }
        }
        // По кругу: каждый тик начинаем с другого получателя, чтобы делить поровну.
        if (pick != Pick.ALL && out.size() > 1) Collections.rotate(out, -(int) (now % out.size()));
        return Collections.unmodifiableList(out);
    }

    /**
     * Сколько всего лежит в хранилищах у всех связанных интерфейсов и сколько в них влезает — для подсказки (заряд).
     * Под защитой от повторного входа: чужое хранилище, отвечая, может спросить интерфейс, и счёт пошёл бы по кругу.
     */
    public long[] totals(ToLongFunction<T> amount, ToLongFunction<T> capacity) {
        long[] out = new long[2];
        if (InterfaceNetwork.depth > 0) return out;
        InterfaceNetwork.depth++;
        try {
            for (T h : get(Pick.ALL)) {
                out[0] = LongMath.saturatedAdd(out[0], amount.applyAsLong(h));
                out[1] = LongMath.saturatedAdd(out[1], capacity.applyAsLong(h));
            }
        } finally {
            InterfaceNetwork.depth--;
        }
        return out;
    }
}
