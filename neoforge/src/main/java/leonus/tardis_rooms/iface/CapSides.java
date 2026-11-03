package leonus.tardis_rooms.iface;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Соседи интерфейса одного вида на NeoForge: по {@link BlockCapabilityCache} на сторону. Кэш создаётся при первом
 * обращении (только на сервере) и обновляется сам, когда соседа ставят, ломают, меняют или выгружают его чанк.
 */
final class CapSides<T> implements LinkedStorages.Sides<T> {
    private final InterfaceBlockEntity be;
    private final BlockCapability<T, Direction> cap;
    private final Map<Direction, BlockCapabilityCache<T, Direction>> caches = new EnumMap<>(Direction.class);

    CapSides(InterfaceBlockEntity be, BlockCapability<T, Direction> cap) {
        this.be = be;
        this.cap = cap;
    }

    @Override
    public @Nullable T at(Direction side) {
        if (be.isRemoved()) {
            // Убранный блок кэши больше не обслуживают (игра сняла их с учёта); вернётся в мир — создадим заново.
            caches.clear();
            return null;
        }
        BlockCapabilityCache<T, Direction> c = caches.get(side);
        if (c == null) {
            if (!(be.getLevel() instanceof ServerLevel level)) return null;
            c = BlockCapabilityCache.create(cap, level, be.getBlockPos().relative(side), side.getOpposite(), () -> !be.isRemoved(), () -> {});
            caches.put(side, c);
        }
        return c.getCapability();
    }
}
