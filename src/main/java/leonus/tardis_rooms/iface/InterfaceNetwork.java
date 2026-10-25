package leonus.tardis_rooms.iface;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Связи интерфейсов: интерфейсы одной ТАРДИС с одинаковым идентификатором связаны (как туннели P2P в AE2).
 * Хранит только загруженные интерфейсы (сервер). Очищается при остановке сервера.
 */
public final class InterfaceNetwork {
    record Key(ResourceKey<Level> tardis, int id) {}

    private static final Map<Key, Set<InterfaceBlockEntity>> LINKS = new HashMap<>();

    /** Глубина вложенной передачи: передача из интерфейса в соседа не должна снова войти в сеть. */
    static int depth;

    static void join(Key key, InterfaceBlockEntity be) {
        LINKS.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(be);
    }

    static void leave(Key key, InterfaceBlockEntity be) {
        Set<InterfaceBlockEntity> s = LINKS.get(key);
        if (s == null) return;
        s.remove(be);
        if (s.isEmpty()) LINKS.remove(key);
    }

    /** Остальные загруженные интерфейсы с тем же идентификатором в той же ТАРДИС. */
    static List<InterfaceBlockEntity> peers(InterfaceBlockEntity self) {
        Key key = self.linkKey();
        if (key == null) return List.of();
        Set<InterfaceBlockEntity> s = LINKS.get(key);
        if (s == null) return List.of();
        List<InterfaceBlockEntity> out = new java.util.ArrayList<>(s.size());
        for (InterfaceBlockEntity b : s) if (b != self && !b.isRemoved()) out.add(b);
        return out;
    }

    public static void clear() {
        LINKS.clear();
        depth = 0;
    }

    private InterfaceNetwork() {}
}
