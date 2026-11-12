package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Что интерфейсу даёт мод, в который он встроен: область связи, инструмент настройки (им открывается меню) и права —
 * кому можно ставить, снимать и настраивать интерфейс. Сам интерфейс о ТАРДИС не знает; у этого аддона —
 * {@link leonus.tardis_rooms.iface.tardis.TardisInterfaces}.
 */
public record InterfaceHost(LinkScope scope, Predicate<ItemStack> configurator, Access access) {
    /** Пока ничего не задано: интерфейсы нигде не работают, меню ничем не открывается. */
    public static final InterfaceHost NONE = new InterfaceHost((level, pos) -> List.of(), stack -> false, Access.ALLOW_ALL);

    private static volatile InterfaceHost current = NONE;

    /**
     * Можно ли игроку поставить, снять или настроить интерфейс на месте {@code pos} (в его мире). При отказе сам
     * объясняет игроку почему; {@code what} — что игрок пытался сделать (для лога). Только на сервере.
     */
    @FunctionalInterface
    public interface Access {
        Access ALLOW_ALL = (player, pos, what) -> true;

        boolean mayConfigure(ServerPlayer player, BlockPos pos, String what);
    }

    public InterfaceHost {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(configurator);
        Objects.requireNonNull(access);
    }

    public static InterfaceHost current() { return current; }

    /** Задаётся при запуске мода; игровые тесты подставляют свою область. */
    public static void set(InterfaceHost host) { current = Objects.requireNonNull(host); }

    /** Инструмент настройки интерфейса (открывает меню, хранит ID). */
    public static boolean isConfigurator(ItemStack stack) { return current.configurator.test(stack); }

    /** Инструмент настройки в любой руке. */
    public static boolean holdsConfigurator(Player player) {
        return isConfigurator(player.getMainHandItem()) || isConfigurator(player.getOffhandItem());
    }

    /** См. {@link Access}. */
    public static boolean mayConfigure(ServerPlayer player, BlockPos pos, String what) {
        return current.access.mayConfigure(player, pos, what);
    }
}
