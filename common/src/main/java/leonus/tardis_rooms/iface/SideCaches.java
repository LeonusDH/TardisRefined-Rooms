package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Соседи интерфейса одного вида: по кэшу загрузчика на сторону (Forge — свой ForgeInterfaceTransport.CapCache, Fabric — BlockApiCache).
 * Кэш создаётся при первом обращении, только на сервере, и обновляется сам, когда соседа ставят, ломают или меняют.
 * Платформа даёт только, как кэш создать ({@link Factory}) и как спросить у него соседа ({@link Query}).
 */
public final class SideCaches<T, C> implements LinkedStorages.Sides<T> {
    /** Кэш для соседа {@code pos}, примыкающего к своей стороне {@code side}. */
    @FunctionalInterface
    public interface Factory<C> {
        C create(ServerLevel level, BlockPos pos, Direction side);
    }

    /** Хранилище соседа из кэша (своя сторона — {@code side}), или null. */
    @FunctionalInterface
    public interface Query<T, C> {
        @Nullable T get(C cache, Direction side);
    }

    private final InterfaceBlockEntity be;
    private final Factory<C> factory;
    private final Query<T, C> query;
    private final Map<Direction, C> caches = new EnumMap<>(Direction.class);

    public SideCaches(InterfaceBlockEntity be, Factory<C> factory, Query<T, C> query) {
        this.be = be;
        this.factory = factory;
        this.query = query;
    }

    @Override
    public @Nullable T at(Direction side) {
        if (be.isRemoved()) {
            // Убранному блоку соседи не нужны; вернётся в мир — создадим заново.
            caches.clear();
            return null;
        }
        C c = caches.get(side);
        if (c == null) {
            if (!(be.getLevel() instanceof ServerLevel level)) return null;
            c = factory.create(level, be.getBlockPos().relative(side), side);
            caches.put(side, c);
        }
        return query.get(c, side);
    }
}
