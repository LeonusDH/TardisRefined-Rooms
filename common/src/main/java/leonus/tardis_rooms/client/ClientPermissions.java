package leonus.tardis_rooms.client;

import leonus.tardis_rooms.room.RoomPermissions;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * Что игроку запрещено, по последним сведениям сервера ({@link leonus.tardis_rooms.room.RoomPermissionSync}).
 * Сведения — от того соединения, в котором пришли: зашёл на другой сервер — снова «можно всё», пока тот не скажет иначе.
 */
public final class ClientPermissions {
    private static Map<ResourceLocation, Integer> denied = Map.of();
    private static Object connection;

    private ClientPermissions() {}

    /** Пакет с сервера (главный поток клиента). */
    public static void accept(Map<ResourceLocation, Integer> fromServer) {
        denied = fromServer;
        connection = Minecraft.getInstance().getConnection();
    }

    public static boolean allowed(ResourceKey<Level> tardis, RoomPermissions.Action action) {
        if (connection == null || connection != Minecraft.getInstance().getConnection()) return true;
        return (denied.getOrDefault(tardis.location(), 0) & 1 << action.ordinal()) == 0;
    }
}
