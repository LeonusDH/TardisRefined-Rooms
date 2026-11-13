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

    /** Кому можно ставить, снимать и настраивать интерфейс. При отказе сам объясняет игроку почему. */
    public interface Access {
        Access ALLOW_ALL = new Access() {
            @Override public boolean mayConfigure(ServerPlayer player, BlockPos pos, String what) { return true; }

            @Override public boolean mayConfigureOnClient(Player player, BlockPos pos) { return true; }
        };

        /** Сервер: решение для места {@code pos} (в мире игрока); {@code what} — что игрок пытался сделать (для лога). */
        boolean mayConfigure(ServerPlayer player, BlockPos pos, String what);

        /** Клиент: то же по сведениям сервера — чтобы не открывать меню и не ставить блок зря. Решает всё равно сервер. */
        boolean mayConfigureOnClient(Player player, BlockPos pos);
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

    /** См. {@link Access#mayConfigure}. */
    public static boolean mayConfigure(ServerPlayer player, BlockPos pos, String what) {
        return current.access.mayConfigure(player, pos, what);
    }

    /** См. {@link Access#mayConfigureOnClient}. */
    public static boolean mayConfigureOnClient(Player player, BlockPos pos) {
        return current.access.mayConfigureOnClient(player, pos);
    }
}
