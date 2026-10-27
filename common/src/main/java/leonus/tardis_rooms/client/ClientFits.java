package leonus.tardis_rooms.client;

import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.FitResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/** Последний ответ сервера «что влезает» для открытого узла. */
public final class ClientFits {
    private static BlockPos pos;
    private static final Set<ResourceLocation> BLOCKED = new HashSet<>();
    private static final Set<ResourceLocation> OFF_GRID = new HashSet<>();
    private static final Set<ResourceLocation> CUTS_OFF = new HashSet<>();

    public static void reset() {
        pos = null;
        BLOCKED.clear(); OFF_GRID.clear(); CUTS_OFF.clear();
    }

    public static void accept(FitResultPayload p) {
        pos = p.pos();
        BLOCKED.clear(); BLOCKED.addAll(p.blocked());
        OFF_GRID.clear(); OFF_GRID.addAll(p.offGrid());
        CUTS_OFF.clear(); CUTS_OFF.addAll(p.cutsOff());
        if (Minecraft.getInstance().screen instanceof RoomMonitorScreen s && pos.equals(s.nodePos)) s.onDataChanged();
    }

    public static boolean known(BlockPos nodePos) { return nodePos != null && nodePos.equals(pos); }
    public static boolean available(ResourceLocation id) { return !BLOCKED.contains(id) && !OFF_GRID.contains(id) && !CUTS_OFF.contains(id); }

    /** Причина, почему вариант недоступен. */
    public static TRoomsText reason(ResourceLocation id, leonus.tardis_rooms.room.Node node) {
        if (OFF_GRID.contains(id)) {
            // У помещений из датапаков пустой план — не то направление (поворачивать нельзя) или не хватает высоты.
            for (leonus.tardis_rooms.room.custom.CustomRoomInfo r : ClientCustomRooms.list())
                if (r.id().equals(id))
                    return r.fitsDirection(node.side()) ? TRoomsText.SCREEN_CUSTOM_NO_HEIGHT : TRoomsText.SCREEN_CUSTOM_DIRECTION;
            return TRoomsText.SCREEN_OFF_GRID;
        }
        if (CUTS_OFF.contains(id)) return TRoomsText.SCREEN_CUTS_OFF;
        return TRoomsText.SCREEN_BLOCKED;
    }

    private ClientFits() {}
}
