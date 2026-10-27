package leonus.tardis_rooms.client;

import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.network.ScrewdriverModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import leonus.tardis_rooms.platform.Platform;

/** События только клиента. Подписывает на них платформа. */
public final class ClientEvents {
    /**
     * Прокрутка колёсика вне экранов. Shift + колёсико с отвёрткой в руке — сменить режим отвёртки.
     * {@code true} — событие поглощено (слот хотбара при этом не листается).
     */
    public static boolean onScroll(double deltaY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().screen != null) return false;
        if (!player.isShiftKeyDown() || !InterfaceTools.isScrewdriver(player.getMainHandItem())) return false;
        if (deltaY == 0) return false;
        Platform.sendToServer(new ScrewdriverModePayload(deltaY > 0 ? 1 : -1));
        return true;
    }

    private ClientEvents() {}
}
