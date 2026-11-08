package leonus.tardis_rooms.room;

import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.BlockPos;

/** Блок управления помещением, встроенный в пол: отвёрткой — заменить или удалить помещение. */
public class RoomCoreBlock extends ScrewdriverBlock {
    public RoomCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public TRoomsText breakMessage() {
        return TRoomsText.MSG_USE_SCREWDRIVER_CORE;
    }

    @Override
    protected void openScreen(BlockPos pos) {
        leonus.tardis_rooms.client.ClientAccess.openManage(pos);
    }
}
