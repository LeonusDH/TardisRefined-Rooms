package leonus.tardis_rooms.iface.tardis;

import leonus.tardis_rooms.iface.LinkScope;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import whocraft.tardis_refined.common.blockentity.shell.ShellBaseBlockEntity;
import whocraft.tardis_refined.registry.TRDimensionTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Область связи — ТАРДИС (Tardis Refined). В ТАРДИС — сама ТАРДИС; снаружи — ТАРДИС, к оболочке которой интерфейс
 * стоит вплотную (несколько разных — конфликт).
 */
public final class TardisLinkScope implements LinkScope {
    public static final TardisLinkScope INSTANCE = new TardisLinkScope();

    private TardisLinkScope() {}

    @Override
    public List<ResourceKey<Level>> scopes(Level level, BlockPos pos) {
        if (TRDimensionTypes.isTARDISDimension(level)) return List.of(level.dimension());
        return nearbyTardises(level, pos);
    }

    /** Разные ТАРДИС, чьи оболочки стоят вплотную к блоку (куб 3×3×3 и на блок ниже — оболочка высотой 2). */
    private static List<ResourceKey<Level>> nearbyTardises(Level level, BlockPos pos) {
        List<ResourceKey<Level>> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -2, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockEntity(p) instanceof ShellBaseBlockEntity shell) {
                ResourceKey<Level> id = shell.getTardisId();
                if (id != null && !out.contains(id)) out.add(id);
            }
        }
        return out;
    }
}
