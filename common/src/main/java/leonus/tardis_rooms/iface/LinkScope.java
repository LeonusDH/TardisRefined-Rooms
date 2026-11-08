package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Область связи: к чему относится место. Интерфейсы одной области с одинаковым ID связаны; идентификаторы выдаёт
 * измерение области ({@link InterfaceIds}). Область задаёт тот, кто встраивает интерфейс (у этого аддона —
 * ТАРДИС: {@link leonus.tardis_rooms.iface.tardis.TardisLinkScope}); сам интерфейс о ТАРДИС не знает.
 */
@FunctionalInterface
public interface LinkScope {
    /** Области места: пусто — ни одной (интерфейс здесь не работает), больше одной — конфликт. */
    List<ResourceKey<Level>> scopes(Level level, BlockPos pos);

    /** Области места по текущей области связи игры. */
    static List<ResourceKey<Level>> of(Level level, BlockPos pos) {
        return LinkScopes.current().scopes(level, pos);
    }
}
