package leonus.tardis_rooms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Общее для всех платформ: ID мода и лог. Точки входа — в модулях платформ (forge/TardisRoomsForge, fabric/TardisRoomsFabric). */
public final class TardisRooms {
    public static final String MODID = "tardis_rooms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /** Конфиги лежат в своей папке: config/tardis_rooms/. */
    public static final String COMMON_CONFIG = MODID + "/common.toml";
    public static final String CLIENT_CONFIG = MODID + "/client.toml";

    /** Отладочная запись в лог — только при debug = true в config/tardis_rooms/common.toml. */
    /** Измерение ТАРДИС (в TR 1.20.1 своей проверки ещё нет — та же, что в TR для 1.21.1). */
    public static boolean isTardisDimension(net.minecraft.world.level.Level level) {
        return level.dimensionTypeRegistration().unwrapKey().orElse(null) == whocraft.tardis_refined.registry.TRDimensionTypes.TARDIS;
    }

    public static void debug(String message, Object... args) {
        if (TardisRoomsConfig.debug()) LOGGER.info("[debug] " + message, args);
    }

    private TardisRooms() {}
}
