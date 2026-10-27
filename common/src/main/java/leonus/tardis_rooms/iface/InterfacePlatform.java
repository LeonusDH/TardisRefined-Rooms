package leonus.tardis_rooms.iface;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

    /** Карта памяти AE2 (копирует и вставляет ID интерфейса). */
    @ExpectPlatform
    public static boolean isMemoryCard(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ItemInteractionResult useMemoryCard(ItemStack stack, InterfaceBlockEntity be, Player player) {
        throw new AssertionError();
    }

    private InterfacePlatform() {}
}
