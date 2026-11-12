package leonus.tardis_rooms;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import leonus.tardis_rooms.platform.Platform;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Настройки, которые нужны при запуске мода, раньше, чем загрузится его конфиг (на Fabric Forge Config API Port
 * загружает его позже). Читаются прямо из файла один раз; меняются только перезапуском игры.
 */
public final class StartupConfig {
    /** {@code [compat] acuaTardisGadgets} в config/tardis_rooms/common.toml. */
    static final String ACUA_PATH = "acuaTardisGadgets";
    private static final String ACUA_FULL_PATH = "compat." + ACUA_PATH;

    private static Boolean acua;

    private StartupConfig() {}

    /** Совместимость с AcuaTARDISGadgets (права в Security Manager). */
    public static synchronized boolean compatAcua() {
        if (acua == null) acua = readBoolean(Platform.configDir().resolve(TardisRooms.COMMON_CONFIG), ACUA_FULL_PATH, true);
        return acua;
    }

    /**
     * Логическое значение {@code path} (через точку) из TOML-файла. Нет файла, нет значения или оно не логическое —
     * {@code fallback}; файл не читается — тоже {@code fallback} и предупреждение в лог.
     */
    static boolean readBoolean(Path file, String path, boolean fallback) {
        if (!Files.isRegularFile(file)) return fallback;
        try (CommentedFileConfig config = CommentedFileConfig.of(file, TomlFormat.instance())) {
            config.load();
            return config.get(path) instanceof Boolean b ? b : fallback;
        } catch (RuntimeException e) {
            TardisRooms.LOGGER.warn("Could not read {} from {}, using {}: {}", path, file, fallback, e.toString());
            return fallback;
        }
    }
}
