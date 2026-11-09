package leonus.tardis_rooms.fabric.client;

import leonus.tardis_rooms.iface.tardis.ScrewdriverTools;
import leonus.tardis_rooms.network.Payload;
import leonus.tardis_rooms.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Клиентская точка входа на Fabric. Прокрутку колёсика ловит MouseHandlerMixin (события для неё в Fabric API нет). */
public final class TardisRoomsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (Payloads.Entry<?> e : Payloads.ALL) if (!e.toServer()) client(e);
        ItemTooltipCallback.EVENT.register((stack, flag, lines) -> ScrewdriverTools.appendTooltip(stack, lines));
    }

    /** Пакет с сервера: обработчик — на главном потоке клиента. */
    private static <T extends Payload> void client(Payloads.Entry<T> e) {
        ClientPlayNetworking.registerGlobalReceiver(e.id(), (client, handler, buf, sender) -> {
            T msg = e.decode(buf);
            client.execute(() -> e.client().accept(msg));
        });
    }
}
