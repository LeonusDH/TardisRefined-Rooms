package leonus.tardis_rooms.iface;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Соседи интерфейса одного вида на Fabric: по {@link BlockApiCache} на сторону. Кэш создаётся при первом обращении
 * (только на сервере); Fabric сам следит, когда соседа ставят, ломают или меняют.
 */
final class ApiSides<T> implements LinkedStorages.Sides<T> {
    private final InterfaceBlockEntity be;
    private final BlockApiLookup<T, Direction> lookup;
    private final Map<Direction, BlockApiCache<T, Direction>> caches = new EnumMap<>(Direction.class);

    ApiSides(InterfaceBlockEntity be, BlockApiLookup<T, Direction> lookup) {
        this.be = be;
        this.lookup = lookup;
    }

    @Override
    public @Nullable T at(Direction side) {
        if (be.isRemoved()) {
            // Убранному блоку соседи не нужны; вернётся в мир — создадим кэши заново.
            caches.clear();
            return null;
        }
        BlockApiCache<T, Direction> c = caches.get(side);
        if (c == null) {
            if (!(be.getLevel() instanceof ServerLevel level)) return null;
            c = BlockApiCache.create(lookup, level, be.getBlockPos().relative(side));
            caches.put(side, c);
        }
        return c.find(side.getOpposite());
    }
}
