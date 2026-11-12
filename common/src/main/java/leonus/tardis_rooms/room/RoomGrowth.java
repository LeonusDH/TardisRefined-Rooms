package leonus.tardis_rooms.room;

import leonus.tardis_rooms.Screwdriver;
import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.room.custom.CustomRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import whocraft.tardis_refined.registry.TRBlockRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class RoomGrowth {
    /** Результат проверки одного варианта для меню. */
    public enum Fit { OK, BLOCKED, OFF_GRID, CUTS_OFF }

    // ---------- проверки ----------

    /** Проверяет все варианты для узла разом. {@code replacing} — заменяемое помещение (его место считается свободным). */
    public static Map<ResourceLocation, Fit> checkAll(ServerLevel level, Node node, ModuleRegistry.Piece replacing) {
        Map<Integer, Map<Long, Boolean>> cache = new HashMap<>();
        Map<ResourceLocation, Fit> out = new HashMap<>();
        Set<Object> before = null; // связность «до» одна на все варианты — считаем раз и только если понадобится
        for (PieceCatalog.Option o : PieceCatalog.options(node, CustomRooms.INSTANCE.infos())) {
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

    /** Дальше этого (в блоках) от блока управления показ по кнопке в чате не работает. */
    private static final int SHOW_RANGE = 64;

    /**
     * Показать игроку, что мешает удалить помещение: кнопка в меню блока управления ({@code inChat} — сообщение
     * в чат) или нажатие на сообщение в чате (повтор — над хотбаром). Игрок может уже отойти искать: до
     * {@value #SHOW_RANGE} блоков. Ничего не мешает — так и сказать. {@code arm} — открыто окно подтверждения
     * «удалить вместе с ними»: пошёл отсчёт ({@link ForceRemoval}).
     */
    public static void showBlockers(ServerPlayer player, BlockPos core, boolean inChat, boolean arm) {
        String what = "showing what's in the way of the room at " + core.toShortString();
        if (!(player.level() instanceof ServerLevel level) || !Lattice.isTardisLevel(level)) { ignored(player, what, "not in a TARDIS"); return; }
        if (player.distanceToSqr(core.getCenter()) > SHOW_RANGE * SHOW_RANGE) { ignored(player, what, "too far"); return; }
        ModuleRegistry.Piece piece = ModuleRegistry.get(level).pieceAtCore(core);
        if (piece == null) { ignored(player, what, "no room there"); return; }
        if (arm) ForceRemoval.arm(player.getUUID(), level.dimension(), core, System.currentTimeMillis());
        List<BlockPos> blockers = blockers(level, piece);
        TardisRooms.debug("{}: what's in the way of #{}{}: {}", TardisRooms.who(player), piece.id, arm ? " (confirmation window opened, countdown started)" : "",
                blockers.isEmpty() ? "nothing" : describe(level, blockers));
        if (blockers.isEmpty()) {
            msg(player, TRoomsText.MSG_NOTHING_BLOCKS);
            BlockerHighlight.clear(player);
        } else BlockerHighlight.show(player, core, blockers, inChat);
    }

    /** Блок-сущности, которые мешают удалить или заменить помещение (см. {@link RoomBlockEntities}); пусто — не мешают. */
    public static List<BlockPos> blockers(ServerLevel level, ModuleRegistry.Piece piece) {
        return RoomBlockEntities.blockers(level, RoomBlockEntities.boxes(piece.footprint), piece.template, piece.removable, piece.matchers());
    }

    // ---------- рост ----------

    public static void tryGrow(ServerPlayer player, BlockPos pos, ResourceLocation pieceId, BlockPos replaceCore) {
        String what = "growing " + pieceId + " at " + pos.toShortString();
        if (!(player.level() instanceof ServerLevel level) || !Lattice.isTardisLevel(level)) { ignored(player, what, "not in a TARDIS"); return; }
        if (!Screwdriver.held(player)) { msg(player, TRoomsText.MSG_NEED_SCREWDRIVER); return; }
        if (!player.mayBuild() || !level.mayInteract(player, pos)) { ignored(player, what, "may not build here"); return; }
        boolean replace = replaceCore != null && !replaceCore.equals(BlockPos.ZERO);
        if (!RoomPermissions.allowed(player, level.dimension(), replace ? RoomPermissions.Action.MANAGE : RoomPermissions.Action.GROW, what)) return;
        ModuleRegistry reg = ModuleRegistry.get(level);
        ModuleRegistry.Piece replacing = null;
        if (replace) {
            replacing = reg.pieceAtCore(replaceCore);
            if (replacing == null) { ignored(player, what, "no room at " + replaceCore.toShortString() + " to replace"); return; }
            if (player.distanceToSqr(replaceCore.getCenter()) > 64) { ignored(player, what, "too far from the Room Control Block"); return; }
            if (!pos.equals(Lattice.nodePos(replacing.node.mx(), replacing.node.mz(), replacing.node.side(), replacing.node.floorY()))) {
                ignored(player, what, "not the node room #" + replacing.id + " grew from"); return;
            }
        } else {
            if (player.distanceToSqr(pos.getCenter()) > 64) { ignored(player, what, "too far"); return; }
            if (!level.getBlockState(pos).is(TRoomsBlocks.ROOM_NODE.get())) { ignored(player, what, "no growth node there"); return; }
        }

        Optional<Node> found = Node.locate(pos);
        if (found.isEmpty()) { msg(player, TRoomsText.MSG_INVALID); return; }
        Node node = found.get();
        if (Lattice.isForbidden(level, node.targetX(), node.targetZ(), node.floorY())) { msg(player, TRoomsText.MSG_FORBIDDEN); return; }
        if (PieceCatalog.options(node, CustomRooms.INSTANCE.infos()).stream().noneMatch(o -> o.id().equals(pieceId))) { msg(player, TRoomsText.MSG_UNKNOWN_PIECE); return; }
        Optional<PieceCatalog.Plan> plan = PieceCatalog.plan(pieceId, node);
        if (plan.isEmpty()) {
            var custom = CustomRooms.INSTANCE.get(pieceId);
            msg(player, custom.isEmpty() ? TRoomsText.MSG_OFF_GRID
                    : custom.get().def().rotatable() || custom.get().def().entrance().side() == node.side().getOpposite()
                    ? TRoomsText.MSG_CUSTOM_NO_HEIGHT : TRoomsText.MSG_CUSTOM_DIRECTION);
            return;
        }
        if (!isFree(level, plan.get(), new HashMap<>(), replacing)) { msg(player, TRoomsText.MSG_OCCUPIED); return; }
        if (replacing != null) {
            if (!Connectivity.canReplace(level, replacing, plan.get())) { msg(player, TRoomsText.MSG_CUTS_OFF_REPLACE); return; }
            List<BlockPos> blockers = blockers(level, replacing);
            if (!blockers.isEmpty()) {
                BlockerHighlight.show(player, replacing.core, blockers, true);
                TardisRooms.debug("{}: replacing #{} refused: in the way: {}", TardisRooms.who(player), replacing.id, describe(level, blockers));
                return;
            }
            if (TardisRoomsConfig.debug()) TardisRooms.debug("Replacing #{}; graph: {}", replacing.id, Connectivity.describe(level));
            demolish(level, replacing);
        }

        plan.get().build().accept(level);
        Lattice.clearPanel(level, node.mx(), node.mz(), node.side(), node.floorY());
        BlockPos core = placeCore(level, plan.get());
        // Отпечатки — сразу после постройки: всё, что в помещении сейчас, — его шаблон.
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(level, RoomBlockEntities.boxes(plan.get().footprint()));
        ModuleRegistry.Piece added = reg.addPiece(pieceId, node, plan.get().footprint(), core, template, plan.get().removable(), plan.get().removableMatch());
        for (Map.Entry<Integer, Set<Long>> e : plan.get().footprint().entrySet()) Lattice.refresh(level, e.getValue(), e.getKey());
        TardisRooms.LOGGER.info("{} {} {} as #{} from module {},{} {} floor {} in {}, control block at {}", TardisRooms.who(player),
                replacing != null ? "replaced #" + replacing.id + " with" : "grew", pieceId, added.id, node.mx(), node.mz(), node.side(),
                node.floorY(), level.dimension().location(), core.toShortString());
        TardisRooms.debug("Room #{}: {} template block entities remembered, {} removable places, {} removable descriptions",
                added.id, template.size(), added.removable.size(), added.removableMatch.size());
        msg(player, replacing != null ? TRoomsText.MSG_REPLACED : TRoomsText.MSG_GROWN);
    }

    /**
     * Удаление по запросу игрока с блока управления. {@code force} — из окна подтверждения: удалить вместе с тем,
     * что мешает (сервер сам проверяет, что окно было открыто достаточно долго, см. {@link ForceRemoval}).
     */
    public static void tryRemove(ServerPlayer player, BlockPos core, boolean force) {
        String what = "removing the room at " + core.toShortString() + (force ? " (with what's in the way)" : "");
        if (!(player.level() instanceof ServerLevel level) || !Lattice.isTardisLevel(level)) { ignored(player, what, "not in a TARDIS"); return; }
        if (!Screwdriver.held(player)) { msg(player, TRoomsText.MSG_NEED_SCREWDRIVER); return; }
        if (player.distanceToSqr(core.getCenter()) > 64) { ignored(player, what, "too far"); return; }
        if (!player.mayBuild() || !level.mayInteract(player, core)) { ignored(player, what, "may not build here"); return; }
        if (!RoomPermissions.allowed(player, level.dimension(), RoomPermissions.Action.MANAGE, what)) return;
        ModuleRegistry.Piece piece = ModuleRegistry.get(level).pieceAtCore(core);
        if (piece == null) { ignored(player, what, "no room there"); return; }
        if (!Connectivity.canRemove(level, piece)) {
            msg(player, TRoomsText.MSG_CUTS_OFF);
            return;
        }
        List<BlockPos> blockers = blockers(level, piece);
        if (!blockers.isEmpty()) {
            if (!force || !ForceRemoval.take(player.getUUID(), level.dimension(), core, System.currentTimeMillis())) {
                BlockerHighlight.show(player, core, blockers, true);
                TardisRooms.debug("{}: removal of #{} refused{}: in the way: {}", TardisRooms.who(player), piece.id,
                        force ? " (confirmation window not open for 10 s, or expired)" : "", describe(level, blockers));
                return;
            }
            // Вещи пропадут — это стоит видеть в обычном логе: кто, что и где.
            TardisRooms.LOGGER.info("{} removes #{} together with what's in the way, which is lost: {}", TardisRooms.who(player), piece.id, describe(level, blockers));
        }
        if (TardisRoomsConfig.debug()) TardisRooms.debug("Removing #{}; graph: {}", piece.id, Connectivity.describe(level));
        demolish(level, piece);
        TardisRooms.LOGGER.info("{} removed {} (#{}) in {}", TardisRooms.who(player), piece.pieceId, piece.id, level.dimension().location());
        msg(player, TRoomsText.MSG_REMOVED);
    }

    /**
     * Сносит помещение: игроков переносит в проход, откуда оно пристроено, заливает объём камнем TARDIS,
     * снимает запись и возвращает узлы роста там, где в помещение вели проходы.
     */
    static void demolish(ServerLevel level, ModuleRegistry.Piece piece) {
        int moved = 0, dropped = 0, interfaces = 0;
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
                for (HangingEntity h : level.getEntitiesOfClass(HangingEntity.class, box)) { h.dropItem(null); h.discard(); dropped++; }
                for (Entity en : level.getEntitiesOfClass(Entity.class, box, x -> !x.isPassenger() && !(x instanceof HangingEntity))) {
                    BlockPos to = en instanceof net.minecraft.world.entity.vehicle.AbstractMinecart || en instanceof net.minecraft.world.entity.vehicle.Boat ? vehicles : safe;
                    en.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                    moved++;
                }
            }
        }
        // Фонари, которые наши заглушки поставили в камень соседей, — убрать (иначе останутся в пустом камне).
        List<Lattice.Edge> edges = Lattice.outerEdges(piece.footprint);
        for (Lattice.Edge e : edges) Lattice.clearLampsBehind(level, e.mx(), e.mz(), e.side(), e.floor());
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
                            if (s != stone && !s.is(Blocks.BEDROCK)) {
                                if (s.hasBlockEntity()) {
                                    // Интерфейс — как при поломке: связанные сразу пересчитывают связь и сигнал без него.
                                    if (level.getBlockEntity(mp) instanceof InterfaceBlockEntity ibe) { ibe.removedFromWorld(); interfaces++; }
                                    level.removeBlockEntity(mp); // без высыпания содержимого
                                }
                                // Весь объём всё равно становится камнем: соседям внутри обновлять форму незачем.
                                level.setBlock(mp, stone, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                            }
                        }
            }
        }
        ModuleRegistry reg = ModuleRegistry.get(level);
        reg.removePiece(piece);
        TardisRooms.debug("Room #{} demolished: {} entities moved to {}, {} item frames and paintings dropped, {} interfaces removed",
                piece.id, moved, safe.toShortString(), dropped, interfaces);
        // Проходы соседей, вёдшие в помещение, снова становятся узлами роста.
        for (Lattice.Edge e : edges) {
            Direction back = e.side().getOpposite();
            // Ниша в стене соседа на месте прохода — не проход, узел туда не ставим.
            if (Boolean.FALSE.equals(Connectivity.plannedExit(reg, Lattice.key(e.nx(), e.nz()), back, e.floor()))) continue;
            BlockPos theirs = Lattice.nodePos(e.nx(), e.nz(), back, e.floor());
            BlockState ts = level.getBlockState(theirs);
            if (ts.isAir()) {
                Lattice.placePanel(level, e.nx(), e.nz(), back, e.floor(), true);
            } else if (ts.is(TRoomsBlocks.ROOM_WALL.get())) {
                level.setBlock(theirs, TRoomsBlocks.ROOM_NODE.get().defaultBlockState(), Block.UPDATE_ALL);
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
        for (Lattice.Edge e : Lattice.outerEdges(piece.footprint)) {
            spot = standIn(level, e.nx(), e.nz(), e.side().getOpposite(), e.floor());
            if (spot != null) return spot;
        }
        for (long m : Lattice.hubModules(level))
            for (int floor : Lattice.FLOORS)
                for (Direction side : Lattice.SIDES) {
                    if (floor > Lattice.HUB_FLOOR) continue;
                    spot = standIn(level, Lattice.keyX(m), Lattice.keyZ(m), side, floor);
                    if (spot != null) return spot;
                }
        Direction in = node.side().getOpposite();
        BlockPos fallback = Lattice.nodePos(node.mx(), node.mz(), node.side(), node.floorY()).relative(in, 2).atY(node.floorY() + 2);
        TardisRooms.LOGGER.warn("Removing room #{} in {}: no open passage to move players to, using {}", piece.id,
                level.dimension().location(), fallback.toShortString());
        return new Spot(fallback, in);
    }

    /** Место в двух блоках от прохода внутрь модуля, где можно стоять (пол снизу, два блока воздуха), или null. */
    private static Spot standIn(ServerLevel level, int mx, int mz, Direction side, int floor) {
        BlockPos p = Lattice.openingPos(mx, mz, side, floor, Lattice.NODE_A, Lattice.OPEN_DY_MIN).relative(side.getOpposite(), 2);
        if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) return null;
        return level.getBlockState(p.below()).isAir() ? null : new Spot(p, side.getOpposite());
    }

    /** Ставит блок управления в пол помещения — в ближайшее к центру место, где можно стоять. */
    private static BlockPos placeCore(ServerLevel level, PieceCatalog.Plan plan) {
        if (plan.coreExact() != null) {
            level.setBlock(plan.coreExact(), TRoomsBlocks.ROOM_CORE.get().defaultBlockState(), Block.UPDATE_ALL);
            return plan.coreExact();
        }
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
        TardisRooms.debug("{}: {}", TardisRooms.who(player), text.key());
    }

    /** Запрос игрока отброшен без сообщения ему (не та позиция, далеко, нет прав) — в подробности, с причиной. */
    static void ignored(ServerPlayer player, String what, String why) {
        TardisRooms.debug("{}: {} ignored: {}", TardisRooms.who(player), what, why);
    }

    /** Мешающие блоки для лога: «minecraft:chest ×2, minecraft:lectern ×1». */
    static String describe(ServerLevel level, List<BlockPos> blocks) {
        Map<String, Integer> kinds = new TreeMap<>();
        for (BlockPos p : blocks) kinds.merge(BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString(), 1, Integer::sum);
        StringBuilder sb = new StringBuilder();
        kinds.forEach((k, n) -> sb.append(sb.isEmpty() ? "" : ", ").append(k).append(" \u00d7").append(n));
        return sb.toString();
    }

    private RoomGrowth() {}
}
