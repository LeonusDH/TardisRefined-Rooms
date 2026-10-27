package leonus.tardis_rooms.fabric.client;

import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.network.CustomRoomsPayload;
import leonus.tardis_rooms.network.FitResultPayload;
import leonus.tardis_rooms.network.ManagePayloads;
import leonus.tardis_rooms.network.MapPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Consumer;

/** Клиентская точка входа на Fabric. Прокрутку колёсика ловит MouseHandlerMixin (события для неё в Fabric API нет). */
public final class TardisRoomsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        client(FitResultPayload.TYPE, FitResultPayload::handle);
        client(ManagePayloads.Info.TYPE, ManagePayloads.Info::handle);
        client(MapPayloads.Info.TYPE, MapPayloads.Info::handle);
        client(CustomRoomsPayload.TYPE, CustomRoomsPayload::handle);
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> InterfaceTools.appendTooltip(stack, lines));
    }

    /** Пакет с сервера: обработчик — на главном потоке клиента. */
    private static <T extends CustomPacketPayload> void client(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> ctx.client().execute(() -> handler.accept(payload)));
    }
}
