package leonus.tardis_rooms.iface;

import dev.architectury.injectables.annotations.ExpectPlatform;

/** Интерфейс ТАРДИС: то, что у каждого загрузчика своё. Реализация — {@code leonus.tardis_rooms.iface.<платформа>.InterfacePlatformImpl}. */
public final class InterfacePlatform {
    /** Передача ресурсов для этого интерфейса. */
    @ExpectPlatform
    public static InterfaceTransport createTransport(InterfaceBlockEntity be) {
        throw new AssertionError();
    }

    /** Есть ли химикаты (Mekanism) и включены ли они в конфиге. */
    @ExpectPlatform
    public static boolean chemicalsAvailable() {
        throw new AssertionError();
    }

    /** Есть ли сеть ME (AE2) и включена ли она в конфиге. */
    @ExpectPlatform
    public static boolean meAvailable() {
        throw new AssertionError();
    }

    private InterfacePlatform() {}
}
