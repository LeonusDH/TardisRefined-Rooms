package leonus.tardis_rooms.iface.tardis;

import leonus.tardis_rooms.Screwdriver;
import leonus.tardis_rooms.iface.InterfaceHost;
import leonus.tardis_rooms.iface.LinkScope;
import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.iface.kind.TransferKinds;
import leonus.tardis_rooms.room.RoomPermissions;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

/**
 * Как этот аддон встраивает интерфейс: встроенные виды передачи, область связи — ТАРДИС, настройка — звуковой отвёрткой,
 * права — те же, что на помещения ({@link RoomPermissions}): у оболочки нужно право в ТАРДИС этой оболочки.
 */
public final class TardisInterfaces {
    private TardisInterfaces() {}

    /**
     * При запуске мода, раньше всего остального: номера видов нужны интерфейсам с первого же блока.
     * {@code parts} — части встроенных видов на этой платформе.
     */
    public static void bootstrap(Function<ResourceLocation, TransferKind.PartFactory> parts) {
        TransferKinds.bootstrap(List.of(BuiltinKinds.plugin(parts)));
        InterfaceHost.set(new InterfaceHost(TardisLinkScope.INSTANCE, Screwdriver::is, (player, pos, what) -> RoomPermissions.allowedInAll(
                player, LinkScope.of(player.level(), pos), RoomPermissions.Action.CONFIGURE_INTERFACE, what)));
    }
}
