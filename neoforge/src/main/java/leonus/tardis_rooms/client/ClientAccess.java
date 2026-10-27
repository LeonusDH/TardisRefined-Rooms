package leonus.tardis_rooms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Точка входа в клиентский код из общего: вызывается только на клиенте. */
public final class ClientAccess {
    public static void openRoomSelector(BlockPos pos) {
        Minecraft.getInstance().setScreen(new RoomCategoryScreen(pos, BlockPos.ZERO));
    }

    public static void openManage(BlockPos core) {
        Minecraft.getInstance().setScreen(new RoomManageScreen(core));
    }

    public static void openInterface(BlockPos pos) {
        Minecraft.getInstance().setScreen(new InterfaceScreen(pos));
    }

    private ClientAccess() {}
}
