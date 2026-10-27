package leonus.tardis_rooms.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

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

    /** Клиент → сервер. Пакет должен быть зарегистрирован платформой. */
    @ExpectPlatform
    public static void sendToServer(CustomPacketPayload payload) {
        throw new AssertionError();
    }

    /** Сервер → клиент этого игрока. */
    @ExpectPlatform
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        throw new AssertionError();
    }

    private Platform() {}
}
