package leonus.tardis_rooms.iface.kind;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Часть вида передачи в одном интерфейсе: его состояние (соседи, представление для труб) и хуки жизненного цикла.
 * Создаётся вместе с интерфейсом ({@link TransferKind.PartFactory}). Хуки, которые виду не нужны, можно не реализовывать.
 * Все хуки, кроме {@link #useItem} и {@link #signal}, — только на сервере.
 */
public interface KindPart {
    /**
     * Грань «вставлять» ({@code pull = false}) или «забирать» ({@code pull = true}): одна операция с соседом
     * со своей стороны {@code face}. Вызывается, только если вид включён и доступен.
     */
    default void moveFace(Direction face, boolean pull) {}

    /** Настройки интерфейса изменились (виды, направление, связь, стороны). */
    default void invalidate() {}

    /** Раз в секунду, после пересчёта связи. */
    default void tick() {}

    /** Соседний блок изменился. */
    default void neighborChanged() {}

    /**
     * Сила редстоун-сигнала, который интерфейс выдаёт со своей стороны {@code side} (сторона уже открыта).
     * Сигнал интерфейса — наибольший среди частей.
     */
    default int signal(Direction side) { return 0; }

    /**
     * Игрок применил предмет к интерфейсу. Вызывается, если вид доступен (даже когда у этого интерфейса он выключен).
     * null — предмет этому виду не нужен, спросить следующий.
     */
    default @Nullable InteractionResult useItem(ItemStack stack, Player player) { return null; }

    /** Чанк с интерфейсом выгружается. Блок остаётся в мире: соседей и связанных не трогать. */
    default void unload() {}

    /** Интерфейс убирают из мира (сломан, заменён): ещё связан, сейчас покинет сеть. */
    default void remove() {}
}
