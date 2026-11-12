package leonus.tardis_rooms.compat.acua;

import leonus.tardis_rooms.StartupConfig;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.room.RoomPermissions;

/**
 * Совместимость с AcuaTARDISGadgets: права аддона — в его Security Manager. Здесь нет ни одного его класса, поэтому
 * без мода ничего из него не загружается; всё, что его касается, — в {@link AcuaPermissions}.
 * Убрать совместимость — удалить этот пакет и вызов {@link #init()}.
 */
public final class AcuaCompat {
    public static final String MOD_ID = "acuatardisgadgets";

    private AcuaCompat() {}

    /**
     * При запуске мода, до регистрации (на Forge — в NewRegistryEvent): права нужно добавить в реестр Acua, пока он
     * открыт. Выключено в конфиге — прав нет, проверок нет (настройка читается из файла один раз:
     * {@link StartupConfig#compatAcua()}).
     */
    public static void init() {
        if (!Platform.isModLoaded(MOD_ID) || !StartupConfig.compatAcua()) return;
        AcuaPermissions.register();
        RoomPermissions.set(AcuaPermissions::allowed);
    }
}
