package leonus.tardis_rooms.iface.fabric;

import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceTransport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Реализация {@link leonus.tardis_rooms.iface.InterfacePlatform} для Fabric.
 * Пока без передачи ресурсов (шаг 1.5 — Transfer API), без химикатов (Mekanism на Fabric нет) и без AE2 (шаг 1.6).
 */
public final class InterfacePlatformImpl {
    private static final InterfaceTransport NONE = new InterfaceTransport() {
        @Override public void moveFace(BlockPos neighbour, Direction side, boolean pull) {}
        @Override public void invalidate() {}
        @Override public void tick() {}
        @Override public void remove() {}
    };

    public static InterfaceTransport createTransport(InterfaceBlockEntity be) {
        return NONE;
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
