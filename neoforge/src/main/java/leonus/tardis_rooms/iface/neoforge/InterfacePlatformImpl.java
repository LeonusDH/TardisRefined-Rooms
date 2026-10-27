package leonus.tardis_rooms.iface.neoforge;

import leonus.tardis_rooms.iface.AE2Compat;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceTransport;
import leonus.tardis_rooms.iface.MekanismCompat;
import leonus.tardis_rooms.iface.NeoForgeInterfaceTransport;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Реализация {@link leonus.tardis_rooms.iface.InterfacePlatform} для NeoForge. */
public final class InterfacePlatformImpl {
    public static InterfaceTransport createTransport(BlockEntity be) {
        return new NeoForgeInterfaceTransport((InterfaceBlockEntity) be);
    }

    public static boolean chemicalsAvailable() {
        return MekanismCompat.enabled();
    }

    public static boolean meAvailable() {
        return AE2Compat.enabled();
    }

    public static boolean isMemoryCard(ItemStack stack) {
        return AE2Compat.isMemoryCard(stack);
    }

    public static ItemInteractionResult useMemoryCard(ItemStack stack, BlockEntity be, Player player) {
        return AE2Compat.useMemoryCard(stack, (InterfaceBlockEntity) be, player);
    }

    private InterfacePlatformImpl() {}
}
