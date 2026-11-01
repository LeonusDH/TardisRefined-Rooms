package leonus.tardis_rooms.iface;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Сеть ME и карта памяти AE2 на Forge 1.20.1 — следующий шаг бэкпорта (у AE2 15 другой API). Пока выключено. */
public final class AE2Compat {
    public static boolean enabled() {
        return false;
    }

    static void update(InterfaceBlockEntity be) {}

    static void destroy(InterfaceBlockEntity be) {}

    public static boolean isMemoryCard(ItemStack stack) {
        return false;
    }

    public static InteractionResult useMemoryCard(ItemStack stack, InterfaceBlockEntity be, Player player) {
        return InteractionResult.PASS;
    }

    private AE2Compat() {}
}
