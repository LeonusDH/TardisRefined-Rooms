package leonus.tardis_rooms.iface.fabric;

import leonus.tardis_rooms.iface.AE2Compat;
import leonus.tardis_rooms.iface.FabricInterfaceTransport;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceTransport;

/**
 * Реализация {@link leonus.tardis_rooms.iface.InterfacePlatform} для Fabric.
 * Химикатов нет (Mekanism на Fabric нет); сеть ME и карта памяти — через AE2 ({@link AE2Compat}).
 */
public final class InterfacePlatformImpl {
    public static InterfaceTransport createTransport(InterfaceBlockEntity be) {
        return new FabricInterfaceTransport();
    }

    public static boolean chemicalsAvailable() {
        return false;
    }

    public static boolean meAvailable() {
        return AE2Compat.enabled();
    }

    private InterfacePlatformImpl() {}
}
