package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TardisRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Область связи: к какой ТАРДИС относится место. Интерфейсы одной области с одинаковым ID связаны.
 * В ТАРДИС — сама ТАРДИС, снаружи — ТАРДИС, к оболочке которой интерфейс стоит вплотную (несколько — конфликт).
 */
public final class LinkScope {
    @FunctionalInterface
    public interface Finder {
        /** ТАРДИС, к которым относится место: пусто — ни к одной, больше одной — конфликт. */
        List<ResourceKey<Level>> tardises(Level level, BlockPos pos);
    }

    private static Finder finder = LinkScope::tardisRefined;

    private LinkScope() {}

    public static List<ResourceKey<Level>> of(Level level, BlockPos pos) {
        return finder.tardises(level, pos);
    }

    /** Только для игровых тестов: там нет ТАРДИС, и область задаёт сам тест. */
    public static void overrideForTests(Finder f) {
        finder = f;
    }

    private static List<ResourceKey<Level>> tardisRefined(Level level, BlockPos pos) {
        if (TardisRooms.isTardisDimension(level)) return List.of(level.dimension());
        return InterfaceBlockEntity.nearbyTardises(level, pos);
    }
}
