package leonus.tardis_rooms.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

/**
 * То, что у каждого загрузчика своё. Реализация — в классе {@code leonus.tardis_rooms.platform.<платформа>.PlatformImpl}
 * (плагин Architectury подставляет её при сборке; API Architectury для этого не нужен).
 */
public final class Platform {
    /** Установлен ли мод с этим ID. */
    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError();
    }

    /** Папка config/ игры или сервера. */
    @ExpectPlatform
    public static Path configDir() {
        throw new AssertionError();
    }

    private Platform() {}
}
