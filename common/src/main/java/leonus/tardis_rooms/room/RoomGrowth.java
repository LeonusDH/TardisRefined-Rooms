package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.common.items.ScrewdriverItem;
import whocraft.tardis_refined.registry.TRBlockRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RoomGrowth {
    /** Результат проверки одного варианта для меню. */
    public enum Fit { OK, BLOCKED, OFF_GRID, CUTS_OFF }

    public static boolean holdsScrewdriver(ServerPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ScrewdriverItem
                || player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ScrewdriverItem;
    }

    // ---------- проверки ----------

    /** Проверяет все варианты для узла разом. {@code replacing} — заменяемое помещение (его место считается свободным). */
    public static Map<ResourceLocation, Fit> checkAll(ServerLevel level, Node node, ModuleRegistry.Piece replacing) {
        Map<Integer, Map<Long, Boolean>> cache = new HashMap<>();
        Map<ResourceLocation, Fit> out = new HashMap<>();
        Set<Object> before = null; // связность «до» одна на все варианты — считаем раз и только если понадобится
        for (PieceCatalog.Option o : PieceCatalog.options(node, leonus.tardis_rooms.room.custom.CustomRooms.INSTANCE.infos())) {
            Optional<PieceCatalog.Plan> plan = PieceCatalog.plan(o.id(), node);
            if (plan.isEmpty()) { out.put(o.id(), Fit.OFF_GRID); continue; }
            if (!isFree(level, plan.get(), cache, replacing)) { out.put(o.id(), Fit.BLOCKED); continue; }
            if (replacing != null) {
                if (before == null) before = Connectivity.reachableNow(level);
                if (!Connectivity.canReplace(level, replacing, plan.get(), before)) { out.put(o.id(), Fit.CUTS_OFF); continue; }
            }
            out.put(o.id(), Fit.OK);
        }
        return out;
    }

    private static boolean isFree(ServerLevel level, PieceCatalog.Plan plan, Map<Integer, Map<Long, Boolean>> cache, ModuleRegistry.Piece replacing) {
        ModuleRegistry reg = ModuleRegistry.get(level);
        for (Map.Entry<Integer, Set<Long>> e : plan.footprint().entrySet()) {
            int floor = e.getKey();
            Set<Long> own = replacing == null ? Set.of() : replacing.footprint.getOrDefault(floor, Set.of());
            Map<Long, Boolean> floorCache = cache.computeIfAbsent(floor, f -> new HashMap<>());
            for (long m : e.getValue()) {
                if (own.contains(m)) continue;
                Boolean free = floorCache.get(m);
                if (free == null) {
                    int mx = Lattice.keyX(m), mz = Lattice.keyZ(m);
                    free = !Lattice.isForbidden(level, mx, mz, floor) && !reg.isOccupied(mx, mz, floor) && Lattice.isUntouched(level, mx, mz, floor);
                    floorCache.put(m, free);
                }
                if (!free) return false;
            }
        }
        return true;
    }

    /** Почему нельзя удалить (ключ перевода) или null, если можно. */
    public static TRoomsText removalProblem(ServerLevel level, ModuleRegistry.Piece piece) {
        if (!Connectivity.canRemove(level, piece)) return TRoomsText.MSG_CUTS_OFF;
        if (hasForeignBlockEntities(level, piece)) return TRoomsText.MSG_HAS_CONTAINERS;
        return null;
    }

    /**
     * Есть ли внутри чужие блок-сущности (сундуки и т.п.); блоки TR и наши служебные не считаются.
     * Интерфейс ТАРДИС считается: игрок его поставил, молча замуровывать его вместе с настройками нельзя.
     */
    private static boolean hasForeignBlockEntities(ServerLevel level, ModuleRegistry.Piece piece) {
        for (Map.Entry<Integer, Set<Long>> e : piece.footprint.entrySet()) {
            int floor = e.getKey();
            for (long m : e.getValue()) {
                int ox = Lattice.origin(Lattice.keyX(m)), oz = Lattice.origin(Lattice.keyZ(m));
                for (int cx = ox >> 4; cx <= (ox + Lattice.MODULE - 1) >> 4; cx++) {
                    for (int cz = oz >> 4; cz <= (oz + Lattice.MODULE - 1) >> 4; cz++) {
                        LevelChunk chunk = level.getChunk(cx, cz);
                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            BlockPos p = be.getBlockPos();
                            if (p.getX() < ox || p.getX() >= ox + Lattice.MODULE || p.getZ() < oz || p.getZ() >= oz + Lattice.MODULE) continue;
                            if (p.getY() < floor || p.getY() >= floor + Lattice.BAND) continue;
                            if (be instanceof leonus.tardis_rooms.iface.InterfaceBlockEntity) return true;
                            String ns = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getNamespace();
                            if (!ns.equals(TardisRefined.MODID) && !ns.equals(TardisRooms.MODID)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ---------- рост ----------

    public static void tryGrow(ServerPlayer player, BlockPos pos, ResourceLocation pieceId, BlockPos replaceCore) {
        if (!(player.level() instanceof ServerLevel level) || !Lattice.isTardisLevel(level)) return;
        if (!holdsScrewdriver(player)) { msg(player, TRoomsText.MSG_NEED_SCREWDRIVER); return; }
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return;
        ModuleRegistry reg = ModuleRegistry.get(level);
        ModuleRegistry.Piece replacing = null;
        if (replaceCore != null && !replaceCore.equals(BlockPos.ZERO)) {
            replacing = reg.pieceAtCore(replaceCore);
            if (replacing == null || player.distanceToSqr(replaceCore.getCenter()) > 64) return;
            if (!pos.equals(Lattice.nodePos(replacing.node.mx(), replacing.node.mz(), replacing.node.side(), replacing.node.floorY()))) return;
        } else {
            if (player.distanceToSqr(pos.getCenter()) > 64) return;
            if (!level.getBlockState(pos).is(TRoomsBlocks.ROOM_NODE.get())) return;
        }

        Optional<Node> found = Node.locate(pos);
        if (found.isEmpty()) { msg(player, TRoomsText.MSG_INVALID); return; }
        Node node = found.get();
        if (Lattice.isForbidden(level, node.targetX(), node.targetZ(), node.floorY())) { msg(player, TRoomsText.MSG_FORBIDDEN); return; }
        if (PieceCatalog.options(node, leonus.tardis_rooms.room.custom.CustomRooms.INSTANCE.infos()).stream().noneMatch(o -> o.id().equals(pieceId))) { msg(player, TRoomsText.MSG_UNKNOWN_PIECE); return; }
        Optional<PieceCatalog.Plan> plan = PieceCatalog.plan(pieceId, node);
        if (plan.isEmpty()) { msg(player, TRoomsText.MSG_OFF_GRID); return; }
        if (!isFree(level, plan.get(), new HashMap<>(), replacing)) { msg(player, TRoomsText.MSG_OCCUPIED); return; }
        if (replacing != null) {
            if (!Connectivity.canReplace(level, replacing, plan.get())) { msg(player, TRoomsText.MSG_CUTS_OFF_REPLACE); return; }
            if (hasForeignBlockEntities(level, replacing)) { msg(player, TRoomsText.MSG_HAS_CONTAINERS); return; }
            if (TardisRoomsConfig.debug()) TardisRooms.debug("Replacing #{}; graph: {}", replacing.id, Connectivity.describe(level));
            demolish(level, replacing);
        }

        plan.get().build().accept(level);
        Lattice.clearPanel(level, node.mx(), node.mz(), node.side(), node.floorY());
        BlockPos core = placeCore(level, plan.get());
        reg.addPiece(pieceId, node, plan.get().footprint(), core);
        for (Map.Entry<Integer, Set<Long>> e : plan.get().footprint().entrySet()) Lattice.refresh(level, e.getValue(), e.getKey());
        TardisRooms.debug("{} {} {} from module {},{} {} floor {} in {}", player.getGameProfile().getName(),
                replacing != null ? "replaced with" : "grew", pieceId, node.mx(), node.mz(), node.side(), node.floorY(), level.dimension().location());
        msg(player, replacing != null ? TRoomsText.MSG_REPLACED : TRoomsText.MSG_GROWN);
    }

    /** Удаление по запросу игрока с блока управления. */
    public static void tryRemove(ServerPlayer player, BlockPos core) {
        if (!(player.level() instanceof ServerLevel level) || !Lattice.isTardisLevel(level)) return;
        if (!holdsScrewdriver(player)) { msg(player, TRoomsText.MSG_NEED_SCREWDRIVER); return; }
        if (player.distanceToSqr(core.getCenter()) > 64) return;
        if (!player.mayBuild() || !level.mayInteract(player, core)) return;
        ModuleRegistry.Piece piece = ModuleRegistry.get(level).pieceAtCore(core);
        if (piece == null) return;
        TRoomsText problem = removalProblem(level, piece);
        if (problem != null) {
            player.displayClientMessage(problem.text(), true);
            TardisRooms.debug("{}: removal of #{} refused: {}", player.getGameProfile().getName(), piece.id, problem.key());
            return;
        }
        if (TardisRoomsConfig.debug()) TardisRooms.debug("Removing #{}; graph: {}", piece.id, Connectivity.describe(level));
        demolish(level, piece);
        TardisRooms.debug("{} removed {} (#{}) in {}", player.getGameProfile().getName(), piece.pieceId, piece.id, level.dimension().location());
        msg(player, TRoomsText.MSG_REMOVED);
    }

    /**
     * Сносит помещение: игроков переносит в проход, откуда оно пристроено, заливает объём камнем TARDIS,
     * снимает запись и возвращает узлы роста там, где в помещение вели проходы.
     */
    static void demolish(ServerLevel level, ModuleRegistry.Piece piece) {
        Spot spot = safeSpot(level, piece);
        BlockPos safe = spot.pos();
        // Лодки и вагонетки — чуть дальше по проходу, иначе подхватят вынесенных рядом мобов.
        BlockPos further = safe.relative(spot.inward(), 2);
        BlockPos vehicles = level.getBlockState(further).isAir() && level.getBlockState(further.above()).isAir()
                && !level.getBlockState(further.below()).isAir() ? further : safe;
        for (Map.Entry<Integer, Set<Long>> e : piece.footprint.entrySet()) {
            int floor = e.getKey();
            for (long m : e.getValue()) {
                int ox = Lattice.origin(Lattice.keyX(m)), oz = Lattice.origin(Lattice.keyZ(m));
                AABB box = new AABB(ox, floor, oz, ox + Lattice.MODULE, floor + Lattice.BAND, oz + Lattice.MODULE);
                // Рамки и картины роняют свой предмет, затем всё живое и предметы выносим в проход — не замуровывать.
                for (HangingEntity h : level.getEntitiesOfClass(HangingEntity.class, box)) { h.dropItem(null); h.discard(); }
                for (Entity en : level.getEntitiesOfClass(Entity.class, box, x -> !x.isPassenger() && !(x instanceof HangingEntity))) {
                    BlockPos to = en instanceof VehicleEntity ? vehicles : safe;
                    en.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                }
            }
        }
        // Фонари, которые наши заглушки поставили в камень соседей, — убрать (иначе останутся в пустом камне).
        for (Map.Entry<Integer, Set<Long>> e : piece.footprint.entrySet())
            for (long m : e.getValue())
                for (Direction side : Lattice.SIDES) {
                    int nx = Lattice.keyX(m) + side.getStepX(), nz = Lattice.keyZ(m) + side.getStepZ();
                    if (!e.getValue().contains(Lattice.key(nx, nz))) Lattice.clearLampsBehind(level, Lattice.keyX(m), Lattice.keyZ(m), side, e.getKey());
                }
        BlockState stone = TRBlockRegistry.FOOLS_STONE.get().defaultBlockState();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (Map.Entry<Integer, Set<Long>> e : piece.footprint.entrySet()) {
            int floor = e.getKey();
            for (long m : e.getValue()) {
                int ox = Lattice.origin(Lattice.keyX(m)), oz = Lattice.origin(Lattice.keyZ(m));
                for (int y = floor; y < floor + Lattice.BAND; y++)
                    for (int x = ox; x < ox + Lattice.MODULE; x++)
                        for (int z = oz; z < oz + Lattice.MODULE; z++) {
                            mp.set(x, y, z);
                            BlockState s = level.getBlockState(mp);
                            if (s != stone && !s.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                                if (s.hasBlockEntity()) level.removeBlockEntity(mp); // без высыпания содержимого
                                // Весь объём всё равно становится камнем: соседям внутри обновлять форму незачем.
                                level.setBlock(mp, stone, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                            }
                        }
            }
        }
        ModuleRegistry.get(level).removePiece(piece);
        // Проходы соседей, вёдшие в помещение, снова становятся узлами роста.
        for (Map.Entry<Integer, Set<Long>> e : piece.footprint.entrySet()) {
            int floor = e.getKey();
            for (long m : e.getValue()) {
                for (Direction side : Lattice.SIDES) {
                    int nx = Lattice.keyX(m) + side.getStepX(), nz = Lattice.keyZ(m) + side.getStepZ();
                    if (e.getValue().contains(Lattice.key(nx, nz))) continue;
                    // Ниша в стене соседа на месте прохода — не проход, узел туда не ставим.
                    if (Boolean.FALSE.equals(Connectivity.plannedExit(ModuleRegistry.get(level), Lattice.key(nx, nz), side.getOpposite(), floor))) continue;
                    BlockPos theirs = Lattice.nodePos(nx, nz, side.getOpposite(), floor);
                    BlockState ts = level.getBlockState(theirs);
                    if (ts.isAir()) {
                        Lattice.placePanel(level, nx, nz, side.getOpposite(), floor, true);
                    } else if (ts.is(TRoomsBlocks.ROOM_WALL.get())) {
                        level.setBlock(theirs, TRoomsBlocks.ROOM_NODE.get().defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    /**
     * Куда вынести игроков при сносе: в проход, откуда пристроено помещение; если его уже нет
     * (родителя снесли, помещение держалось на другом пути) — в любой открытый проход соседей, затем хаба.
     */
    private record Spot(BlockPos pos, Direction inward) {}

    private static Spot safeSpot(ServerLevel level, ModuleRegistry.Piece piece) {
        Node node = piece.node;
        Spot spot = standIn(level, node.mx(), node.mz(), node.side(), node.floorY());
        if (spot != null) return spot;
        for (Map.Entry<Integer, Set<Long>> e : piece.footprint.entrySet()) {
            for (long m : e.getValue()) {
                for (Direction side : Lattice.SIDES) {
                    int nx = Lattice.keyX(m) + side.getStepX(), nz = Lattice.keyZ(m) + side.getStepZ();
                    if (e.getValue().contains(Lattice.key(nx, nz))) continue;
                    spot = standIn(level, nx, nz, side.getOpposite(), e.getKey());
                    if (spot != null) return spot;
                }
            }
        }
        for (long m : Lattice.hubModules(level))
            for (int floor : Lattice.FLOORS)
                for (Direction side : Lattice.SIDES) {
                    if (floor > Lattice.HUB_FLOOR) continue;
                    spot = standIn(level, Lattice.keyX(m), Lattice.keyZ(m), side, floor);
                    if (spot != null) return spot;
                }
        Direction in = node.side().getOpposite();
        return new Spot(Lattice.nodePos(node.mx(), node.mz(), node.side(), node.floorY()).relative(in, 2).atY(node.floorY() + 2), in);
    }

    /** Место в двух блоках от прохода внутрь модуля, где можно стоять (пол снизу, два блока воздуха), или null. */
    private static Spot standIn(ServerLevel level, int mx, int mz, Direction side, int floor) {
        BlockPos p = Lattice.openingPos(mx, mz, side, floor, Lattice.NODE_A, Lattice.OPEN_DY_MIN).relative(side.getOpposite(), 2);
        if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) return null;
        return level.getBlockState(p.below()).isAir() ? null : new Spot(p, side.getOpposite());
    }

    /** Ставит блок управления в пол помещения — в ближайшее к центру место, где можно стоять. */
    private static BlockPos placeCore(ServerLevel level, PieceCatalog.Plan plan) {
        int floor = plan.coreFloor() != null ? plan.coreFloor()
                : plan.footprint().keySet().stream().min(Integer::compare).orElse(Lattice.FLOOR_LOWER);
        Set<Long> mods = plan.footprint().get(floor);
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long m : mods) {
            int ox = Lattice.origin(Lattice.keyX(m)), oz = Lattice.origin(Lattice.keyZ(m));
            minX = Math.min(minX, ox); minZ = Math.min(minZ, oz);
            maxX = Math.max(maxX, ox + Lattice.MODULE - 1); maxZ = Math.max(maxZ, oz + Lattice.MODULE - 1);
        }
        double cx = (minX + maxX) / 2.0, cz = (minZ + maxZ) / 2.0;
        if (plan.coreSpot() != null) { cx = plan.coreSpot()[0]; cz = plan.coreSpot()[1]; }
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                double d = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                if (d >= bestD) continue;
                BlockPos f = new BlockPos(x, floor + 1, z);
                BlockState s = level.getBlockState(f);
                if (s.isAir() || !s.isCollisionShapeFullBlock(level, f)) continue;
                if (!level.getBlockState(f.above()).isAir() || !level.getBlockState(f.above(2)).isAir()) continue;
                best = f;
                bestD = d;
            }
        }
        // Без блока управления помещением нельзя было бы управлять — если пола нет, ставим в расчётную точку.
        BlockPos core = best != null ? best : BlockPos.containing(cx, floor + 1, cz);
        level.setBlock(core, TRoomsBlocks.ROOM_CORE.get().defaultBlockState(), Block.UPDATE_ALL);
        return core;
    }

    static void msg(ServerPlayer player, TRoomsText text) {
        player.displayClientMessage(text.text(), true);
        TardisRooms.debug("{}: {}", player.getGameProfile().getName(), text.key());
    }

    private RoomGrowth() {}
}
