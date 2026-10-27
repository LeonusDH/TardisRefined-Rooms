package leonus.tardis_rooms.fabric;

import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceNetwork;
import leonus.tardis_rooms.iface.InterfaceTools;
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


/** События Fabric → общий код. Здесь только перевод: что делать, решает общий код. */
final class FabricEvents {
    static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            InterfaceTools.UseResult r = InterfaceTools.onUseBlock(player, level, hit, player.getItemInHand(hand));
            if (!r.handled()) return InteractionResult.PASS;
            // На Fabric PASS значит «продолжай как обычно» (сработали бы блок и предмет), а нам нужно «стоп»: FAIL.
            return r.result() == InteractionResult.PASS ? InteractionResult.FAIL : r.result();
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            // Защищённые блоки (узел, блок управления, заглушка) не даём даже начать ломать.
            if (ProtectedBlock.onAttack(player, level, pos)) return InteractionResult.FAIL;
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
        // onLoad только отмечает «пересчитать связь на ближайшем тике», поэтому его можно звать прямо при загрузке чанка.
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
            if (be instanceof InterfaceBlockEntity ibe) ibe.onLoad();
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> {
            if (be instanceof InterfaceBlockEntity ibe) ibe.onChunkUnloaded();
        });
        ServerTickEvents.END_SERVER_TICK.register(HubInitializer::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HubInitializer.onServerStopped();
            InterfaceNetwork.clear();
        });
    }

    private FabricEvents() {}
}
