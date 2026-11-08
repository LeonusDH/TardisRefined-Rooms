package leonus.tardis_rooms.room;

import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.BlockPos;

/** Узел роста в проходе: отвёрткой — каталог помещений, которые можно вырастить за стеной. */
public class RoomNodeBlock extends ScrewdriverBlock {
    public RoomNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public TRoomsText breakMessage() {
        return TRoomsText.MSG_USE_SCREWDRIVER;
    }

    @Override
    protected void openScreen(BlockPos pos) {
        leonus.tardis_rooms.client.ClientAccess.openRoomSelector(pos);
    }
}
