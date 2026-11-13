package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

/**
 * Права игрока на действия аддона в чужой ТАРДИС. Своей системы прав у аддона нет: по умолчанию разрешено всё, а решение
 * подставляет совместимость с модом, у которого такая система есть ({@link #set}). Решает сервер; клиент знает его
 * решения ({@link RoomPermissionSync}), чтобы не открывать меню и не ставить блок зря.
 */
public final class RoomPermissions {
    /** Что игрок делает. */
    public enum Action {
        /** Вырастить новое помещение или коридор. */
        GROW(TRoomsText.MSG_NO_PERMISSION_GROW),
        /** Заменить, убрать или переименовать помещение. */
        MANAGE(TRoomsText.MSG_NO_PERMISSION_MANAGE),
        /** Поставить, снять, сломать или настроить интерфейс. */
        CONFIGURE_INTERFACE(TRoomsText.MSG_NO_PERMISSION_INTERFACE);

        private final TRoomsText denied;

        Action(TRoomsText denied) { this.denied = denied; }

        /** Что сказать игроку при отказе. */
        public TRoomsText denied() { return denied; }
    }

    /** Решение: можно ли игроку {@code action} в ТАРДИС с измерением интерьера {@code tardis}. */
    @FunctionalInterface
    public interface Check {
        boolean allowed(ServerPlayer player, ResourceKey<Level> tardis, Action action);
    }

    /** Без совместимости: всем можно всё. */
    public static final Check ALLOW_ALL = (player, tardis, action) -> true;

    private static volatile Check check = ALLOW_ALL;

    private RoomPermissions() {}

    public static Check current() { return check; }

    /** Задаётся при запуске мода (совместимость) и в игровых тестах. */
    public static void set(Check c) { check = Objects.requireNonNull(c); }

    /**
     * Можно ли игроку {@code action} в ТАРДИС {@code tardis}. Нет — сообщение над хотбаром и строка в отладочный лог
     * ({@code what} — что игрок пытался сделать). Ошибка в проверке — тоже отказ: лучше не дать, чем дать лишнее.
     */
    public static boolean allowed(ServerPlayer player, ResourceKey<Level> tardis, Action action, String what) {
        boolean ok;
        try {
            ok = check.allowed(player, tardis, action);
        } catch (RuntimeException e) {
            TardisRooms.LOGGER.error("Permission check for {} in {} failed, refusing: {}", TardisRooms.who(player), tardis.location(), what, e);
            ok = false;
        }
        if (!ok) {
            TardisRooms.debug("{}: {} refused: no permission ({}) in {}", TardisRooms.who(player), what, action, tardis.location());
            player.displayClientMessage(action.denied().text(), true);
        }
        return ok;
    }

    /**
     * То же для места, которое относится к нескольким ТАРДИС сразу (интерфейс вплотную к оболочке — к ТАРДИС этой
     * оболочки): нужно право в каждой. Пусто — место не в ТАРДИС, решать нечего.
     */
    public static boolean allowedInAll(ServerPlayer player, List<ResourceKey<Level>> tardises, Action action, String what) {
        for (ResourceKey<Level> tardis : tardises) {
            if (!allowed(player, tardis, action, what)) return false;
        }
        return true;
    }

    /**
     * Клиент: можно ли игроку {@code action} в ТАРДИС {@code tardis} по последним сведениям сервера. Нет — то же
     * сообщение, что дал бы сервер. Только подсказка: сервер всё равно проверяет сам.
     */
    public static boolean allowedOnClient(Player player, ResourceKey<Level> tardis, Action action) {
        if (leonus.tardis_rooms.client.ClientPermissions.allowed(tardis, action)) return true;
        player.displayClientMessage(action.denied().text(), true);
        return false;
    }

    /** {@link #allowedOnClient} для места, которое относится к нескольким ТАРДИС (см. {@link #allowedInAll}). */
    public static boolean allowedInAllOnClient(Player player, List<ResourceKey<Level>> tardises, Action action) {
        for (ResourceKey<Level> tardis : tardises) {
            if (!allowedOnClient(player, tardis, action)) return false;
        }
        return true;
    }
}
