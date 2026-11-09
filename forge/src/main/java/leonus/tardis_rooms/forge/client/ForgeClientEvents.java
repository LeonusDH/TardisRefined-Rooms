package leonus.tardis_rooms.forge.client;

import leonus.tardis_rooms.client.BlockHighlights;
import leonus.tardis_rooms.client.ClientEvents;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;

/** Клиентские события Forge → общий код. */
public final class ForgeClientEvents {
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((InputEvent.MouseScrollingEvent e) -> {
            if (ClientEvents.onScroll(e.getScrollDelta())) e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) BlockHighlights.render(e.getCamera());
        });
    }

    private ForgeClientEvents() {}
}
