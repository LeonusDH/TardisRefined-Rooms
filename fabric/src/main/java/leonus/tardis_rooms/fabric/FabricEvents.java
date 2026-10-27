package leonus.tardis_rooms.fabric;

import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceNetwork;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.room.HubInitializer;
import leonus.tardis_rooms.room.ProtectedBlock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

import java.util.ArrayList;
import java.util.List;

/** События Fabric → общий код. Здесь только перевод: что делать, решает общий код. */
final class FabricEvents {
    /** Интерфейсы, загруженные за тик: onLoad — в конце тика, когда чанк уже в мире (на NeoForge так же). */
    private static final List<InterfaceBlockEntity> LOADED = new ArrayList<>();

    static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            InterfaceTools.UseResult r = InterfaceTools.onUseBlock(player, level, hit, player.getItemInHand(hand));
            return r.handled() ? r.result() : InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            // Защищённые блоки (узел, блок управления, заглушка): в творческом режиме ломаются с одного удара — не даём начать.
            if (player.isCreative() && level.getBlockState(pos).getBlock() instanceof ProtectedBlock) {
                if (level.isClientSide()) player.displayClientMessage(TRoomsText.MSG_PROTECTED.text(), true);
                return InteractionResult.FAIL;
            }
            if (!InterfaceTools.onAttackBlock(player, level, pos)) return InteractionResult.PASS;
            if (level.isClientSide()) InterfaceTools.requestPaste(pos);
            return InteractionResult.FAIL; // FAIL: на сервер не уходит «начал ломать»
        });
        // Подстраховка на сервере: защищённый блок не ломается, даже если клиент без мода прислал «сломал».
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) ->
                !(state.getBlock() instanceof ProtectedBlock pb) || pb.onDestroyedByPlayer(state, level, pos, player, false, level.getFluidState(pos)));
        UseItemCallback.EVENT.register((player, level, hand) -> {
            var stack = player.getItemInHand(hand);
            if (player.isSpectator() || !InterfaceTools.onUseItem(player, level, stack)) return InteractionResultHolder.pass(stack);
            return InteractionResultHolder.success(stack);
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> HubInitializer.onEntityJoin(level, entity));
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
            if (be instanceof InterfaceBlockEntity ibe) LOADED.add(ibe);
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> {
            if (be instanceof InterfaceBlockEntity ibe) {
                LOADED.remove(ibe);
                ibe.onChunkUnloaded();
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!LOADED.isEmpty()) {
                List<InterfaceBlockEntity> now = new ArrayList<>(LOADED);
                LOADED.clear();
                for (InterfaceBlockEntity be : now) if (!be.isRemoved()) be.onLoad();
            }
            HubInitializer.onServerTick(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOADED.clear();
            HubInitializer.onServerStopped();
            InterfaceNetwork.clear();
        });
    }

    private FabricEvents() {}
}
