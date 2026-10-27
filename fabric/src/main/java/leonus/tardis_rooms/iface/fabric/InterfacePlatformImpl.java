package leonus.tardis_rooms.iface.fabric;

import leonus.tardis_rooms.iface.FabricInterfaceTransport;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceTransport;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Реализация {@link leonus.tardis_rooms.iface.InterfacePlatform} для Fabric.
 * Химикатов нет (Mekanism на Fabric нет); AE2 пока нет (шаг 1.6).
 */
public final class InterfacePlatformImpl {
    public static InterfaceTransport createTransport(InterfaceBlockEntity be) {
        return new FabricInterfaceTransport(be);
    }

    public static boolean chemicalsAvailable() {
        return false;
    }

    public static boolean meAvailable() {
        return false;
    }

    public static boolean isMemoryCard(ItemStack stack) {
        return false;
    }

    public static ItemInteractionResult useMemoryCard(ItemStack stack, InterfaceBlockEntity be, Player player) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InterfacePlatformImpl() {}
}
