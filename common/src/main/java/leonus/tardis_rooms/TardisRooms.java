package leonus.tardis_rooms;

import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.iface.kind.TransferKinds;
import leonus.tardis_rooms.platform.Platform;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Общее для всех платформ: ID мода и лог. Точки входа — в модулях платформ (forge/TardisRoomsForge, fabric/TardisRoomsFabric). */
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

    /** Измерение ТАРДИС (в TR 1.20.1 своей проверки ещё нет — та же, что в TR для 1.21.1). */
    public static boolean isTardisDimension(net.minecraft.world.level.Level level) {
        return level.dimensionTypeRegistration().unwrapKey().orElse(null) == whocraft.tardis_refined.registry.TRDimensionTypes.TARDIS;
    }

    /** Подробности: всегда в debug.log; с debug = true в config/tardis_rooms/common.toml — ещё и в обычный лог. */
    public static void debug(String message, Object... args) {
        if (TardisRoomsConfig.debug()) LOGGER.info("[debug] " + message, args);
        else LOGGER.debug(message, args);
    }

    /** Пишутся ли подробности хоть куда-нибудь — чтобы не собирать дорогие сообщения впустую. */
    public static boolean debugEnabled() {
        return TardisRoomsConfig.debug() || LOGGER.isDebugEnabled();
    }

    /** Игрок для лога — по имени. */
    public static String who(Player player) {
        return player.getGameProfile().getName();
    }

    /** Совместимость с другим модом: название для лога, id мода и включена ли она в настройках. */
    private record Compat(String name, String modId, BooleanSupplier enabled) {}

    private static final List<Compat> COMPATS = new ArrayList<>();

    /** Платформа сообщает, с какими модами у неё есть совместимость (для сводки при запуске). */
    public static void compat(String name, String modId, BooleanSupplier enabled) {
        COMPATS.add(new Compat(name, modId, enabled));
    }

    /** Сервер запущен: сводка настроек, видов передачи и совместимостей — с чем работает аддон в этой игре. */
    public static void onServerStarted() {
        // Выключенная в настройках совместимость не упоминается; включённая — работает или её мода нет.
        for (Compat c : COMPATS) {
            if (!c.enabled().getAsBoolean()) continue;
            if (Platform.isModLoaded(c.modId())) LOGGER.info("TARDIS Refined: Rooms: {} compatibility enabled", c.name());
            else LOGGER.info("TARDIS Refined: Rooms: {} compatibility is on, but {} is not installed", c.name(), c.name());
        }
        StringBuilder kinds = new StringBuilder();
        for (TransferKind k : TransferKinds.all()) {
            if (!kinds.isEmpty()) kinds.append(", ");
            kinds.append(k.id().getPath()).append(!k.hasParts() ? " (not supported here)" : k.available() ? "" : " (off)");
        }
        LOGGER.info("TARDIS Refined: Rooms: interface kinds: {}; procedural rooms off: {}, simplify hub: {}, debug: {}, "
                        + "removable_with_room entries: {}",
                kinds, TardisRoomsConfig.disableProceduralRooms(), TardisRoomsConfig.simplifyHub(), TardisRoomsConfig.debug(),
                TardisRoomsConfig.removableBlocks().size());
    }

    private TardisRooms() {}
}
