package leonus.tardis_rooms.neoforge;

import leonus.tardis_rooms.iface.InterfaceNetwork;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.room.HubInitializer;
import leonus.tardis_rooms.room.ProtectedBlock;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** События NeoForge → общий код. Здесь только перевод: что делать, решает общий код. */
final class NeoForgeEvents {
    static void register(IEventBus bus) {
        bus.addListener(NeoForgeEvents::onRightClickBlock);
        bus.addListener(NeoForgeEvents::onLeftClickBlock);
        bus.addListener(NeoForgeEvents::onRightClickItem);
        bus.addListener((ItemTooltipEvent e) -> InterfaceTools.appendTooltip(e.getItemStack(), e.getToolTip()));
        bus.addListener((EntityJoinLevelEvent e) -> HubInitializer.onEntityJoin(e.getLevel(), e.getEntity()));
        bus.addListener((ServerTickEvent.Post e) -> HubInitializer.onServerTick(e.getServer()));
        bus.addListener((ServerStoppedEvent e) -> {
            HubInitializer.onServerStopped();
            InterfaceNetwork.clear();
        });
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        InterfaceTools.UseResult r = InterfaceTools.onUseBlock(e.getEntity(), e.getLevel(), e.getHitVec(), e.getItemStack());
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
        if (!InterfaceTools.onAttackBlock(e.getEntity(), e.getLevel(), e.getPos())) return;
        e.setCanceled(true);
        if (e.getLevel().isClientSide() && e.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            InterfaceTools.requestPaste(e.getPos());
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (!InterfaceTools.onUseItem(e.getEntity(), e.getLevel(), e.getItemStack())) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    private NeoForgeEvents() {}
}
