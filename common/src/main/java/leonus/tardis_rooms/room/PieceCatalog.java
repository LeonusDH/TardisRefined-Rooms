package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.room.custom.CustomRoomInfo;
import leonus.tardis_rooms.room.custom.CustomRoomPlan;
import leonus.tardis_rooms.room.custom.CustomRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import whocraft.tardis_refined.common.world.chunk.ARSStructurePiece;
import whocraft.tardis_refined.registry.TRARSStructurePieceRegistry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Всё, что можно пристроить к узлу, и как именно это ставится. Общий код: меню берёт отсюда
 * подписи и превью, сервер — план постройки (какие модули на каких этажах займёт и как строить).
 */
public final class PieceCatalog {
    public enum Group {
        CORRIDOR, ROOM, LIFT, PRESET, SPECIAL, DATAPACK;

        /** Группа меню для помещения из датапака по её имени в JSON. */
        public static Group ofCustom(String name) {
            return switch (name) {
                case "corridor" -> CORRIDOR;
                case "room" -> ROOM;
                case "special" -> SPECIAL;
                default -> DATAPACK;
            };
        }
    }

    public enum CorridorShape {
        STRAIGHT(true, false, false), LEFT(false, true, false), RIGHT(false, false, true),
        T_LR(false, true, true), T_FL(true, true, false), T_FR(true, false, true), CROSS(true, true, true);
        final boolean forward, left, right;
        CorridorShape(boolean f, boolean l, boolean r) { forward = f; left = l; right = r; }
        public String id() { return name().toLowerCase(); }

        public Set<Direction> sides(Direction facing) {
            Set<Direction> s = EnumSet.of(facing.getOpposite());
            if (forward) s.add(facing);
            if (left) s.add(facing.getCounterClockWise());
            if (right) s.add(facing.getClockWise());
            return s;
        }
    }

    public enum RoomType {
        SMALL(1, 1, Layout.Align.NONE, false), LONG(2, 1, Layout.Align.NONE, false),
        LARGE(2, 2, Layout.Align.RIGHT, false), HALL(2, 2, Layout.Align.NEGATIVE, true);
        final int depth, width;
        final Layout.Align align;
        final boolean allExits;
        RoomType(int d, int w, Layout.Align a, boolean all) { depth = d; width = w; align = a; allExits = all; }
        public String id() { return name().toLowerCase(); }

        Layout layout(Node node) {
            return Layout.room(node.targetX(), node.targetZ(), node.side(), depth, width, align,
                    allExits ? ALL_SIDES : Set.of());
        }

        private static final Set<Direction> ALL_SIDES = Set.copyOf(Lattice.SIDES);
    }

    /** Высота комнатной плитки TR и высота двухэтажного зала (от пола нижнего этажа до потолка верхнего). */
    public static final int BASE_HEIGHT = 9;
    public static final int TWO_FLOOR_HEIGHT = Lattice.BAND + 27;

    /**
     * Наибольшая высота комнаты (в ярусах по {@link #BASE_HEIGHT}) на уровне {@code floor}.
     * Комната ×3 (27 блоков) ещё помещается в полосу уровня (28) под следующим уровнем, но на верхнем уровне
     * дошла бы до 152 — за край камня TARDIS (150), в слой бедрока. Поэтому на верхнем уровне — не выше ×2.
     */
    public static int maxRoomHeight(int floor) {
        int h = Lattice.above(floor) > 0 ? 3 : 2;
        // На всякий случай: комната не должна доставать до следующего уровня (между ними — хотя бы слой камня).
        while (h > 1 && BASE_HEIGHT * h > Lattice.BAND - 1) h--;
        return h;
    }

    public static final ResourceLocation GRAVITY_WELL = rl("gravity_well");
    public static final ResourceLocation HALL_2F = rl("room/hall_2f");

    /**
     * Превью: текстура, её размер в блоках и центр прохода входа (в блоках от левого верхнего угла).
     * {@code entrySide} — сторона входа на самой текстуре, если она нарисована не по сторонам света мира
     * (помещения из датапаков: план шаблона без поворота); null — текстура уже повёрнута как в мире.
     */
    public record Preview(ResourceLocation texture, int widthBlocks, int depthBlocks, int entryX, int entryZ, Direction entrySide) {
        public Preview(ResourceLocation texture, int widthBlocks, int depthBlocks, int entryX, int entryZ) {
            this(texture, widthBlocks, depthBlocks, entryX, entryZ, null);
        }
    }

    public record Option(Group group, ResourceLocation id, Component label, Component tooltip, Preview preview) {}

    /** Выход помещения: модуль и сторона, через которую проход ведёт наружу. */
    public record ModSide(long module, Direction side) {}

    /** План постройки: модули и выходы по этажам и действие. */
    /**
     * {@code coreFloor} — уровень блока управления (null — нижний уровень помещения);
     * {@code coreExact} — точное место блока управления (помещения из датапаков), иначе ищется место у {@code coreSpot}.
     */
    public record Plan(Map<Integer, Set<Long>> footprint, Map<Integer, Set<ModSide>> exits, Consumer<ServerLevel> build, int[] coreSpot, Integer coreFloor, BlockPos coreExact) {
        Plan(Map<Integer, Set<Long>> footprint, Map<Integer, Set<ModSide>> exits, Consumer<ServerLevel> build, int[] coreSpot, Integer coreFloor) {
            this(footprint, exits, build, coreSpot, coreFloor, null);
        }

        Plan(Map<Integer, Set<Long>> footprint, Map<Integer, Set<ModSide>> exits, Consumer<ServerLevel> build, int[] coreSpot) {
            this(footprint, exits, build, coreSpot, null);
        }

        Plan(Map<Integer, Set<Long>> footprint, Map<Integer, Set<ModSide>> exits, Consumer<ServerLevel> build) {
            this(footprint, exits, build, null, null);
        }
    }

    /**
     * Нижний уровень помещения на два уровня от узла: обычно уровень узла (растёт вверх),
     * на верхнем уровне — уровень под ним (растёт вниз); −1, если двух уровней не набрать.
     */
    public static int twoFloorBase(int floor) {
        if (Lattice.above(floor) > 0) return floor;
        return Lattice.below(floor);
    }

    /** Выходы раскладки в виде «модуль + сторона». */
    static Set<ModSide> exitsOf(Layout l) {
        Set<ModSide> out = new HashSet<>();
        for (Layout.Exit e : l.exits) {
            int off = e.along() / 4;
            int mx = switch (e.side()) { case WEST -> l.mx0; case EAST -> l.mx0 + l.mw - 1; default -> l.mx0 + off; };
            int mz = switch (e.side()) { case NORTH -> l.mz0; case SOUTH -> l.mz0 + l.md - 1; default -> l.mz0 + off; };
            out.add(new ModSide(Lattice.key(mx, mz), e.side()));
        }
        return out;
    }

    /** Выходы клетки TR (48): по проходу на каждой стороне, на «чётном» модуле. */
    static Set<ModSide> trExits(int cx, int cz) {
        return Set.of(new ModSide(Lattice.key(cx * 2, cz * 2), Direction.NORTH), new ModSide(Lattice.key(cx * 2, cz * 2 + 1), Direction.SOUTH),
                new ModSide(Lattice.key(cx * 2, cz * 2), Direction.WEST), new ModSide(Lattice.key(cx * 2 + 1, cz * 2), Direction.EAST));
    }

    // ---------- список вариантов для узла ----------

    /**
     * Варианты для узла. {@code custom} — помещения из датапаков: на сервере {@code CustomRooms.INSTANCE.infos()},
     * на клиенте — присланные сервером ({@code ClientCustomRooms.list()}).
     */
    public static List<Option> options(Node node, List<CustomRoomInfo> custom) {
        List<Option> list = new ArrayList<>();
        Direction facing = node.side();
        for (CorridorShape shape : CorridorShape.values()) {
            Layout l = Layout.corridor(node.targetX(), node.targetZ(), shape.sides(facing));
            list.add(new Option(Group.CORRIDOR, rl("corridor/" + shape.id()),
                    TRoomsText.corridor(shape.id()).text(),
                    TRoomsText.CORRIDOR_TOOLTIP.text(),
                    layoutPreview("m_corridor_" + letters(shape.sides(facing)), l, facing)));
        }
        for (RoomType type : RoomType.values()) {
            Layout l = type.layout(node);
            Preview pv = layoutPreview("m_room_" + type.id() + "_" + letter(facing), l, facing);
            for (int h = 1; h <= maxRoomHeight(node.floorY()); h++) {
                list.add(new Option(Group.ROOM, rl("room/" + type.id() + "_x" + h),
                        TRoomsText.room(type.id()).text().append(" ×" + h),
                        TRoomsText.roomTooltip(type.id()).text(BASE_HEIGHT * h), pv));
            }
        }
        for (boolean up : new boolean[]{true, false}) {
            if ((up ? Lattice.above(node.floorY()) : Lattice.below(node.floorY())) < 0) continue;
            for (CorridorShape shape : CorridorShape.values()) {
                Layout l = Layout.corridor(node.targetX(), node.targetZ(), shape.sides(facing));
                list.add(new Option(Group.LIFT, liftId(up, shape),
                        (up ? TRoomsText.LIFT_UP : TRoomsText.LIFT_DOWN).text(TRoomsText.corridor(shape.id()).text()),
                        (up ? TRoomsText.LIFT_UP_TOOLTIP : TRoomsText.LIFT_DOWN_TOOLTIP).text(),
                        layoutPreview("m_corridor_" + letters(shape.sides(facing)), l, facing)));
            }
            Layout end = Layout.corridor(node.targetX(), node.targetZ(), Set.of(facing.getOpposite()));
            for (LiftExit e : LiftExit.values()) {
                list.add(new Option(Group.LIFT, rl("lift/" + (up ? "up" : "down") + "_end_" + e.id()),
                        (up ? TRoomsText.LIFT_UP : TRoomsText.LIFT_DOWN).text(TRoomsText.liftEnd(e.id()).text()),
                        (up ? TRoomsText.LIFT_END_UP_TOOLTIP : TRoomsText.LIFT_END_DOWN_TOOLTIP).text(),
                        layoutPreview("m_corridor_" + letter(facing.getOpposite()), end, facing)));
            }
        }
        list.add(new Option(Group.ROOM, HALL_2F, TRoomsText.ROOM_HALL_2F.text(),
                TRoomsText.ROOM_HALL_2F_TOOLTIP.text(),
                layoutPreview("m_room_hall_" + letter(facing), RoomType.HALL.layout(node), facing)));
        int i = 1;
        for (ARSStructurePiece p : TRARSStructurePieceRegistry.ROOMS) list.add(preset(p, TRoomsText.PRESET_ROOM, i++, facing));
        i = 1;
        for (ARSStructurePiece p : TRARSStructurePieceRegistry.CORRIDORS) list.add(preset(p, TRoomsText.PRESET_CORRIDOR, i++, facing));
        list.add(new Option(Group.SPECIAL, GRAVITY_WELL, TRoomsText.GRAVITY_WELL.text(),
                TRoomsText.GRAVITY_WELL_TOOLTIP.text(), trPreview("gravity_well", facing)));
        for (CustomRoomInfo c : custom) list.add(custom(c));
        return list;
    }

    private static Option custom(CustomRoomInfo c) {
        int[] e = c.entranceBlock();
        Component tip = c.description().map(Component::copy)
                .orElseGet(() -> TRoomsText.CUSTOM_TOOLTIP.text(c.id().getNamespace()));
        return new Option(Group.ofCustom(c.group()), c.id(), c.name().copy(), tip,
                new Preview(c.previewTexture(), c.widthBlocks(), c.depthBlocks(), e[0], e[1], c.entrance().side()));
    }

    private static Option preset(ARSStructurePiece p, TRoomsText name, int index, Direction facing) {
        ResourceLocation id = p.getResourceLocation();
        String path = id.getPath();
        return new Option(Group.PRESET, id, name.text(index),
                TRoomsText.PRESET_TOOLTIP.text(path.substring(path.lastIndexOf('/') + 1)),
                trPreview(path.replace('/', '_'), facing));
    }

    private static Preview layoutPreview(String name, Layout l, Direction facing) {
        Layout.Exit entry = l.exits.stream().filter(e -> e.side() == facing.getOpposite()).findFirst().orElse(l.exits.get(0));
        int[] b = l.exitBlock(entry);
        return new Preview(texture(name), l.mw * Lattice.MODULE, l.md * Lattice.MODULE, b[0], b[1]);
    }

    private static Preview trPreview(String name, Direction facing) {
        int[] b = switch (facing.getOpposite()) {
            case NORTH -> new int[]{9, 0};
            case SOUTH -> new int[]{9, 47};
            case WEST -> new int[]{0, 9};
            default -> new int[]{47, 9};
        };
        return new Preview(texture(name), 48, 48, b[0], b[1]);
    }

    // ---------- план постройки (сервер) ----------

    public static boolean isPreset(ResourceLocation id) {
        for (ARSStructurePiece p : TRARSStructurePieceRegistry.ROOMS) if (p.getResourceLocation().equals(id)) return true;
        for (ARSStructurePiece p : TRARSStructurePieceRegistry.CORRIDORS) if (p.getResourceLocation().equals(id)) return true;
        return false;
    }

    /** Нужна ли варианту сетка клеток TR (48): пресеты TR и колодец ставятся только по ней. */
    public static boolean needsTrGrid(ResourceLocation id) {
        return GRAVITY_WELL.equals(id) || isPreset(id);
    }

    /** Клетка TR (48), в которую ведёт узел, если узел стоит ровно на проходе клетки TR. */
    public static Optional<int[]> trCell(Node node) {
        int tx = node.targetX(), tz = node.targetZ();
        boolean ok = switch (node.side()) {
            case NORTH -> Math.floorMod(tx, 2) == 0 && Math.floorMod(tz, 2) == 1;
            case SOUTH -> Math.floorMod(tx, 2) == 0 && Math.floorMod(tz, 2) == 0;
            case WEST -> Math.floorMod(tz, 2) == 0 && Math.floorMod(tx, 2) == 1;
            case EAST -> Math.floorMod(tz, 2) == 0 && Math.floorMod(tx, 2) == 0;
            default -> false;
        };
        return ok ? Optional.of(new int[]{Math.floorDiv(tx, 2), Math.floorDiv(tz, 2)}) : Optional.empty();
    }

    public static Optional<Plan> plan(ResourceLocation id, Node node) {
        Direction facing = node.side();
        CorridorShape shape = corridorShape(id);
        if (shape != null) {
            Layout l = Layout.corridor(node.targetX(), node.targetZ(), shape.sides(facing));
            return Optional.of(single(l, node.floorY(), lv -> l.place(lv, node.floorY(), BASE_HEIGHT)));
        }
        String lift = sub(id, "lift/");
        if (lift != null) return liftPlan(lift, node);
        if (HALL_2F.equals(id)) {
            int base = twoFloorBase(node.floorY());
            if (base < 0) return Optional.empty();
            Layout l = RoomType.HALL.layout(node);
            Map<Integer, Set<Long>> fp = new HashMap<>();
            Map<Integer, Set<ModSide>> ex = new HashMap<>();
            for (int f : List.of(base, base + Lattice.BAND)) { fp.put(f, l.footprint()); ex.put(f, exitsOf(l)); }
            return Optional.of(new Plan(fp, ex, lv -> buildTwoFloorHall(lv, l, base), l.coreSpot()));
        }
        RoomType type = roomType(id);
        int h = roomHeight(id);
        if (type != null && h > 0 && h <= maxRoomHeight(node.floorY())) {
            Layout l = type.layout(node);
            return Optional.of(single(l, node.floorY(), lv -> l.place(lv, node.floorY(), BASE_HEIGHT * h)));
        }
        if (needsTrGrid(id)) {
            Optional<int[]> cell = trCell(node);
            if (cell.isEmpty()) return Optional.empty();
            int cx = cell.get()[0], cz = cell.get()[1];
            Set<Long> mods = new HashSet<>();
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) mods.add(Lattice.key(cx * 2 + dx, cz * 2 + dz));
            BlockPos origin = new BlockPos(Lattice.origin(cx * 2), 0, Lattice.origin(cz * 2));
            Map<Integer, Set<Long>> fp = new HashMap<>();
            Map<Integer, Set<ModSide>> ex = new HashMap<>();
            if (GRAVITY_WELL.equals(id)) {
                int base = twoFloorBase(node.floorY());
                if (base < 0) return Optional.empty();
                for (int f : List.of(base, base + Lattice.BAND)) { fp.put(f, mods); ex.put(f, trExits(cx, cz)); }
                return Optional.of(new Plan(fp, ex, lv -> {
                    placeTemplate(lv, Layout.WELL_BOTTOM, origin.atY(base));
                    placeTemplate(lv, Layout.WELL_TOP, origin.atY(base + Lattice.BAND));
                }));
            }
            fp.put(node.floorY(), mods);
            ex.put(node.floorY(), trExits(cx, cz));
            return Optional.of(new Plan(fp, ex, lv -> placeTemplate(lv, id, origin.atY(node.floorY()))));
        }
        Optional<CustomRooms.Loaded> custom = CustomRooms.INSTANCE.get(id);
        if (custom.isPresent()) return CustomRoomPlan.plan(custom.get(), node);
        return Optional.empty();
    }

    // ---------- лифт ----------

    /** Куда ведёт единственный выход лифта-тупика на другом уровне (относительно направления роста). */
    public enum LiftExit {
        BACK, FORWARD, LEFT, RIGHT;
        public String id() { return name().toLowerCase(); }

        Direction side(Direction facing) {
            return switch (this) {
                case BACK -> facing.getOpposite();
                case FORWARD -> facing;
                case LEFT -> facing.getCounterClockWise();
                case RIGHT -> facing.getClockWise();
            };
        }
    }

    private static ResourceLocation liftId(boolean up, CorridorShape shape) {
        return rl("lift/" + (up ? "up_" : "down_") + shape.id());
    }

    /**
     * Лифт: один модуль на два соседних уровня — шахта TR с гравитационным блоком.
     * На уровне узла проходы выбранной формы, на втором — перекрёсток (лишние проходы станут узлами роста).
     * Блок управления — на уровне узла, в проходе входа в паре блоков от площадки шахты.
     */
    private static Optional<Plan> liftPlan(String spec, Node node) {
        boolean up = spec.startsWith("up_");
        if (!up && !spec.startsWith("down_")) return Optional.empty();
        String s = spec.substring(up ? 3 : 5);
        Direction facing = node.side();
        Set<Direction> hereSides = null, thereSides = null;
        if (s.startsWith("end_")) {
            // Тупик: здесь только вход, на другом уровне один выход.
            for (LiftExit e : LiftExit.values()) if (("end_" + e.id()).equals(s)) {
                hereSides = Set.of(facing.getOpposite());
                thereSides = Set.of(e.side(facing));
            }
        } else {
            for (CorridorShape c : CorridorShape.values()) if (c.id().equals(s)) {
                hereSides = c.sides(facing);
                thereSides = CorridorShape.CROSS.sides(facing);
            }
        }
        if (hereSides == null) return Optional.empty();
        int start = node.floorY();
        int other = up ? Lattice.above(start) : Lattice.below(start);
        if (other < 0) return Optional.empty();
        Layout here = Layout.corridor(node.targetX(), node.targetZ(), hereSides);
        Layout there = Layout.corridor(node.targetX(), node.targetZ(), thereSides);
        int lower = Math.min(start, other), upper = Math.max(start, other);
        Layout lowerL = up ? here : there, upperL = up ? there : here;
        Map<Integer, Set<Long>> fp = new HashMap<>();
        Map<Integer, Set<ModSide>> ex = new HashMap<>();
        fp.put(start, here.footprint()); ex.put(start, exitsOf(here));
        fp.put(other, there.footprint()); ex.put(other, exitsOf(there));
        int[] c = here.coreSpot();
        Direction back = facing.getOpposite();
        int[] core = {c[0] + back.getStepX() * Layout.LIFT_CORE_OFFSET, c[1] + back.getStepZ() * Layout.LIFT_CORE_OFFSET};
        return Optional.of(new Plan(fp, ex, lv -> Layout.buildLift(lv, lowerL, upperL, lower, upper), core, start));
    }

    private static Plan single(Layout l, int floorY, Consumer<ServerLevel> build) {
        Map<Integer, Set<Long>> fp = new HashMap<>();
        Map<Integer, Set<ModSide>> ex = new HashMap<>();
        fp.put(floorY, l.footprint());
        ex.put(floorY, exitsOf(l));
        return new Plan(fp, ex, build, l.coreSpot());
    }

    private static void placeTemplate(ServerLevel level, ResourceLocation id, BlockPos pos) {
        level.getStructureManager().get(id).ifPresent(t ->
                t.placeInWorld(level, pos, pos, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS));
    }

    /** Кольцо смотровой галереи двухэтажного зала: радиусы от центра зала. */
    private static final double GALLERY_R_IN = 11.5, GALLERY_R_OUT = 16.5, GALLERY_LAMP_R = 14;

    /**
     * Зал на оба этажа: растянутые стены, двери внизу; наверху арки дверей, круглая смотровая
     * галерея в центре зала и мостики от каждой верхней двери к ней.
     */
    private static void buildTwoFloorHall(ServerLevel level, Layout l, int base) {
        l.place(level, base, TWO_FLOOR_HEIGHT);
        int ox = Lattice.origin(l.mx0), oz = Lattice.origin(l.mz0);
        int fy = base + Lattice.BAND;
        int[] in = l.interior();
        int[] c = l.coreSpot();
        Set<Long> walk = new HashSet<>();

        for (int x = in[0]; x <= in[2]; x++)
            for (int z = in[1]; z <= in[3]; z++) {
                double d = Math.hypot(x - c[0], z - c[1]);
                if (d >= GALLERY_R_IN && d <= GALLERY_R_OUT) walk.add(BlockPos.asLong(x, 0, z));
            }

        for (Layout.Exit e : l.exits) {
            Direction side = e.side();
            int tileX = switch (side) { case WEST -> 0; case EAST -> l.tx - 1; default -> e.along(); };
            int tileZ = switch (side) { case NORTH -> 0; case SOUTH -> l.tz - 1; default -> e.along(); };
            // Проход за стеной до края зала, затем арка двери (стенная часть дверной плитки TR с порогом).
            l.placeCorridorTile(level, tileX, tileZ, fy, side);
            int doorTX = tileX - side.getStepX(), doorTZ = tileZ - side.getStepZ();
            Layout.stampDoorFrame(level, side, new BlockPos(ox + doorTX * 6, fy, oz + doorTZ * 6));

            // Мостик от двери к кольцу: d — от линии стены внутрь, a — поперёк (d = 1 — ряд стоек стены).
            Direction inward = side.getOpposite();
            Direction along = side.getClockWise();
            int wallX = ox + (side == Direction.WEST ? 6 : side == Direction.EAST ? (l.tx - 1) * 6 : e.along() * 6 + 3);
            int wallZ = oz + (side == Direction.NORTH ? 6 : side == Direction.SOUTH ? (l.tz - 1) * 6 : e.along() * 6 + 3);
            for (int d = 1; d <= 16; d++) {
                for (int a = -2; a <= 2; a++) {
                    if (d == 1 && Math.abs(a) > 1) continue;
                    int x = wallX + inward.getStepX() * d + along.getStepX() * a, z = wallZ + inward.getStepZ() * d + along.getStepZ() * a;
                    if (x >= in[0] && x <= in[2] && z >= in[1] && z <= in[3]) walk.add(BlockPos.asLong(x, 0, z));
                }
                int mx = wallX + inward.getStepX() * d, mz = wallZ + inward.getStepZ() * d;
                double dist = Math.hypot(mx - c[0], mz - c[1]);
                if (dist >= GALLERY_R_IN && dist <= GALLERY_R_OUT) break;
            }
        }

        BlockState floorTop = Blocks.CYAN_TERRACOTTA.defaultBlockState(), floorBase = Blocks.BLACK_CONCRETE.defaultBlockState();
        for (long p : walk) {
            int x = BlockPos.getX(p), z = BlockPos.getZ(p);
            level.setBlock(new BlockPos(x, fy, z), floorBase, Block.UPDATE_CLIENTS);
            level.setBlock(new BlockPos(x, fy + 1, z), floorTop, Block.UPDATE_CLIENTS);
        }
        // Перила: по краю настила, где за ним пустота зала (у стоек и стен — нет).
        Set<Long> rails = new HashSet<>();
        for (long p : walk) {
            int x = BlockPos.getX(p), z = BlockPos.getZ(p);
            for (Direction dir : Lattice.SIDES) {
                int nx = x + dir.getStepX(), nz = z + dir.getStepZ();
                if (walk.contains(BlockPos.asLong(nx, 0, nz))) continue;
                if (nx < in[0] || nx > in[2] || nz < in[1] || nz > in[3]) continue;
                if (level.getBlockState(new BlockPos(nx, fy + 1, nz)).isAir()) { rails.add(p); break; }
            }
        }
        for (long p : rails) level.setBlock(new BlockPos(BlockPos.getX(p), fy + 2, BlockPos.getZ(p)), Blocks.IRON_BARS.defaultBlockState(), Block.UPDATE_ALL);
        // Фонари в полу галереи по кругу.
        for (int k = 0; k < 8; k++) {
            double ang = k * Math.PI / 4;
            int x = (int) Math.round(c[0] + GALLERY_LAMP_R * Math.cos(ang)), z = (int) Math.round(c[1] + GALLERY_LAMP_R * Math.sin(ang));
            long p = BlockPos.asLong(x, 0, z);
            if (walk.contains(p) && !rails.contains(p)) level.setBlock(new BlockPos(x, fy + 1, z), Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---------- разбор id ----------

    public static CorridorShape corridorShape(ResourceLocation id) {
        String s = sub(id, "corridor/");
        if (s == null) return null;
        for (CorridorShape shape : CorridorShape.values()) if (shape.id().equals(s)) return shape;
        return null;
    }

    public static RoomType roomType(ResourceLocation id) {
        String s = sub(id, "room/");
        if (s == null || !s.contains("_x")) return null;
        String t = s.substring(0, s.lastIndexOf("_x"));
        for (RoomType type : RoomType.values()) if (type.id().equals(t)) return type;
        return null;
    }

    public static int roomHeight(ResourceLocation id) {
        String s = sub(id, "room/");
        if (s == null || !s.contains("_x")) return -1;
        try {
            int h = Integer.parseInt(s.substring(s.lastIndexOf("_x") + 2));
            return (h >= 1 && h <= 3) ? h : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String sub(ResourceLocation id, String prefix) {
        if (!id.getNamespace().equals(TardisRooms.MODID) || !id.getPath().startsWith(prefix)) return null;
        return id.getPath().substring(prefix.length());
    }

    private static String letters(Set<Direction> sides) {
        StringBuilder b = new StringBuilder();
        for (Direction d : Lattice.SIDES) if (sides.contains(d)) b.append(letter(d));
        return b.toString();
    }

    private static String letter(Direction d) {
        return switch (d) { case NORTH -> "n"; case SOUTH -> "s"; case WEST -> "w"; case EAST -> "e"; default -> "?"; };
    }

    private static ResourceLocation texture(String name) {
        return rl("textures/gui/preview/" + name + ".png");
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, path);
    }

    private PieceCatalog() {}
}
