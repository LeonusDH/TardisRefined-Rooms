package leonus.tardis_rooms.room;

import leonus.tardis_rooms.network.PermissionsPayload;
import leonus.tardis_rooms.platform.Platform;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Клиент знает, что игроку запрещено, — чтобы не открывать меню и не ставить блок зря. Раз в секунду сервер
 * спрашивает {@link RoomPermissions} о каждой ТАРДИС и, если для игрока что-то изменилось, отправляет ему всё заново
 * ({@link PermissionsPayload}). Игроки разнесены по тикам секунды. Пока проверка «всё можно» — ничего не делает.
 * Решает всё равно сервер: это только подсказка клиенту, она может отставать на секунду.
 */
public final class RoomPermissionSync {
    static final int PERIOD = 20;

    /** Что уже отправлено игроку. Вошёл заново — другой объект игрока, а его клиент начинает с «можно всё». */
    private record Sent(ServerPlayer player, Map<ResourceLocation, Integer> denied) {}

    private static final Map<UUID, Sent> SENT = new HashMap<>();

    private RoomPermissionSync() {}

    /** Сервер, конец тика. */
    public static void onServerTick(MinecraftServer server) {
        RoomPermissions.Check check = RoomPermissions.current();
        if (check == RoomPermissions.ALLOW_ALL && SENT.isEmpty()) return;
        int slot = server.getTickCount() % PERIOD;
        List<ResourceKey<Level>> tardises = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (Math.floorMod(player.getUUID().hashCode(), PERIOD) != slot) continue;
            if (tardises == null) tardises = tardisLevels(server);
            Map<ResourceLocation, Integer> denied = denied(player, tardises, check);
            Sent prev = SENT.get(player.getUUID());
            boolean known = prev != null && prev.player == player;
            if (known ? prev.denied.equals(denied) : denied.isEmpty()) {
                if (!known) SENT.put(player.getUUID(), new Sent(player, denied));
                continue;
            }
            SENT.put(player.getUUID(), new Sent(player, denied));
            Platform.sendToPlayer(player, new PermissionsPayload(denied));
        }
        if (slot == 0) SENT.values().removeIf(s -> s.player.hasDisconnected());
    }

    /**
     * Что игроку запрещено в этих ТАРДИС: измерение → биты действий; где можно всё — нет. Ошибка в проверке — запрет,
     * как и на сервере ({@link RoomPermissions#allowed}), но без сообщений и лога: их даст сам сервер при попытке.
     */
    public static Map<ResourceLocation, Integer> denied(ServerPlayer player, List<ResourceKey<Level>> tardises, RoomPermissions.Check check) {
        Map<ResourceLocation, Integer> denied = new HashMap<>();
        for (ResourceKey<Level> tardis : tardises) {
            int mask = 0;
            for (RoomPermissions.Action action : RoomPermissions.Action.values()) {
                boolean ok;
                try {
                    ok = check.allowed(player, tardis, action);
                } catch (RuntimeException e) {
                    ok = false;
                }
                if (!ok) mask |= 1 << action.ordinal();
            }
            if (mask != 0) denied.put(tardis.location(), mask);
        }
        return denied;
    }

    private static List<ResourceKey<Level>> tardisLevels(MinecraftServer server) {
        List<ResourceKey<Level>> keys = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (Lattice.isTardisLevel(level)) keys.add(level.dimension());
        }
        return keys;
    }
}
