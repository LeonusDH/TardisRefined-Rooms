package leonus.tardis_rooms.room.custom;

import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.Node;
import leonus.tardis_rooms.room.PieceCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * План постройки помещения из датапака у узла: шаблон поворачивается так, чтобы его вход смотрел на узел,
 * и ставится так, чтобы модуль входа оказался за узлом. Этажи шаблона — уровни подряд вверх от уровня узла.
 */
public final class CustomRoomPlan {

    public static Optional<PieceCatalog.Plan> plan(CustomRooms.Loaded room, Node node) {
        CustomRoom def = room.def();
        Direction entry = node.side().getOpposite();
        if (!def.rotatable() && def.entrance().side() != entry) return Optional.empty();
        Rotation rot = rotationFor(def.entrance().side(), entry);

        // Уровни: этаж 0 — уровень узла, следующие — выше. Наверху есть только 18 блоков высоты (выше — бедрок).
        List<Integer> floors = new ArrayList<>();
        int f = node.floorY();
        for (int i = 0; i < def.floors(); i++) {
            if (f < 0) return Optional.empty();
            if (Lattice.above(f) < 0 && room.templates().get(i).getSize().getY() > PieceCatalog.BASE_HEIGHT * PieceCatalog.maxRoomHeight(f))
                return Optional.empty();
            floors.add(f);
            f = Lattice.above(f);
        }

        // Сдвиг: левый верхний угол модуля входа после поворота — на начало модуля за узлом.
        BlockPos entranceMin = rotatedModuleMin(def.entrance().x(), def.entrance().z(), rot);
        int dx = Lattice.origin(node.targetX()) - entranceMin.getX();
        int dz = Lattice.origin(node.targetZ()) - entranceMin.getZ();

        Set<Long> modules = new HashSet<>();
        for (int i = 0; i < room.width(); i++)
            for (int j = 0; j < room.depth(); j++) modules.add(moduleAt(i, j, rot, dx, dz));
        Map<Integer, Set<Long>> footprint = new HashMap<>();
        Map<Integer, Set<PieceCatalog.ModSide>> exits = new HashMap<>();
        for (int floor : floors) {
            footprint.put(floor, modules);
            exits.put(floor, new HashSet<>());
        }
        List<CustomRoom.Exit> all = new ArrayList<>(def.exits());
        all.add(def.entrance());
        for (CustomRoom.Exit e : all)
            exits.get(floors.get(e.floor())).add(new PieceCatalog.ModSide(moduleAt(e.x(), e.z(), rot, dx, dz), rot.rotate(e.side())));

        BlockPos core = def.controlBlock()
                .map(p -> StructureTemplate.transform(p, Mirror.NONE, rot, BlockPos.ZERO).offset(dx, floors.get(0), dz))
                .orElse(null);
        int[] coreSpot = core != null ? new int[]{core.getX(), core.getZ()} : null;

        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rot);
        return Optional.of(new PieceCatalog.Plan(footprint, exits, level -> {
            for (int i = 0; i < floors.size(); i++) {
                BlockPos at = new BlockPos(dx, floors.get(i), dz);
                room.templates().get(i).placeInWorld(level, at, at, settings, level.getRandom(), Block.UPDATE_CLIENTS);
                int height = room.templates().get(i).getSize().getY();
                for (CustomRoom.Exit e : all) {
                    if (e.floor() != i || !flipped(e, rot, dx, dz)) continue;
                    long m = moduleAt(e.x(), e.z(), rot, dx, dz);
                    mirrorDoorway(level, Lattice.keyX(m), Lattice.keyZ(m), rot.rotate(e.side()), floors.get(i), height);
                }
            }
        }, coreSpot, null, core));
    }

    // ---------- проход после поворота ----------

    /**
     * Проходы сетки — на блоках 8..10 стороны модуля (считая от северного/западного края), это не середина стены.
     * Поворот, переворачивающий отсчёт вдоль стены, уводит проход шаблона на 13..15. Тогда полосу стены 8..15
     * (на всю высоту и {@link #DOORWAY_DEPTH} блока вглубь) отражаем: проход встаёт на место, стена — рядом.
     */
    static final int DOORWAY_MIN = Lattice.OPEN_MIN, DOORWAY_MAX = Lattice.MODULE - 1 - Lattice.OPEN_MIN, DOORWAY_DEPTH = 3;

    /** Ушёл ли проход этого выхода после поворота с 8..10 на 13..15. */
    private static boolean flipped(CustomRoom.Exit e, Rotation rot, int dx, int dz) {
        int ox = e.x() * Lattice.MODULE, oz = e.z() * Lattice.MODULE, last = Lattice.MODULE - 1;
        BlockPos p = switch (e.side()) {
            case NORTH -> new BlockPos(ox + Lattice.OPEN_MIN, 0, oz);
            case SOUTH -> new BlockPos(ox + Lattice.OPEN_MIN, 0, oz + last);
            case WEST -> new BlockPos(ox, 0, oz + Lattice.OPEN_MIN);
            default -> new BlockPos(ox + last, 0, oz + Lattice.OPEN_MIN);
        };
        BlockPos w = StructureTemplate.transform(p, Mirror.NONE, rot, BlockPos.ZERO).offset(dx, 0, dz);
        Direction side = rot.rotate(e.side());
        int along = side.getAxis() == Direction.Axis.Z ? w.getX() - Lattice.origin(Lattice.moduleOf(w.getX()))
                : w.getZ() - Lattice.origin(Lattice.moduleOf(w.getZ()));
        return along != Lattice.OPEN_MIN;
    }

    /** Отражает полосу стены 8..15 у стороны {@code side} модуля: блок a ↔ 23 − a, с отражением самих блоков. */
    private static void mirrorDoorway(ServerLevel level, int mx, int mz, Direction side, int floorY, int height) {
        // Вдоль северной/южной стены отсчёт идёт по x — отражаем x (FRONT_BACK), вдоль западной/восточной — z (LEFT_RIGHT).
        Mirror mirror = side.getAxis() == Direction.Axis.Z ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
        Direction inward = side.getOpposite();
        int n = DOORWAY_MAX - DOORWAY_MIN + 1;
        for (int dy = 0; dy < height; dy++)
            for (int depth = 0; depth < DOORWAY_DEPTH; depth++) {
                BlockPos[] pos = new BlockPos[n];
                BlockState[] states = new BlockState[n];
                CompoundTag[] data = new CompoundTag[n];
                for (int k = 0; k < n; k++) {
                    pos[k] = Lattice.openingPos(mx, mz, side, floorY, DOORWAY_MIN + k, dy).relative(inward, depth);
                    states[k] = level.getBlockState(pos[k]);
                    BlockEntity be = level.getBlockEntity(pos[k]);
                    data[k] = be != null ? be.saveWithoutMetadata(level.registryAccess()) : null;
                }
                for (int k = 0; k < n; k++) {
                    int from = n - 1 - k;
                    BlockState to = states[from].mirror(mirror);
                    if (!level.getBlockState(pos[k]).equals(to)) {
                        // Без высыпания содержимого: данные уже сохранены и переедут вместе с блоком.
                        if (level.getBlockEntity(pos[k]) != null) level.removeBlockEntity(pos[k]);
                        level.setBlock(pos[k], to, Block.UPDATE_CLIENTS);
                    }
                    if (data[from] != null && level.getBlockEntity(pos[k]) instanceof BlockEntity be) {
                        be.loadWithComponents(data[from], level.registryAccess());
                        be.setChanged();
                    }
                }
            }
    }

    /** Поворот, который переводит сторону {@code from} шаблона в сторону {@code to} мира. */
    static Rotation rotationFor(Direction from, Direction to) {
        for (Rotation r : Rotation.values()) if (r.rotate(from) == to) return r;
        return Rotation.NONE;
    }

    /** Наименьший угол модуля шаблона (в блоках) после поворота вокруг начала шаблона. */
    private static BlockPos rotatedModuleMin(int i, int j, Rotation rot) {
        int last = Lattice.MODULE - 1;
        BlockPos a = StructureTemplate.transform(new BlockPos(i * Lattice.MODULE, 0, j * Lattice.MODULE), Mirror.NONE, rot, BlockPos.ZERO);
        BlockPos b = StructureTemplate.transform(new BlockPos(i * Lattice.MODULE + last, 0, j * Lattice.MODULE + last), Mirror.NONE, rot, BlockPos.ZERO);
        return new BlockPos(Math.min(a.getX(), b.getX()), 0, Math.min(a.getZ(), b.getZ()));
    }

    private static long moduleAt(int i, int j, Rotation rot, int dx, int dz) {
        BlockPos m = rotatedModuleMin(i, j, rot);
        return Lattice.key(Lattice.moduleOf(m.getX() + dx), Lattice.moduleOf(m.getZ() + dz));
    }

    private CustomRoomPlan() {}
}
