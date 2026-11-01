package leonus.tardis_rooms.neoforge.client;

import leonus.tardis_rooms.client.ClientEvents;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Клиентские события NeoForge → общий код. */
public final class NeoForgeClientEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((InputEvent.MouseScrollingEvent e) -> {
            if (ClientEvents.onScroll(e.getScrollDeltaY())) e.setCanceled(true);
        });
    }

    private NeoForgeClientEvents() {}
}
