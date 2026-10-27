package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.mixin.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.registry.TRBlockRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Готовит хаб каждой TARDIS (при входе игрока, в том числе для уже созданных TARDIS): упрощает его
 * ({@link #rebuildSmall}), если это включено и возможно, и ставит на выходы наружу заглушки с узлами роста,
 * а на выходы в никуда — глухие заглушки.
 */
public final class HubInitializer {
    private static final String DATA_NAME = "tardis_rooms_hub";
    static final int LAYOUT_TR = 0, LAYOUT_SMALL = 1;
    private static final Deque<ResourceKey<Level>> PENDING = new ArrayDeque<>();

    public static void register(IEventBus bus) {
        bus.addListener(HubInitializer::onJoin);
        bus.addListener(HubInitializer::onServerTick);
        // Очередь статическая: в одиночной игре сервер перезапускается при каждом входе в мир.
        bus.addListener((ServerStoppedEvent e) -> PENDING.clear());
    }

    /** Упрощение ещё возможно: хаб TR, и упрощать разрешено. Пока не вышло — пробуем при каждом входе. */
    private static boolean wantsRebuild(HubData d) {
        return d.layout == LAYOUT_TR && TardisRoomsConfig.simplifyHub();
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ServerPlayer && Lattice.isTardisLevel(level)) {
            HubData d = data(level);
            if ((!d.prepared || wantsRebuild(d)) && !PENDING.contains(level.dimension())) PENDING.add(level.dimension());
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        ResourceKey<Level> key = PENDING.poll();
        if (key == null) return;
        ServerLevel level = event.getServer().getLevel(key);
        if (level == null) return;
        HubData data = data(level);
        boolean rebuild = wantsRebuild(data);
        if (data.prepared && !rebuild) return;
        // Хаб другой версии TR (или перестроенный игроком) по координатам из шаблонов не трогаем.
        List<HubPiece> pieces = data.layout == LAYOUT_TR ? loadPieces(level) : List.of();
        boolean known = !pieces.isEmpty() && matchesTemplates(level, pieces);
        if (data.layout == LAYOUT_TR && !known) TardisRooms.debug("Hub of {} differs from TR templates — left as is", key.location());
        if (rebuild && known && rebuildSmall(level, pieces)) data.layout = LAYOUT_SMALL;
        int carved = data.layout == LAYOUT_TR && !data.prepared && known ? carveGaps(level, pieces) : 0;
        int[] placed = sealExits(level);
        data.prepared = true;
        data.setDirty();
        TardisRooms.debug("Hub of {}: layout {}, {} growth panels, {} blind walls, {} blocked doorways cleared",
                key.location(), data.layout == LAYOUT_SMALL ? "small" : "TR", placed[0], placed[1], carved);
    }

    /** Упрощённый ли хаб у этой ТАРДИС (от этого зависит, какие модули — хаб). */
    static boolean isSmallHub(ServerLevel level) {
        return data(level).layout == LAYOUT_SMALL;
    }

    // ---------- выходы ----------

    /**
     * Выходы хаба наружу: к свободному месту — заглушка с узлом роста; в запретную зону или в соседний модуль хаба,
     * если за проходом голый камень TARDIS, — глухая заглушка, чтобы коридор не обрывался в камень.
     * Повторный проход ничего не портит: заглушка ставится только в открытый проход.
     */
    private static int[] sealExits(ServerLevel level) {
        int nodes = 0, walls = 0;
        Block stone = TRBlockRegistry.FOOLS_STONE.get();
        boolean small = isSmallHub(level);
        // У упрощённого хаба наружу выходят только переходники: у смотровой Ока на краю модулей — её же воздух.
        Set<Long> from = small ? Lattice.SMALL_HUB_EXITS : Lattice.hubModules(level);
        for (long m : from) {
            int mx = Lattice.keyX(m), mz = Lattice.keyZ(m);
            for (int floor : Lattice.FLOORS) {
                if (floor > Lattice.HUB_FLOOR) continue;
                for (Direction side : Lattice.SIDES) {
                    int nx = mx + side.getStepX(), nz = mz + side.getStepZ();
                    boolean towardsHub = Lattice.isHub(level, nx, nz);
                    // Внутри хаба TR проходы между модулями — его дело (там коридоры идут и вдоль границ модулей).
                    if (!small && towardsHub) continue;
                    BlockPos np = Lattice.nodePos(mx, mz, side, floor);
                    if (towardsHub || Lattice.isForbidden(level, nx, nz, floor)) {
                        boolean deadEnd = level.getBlockState(np.relative(side)).is(stone);
                        if (deadEnd && Lattice.placePanel(level, mx, mz, side, floor, false)) walls++;
                    } else if (Lattice.looksEmpty(level, nx, nz, floor)) {
                        if (Lattice.placePanel(level, mx, mz, side, floor, true)) {
                            nodes++;
                        } else if (level.getBlockState(np).is(TRoomsBlocks.ROOM_WALL.get())) {
                            // Глухая заглушка осталась с тех пор, когда за ней было нельзя строить (например, хаб
                            // упростился позже, и место Ока стало свободным), — теперь там свободно, нужен узел роста.
                            level.setBlock(np, TRoomsBlocks.ROOM_NODE.get().defaultBlockState(), Block.UPDATE_ALL);
                            nodes++;
                        }
                    }
                }
            }
        }
        return new int[]{nodes, walls};
    }

    // ---------- шаблоны хаба TR ----------

    /** Часть хаба TR: где TR её ставит (как в TardisChunkGenerator), размер и блоки шаблона. */
    private record HubPiece(BlockPos origin, Vec3i size, List<StructureTemplate.StructureBlockInfo> blocks) {}

    private static final String[] PIECE_IDS = {
            "corridors/corridor_hub/corridor_hub_center", "corridors/corridor_hub/corridor_hub_south",
            "corridors/corridor_hub/corridor_hub_south_east", "corridors/corridor_hub/corridor_hub_east",
            "corridors/corridor_hub/corridor_hub_north_east", "corridors/corridor_hub/corridor_hub_north",
            "corridors/corridor_hub/corridor_hub_west"};
    private static final BlockPos[] PIECE_ORIGINS = {
            new BlockPos(992, 76, -16), new BlockPos(992, 97, 32),
            new BlockPos(1040, 97, 32), new BlockPos(1040, 97, -16),
            new BlockPos(1040, 97, -64), new BlockPos(992, 97, -64),
            new BlockPos(944, 97, -16)};

    /** Шаблоны всех частей хаба; пусто, если какого-то нет (другая версия TR). */
    private static List<HubPiece> loadPieces(ServerLevel level) {
        List<HubPiece> out = new ArrayList<>();
        for (int i = 0; i < PIECE_IDS.length; i++) {
            var template = level.getStructureManager().get(ResourceLocation.fromNamespaceAndPath(TardisRefined.MODID, PIECE_IDS[i]));
            if (template.isEmpty()) return List.of();
            var palettes = ((StructureTemplateAccessor) template.get()).tardisRooms$getPalettes();
            if (palettes.isEmpty()) return List.of();
            out.add(new HubPiece(PIECE_ORIGINS[i], template.get().getSize(), palettes.get(0).blocks()));
        }
        return out;
    }

    /**
     * Хаб в мире такой же, как шаблоны TR: на высоте прохода (пол хаба + 3) совпадает не меньше 90 % блоков
     * каждой части. Старые ТАРДИС с другим хабом и хабы, которые игрок перестроил, так не пройдут.
     */
    private static boolean matchesTemplates(ServerLevel level, List<HubPiece> pieces) {
        int checkY = Lattice.HUB_FLOOR + 3;
        for (HubPiece piece : pieces) {
            int total = 0, same = 0;
            for (var info : piece.blocks()) {
                BlockPos w = piece.origin().offset(info.pos());
                if (w.getY() != checkY) continue;
                total++;
                if (level.getBlockState(w).getBlock() == info.state().getBlock()) same++;
            }
            if (total == 0 || same * 10 < total * 9) return false;
        }
        return true;
    }

    // ---------- завалы в шаблонах TR ----------

    /**
     * В шаблонах хаба TR кое-где в проходе забыты блоки (пустое место шаблона — там остаётся камень TARDIS),
     * и коридор оказывается завален. Ищем в шаблоне такие щели — не шире двух блоков, с воздухом шаблона по обе
     * стороны на той же высоте — и, если в мире там камень TARDIS, делаем проход.
     */
    private static int carveGaps(ServerLevel level, List<HubPiece> pieces) {
        Block stone = TRBlockRegistry.FOOLS_STONE.get();
        int carved = 0;
        for (HubPiece piece : pieces) {
            Set<BlockPos> present = new HashSet<>(), air = new HashSet<>();
            for (var info : piece.blocks()) {
                present.add(info.pos());
                if (info.state().isAir()) air.add(info.pos());
            }
            Set<BlockPos> gaps = new HashSet<>();
            Vec3i size = piece.size();
            for (int x = 0; x < size.getX(); x++)
                for (int y = 0; y < size.getY(); y++)
                    for (int z = 0; z < size.getZ(); z++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (!present.contains(p) && (gap(p, 1, 0, present, air) || gap(p, 0, 1, present, air))) gaps.add(p);
                    }
            for (BlockPos p : gaps) {
                // Завал прохода — щель в два блока высотой и больше; одиночная дырка (в потолке) — не проход.
                if (!gaps.contains(p.above()) && !gaps.contains(p.below())) continue;
                BlockPos w = piece.origin().offset(p);
                // Только проходы этажа хаба (над полом): ниже центр хаба заходит в Око, там пустоты шаблона нарочные.
                if (w.getY() < Lattice.HUB_FLOOR + 2 || w.getY() >= Lattice.FLOOR_UPPER) continue;
                if (level.getBlockState(w).is(stone)) {
                    level.setBlock(w, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    carved++;
                }
            }
        }
        return carved;
    }

    /** Пустое место шаблона {@code p} — в щели вдоль оси (dx, dz): воздух шаблона с обеих сторон не дальше двух блоков. */
    private static boolean gap(BlockPos p, int dx, int dz, Set<BlockPos> present, Set<BlockPos> air) {
        return airWithin(p, dx, dz, present, air) && airWithin(p, -dx, -dz, present, air);
    }

    private static boolean airWithin(BlockPos p, int dx, int dz, Set<BlockPos> present, Set<BlockPos> air) {
        for (int i = 1; i <= 2; i++) {
            BlockPos q = p.offset(dx * i, 0, dz * i);
            if (air.contains(q)) return true;
            if (present.contains(q)) return false; // упёрлись в стену шаблона
        }
        return false;
    }

    // ---------- упрощение хаба ----------

    /** Прямоугольник по x/z (мировые координаты, включительно). */
    private record Box(int x0, int z0, int x1, int z1) {
        boolean contains(BlockPos p) {
            return p.getX() >= x0 && p.getX() <= x1 && p.getZ() >= z0 && p.getZ() <= z1;
        }

        AABB aabb(int lo, int hi) {
            return new AABB(x0, lo, z0, x1 + 1, hi + 1, z1 + 1);
        }

        static Box module(int mx, int mz) {
            int x = Lattice.origin(mx), z = Lattice.origin(mz);
            return new Box(x, z, x + Lattice.MODULE - 1, z + Lattice.MODULE - 1);
        }
    }

    /** Модули хаба TR, которые после упрощения становятся свободным местом (заливаются камнем целиком). */
    private static final int[][] CLEARED_MODULES = {
            {40, 0}, {41, 0}, {40, 1},                       // запад (кроме переходника 41,1)
            {42, -2}, {43, -2}, {42, -1}, {43, -1},          // север
            {44, -2}, {45, -2}, {44, -1}, {45, -1},          // северо-восток
            {44, 0}, {45, 0}, {45, 1},                       // восток (кроме смотровой Ока)
            {45, 2}, {44, 3}, {45, 3},                       // юго-восток
            {42, 3}, {43, 3},                                // юг (кроме переходника 42,2)
    };

    /** Что ещё убрать в модулях, которые остаются хабом: всё, кроме центра, смотровой Ока и переходников. */
    private static final Box[] CLEARED_PARTS = {
            new Box(1034, -16, 1039, 3),   // западный край средней комнаты в центральной части
            new Box(1052, 8, 1063, 34),    // коридор восточной части за смотровой Ока
            new Box(1016, 35, 1063, 55),   // южный коридор за смотровой Ока
    };

    /** Западный переходник: копия модуля хаба TR {@code WEST_SOURCE} ставится на модуль ближе к центру. */
    private static final Box WEST_SOURCE = Box.module(40, 1), WEST_ADAPTER = Box.module(41, 1);

    /** Торец южного коридора у развилки южного переходника. */
    private static final Box CAP = new Box(1016, 39, 1016, 43);

    /** Шлюз из консольной (его место задаёт TR): сюда переносим тех, кто стоит в убираемых коридорах. */
    private static final double AIRLOCK_X = 1013.5, AIRLOCK_Y = 99, AIRLOCK_Z = 5.5;

    /**
     * Упрощение хаба: остаются центр со шлюзом, смотровая Ока и два переходника на сетку.
     * Западный переходник — копия западного модуля хаба TR (развилка, коридор на север и на юг), поставленная
     * на модуль ближе к центру: так выход центра (он сдвинут на 4 блока от сетки) поворачивает на проходы сетки.
     * Южный переходник — модуль (42,2) хаба TR как есть: там выход центра уже поворачивает на проходы сетки.
     * Всё остальное заливается камнем TARDIS и становится свободным местом для своих помещений.
     * Если где-то игрок что-то оставил (сундук, механизм, рамка) или уже пристроены помещения, хаб не трогаем совсем.
     */
    private static boolean rebuildSmall(ServerLevel level, List<HubPiece> pieces) {
        // Уже пристроенные помещения держатся за выходы хаба TR — убрать их коридоры значит отрезать помещения.
        if (!ModuleRegistry.get(level).pieces().isEmpty()) {
            TardisRooms.debug("Hub of {}: rooms are already built — hub not simplified", level.dimension().location());
            return false;
        }
        int lo = Lattice.HUB_FLOOR, hi = Lattice.FLOOR_UPPER - 1;
        List<Box> cleared = new ArrayList<>();
        for (int[] m : CLEARED_MODULES) cleared.add(Box.module(m[0], m[1]));
        cleared.addAll(List.of(CLEARED_PARTS));

        Map<BlockPos, Block> template = templateBlocks(pieces);
        List<Box> checked = new ArrayList<>(cleared);
        checked.add(WEST_ADAPTER); // перезапишется копией — там тоже не должно быть чужого
        for (Box b : checked) {
            if (inUse(level, b, lo, hi, template)) {
                TardisRooms.debug("Hub of {}: something was left at {} — hub not simplified", level.dimension().location(), b);
                return false;
            }
        }
        // Кто стоит в убираемых коридорах — к шлюзу в центре хаба, чтобы не замуровать.
        for (Box b : checked)
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, b.aabb(lo, hi))) p.teleportTo(AIRLOCK_X, AIRLOCK_Y, AIRLOCK_Z);

        BlockPos.MutableBlockPos from = new BlockPos.MutableBlockPos(), to = new BlockPos.MutableBlockPos();
        int dx = WEST_ADAPTER.x0() - WEST_SOURCE.x0();
        for (int y = lo; y <= hi; y++)
            for (int x = WEST_SOURCE.x0(); x <= WEST_SOURCE.x1(); x++)
                for (int z = WEST_SOURCE.z0(); z <= WEST_SOURCE.z1(); z++)
                    set(level, to.set(x + dx, y, z), level.getBlockState(from.set(x, y, z)));

        BlockState stone = TRBlockRegistry.FOOLS_STONE.get().defaultBlockState();
        for (Box b : cleared)
            for (int y = lo; y <= hi; y++)
                for (int x = b.x0(); x <= b.x1(); x++)
                    for (int z = b.z0(); z <= b.z1(); z++) {
                        to.set(x, y, z);
                        if (!level.getBlockState(to).is(Blocks.BEDROCK)) set(level, to, stone);
                    }

        // Южный коридор раньше шёл дальше на восток; там, где его обрезали у развилки, — стена, а не голый камень.
        BlockState wall = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        for (int y = lo + 1; y <= lo + 6; y++)
            for (int z = CAP.z0(); z <= CAP.z1(); z++) level.setBlock(to.set(CAP.x0(), y, z), wall, Block.UPDATE_ALL);
        return true;
    }

    /** Ставит блок без выпадения содержимого и без лишних обновлений (весь объём всё равно переделывается). */
    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState old = level.getBlockState(pos);
        if (old == state) return;
        if (old.hasBlockEntity()) level.removeBlockEntity(pos);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** Блоки шаблонов хаба TR по мировым координатам (то, что TR поставила сама). */
    private static Map<BlockPos, Block> templateBlocks(List<HubPiece> pieces) {
        Map<BlockPos, Block> out = new HashMap<>();
        for (HubPiece piece : pieces)
            for (var info : piece.blocks()) out.put(piece.origin().offset(info.pos()), info.state().getBlock());
        return out;
    }

    /**
     * В области есть что-то не из шаблона TR: блок с содержимым, которого TR там не ставила (сундук, механизм),
     * рамка, стойка, предмет на полу и т. п. Таблички и прочее из самого шаблона хаба не считаются.
     */
    private static boolean inUse(ServerLevel level, Box b, int lo, int hi, Map<BlockPos, Block> template) {
        for (int cx = b.x0() >> 4; cx <= b.x1() >> 4; cx++)
            for (int cz = b.z0() >> 4; cz <= b.z1() >> 4; cz++)
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (!b.contains(p) || p.getY() < lo || p.getY() > hi) continue;
                    Block block = be.getBlockState().getBlock();
                    if (template.get(p) == block) continue;
                    if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(TardisRefined.MODID)) {
                        TardisRooms.debug("Hub of {}: {} at {}", level.dimension().location(), block, p);
                        return true;
                    }
                }
        return !level.getEntitiesOfClass(Entity.class, b.aabb(lo, hi), e -> !(e instanceof Player) && !(e instanceof Mob)).isEmpty();
    }

    // ---------- сохранение ----------

    private static HubData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(HubData::new, HubData::load, null), DATA_NAME);
    }

    public static final class HubData extends SavedData {
        /** Выходы хаба закрыты заглушками, завалы в шаблонах расчищены. */
        boolean prepared;
        /** Какой хаб: {@link #LAYOUT_TR} или {@link #LAYOUT_SMALL}. */
        int layout;

        static HubData load(CompoundTag tag, HolderLookup.Provider provider) {
            HubData d = new HubData();
            d.prepared = tag.getBoolean("prepared");
            d.layout = tag.getInt("layout");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putBoolean("prepared", prepared);
            tag.putInt("layout", layout);
            return tag;
        }
    }

    private HubInitializer() {}
}
