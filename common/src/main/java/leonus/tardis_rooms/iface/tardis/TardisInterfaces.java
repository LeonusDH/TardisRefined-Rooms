package leonus.tardis_rooms.iface.tardis;

import leonus.tardis_rooms.iface.LinkScopes;
import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.iface.kind.TransferKinds;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Function;

/** Как этот аддон встраивает интерфейс: встроенные виды передачи и область связи — ТАРДИС. */
public final class TardisInterfaces {
    private TardisInterfaces() {}

    /**
     * При запуске мода, раньше всего остального: номера видов нужны интерфейсам с первого же блока.
     * {@code parts} — части встроенных видов на этой платформе.
     */
    public static void bootstrap(Function<ResourceLocation, TransferKind.PartFactory> parts) {
        TransferKinds.bootstrap(List.of(BuiltinKinds.plugin(parts)));
        LinkScopes.set(TardisLinkScope.INSTANCE);
    }
}
