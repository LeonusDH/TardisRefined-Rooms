package leonus.tardis_rooms;

import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.iface.kind.TransferKinds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Общее для всех платформ: ID мода и лог. Точки входа — в модулях платформ (neoforge/TardisRoomsNeoForge). */
public final class TardisRooms {
    public static final String MODID = "tardis_rooms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /** Конфиги лежат в своей папке: config/tardis_rooms/. */
    public static final String COMMON_CONFIG = MODID + "/common.toml";
    public static final String CLIENT_CONFIG = MODID + "/client.toml";
    /** Блоки, которые уходят вместе с помещением ({@link TardisRoomsConfig#REMOVABLE_SPEC}). */
    public static final String REMOVABLE_CONFIG = MODID + "/removable_with_room.toml";

    /*
     * Уровни лога:
     *  INFO  (latest.log) — запуск и то, что меняет мир: помещение выращено, заменено, удалено; хаб упрощён;
     *  WARN / ERROR       — что-то пошло не так, но игра продолжается;
     *  debug(...)         — подробности: действия игроков, причины отказов, смена состояния интерфейсов. Всегда пишутся
     *                       в debug.log (уровень DEBUG), а с debug = true в common.toml — ещё и в latest.log.
     */

    /** Подробности: всегда в debug.log; с debug = true в config/tardis_rooms/common.toml — ещё и в обычный лог. */
    public static void debug(String message, Object... args) {
        if (TardisRoomsConfig.debug()) LOGGER.info("[debug] " + message, args);
        else LOGGER.debug(message, args);
    }

    /** Пишутся ли подробности хоть куда-нибудь — чтобы не собирать дорогие сообщения впустую. */
    public static boolean debugEnabled() {
        return TardisRoomsConfig.debug() || LOGGER.isDebugEnabled();
    }

    /** Сервер запущен: сводка настроек и видов передачи — с чем работает аддон в этой игре. */
    public static void onServerStarted() {
        StringBuilder kinds = new StringBuilder();
        for (TransferKind k : TransferKinds.all()) {
            if (!kinds.isEmpty()) kinds.append(", ");
            kinds.append(k.id().getPath()).append(k.available() ? "" : " (off)");
        }
        LOGGER.info("TARDIS Refined: Rooms: interface kinds: {}; procedural rooms off: {}, simplify hub: {}, debug: {}, "
                        + "removable_with_room entries: {}",
                kinds, TardisRoomsConfig.disableProceduralRooms(), TardisRoomsConfig.simplifyHub(), TardisRoomsConfig.debug(),
                TardisRoomsConfig.removableBlocks().size());
    }

    private TardisRooms() {}
}
