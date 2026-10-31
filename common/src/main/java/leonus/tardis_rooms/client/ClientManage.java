package leonus.tardis_rooms.client;

import leonus.tardis_rooms.network.ManagePayloads;
import net.minecraft.client.Minecraft;

/** Сведения о помещении от сервера — открытому экрану блока управления. */
public final class ClientManage {
    public static void accept(ManagePayloads.Info i) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof RoomManageScreen.RoomRenameScreen r) screen = r.parent; // ответ на переименование
        if (screen instanceof RoomManageScreen s && s.core.equals(i.core())) s.onInfo(i);
    }

    private ClientManage() {}
}
