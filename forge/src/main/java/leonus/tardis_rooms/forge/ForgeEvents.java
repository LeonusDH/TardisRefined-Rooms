package leonus.tardis_rooms.forge;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.command.TRoomsCommands;
import leonus.tardis_rooms.iface.InterfaceNetwork;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.iface.tardis.ScrewdriverTools;
import leonus.tardis_rooms.room.ForceRemoval;
import leonus.tardis_rooms.room.HubInitializer;
import leonus.tardis_rooms.room.ProtectedBlock;
import leonus.tardis_rooms.room.custom.CustomRooms;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ConfigTracker;

/** События Forge → общий код. Здесь только перевод: что делать, решает общий код. */
final class ForgeEvents {
    static void register(IEventBus bus) {
        bus.addListener(ForgeEvents::onRightClickBlock);
        bus.addListener(ForgeEvents::onLeftClickBlock);
        bus.addListener(ForgeEvents::onRightClickItem);
        // Интерфейс ломает только тот, кому можно его настраивать (событие — на сервере, до поломки).
        bus.addListener((BlockEvent.BreakEvent e) -> {
            if (!InterfaceTools.onBreakBlock(e.getPlayer(), e.getPos(), e.getState())) e.setCanceled(true);
        });
        bus.addListener((ItemTooltipEvent e) -> ScrewdriverTools.appendTooltip(e.getItemStack(), e.getToolTip()));
        bus.addListener((AddReloadListenerEvent e) -> e.addListener(CustomRooms.INSTANCE));
        bus.addListener((OnDatapackSyncEvent e) -> {
            if (e.getPlayer() != null) CustomRooms.sync(e.getPlayer());
            else {
                e.getPlayerList().getPlayers().forEach(CustomRooms::sync);
                var removable = ConfigTracker.INSTANCE.fileMap().get(TardisRooms.REMOVABLE_CONFIG);
                if (removable != null) TardisRoomsConfig.reloadRemovable(removable.getConfigData());
            }
        });
        bus.addListener((RegisterCommandsEvent e) -> TRoomsCommands.register(e.getDispatcher()));
        bus.addListener((EntityJoinLevelEvent e) -> HubInitializer.onEntityJoin(e.getLevel(), e.getEntity()));
        bus.addListener((TickEvent.ServerTickEvent e) -> { if (e.phase == TickEvent.Phase.END) HubInitializer.onServerTick(e.getServer()); });
        bus.addListener((ServerStartedEvent e) -> TardisRooms.onServerStarted());
        bus.addListener((ServerStoppedEvent e) -> {
            HubInitializer.onServerStopped();
            InterfaceNetwork.clear();
            ForceRemoval.clear();
        });
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        InterfaceTools.UseResult r = InterfaceTools.onUseBlock(e.getEntity(), e.getLevel(), e.getHitVec(), e.getItemStack());
        if (!r.handled()) r = ScrewdriverTools.onUseBlock(e.getEntity(), e.getLevel(), e.getHitVec(), e.getItemStack());
        if (!r.handled()) return;
        e.setCanceled(true);
        e.setCancellationResult(r.result());
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        // Защищённые блоки (узел, блок управления, заглушка) не даём даже начать ломать.
        if (ProtectedBlock.onAttack(e.getEntity(), e.getLevel(), e.getPos())) {
            e.setCanceled(true);
            return;
        }
        if (!ScrewdriverTools.onAttackBlock(e.getEntity(), e.getLevel(), e.getPos())) return;
        e.setCanceled(true);
        if (e.getLevel().isClientSide() && e.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            ScrewdriverTools.requestPaste(e.getPos());
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (!ScrewdriverTools.onUseItem(e.getEntity(), e.getLevel(), e.getItemStack())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    private ForgeEvents() {}
}
