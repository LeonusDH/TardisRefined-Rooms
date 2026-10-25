package leonus.tardis_rooms.room;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import whocraft.tardis_refined.registry.TRBlockRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Связность застройки с хабом — обход от хаба по проходам.
 * Узлы: «корень» — хаб; пристроенное помещение — целиком; модуль чужой постройки (например, комнаты,
 * сгенерированные TR до установки аддона) — сам по себе.
 * Ребро — проход между соседними модулями: у помещений — по плану постройки, у остальных — по блокам.
 */
public final class Connectivity {
    private static final Object ROOT = "root";
    private static final Object VIRTUAL = "virtual";

    /** Модуль чужой постройки (не камень TARDIS, но и не наш): свой узел графа. */
    private record Native(long module, int floor) {}

    private record ModFloor(long module, int floor) {}

    /** Условия одного обхода: какое помещение считается снесённым и какой план стоит на его месте. */
    private record Scene(ServerLevel level, ModuleRegistry reg, Integer excluded, PieceCatalog.Plan virtual) {}

    // ---------- кому принадлежит модуль ----------

    private static Object owner(Scene s, long m, int floor) {
        if (s.virtual() != null && s.virtual().footprint().getOrDefault(floor, Set.of()).contains(m)) return VIRTUAL;
        int mx = Lattice.keyX(m), mz = Lattice.keyZ(m);
        if (Lattice.isHub(s.level(), mx, mz, floor)) return ROOT;
        ModuleRegistry.Piece p = s.reg().ownerOf(m, floor);
        if (p != null) return (s.excluded() != null && p.id == s.excluded()) ? null : p.id;
        if (Lattice.isForbidden(s.level(), mx, mz, floor)) return null;
        return looksBuilt(s.level(), mx, mz, floor) ? new Native(m, floor) : null;
    }

    /** Хоть одно место прохода модуля — не камень TARDIS: здесь что-то построено. */
    private static boolean looksBuilt(ServerLevel level, int mx, int mz, int floor) {
        Block stone = TRBlockRegistry.FOOLS_STONE.get();
        for (Direction side : Lattice.SIDES) {
            if (!level.getBlockState(Lattice.nodePos(mx, mz, side, floor)).is(stone)) return true;
        }
        return false;
    }

    private static List<ModFloor> modulesOf(Scene s, Object node) {
        List<ModFloor> out = new ArrayList<>();
        if (node == ROOT) {
            for (long m : Lattice.hubModules(s.level()))
                for (int f : Lattice.FLOORS) if (f <= Lattice.HUB_FLOOR) out.add(new ModFloor(m, f));
        } else if (node == VIRTUAL) {
            s.virtual().footprint().forEach((f, mods) -> { for (long m : mods) out.add(new ModFloor(m, f)); });
        } else if (node instanceof Integer id) {
            ModuleRegistry.Piece p = s.reg().piece(id);
            if (p != null) p.footprint.forEach((f, mods) -> { for (long m : mods) out.add(new ModFloor(m, f)); });
        } else if (node instanceof Native n) {
            out.add(new ModFloor(n.module(), n.floor()));
        }
        return out;
    }

    // ---------- проходы ----------

    private static boolean open(ServerLevel level, long m, Direction side, int floor) {
        return level.getBlockState(Lattice.nodePos(Lattice.keyX(m), Lattice.keyZ(m), side, floor)).isAir();
    }

    /** На этой стороне модуля есть проход (открытый или закрытый нашей панелью). */
    private static boolean hasExit(ServerLevel level, long m, Direction side, int floor) {
        BlockState s = level.getBlockState(Lattice.nodePos(Lattice.keyX(m), Lattice.keyZ(m), side, floor));
        return s.isAir() || Lattice.isPanelBlock(s);
    }

    /**
     * Выход помещения на этой стороне модуля по его плану: TRUE/FALSE, или null — модуль не наше помещение
     * (хаб, чужая постройка, пусто) или план не восстановить; тогда судим по блокам.
     */
    public static Boolean plannedExit(ModuleRegistry reg, long m, Direction side, int floor) {
        ModuleRegistry.Piece p = reg.ownerOf(m, floor);
        if (p == null) return null;
        Map<Integer, Set<PieceCatalog.ModSide>> ex = p.plannedExits();
        if (ex == null) return null;
        return ex.getOrDefault(floor, Set.of()).contains(new PieceCatalog.ModSide(m, side));
    }

    /** Проход со стороны узла {@code who} (модуль {@code m}, сторона {@code side}); {@code toVirtual} — по ту сторону план замены. */
    private static boolean sideOpen(Scene s, Object who, long m, Direction side, int floor, boolean toVirtual) {
        if (who == VIRTUAL) return s.virtual().exits().getOrDefault(floor, Set.of()).contains(new PieceCatalog.ModSide(m, side));
        if (Boolean.FALSE.equals(plannedExit(s.reg(), m, side, floor))) return false;
        // План замены ещё не построен — у соседа достаточно прохода, закрытого нашей панелью.
        return toVirtual ? hasExit(s.level(), m, side, floor) : open(s.level(), m, side, floor);
    }

    // ---------- обход ----------

    /** Всё, что связано с хабом; {@code edges} (если не null) собирает рёбра для лога. */
    private static Set<Object> reach(Scene s, StringBuilder edges) {
        Set<Object> seen = new HashSet<>();
        Deque<Object> q = new ArrayDeque<>();
        q.add(ROOT);
        seen.add(ROOT);
        while (!q.isEmpty()) {
            Object self = q.poll();
            List<ModFloor> mods = modulesOf(s, self);
            Set<ModFloor> own = new HashSet<>(mods);
            for (ModFloor mf : mods) {
                for (Direction side : Lattice.SIDES) {
                    long n = Lattice.key(Lattice.keyX(mf.module()) + side.getStepX(), Lattice.keyZ(mf.module()) + side.getStepZ());
                    if (own.contains(new ModFloor(n, mf.floor()))) continue;
                    Object o = owner(s, n, mf.floor());
                    if (o == null || o.equals(self)) continue;
                    if (!sideOpen(s, self, mf.module(), side, mf.floor(), o == VIRTUAL)) continue;
                    if (!sideOpen(s, o, n, side.getOpposite(), mf.floor(), self == VIRTUAL)) continue;
                    if (edges != null) edges.append(self).append("->").append(o).append("; ");
                    if (seen.add(o)) q.add(o);
                }
            }
        }
        return seen;
    }

    private static boolean keepsEveryone(Set<Object> before, Set<Object> after, int removedId) {
        for (Object n : before) {
            if (n instanceof Integer id && id == removedId) continue;
            if (!after.contains(n)) return false;
        }
        return true;
    }

    /** Что связано с хабом сейчас (непрозрачный снимок для {@link #canReplace}; считается один раз на меню). */
    public static Set<Object> reachableNow(ServerLevel level) {
        return reach(new Scene(level, ModuleRegistry.get(level), null, null), null);
    }

    /** Можно ли убрать помещение: всё, что сейчас связано с хабом (наше и чужое), останется связанным. */
    public static boolean canRemove(ServerLevel level, ModuleRegistry.Piece piece) {
        ModuleRegistry reg = ModuleRegistry.get(level);
        return keepsEveryone(reachableNow(level), reach(new Scene(level, reg, piece.id, null), null), piece.id);
    }

    /** Можно ли заменить помещение планом {@code replacement}, никого не отрезав. */
    public static boolean canReplace(ServerLevel level, ModuleRegistry.Piece piece, PieceCatalog.Plan replacement) {
        return canReplace(level, piece, replacement, reachableNow(level));
    }

    public static boolean canReplace(ServerLevel level, ModuleRegistry.Piece piece, PieceCatalog.Plan replacement, Set<Object> before) {
        return keepsEveryone(before, reach(new Scene(level, ModuleRegistry.get(level), piece.id, replacement), null), piece.id);
    }

    /** Граф связности текстом — в лог при удалении, чтобы разбирать спорные случаи. */
    public static String describe(ServerLevel level) {
        StringBuilder b = new StringBuilder();
        Set<Object> r = reach(new Scene(level, ModuleRegistry.get(level), null, null), b);
        return b.append("reach=").append(r).toString();
    }

    private Connectivity() {}
}
