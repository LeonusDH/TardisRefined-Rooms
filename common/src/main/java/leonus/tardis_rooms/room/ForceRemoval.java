package leonus.tardis_rooms.room;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Удаление помещения вместе с тем, что мешает. Игрок открывает окно подтверждения — сервер запоминает, когда
 * ({@link #arm}); удалить так можно не раньше чем через {@value #WAIT_MS} мс и не позже чем через
 * {@value #EXPIRE_MS} мс, один раз и только это помещение ({@link #take}). Так отсчёт в окне не обойти пакетом.
 * Время — по часам, а не по тикам: отсчёт в окне идёт по часам клиента, а сервер может тормозить.
 */
public final class ForceRemoval {
    /** Окно ждёт 10 с; небольшой запас на задержку сети. */
    public static final long WAIT_MS = 9_500;
    /** Окно открыто слишком давно — откройте заново. */
    public static final long EXPIRE_MS = 5 * 60_000;

    private record Armed(ResourceKey<Level> level, BlockPos core, long at) {}

    private static final Map<UUID, Armed> ARMED = new HashMap<>();

    /** Игрок открыл окно подтверждения для помещения с блоком управления {@code core}. */
    public static void arm(UUID player, ResourceKey<Level> level, BlockPos core, long now) {
        ARMED.put(player, new Armed(level, core.immutable(), now));
    }

    /** Можно ли сейчас удалить это помещение вместе с тем, что мешает. Да — разрешение использовано. */
    public static boolean take(UUID player, ResourceKey<Level> level, BlockPos core, long now) {
        Armed a = ARMED.get(player);
        if (a == null || !a.level().equals(level) || !a.core().equals(core)) return false;
        long waited = now - a.at();
        if (waited < WAIT_MS) return false;
        ARMED.remove(player);
        return waited <= EXPIRE_MS;
    }

    /** Сервер остановлен. */
    public static void clear() {
        ARMED.clear();
    }

    private ForceRemoval() {}
}
