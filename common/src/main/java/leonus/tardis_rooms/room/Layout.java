package leonus.tardis_rooms.room;

import leonus.tardis_rooms.mixin.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import whocraft.tardis_refined.TardisRefined;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Раскладка помещения из плиток TR (corridor_template, 7×7, шаг 6) в мировой ориентации.
 * Футпринт — прямоугольник модулей; сетка плиток 4×(модули). Проходы на краях всегда на плитке ≡1 (mod 4).
 * 'C' коридор, 'R' комната, 'D' вход в комнату, ' ' камень. Плитки повернуть нельзя (сетка несимметрична),
 * поэтому раскладка строится сразу под нужное направление.
 */
public final class Layout {
    public final int mx0, mz0, mw, md;
    public final int tx, tz;
    final char[][] g;
    /** Выходы: сторона футпринта и номер плитки вдоль неё. */
    public final List<Exit> exits = new ArrayList<>();

    public record Exit(Direction side, int along) {}

    /** Левый верхний угол прямоугольника комнаты в плитках (−1 — это коридор, не комната). */
    private int roomX0 = -1, roomZ0 = -1;

    /** Внутреннее пространство комнаты (между стенами): minX, minZ, maxX, maxZ в мировых координатах. */
    public int[] interior() {
        int ox = Lattice.origin(mx0), oz = Lattice.origin(mz0);
        int x0 = Math.max(roomX0, 0), z0 = Math.max(roomZ0, 0);
        return new int[]{ox + x0 * 6 + 1, oz + z0 * 6 + 1, ox + (tx - 1) * 6 - 1, oz + (tz - 1) * 6 - 1};
    }

    /**
     * Где должен стоять блок управления (мировые x, z): у коридора — середина развилки,
     * у комнаты — середина её внутреннего пространства (между стенами).
     */
    public int[] coreSpot() {
        int ox = Lattice.origin(mx0), oz = Lattice.origin(mz0);
        if (roomX0 < 0) return new int[]{ox + 9, oz + 9};
        return new int[]{ox + (roomX0 + tx - 1) * 3, oz + (roomZ0 + tz - 1) * 3};
    }

    private Layout(int mx0, int mz0, int mw, int md) {
        this.mx0 = mx0; this.mz0 = mz0; this.mw = mw; this.md = md;
        this.tx = mw * 4; this.tz = md * 4;
        this.g = new char[tx][tz];
        for (char[] col : g) Arrays.fill(col, ' ');
    }

    public Set<Long> footprint() {
        Set<Long> s = new HashSet<>();
        for (int x = 0; x < mw; x++) for (int z = 0; z < md; z++) s.add(Lattice.key(mx0 + x, mz0 + z));
        return s;
    }

    // ---------- построение раскладок ----------

    public static Layout corridor(int emx, int emz, Set<Direction> sides) {
        Layout l = new Layout(emx, emz, 1, 1);
        if (sides.contains(Direction.NORTH)) { l.g[1][0] = 'C'; l.g[1][1] = 'C'; }
        if (sides.contains(Direction.WEST)) { l.g[0][1] = 'C'; l.g[1][1] = 'C'; }
        if (sides.contains(Direction.SOUTH)) for (int z = 1; z < 4; z++) l.g[1][z] = 'C';
        if (sides.contains(Direction.EAST)) for (int x = 1; x < 4; x++) l.g[x][1] = 'C';
        for (Direction d : sides) l.exits.add(new Exit(d, 1));
        return l;
    }

    public enum Align { NONE, RIGHT, NEGATIVE }

    /**
     * Комната: футпринт {@code depth} модулей вперёд от модуля входа и {@code width} поперёк.
     * {@code extraExits} — дополнительные двери на других сторонах (на втором модуле стороны, плитка 5).
     */
    public static Layout room(int emx, int emz, Direction facing, int depth, int width, Align align, Set<Direction> extraExits) {
        List<Integer> xs = new ArrayList<>(List.of(emx)), zs = new ArrayList<>(List.of(emz));
        for (int i = 1; i < depth; i++) { xs.add(emx + facing.getStepX() * i); zs.add(emz + facing.getStepZ() * i); }
        if (width > 1) {
            int ax, az;
            if (align == Align.RIGHT) { Direction r = facing.getClockWise(); ax = r.getStepX(); az = r.getStepZ(); }
            else if (facing.getAxis() == Direction.Axis.Z) { ax = -1; az = 0; }
            else { ax = 0; az = -1; }
            for (int i = 1; i < width; i++) { xs.add(emx + ax * i); zs.add(emz + az * i); }
        }
        int minX = xs.stream().min(Integer::compare).get(), maxX = xs.stream().max(Integer::compare).get();
        int minZ = zs.stream().min(Integer::compare).get(), maxZ = zs.stream().max(Integer::compare).get();
        Layout l = new Layout(minX, minZ, maxX - minX + 1, maxZ - minZ + 1);

        Direction entry = facing.getOpposite();
        List<int[]> doors = new ArrayList<>();
        doors.add(l.entryTile(emx, emz, entry));
        for (Direction s : extraExits) {
            if (s == entry) continue;
            doors.add(switch (s) {
                case NORTH -> new int[]{5, 0};
                case SOUTH -> new int[]{5, l.tz - 1};
                case WEST -> new int[]{0, 5};
                default -> new int[]{l.tx - 1, 5};
            });
        }
        int x0 = doors.stream().anyMatch(d -> d[0] == 0) ? 1 : 0;
        int z0 = doors.stream().anyMatch(d -> d[1] == 0) ? 1 : 0;
        for (int x = x0; x < l.tx - 1; x++) for (int z = z0; z < l.tz - 1; z++) l.g[x][z] = 'R';
        l.roomX0 = x0;
        l.roomZ0 = z0;
        for (int[] d : doors) {
            l.g[d[0]][d[1]] = 'D';
            if (d[1] == 0) l.exits.add(new Exit(Direction.NORTH, d[0]));
            else if (d[1] == l.tz - 1) l.exits.add(new Exit(Direction.SOUTH, d[0]));
            else if (d[0] == 0) l.exits.add(new Exit(Direction.WEST, d[1]));
            else l.exits.add(new Exit(Direction.EAST, d[1]));
        }
        return l;
    }

    /** Плитка входа на краю футпринта напротив модуля входа. */
    int[] entryTile(int emx, int emz, Direction side) {
        int lx = (emx - mx0) * 4, lz = (emz - mz0) * 4;
        return switch (side) {
            case NORTH -> new int[]{lx + 1, 0};
            case SOUTH -> new int[]{lx + 1, tz - 1};
            case WEST -> new int[]{0, lz + 1};
            default -> new int[]{tx - 1, lz + 1};
        };
    }

    /** Координаты (в блоках от угла футпринта) центра прохода выхода — для рамки на превью. */
    public int[] exitBlock(Exit e) {
        return switch (e.side()) {
            case NORTH -> new int[]{e.along() * 6 + 3, 0};
            case SOUTH -> new int[]{e.along() * 6 + 3, tz * 6 - 1};
            case WEST -> new int[]{0, e.along() * 6 + 3};
            default -> new int[]{tx * 6 - 1, e.along() * 6 + 3};
        };
    }

    // ---------- выбор плиток (порт правил CorridorGenerator TR) ----------

    /** За краем сетки: для коридоров и входов «занято» (проход продолжается), для комнат «пусто» (будет стена). */
    private char at(int x, int z, boolean forRoom) {
        if (x < 0 || z < 0 || x >= tx || z >= tz) return forRoom ? ' ' : 'X';
        return g[x][z];
    }

    String pick(int x, int z) {
        char t = g[x][z];
        boolean fr = t == 'R';
        char n = at(x, z - 1, fr), s = at(x, z + 1, fr), e = at(x + 1, z, fr), w = at(x - 1, z, fr);
        boolean N = n != ' ', S = s != ' ', E = e != ' ', W = w != ' ';
        if (t == 'D') {
            if (N && S && !E && !W) return "room_entry_ns";
            if (!N && !S && E && W) return "room_entry_ew";
            return null;
        }
        if (t == 'R') {
            if (N && S && E && W) {
                if (n == 'D') return "room_door_n";
                if (s == 'D') return "room_door_s";
                if (e == 'D') return "room_door_e";
                if (w == 'D') return "room_door_w";
                return "room_center";
            }
            if (N && !S && E && !W) return "room_wall_ne";
            if (N && !S && !E && W) return "room_wall_nw";
            if (!N && S && E && !W) return "room_wall_se";
            if (!N && S && !E && W) return "room_wall_sw";
            if (N && !S && E && W) return "room_wall_nwe";
            if (N && S && E && !W) return "room_wall_nse";
            if (!N && S && E && W) return "room_wall_sew";
            if (N && S && !E && W) return "room_wall_nsw";
            return null;
        }
        if (N && S && !E && !W) return "corridor_piece_ns";
        if (!N && !S && E && W) return "corridor_piece_ew";
        if (N && S && E && W) return "corridor_piece_cross";
        if (N && !S && E && !W) return "corridor_piece_ne";
        if (N && !S && !E && W) return "corridor_piece_nw";
        if (!N && S && E && !W) return "corridor_piece_se";
        if (!N && S && !E && W) return "corridor_piece_sw";
        if (N && !S && E && W) return "corridor_piece_new";
        if (N && S && E && !W) return "corridor_piece_nse";
        if (!N && S && E && W) return "corridor_piece_sew";
        if (N && S && !E && W) return "corridor_piece_nsw";
        return null;
    }

    // ---------- постройка ----------

    private static final int ROOM_TILE_HEIGHT = 9;
    /** Слой, выше которого вставляются слои стены при растяжении (y5–y6 — верх стены, y7–y8 — карниз и потолок). */
    private static final int STRETCH_AFTER = 6;

    /**
     * Ставит раскладку на этаж {@code floorY}. {@code roomHeight} — полная высота комнатных плиток
     * (9 = как у TR; больше — стены растягиваются, двери и коридоры остаются обычной высоты,
     * в полу появляются фонари).
     */
    public void place(ServerLevel level, int floorY, int roomHeight) {
        int ox = Lattice.origin(mx0), oz = Lattice.origin(mz0);
        BoundingBox box = new BoundingBox(ox, floorY, oz, ox + mw * Lattice.MODULE - 1, floorY + Math.max(roomHeight, 9) + 1, oz + md * Lattice.MODULE - 1);
        int extra = Math.max(0, roomHeight - ROOM_TILE_HEIGHT);
        List<Niche> niches = new ArrayList<>();
        for (int x = 0; x < tx; x++) {
            for (int z = 0; z < tz; z++) {
                if (g[x][z] == ' ') continue;
                String name = pick(x, z);
                if (name == null) {
                    Direction open = g[x][z] == 'C' ? deadEndSide(x, z) : null;
                    if (open != null) placeDeadEnd(level, new BlockPos(ox + x * 6, floorY, oz + z * 6), box, open, niches);
                    continue;
                }
                Optional<StructureTemplate> t = template(level, name);
                if (t.isEmpty()) continue;
                BlockPos pos = new BlockPos(ox + x * 6, floorY, oz + z * 6);
                if (g[x][z] == 'R') {
                    placeStretched(level, t.get(), pos, box, extra);
                } else {
                    BoundingBox tileBox = box;
                    // Вход в высокую комнату не заходит на линию стены: иначе его край видно в высокой нише.
                    if (g[x][z] == 'D' && extra > 0) tileBox = withoutWallLine(box, pos, edgeSide(x, z));
                    t.get().placeInWorld(level, pos, pos, settings(tileBox), level.getRandom(), Block.UPDATE_CLIENTS);
                    collectNiches(t.get(), pos, tileBox, niches);
                }
            }
        }
        lightNicheBacks(level, box, niches);
    }

    /** Ниша (место снятых прутьев) и сторона, куда она смотрит из плитки. */
    private record Niche(BlockPos pos, Direction out) {}

    private static void collectNiches(StructureTemplate t, BlockPos origin, BoundingBox clip, List<Niche> out) {
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) t).tardisRooms$getPalettes();
        if (palettes.isEmpty()) return;
        for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) {
            if (!(info.state().getBlock() instanceof IronBarsBlock)) continue;
            int bx = info.pos().getX(), bz = info.pos().getZ();
            boolean ex = bx == 0 || bx == 6, ez = bz == 0 || bz == 6;
            if (ex == ez) continue; // угол или середина — не ниша края
            Direction d = ex ? (bx == 0 ? Direction.WEST : Direction.EAST) : (bz == 0 ? Direction.NORTH : Direction.SOUTH);
            BlockPos w = origin.offset(info.pos());
            if (clip.isInside(w)) out.add(new Niche(w, d));
        }
    }

    /**
     * В нишах коридоров TR прутья сняты — за нишей виден камень TARDIS, если рядом нет плитки.
     * Внутри своего футпринта ставим туда морской фонарь, как в световых полосах коридоров.
     * Ниши на краю модуля (за ними — соседний модуль) закрывают фонари заглушек ({@link Lattice#lampEdge}).
     */
    private static void lightNicheBacks(ServerLevel level, BoundingBox box, List<Niche> niches) {
        net.minecraft.world.level.block.Block stone = whocraft.tardis_refined.registry.TRBlockRegistry.FOOLS_STONE.get();
        for (Niche n : niches) {
            if (!level.getBlockState(n.pos()).isAir()) continue;
            BlockPos back = n.pos().relative(n.out());
            if (!box.isInside(back.getX(), box.minY(), back.getZ())) continue;
            if (level.getBlockState(back).is(stone)) level.setBlock(back, Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Коридорная плитка с единственным соседом (тупик лифта): сторона этого соседа, иначе null. */
    private Direction deadEndSide(int x, int z) {
        Direction found = null;
        int count = 0;
        for (Direction d : Lattice.SIDES) {
            if (at(x + d.getStepX(), z + d.getStepZ(), false) != ' ') { found = d; count++; }
        }
        return count == 1 ? found : null;
    }

    /**
     * Тупик: у TR нет такой плитки — ставим прямую плитку вдоль прохода и закрываем дальний конец
     * стеной поперечной плитки (её двумя крайними рядами).
     */
    private static void placeDeadEnd(ServerLevel level, BlockPos pos, BoundingBox box, Direction open, List<Niche> niches) {
        boolean z = open.getAxis() == Direction.Axis.Z;
        Optional<StructureTemplate> straight = template(level, z ? "corridor_piece_ns" : "corridor_piece_ew");
        Optional<StructureTemplate> across = template(level, z ? "corridor_piece_ew" : "corridor_piece_ns");
        if (straight.isEmpty() || across.isEmpty()) return;
        straight.get().placeInWorld(level, pos, pos, settings(box), level.getRandom(), Block.UPDATE_CLIENTS);
        collectNiches(straight.get(), pos, box, niches);
        int x0 = pos.getX(), z0 = pos.getZ(), x1 = x0 + 6, z1 = z0 + 6;
        switch (open.getOpposite()) {
            case SOUTH -> z0 = z1 - 1;
            case NORTH -> z1 = z0 + 1;
            case EAST -> x0 = x1 - 1;
            default -> x1 = x0 + 1;
        }
        BoundingBox cap = new BoundingBox(Math.max(x0, box.minX()), box.minY(), Math.max(z0, box.minZ()),
                Math.min(x1, box.maxX()), box.maxY(), Math.min(z1, box.maxZ()));
        across.get().placeInWorld(level, pos, pos, settings(cap), level.getRandom(), Block.UPDATE_CLIENTS);
        collectNiches(across.get(), pos, cap, niches);
    }

    /** Сторона футпринта, на краю которой стоит плитка. */
    private Direction edgeSide(int x, int z) {
        if (z == 0) return Direction.NORTH;
        if (z == tz - 1) return Direction.SOUTH;
        if (x == 0) return Direction.WEST;
        return Direction.EAST;
    }

    /** Рамка плитки у края {@code side} без её стороны, обращённой внутрь (там проходит стена комнаты). */
    private static BoundingBox withoutWallLine(BoundingBox box, BlockPos tile, Direction side) {
        int x0 = tile.getX(), z0 = tile.getZ(), x1 = x0 + 6, z1 = z0 + 6;
        switch (side) {
            case SOUTH -> z0 += 1;
            case NORTH -> z1 -= 1;
            case WEST -> x1 -= 1;
            default -> x0 += 1;
        }
        return new BoundingBox(Math.max(x0, box.minX()), box.minY(), Math.max(z0, box.minZ()),
                Math.min(x1, box.maxX()), box.maxY(), Math.min(z1, box.maxZ()));
    }

    /** Ставит плитку коридора у края {@code side} (проходы верхнего этажа двухэтажного зала), не заходя на линию стены. */
    public void placeCorridorTile(ServerLevel level, int tileX, int tileZ, int floorY, Direction side) {
        int ox = Lattice.origin(mx0), oz = Lattice.origin(mz0);
        BoundingBox box = new BoundingBox(ox, floorY, oz, ox + mw * Lattice.MODULE - 1, floorY + 8, oz + md * Lattice.MODULE - 1);
        BlockPos pos = new BlockPos(ox + tileX * 6, floorY, oz + tileZ * 6);
        BoundingBox tileBox = withoutWallLine(box, pos, side);
        template(level, side.getAxis() == Direction.Axis.Z ? "corridor_piece_ns" : "corridor_piece_ew").ifPresent(t ->
                t.placeInWorld(level, pos, pos, settings(tileBox), level.getRandom(), Block.UPDATE_CLIENTS));
    }

    /**
     * Переносит стенную часть дверной плитки TR (пол порога, арку с рамой и перемычкой, слои 0..6) в стену на этаже
     * {@code floorY}: так верхние проходы двухэтажного зала выглядят как нижние двери.
     * {@code tileOrigin} — угол дверной плитки (y = пол этажа), {@code side} — сторона стены с дверью.
     */
    public static void stampDoorFrame(ServerLevel level, Direction side, BlockPos tileOrigin) {
        String name = "room_door_" + switch (side) { case NORTH -> "n"; case SOUTH -> "s"; case WEST -> "w"; default -> "e"; };
        Optional<StructureTemplate> t = template(level, name);
        if (t.isEmpty()) return;
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) t.get()).tardisRooms$getPalettes();
        if (palettes.isEmpty()) return;
        for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) {
            int bx = info.pos().getX(), by = info.pos().getY(), bz = info.pos().getZ();
            if (by > 6) continue;
            boolean wallRows, decorRow;
            switch (side) {
                case NORTH -> { wallRows = bz <= 1; decorRow = bz == 1; }
                case SOUTH -> { wallRows = bz >= 5; decorRow = bz == 5; }
                case WEST -> { wallRows = bx <= 1; decorRow = bx == 1; }
                default -> { wallRows = bx >= 5; decorRow = bx == 5; }
            }
            if (!wallRows) continue;
            // Колонки ниш на краях плитки не трогаем: в высоком зале ниши идут цельными на всю высоту.
            boolean nicheColumn = side.getAxis() == Direction.Axis.Z ? (bx == 0 || bx == 6) : (bz == 0 || bz == 6);
            if (nicheColumn) continue;
            BlockState state = info.state();
            if (state.getBlock() instanceof IronBarsBlock) state = Blocks.AIR.defaultBlockState();
            level.setBlock(tileOrigin.offset(bx, by, bz), state, Block.UPDATE_CLIENTS);
        }
    }

    // ---------- лифт ----------

    /** Гравитационный колодец TR: низ (площадка, шахта) и верх (проём в полу). */
    static final ResourceLocation WELL_BOTTOM = ResourceLocation.fromNamespaceAndPath(TardisRefined.MODID, "corridors/corridor_connection_bottom");
    static final ResourceLocation WELL_TOP = ResourceLocation.fromNamespaceAndPath(TardisRefined.MODID, "corridors/corridor_connection_top");
    /** Шахта в модуле: кольцо стен 6..12, внутри 7..11 (как развилка коридора), центр 9. */
    private static final int SHAFT_MIN = 6, SHAFT_MAX = 12, PIT_MIN = 7, PIT_MAX = 11;
    /** Блок управления лифтом — в проходе входа, на столько блоков от центра шахты (2 блока от края площадки). */
    public static final int LIFT_CORE_OFFSET = 4;

    /**
     * Строит лифт: коридоры на обоих уровнях, на нижнем — площадка с гравитационным блоком TR,
     * сквозь потолок нижнего коридора — шахта TR до пола верхнего, в полу верхнего — проём TR со столбиками.
     * Все детали берутся из колодца TR (corridor_connection_bottom/top), поэтому выглядят так же.
     */
    public static void buildLift(ServerLevel level, Layout lower, Layout upper, int lowerFloor, int upperFloor) {
        lower.place(level, lowerFloor, ROOM_TILE_HEIGHT);
        upper.place(level, upperFloor, ROOM_TILE_HEIGHT);
        Map<BlockPos, BlockState> bottom = blocksOf(level, WELL_BOTTOM), top = blocksOf(level, WELL_TOP);
        if (bottom.isEmpty() || top.isEmpty()) return;
        int ox = Lattice.origin(lower.mx0), oz = Lattice.origin(lower.mz0);

        // Нижний уровень: пол и площадка шахты (y1..y2), гравитационный блок в центре.
        copy(level, bottom, 1, 2, PIT_MIN, PIT_MAX, ox, lowerFloor, oz, 0);
        // Проём в потолке нижнего коридора (y5..y6) — восьмиугольник 5×5; углы — колонны шахты (как у TR).
        for (int y = 5; y <= 6; y++)
            for (int x = PIT_MIN; x <= PIT_MAX; x++)
                for (int z = PIT_MIN; z <= PIT_MAX; z++) {
                    boolean corner = (x == PIT_MIN || x == PIT_MAX) && (z == PIT_MIN || z == PIT_MAX);
                    level.setBlock(new BlockPos(ox + x, lowerFloor + y, oz + z),
                            corner ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
        // Кольцо вокруг проёма на тех же слоях: потолок плитки (у тупика — его глухая стена) местами из камня TARDIS,
        // а в проёме он виден сбоку. Камень заменяем полированным глубинным сланцем, как стены шахты.
        BlockState ringBlock = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        net.minecraft.world.level.block.Block foolsStone = whocraft.tardis_refined.registry.TRBlockRegistry.FOOLS_STONE.get();
        for (int y = 5; y <= 6; y++)
            for (int x = SHAFT_MIN; x <= SHAFT_MAX; x++)
                for (int z = SHAFT_MIN; z <= SHAFT_MAX; z++) {
                    if (x != SHAFT_MIN && x != SHAFT_MAX && z != SHAFT_MIN && z != SHAFT_MAX) continue;
                    BlockPos p = new BlockPos(ox + x, lowerFloor + y, oz + z);
                    if (level.getBlockState(p).is(foolsStone)) level.setBlock(p, ringBlock, Block.UPDATE_CLIENTS);
                }
        // Шахта: слои стен TR (сплошное кольцо, снег, фонари, снег — по кругу) до пола верхнего уровня.
        for (int y = lowerFloor + 7; y < upperFloor; y++) {
            int src = 11 + ((y - (lowerFloor + 7) + 3) % 4);
            for (int x = SHAFT_MIN; x <= SHAFT_MAX; x++)
                for (int z = SHAFT_MIN; z <= SHAFT_MAX; z++) {
                    BlockState st = bottom.get(new BlockPos(x, src, z));
                    if (st != null) level.setBlock(new BlockPos(ox + x, y, oz + z), st, Block.UPDATE_CLIENTS);
                }
        }
        // Верхний уровень: проём 3×3 в полу с бортиком и столбики по углам (y0..y4), потолок — как у развилки.
        copy(level, top, 0, 4, PIT_MIN, PIT_MAX, ox, upperFloor, oz, 0);
    }

    private static void copy(ServerLevel level, Map<BlockPos, BlockState> src, int y0, int y1, int min, int max, int ox, int floorY, int oz, int dy) {
        for (int y = y0; y <= y1; y++)
            for (int x = min; x <= max; x++)
                for (int z = min; z <= max; z++) {
                    BlockState st = src.get(new BlockPos(x, y, z));
                    if (st != null) level.setBlock(new BlockPos(ox + x, floorY + y + dy, oz + z), st, Block.UPDATE_CLIENTS);
                }
    }

    /** Блоки шаблона по локальным координатам (без прутьев: в TR в нишах пусто). */
    private static Map<BlockPos, BlockState> blocksOf(ServerLevel level, ResourceLocation id) {
        Map<BlockPos, BlockState> out = new HashMap<>();
        Optional<StructureTemplate> t = level.getStructureManager().get(id);
        if (t.isEmpty()) return out;
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) t.get()).tardisRooms$getPalettes();
        if (palettes.isEmpty()) return out;
        for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) {
            BlockState st = info.state();
            if (st.getBlock() instanceof IronBarsBlock) st = Blocks.AIR.defaultBlockState();
            out.put(info.pos(), st);
        }
        return out;
    }

    /** В готовых блоках TR в нишах плиток пусто — так же убираем прутья и мы: заменяем их воздухом. */
    private static StructurePlaceSettings settings(BoundingBox box) {
        return new StructurePlaceSettings().setBoundingBox(box).addProcessor(new net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor(List.of(
                new net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule(
                        new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(Blocks.IRON_BARS),
                        net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest.INSTANCE,
                        Blocks.AIR.defaultBlockState()))));
    }

    private static Optional<StructureTemplate> template(ServerLevel level, String name) {
        return level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath(TardisRefined.MODID, "corridor_template/" + name));
    }

    /**
     * Ставит комнатную плитку, вставив {@code extra} слоёв стены над y=6. Световые полосы TR
     * (чередование снежного блока и морского фонаря) продолжаются, остальное повторяет верхний слой стены.
     */
    private static void placeStretched(ServerLevel level, StructureTemplate template, BlockPos origin, BoundingBox box, int extra) {
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) template).tardisRooms$getPalettes();
        if (palettes.isEmpty()) return;
        List<StructureTemplate.StructureBlockInfo> blocks = palettes.get(0).blocks();
        Map<Long, BlockState> layer2 = new HashMap<>(), layer5 = new HashMap<>(), layer6 = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            int y = info.pos().getY();
            if (y == 2) layer2.put(colKey(info.pos()), info.state());
            if (y == 5) layer5.put(colKey(info.pos()), info.state());
            if (y == 6) layer6.put(colKey(info.pos()), info.state());
        }
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockPos p = info.pos();
            int y = p.getY();
            long col = colKey(p);
            boolean niche = isNiche(layer2.get(col), layer6.get(col));
            BlockState state = info.state();
            int wy = y > STRETCH_AFTER ? y + extra : y;
            if (niche) {
                // Ниша в стене (ступенька снизу, козырёк сверху): прутья убираем, как в готовых комнатах TR,
                // козырёк поднимаем под карниз — ниша идёт на всю высоту стены.
                if (y == STRETCH_AFTER) wy = STRETCH_AFTER + extra;
            }
            if (state.getBlock() instanceof IronBarsBlock) state = Blocks.AIR.defaultBlockState();
            BlockPos world = origin.offset(p.getX(), wy, p.getZ());
            if (box.isInside(world.getX(), box.minY(), world.getZ())) level.setBlock(world, state, Block.UPDATE_CLIENTS);
        }
        if (extra <= 0) return;
        boolean ceilingLamp = blocks.stream()
                .anyMatch(i -> i.pos().getX() == 3 && i.pos().getY() == 8 && i.pos().getZ() == 3 && i.state().is(Blocks.SEA_LANTERN));
        for (Map.Entry<Long, BlockState> e : layer6.entrySet()) {
            int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey();
            boolean niche = isNiche(layer2.get(e.getKey()), e.getValue());
            BlockState b6 = e.getValue();
            BlockState b5 = layer5.get(e.getKey());
            for (int i = 0; i < extra; i++) {
                // в нише: на месте старого козырька и выше — пустота до поднятого козырька
                BlockState s = niche ? Blocks.AIR.defaultBlockState() : fill(b5, b6, i);
                int wy = niche ? STRETCH_AFTER + i : STRETCH_AFTER + 1 + i;
                BlockPos world = origin.offset(x, wy, z);
                if (box.isInside(world.getX(), box.minY(), world.getZ())) level.setBlock(world, s, Block.UPDATE_CLIENTS);
            }
        }
        if (ceilingLamp && box.isInside(origin.getX() + 3, box.minY(), origin.getZ() + 3)) {
            // Потолочный фонарь высокой комнаты далеко от пола — встраиваем фонарь в пол под ним.
            level.setBlock(origin.offset(3, 1, 3), Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Колонка-ниша TR: снизу (y2) обычная ступенька, сверху (y6) перевёрнутая. */
    private static boolean isNiche(BlockState b2, BlockState b6) {
        return b2 != null && b6 != null && b2.getBlock() instanceof StairBlock && b6.getBlock() instanceof StairBlock
                && b2.getValue(StairBlock.HALF) == net.minecraft.world.level.block.state.properties.Half.BOTTOM
                && b6.getValue(StairBlock.HALF) == net.minecraft.world.level.block.state.properties.Half.TOP;
    }

    private static BlockState fill(BlockState b5, BlockState b6, int i) {
        if (b5 != null && isLight(b5) != isLight(b6) && (isLight(b5) || isLight(b6)) && (isStripe(b5) && isStripe(b6))) {
            return (i % 2 == 0) ? b5 : b6;
        }
        Block b = b6.getBlock();
        if (b instanceof StairBlock || b instanceof SlabBlock) {
            // Блок из шаблона, мира нет: форму берём в пустом мире. Полный куб — значит, не ступенька, не плита и не решётка.
            return (b5 != null && b5.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
                    ? b5 : Blocks.POLISHED_BASALT.defaultBlockState();
        }
        return b6;
    }

    private static boolean isLight(BlockState s) { return s.is(Blocks.SEA_LANTERN); }
    private static boolean isStripe(BlockState s) { return s.is(Blocks.SEA_LANTERN) || s.is(Blocks.SNOW_BLOCK); }

    private static long colKey(BlockPos p) {
        return ((long) p.getX() << 32) ^ (p.getZ() & 0xffffffffL);
    }
}
