package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/** Химикаты Mekanism на Forge 1.20.1 — следующий шаг бэкпорта (у Mekanism 10.4 другой API). Пока выключено. */
public final class MekanismCompat {
    public static boolean enabled() {
        return false;
    }

    static Object newView(InterfaceBlockEntity be, ForgeInterfaceTransport transport) {
        return null;
    }

    static <T> LazyOptional<T> capability(InterfaceBlockEntity be, ForgeInterfaceTransport transport, Capability<T> cap) {
        return LazyOptional.empty();
    }

    static void invalidate(ForgeInterfaceTransport transport) {}

    static void moveFace(InterfaceBlockEntity be, ForgeInterfaceTransport transport, BlockPos np, Direction side, boolean pull, int max) {}

    private MekanismCompat() {}
}
