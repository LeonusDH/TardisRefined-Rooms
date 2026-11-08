package leonus.tardis_rooms.iface.neoforge;

import leonus.tardis_rooms.iface.AE2Compat;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceTransport;
import leonus.tardis_rooms.iface.MekanismCompat;
import leonus.tardis_rooms.iface.NeoForgeInterfaceTransport;

/** Реализация {@link leonus.tardis_rooms.iface.InterfacePlatform} для NeoForge. */
public final class InterfacePlatformImpl {
    public static InterfaceTransport createTransport(InterfaceBlockEntity be) {
        return new NeoForgeInterfaceTransport(be);
    }

    public static boolean chemicalsAvailable() {
        return MekanismCompat.enabled();
    }

    public static boolean meAvailable() {
        return AE2Compat.enabled();
    }

    private InterfacePlatformImpl() {}
}
