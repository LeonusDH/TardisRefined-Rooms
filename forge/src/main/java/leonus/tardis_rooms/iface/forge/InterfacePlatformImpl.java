package leonus.tardis_rooms.iface.forge;

import leonus.tardis_rooms.iface.AE2Compat;
import leonus.tardis_rooms.iface.ForgeInterfaceTransport;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceTransport;
import leonus.tardis_rooms.iface.MekanismCompat;

/** Реализация {@link leonus.tardis_rooms.iface.InterfacePlatform} для Forge. */
public final class InterfacePlatformImpl {
    public static InterfaceTransport createTransport(InterfaceBlockEntity be) {
        return new ForgeInterfaceTransport(be);
    }

    public static boolean chemicalsAvailable() {
        return MekanismCompat.enabled();
    }

    public static boolean meAvailable() {
        return AE2Compat.enabled();
    }

    private InterfacePlatformImpl() {}
}
