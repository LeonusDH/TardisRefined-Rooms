package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранилища у соседей связанных интерфейсов — общее для всех платформ. Платформа даёт только способ найти хранилище
 * у блока ({@link Finder}: на NeoForge — capability, на Fabric — BlockApiLookup). Кэш на тик: трубы спрашивают помногу раз.
 */
public final class LinkedStorages {
    /** Какие соседи нужны. */
    public enum Pick {
        /** У интерфейсов с выходом — куда отдавать. Грани «забирать» пропускаются: они сами вынимают из соседа (гоняли бы по кругу). */
        TO,
        /** У интерфейсов со входом — откуда забирать. Грани «вставлять» пропускаются: они сами кладут в соседа. */
        FROM,
        /** Все, каждый один раз (у интерфейса «вход и выход» сосед попал бы и в TO, и в FROM) — для подсказки. */
        ALL
    }

    /** Хранилище нужного вида у блока {@code pos} со стороны {@code side}, или null. */
    @FunctionalInterface
    public interface Finder<T> {
        @Nullable T find(Level level, BlockPos pos, Direction side);
    }

    private final InterfaceBlockEntity be;
    private long cacheTick = -1;
    /** По выборке: вид хранилища ({@code key} — capability или lookup) → список на этот тик. */
    private final Map<Pick, Map<Object, List<?>>> cache = new EnumMap<>(Pick.class);

    public LinkedStorages(InterfaceBlockEntity be) {
        this.be = be;
        for (Pick p : Pick.values()) cache.put(p, new HashMap<>());
    }

    /**
     * Хранилища у соседей связанных интерфейсов, у которых включён вид передачи {@code type}. Соседние интерфейсы
     * пропускаются. {@code key} — по нему кэш различает виды хранилищ (одна и та же capability или lookup).
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> get(Object key, Finder<T> finder, TransferType type, Pick pick) {
        Level level = be.getLevel();
        if (level == null || !be.isConnected()) return List.of();
        long now = level.getGameTime();
        if (now != cacheTick) {
            cache.values().forEach(Map::clear);
            cacheTick = now;
        }
        Map<Object, List<?>> byKey = cache.get(pick);
        List<?> cached = byKey.get(key);
        if (cached != null) return (List<T>) cached;
        List<T> out = new ArrayList<>();
        for (InterfaceBlockEntity peer : InterfaceNetwork.peers(be)) {
            if (!peer.enabled(type) || (pick == Pick.TO && !peer.flow().out) || (pick == Pick.FROM && !peer.flow().in)) continue;
            Level pl = peer.getLevel();
            if (pl == null) continue;
            for (Direction d : Direction.values()) {
                FaceMode fm = peer.face(d);
                if ((pick == Pick.TO && fm == FaceMode.PULL) || (pick == Pick.FROM && fm == FaceMode.PUSH)) continue;
                BlockPos np = peer.getBlockPos().relative(d);
                if (!pl.isLoaded(np) || pl.getBlockEntity(np) instanceof InterfaceBlockEntity) continue;
                T h = finder.find(pl, np, d.getOpposite());
                if (h != null) out.add(h);
            }
        }
        // По кругу: каждый раз начинаем с другого получателя, чтобы делить поровну.
        if (pick != Pick.ALL && out.size() > 1) Collections.rotate(out, -(int) (now % out.size()));
        byKey.put(key, out);
        return out;
    }
}
