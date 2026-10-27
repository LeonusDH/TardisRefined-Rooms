package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TRoomsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import whocraft.tardis_refined.common.world.chunk.TardisChunkGenerator;
import whocraft.tardis_refined.registry.TRBlockRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Решётка модулей 24×24: ровно половина клетки TR (48), с тем же началом (−16).
 * Проход каждого модуля — на 9-м блоке стороны (8..10), высота 2..5 от пола, как у хаба и пресетов TR.
 * Поэтому все модули, хаб и пресеты TR стыкуются друг с другом всегда.
 */
public final class Lattice {
    public static final int MODULE = 24;
    public static final int OFFSET = -16;
    /** Уровень хаба TR — на него выходят из рубки («первый уровень»). */
    public static final int FLOOR_LOWER = 97;
    /** Единственный уровень над хабом; выше строить нельзя (там кончается камень TARDIS). */
    public static final int FLOOR_UPPER = 125;
    public static final int HUB_FLOOR = FLOOR_LOWER;
    public static final int BAND = 28;
    /** Уровни снизу вверх: три под хабом (в камне TARDIS, 0..150), хаб и один над ним. */
    public static final List<Integer> FLOORS = List.of(13, 41, 69, FLOOR_LOWER, FLOOR_UPPER);

    /** Уровень над {@code floor} или −1. */
    public static int above(int floor) {
        return FLOORS.contains(floor + BAND) ? floor + BAND : -1;
    }

    /** Уровень под {@code floor} или −1. */
    public static int below(int floor) {
        return FLOORS.contains(floor - BAND) ? floor - BAND : -1;
    }
    public static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    public static final int OPEN_MIN = 8, OPEN_MAX = 10, OPEN_DY_MIN = 2, OPEN_DY_MAX = 5;
    public static final int NODE_A = 9, NODE_DY = 3;
    /**
     * Верх заглушки. Проход 2..5, но верхний ряд у прохода бывает разный: с одних сторон модуля это ступеньки
     * арки, с других — пусто. Чтобы заглушки везде выглядели одинаково, она 3×3 (2..4), а над ней — фонари.
     */
    static final int PANEL_DY_MAX = 4;

    /** Клетки TR (48) центрального хаба и клетка, куда заходит край Ока Гармонии. */
    private static final int[][] HUB_TR_CELLS = {{20, 0}, {21, -1}, {21, 0}, {21, 1}, {22, -1}, {22, 0}, {22, 1}};
    private static final int[][] RESERVED_TR_CELLS = {{20, 1}};
    private static final Set<Long> HUB_MODULES = modulesOf(HUB_TR_CELLS);
    private static final Set<Long> RESERVED_MODULES = modulesOf(RESERVED_TR_CELLS);
    /**
     * Упрощённый хаб ({@link HubInitializer}): центр TR со шлюзом, смотровая Ока (она заходит в восточную и южные
     * части) и два переходника от выходов центра на сетку — западный (41,1) и южный (42,2).
     */
    static final Set<Long> SMALL_HUB_MODULES = Set.of(
            key(42, 0), key(43, 0), key(42, 1), key(43, 1),
            key(44, 1), key(43, 2), key(44, 2),
            key(41, 1), key(42, 2));
    /** Модули упрощённого хаба, из которых выходят проходы наружу (на них ставятся узлы роста). */
    static final Set<Long> SMALL_HUB_EXITS = Set.of(key(41, 1), key(42, 2));
    /**
     * Под хабом: шахта хаба уходит до y76, Око Гармонии с его колодцем — от y43 до хаба.
     * Хаб, Око и кольцо в один модуль вокруг — запрет, чтобы туда не простроиться и не увидеть оттуда коридоров.
     */
    private static final Set<Long> DEEP_FORBIDDEN = rect(39, -3, 46, 4);

    private static Set<Long> rect(int x0, int z0, int x1, int z1) {
        Set<Long> s = new HashSet<>();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) s.add(key(x, z));
        return Set.copyOf(s);
    }

    public static long key(int mx, int mz) {
        return ((long) mx << 32) ^ (mz & 0xffffffffL);
    }

    public static int keyX(long key) { return (int) (key >> 32); }
    public static int keyZ(long key) { return (int) key; }

    private static Set<Long> modulesOf(int[][] trCells) {
        Set<Long> s = new HashSet<>();
        for (int[] c : trCells)
            for (int dx = 0; dx < 2; dx++)
                for (int dz = 0; dz < 2; dz++) s.add(key(c[0] * 2 + dx, c[1] * 2 + dz));
        return Set.copyOf(s);
    }

    public static int moduleOf(int coord) {
        return Math.floorDiv(coord - OFFSET, MODULE);
    }

    public static int origin(int module) {
        return module * MODULE + OFFSET;
    }

    public static boolean isTardisLevel(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof TardisChunkGenerator;
    }

    /** Модули хаба этой ТАРДИС: хаб TR целиком или упрощённый. */
    public static Set<Long> hubModules(ServerLevel level) {
        return HubInitializer.isSmallHub(level) ? SMALL_HUB_MODULES : HUB_MODULES;
    }

    /** Модуль хаба (на любом уровне). */
    public static boolean isHub(ServerLevel level, int mx, int mz) {
        return hubModules(level).contains(key(mx, mz));
    }

    /**
     * Хаб на этом уровне. Над хабом (верхний уровень) хаба нет: постройки TR кончаются ниже (центр хаба — до y116,
     * Око — до y96), а коридоры, которые TR раньше ставила там поверх хаба, аддон больше не генерирует — место наше.
     */
    public static boolean isHub(ServerLevel level, int mx, int mz, int floor) {
        return floor <= HUB_FLOOR && isHub(level, mx, mz);
    }

    /**
     * Нельзя строить на уровне {@code floor}: зона десктопа (её стирает смена десктопа) — на всех уровнях;
     * хаб (и, у хаба TR, место Ока) — до уровня хаба включительно; под хабом — ещё и кольцо вокруг хаба и Ока.
     */
    public static boolean isForbidden(ServerLevel level, int mx, int mz, int floor) {
        int cx = Math.floorDiv(mx, 2), cz = Math.floorDiv(mz, 2);
        if (Math.abs(cx) <= 8 && Math.abs(cz) <= 8) return true;
        if (floor <= HUB_FLOOR) {
            if (isHub(level, mx, mz)) return true;
            if (!HubInitializer.isSmallHub(level) && RESERVED_MODULES.contains(key(mx, mz))) return true;
        }
        return floor < HUB_FLOOR && DEEP_FORBIDDEN.contains(key(mx, mz));
    }

    /** Блок прохода на стороне модуля: {@code a} вдоль стороны, {@code dy} от пола. */
    public static BlockPos openingPos(int mx, int mz, Direction side, int floorY, int a, int dy) {
        int ox = origin(mx), oz = origin(mz), y = floorY + dy;
        return switch (side) {
            case NORTH -> new BlockPos(ox + a, y, oz);
            case SOUTH -> new BlockPos(ox + a, y, oz + MODULE - 1);
            case WEST -> new BlockPos(ox, y, oz + a);
            case EAST -> new BlockPos(ox + MODULE - 1, y, oz + a);
            default -> throw new IllegalArgumentException(side.toString());
        };
    }

    public static BlockPos nodePos(int mx, int mz, Direction side, int floorY) {
        return openingPos(mx, mz, side, floorY, NODE_A, NODE_DY);
    }

    /** Быстрая проверка для расстановки узлов: на местах всех четырёх проходов модуля камень. */
    public static boolean looksEmpty(ServerLevel level, int mx, int mz, int floorY) {
        if (isHub(level, mx, mz, floorY)) return false;
        Block stone = TRBlockRegistry.FOOLS_STONE.get();
        for (Direction side : SIDES) {
            if (!level.getBlockState(nodePos(mx, mz, side, floorY)).is(stone)) return false;
        }
        return true;
    }

    /** Строгая проверка перед постройкой: весь объём модуля на этаже — нетронутый камень TARDIS или бедрок. */
    public static boolean isUntouched(ServerLevel level, int mx, int mz, int floorY) {
        Block stone = TRBlockRegistry.FOOLS_STONE.get();
        int ox = origin(mx), oz = origin(mz);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = floorY; y < floorY + BAND; y++)
            for (int x = ox; x < ox + MODULE; x++)
                for (int z = oz; z < oz + MODULE; z++) {
                    BlockState s = level.getBlockState(pos.set(x, y, z));
                    // Фонари за заглушками соседей стоят в нашем камне — место от этого не занято.
                    if (!s.is(stone) && !s.is(Blocks.BEDROCK) && !s.is(TRoomsBlocks.PANEL_LAMP.get())) return false;
                }
        return true;
    }

    /** Закрывает проход панелью (с узлом роста в центре или глухой). Возвращает false, если прохода нет. */
    public static boolean placePanel(ServerLevel level, int mx, int mz, Direction side, int floorY, boolean withNode) {
        if (!level.getBlockState(nodePos(mx, mz, side, floorY)).isAir()) return false;
        BlockState wall = TRoomsBlocks.ROOM_WALL.get().defaultBlockState();
        BlockState node = TRoomsBlocks.ROOM_NODE.get().defaultBlockState();
        for (int a = OPEN_MIN; a <= OPEN_MAX; a++)
            for (int dy = OPEN_DY_MIN; dy <= PANEL_DY_MAX; dy++) {
                BlockPos pos = openingPos(mx, mz, side, floorY, a, dy);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, (withNode && a == NODE_A && dy == NODE_DY) ? node : wall, Block.UPDATE_ALL);
                }
            }
        lampEdge(level, mx, mz, side, floorY);
        return true;
    }

    /** Край прохода вокруг заглушки (проход коридора шире панели: 5 против 3). */
    private static final int EDGE_MIN = OPEN_MIN - 2, EDGE_MAX = OPEN_MAX + 2;

    /**
     * Всё, что у края прохода не закрывает обзор целиком (ниши, ступеньки, плиты, люки), открывает вид на камень
     * TARDIS соседнего модуля. За такими местами, прямо в камне соседа, ставим фонари — как световые полосы коридоров.
     * Пока сосед — нетронутый камень, это ему не мешает ({@link #isUntouched} их не считает); при постройке там
     * их перекрывают плитки нового помещения, при сносе нашего помещения их убирает {@link #clearLampsBehind}.
     */
    public static void lampEdge(ServerLevel level, int mx, int mz, Direction side, int floorY) {
        Block stone = TRBlockRegistry.FOOLS_STONE.get();
        BlockState lamp = TRoomsBlocks.PANEL_LAMP.get().defaultBlockState();
        for (int a = EDGE_MIN; a <= EDGE_MAX; a++)
            for (int dy = OPEN_DY_MIN; dy <= OPEN_DY_MAX; dy++) {
                BlockPos pos = openingPos(mx, mz, side, floorY, a, dy);
                BlockState here = level.getBlockState(pos);
                if (!here.isAir() && here.isSolidRender(level, pos)) continue;
                BlockPos behind = pos.relative(side);
                if (level.getBlockState(behind).is(stone)) level.setBlock(behind, lamp, Block.UPDATE_ALL);
            }
    }

    /** Убирает фонари, которые наш край прохода поставил в камень соседа (перед сносом нашего помещения). */
    public static void clearLampsBehind(ServerLevel level, int mx, int mz, Direction side, int floorY) {
        BlockState stone = TRBlockRegistry.FOOLS_STONE.get().defaultBlockState();
        for (int a = EDGE_MIN; a <= EDGE_MAX; a++)
            for (int dy = OPEN_DY_MIN; dy <= OPEN_DY_MAX; dy++) {
                BlockPos behind = openingPos(mx, mz, side, floorY, a, dy).relative(side);
                if (level.getBlockState(behind).is(TRoomsBlocks.PANEL_LAMP.get())) level.setBlock(behind, stone, Block.UPDATE_ALL);
            }
    }

    public static boolean isPanelBlock(BlockState s) {
        return s.is(TRoomsBlocks.ROOM_WALL.get()) || s.is(TRoomsBlocks.ROOM_NODE.get());
    }

    /** Убирает панель аддона и её фонари из прохода (чужие блоки не трогает). */
    public static void clearPanel(ServerLevel level, int mx, int mz, Direction side, int floorY) {
        for (int a = EDGE_MIN; a <= EDGE_MAX; a++)
            for (int dy = OPEN_DY_MIN; dy <= OPEN_DY_MAX; dy++) {
                BlockPos pos = openingPos(mx, mz, side, floorY, a, dy);
                BlockState s = level.getBlockState(pos);
                if (isPanelBlock(s) || s.is(TRoomsBlocks.PANEL_LAMP.get())) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
    }

    /**
     * После постройки: проходы наружу получают узлы (сосед свободен), соединяются (у соседа напротив проход)
     * или закрываются глухой панелью (сосед упирается стеной/запрещён). Узлы соседей, смотрящие в нашу стену, гаснут.
     */
    public static void refresh(ServerLevel level, Set<Long> footprint, int floorY) {
        ModuleRegistry reg = ModuleRegistry.get(level);
        for (long m : footprint) {
            int mx = keyX(m), mz = keyZ(m);
            for (Direction side : SIDES) {
                int nx = mx + side.getStepX(), nz = mz + side.getStepZ();
                if (footprint.contains(key(nx, nz))) continue;
                BlockPos ours = nodePos(mx, mz, side, floorY);
                BlockPos theirs = nodePos(nx, nz, side.getOpposite(), floorY);
                BlockState theirState = level.getBlockState(theirs);
                if (level.getBlockState(ours).isAir()) {
                    if (!isForbidden(level, nx, nz, floorY) && !reg.isOccupied(nx, nz, floorY) && looksEmpty(level, nx, nz, floorY)) {
                        placePanel(level, mx, mz, side, floorY, true);
                    } else if (!Boolean.FALSE.equals(Connectivity.plannedExit(reg, key(nx, nz), side.getOpposite(), floorY))
                            && (theirState.isAir() || isPanelBlock(theirState))) {
                        clearPanel(level, nx, nz, side.getOpposite(), floorY);
                    } else {
                        placePanel(level, mx, mz, side, floorY, false);
                    }
                } else if (theirState.is(TRoomsBlocks.ROOM_NODE.get())) {
                    level.setBlock(theirs, TRoomsBlocks.ROOM_WALL.get().defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private Lattice() {}
}
