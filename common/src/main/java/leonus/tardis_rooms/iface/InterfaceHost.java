package leonus.tardis_rooms.iface;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Что интерфейсу даёт мод, в который он встроен: область связи и инструмент настройки (им открывается меню).
 * Сам интерфейс о ТАРДИС не знает; у этого аддона — {@link leonus.tardis_rooms.iface.tardis.TardisInterfaces}.
 */
public record InterfaceHost(LinkScope scope, Predicate<ItemStack> configurator) {
    /** Пока ничего не задано: интерфейсы нигде не работают, меню ничем не открывается. */
    public static final InterfaceHost NONE = new InterfaceHost((level, pos) -> List.of(), stack -> false);

    private static volatile InterfaceHost current = NONE;

    public InterfaceHost {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(configurator);
    }

    public static InterfaceHost current() { return current; }

    /** Задаётся при запуске мода; игровые тесты подставляют свою область. */
    public static void set(InterfaceHost host) { current = Objects.requireNonNull(host); }

    /** Инструмент настройки интерфейса (открывает меню, хранит ID). */
    public static boolean isConfigurator(ItemStack stack) { return current.configurator.test(stack); }
}
