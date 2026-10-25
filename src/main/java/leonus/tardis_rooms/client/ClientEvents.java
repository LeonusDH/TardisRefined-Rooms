package leonus.tardis_rooms.client;

import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.network.ScrewdriverModePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** События только клиента. */
public final class ClientEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientEvents::onScroll);
    }

    /** Shift + колёсико с отвёрткой в руке — сменить режим отвёртки (слот хотбара при этом не листается). */
    private static void onScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().screen != null) return;
        if (!player.isShiftKeyDown() || !InterfaceTools.isScrewdriver(player.getMainHandItem())) return;
        double dy = event.getScrollDeltaY();
        if (dy == 0) return;
        event.setCanceled(true);
        PacketDistributor.sendToServer(new ScrewdriverModePayload(dy > 0 ? 1 : -1));
    }

    private ClientEvents() {}
}
