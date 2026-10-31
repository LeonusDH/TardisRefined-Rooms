package leonus.tardis_rooms.room;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Сохранённое состояние застройки одной TARDIS: пристроенные помещения (из чего состоит, откуда пристроено,
 * где блок управления) и какие модули на каких этажах они занимают.
 */
public final class ModuleRegistry extends SavedData {
    private static final String NAME = "tardis_rooms_modules";

    /** Пристроенное помещение. */
    public static final class Piece {
        public final int id;
        public final ResourceLocation pieceId;
        public final Node node;
        public final Map<Integer, Set<Long>> footprint;
        public BlockPos core;
        /** Название, данное игроком; null — без названия (показывается тип помещения). */
        public String customName;

        Piece(int id, ResourceLocation pieceId, Node node, Map<Integer, Set<Long>> footprint, BlockPos core) {
            this.id = id; this.pieceId = pieceId; this.node = node; this.footprint = footprint; this.core = core;
        }

        private Map<Integer, Set<PieceCatalog.ModSide>> plannedExits;
        private boolean exitsResolved;

        /**
         * Выходы помещения по его плану (тот же расчёт, что при постройке), или null, если план не восстановить.
         * Нужны, чтобы ниша в стене на месте прохода не считалась проходом.
         */
        public Map<Integer, Set<PieceCatalog.ModSide>> plannedExits() {
            if (!exitsResolved) {
                exitsResolved = true;
                plannedExits = PieceCatalog.plan(pieceId, node).map(PieceCatalog.Plan::exits).orElse(null);
            }
            return plannedExits;
        }
    }

    private final Map<Integer, Piece> pieces = new HashMap<>();
    /** Этаж → модуль → номер помещения, которое его занимает. */
    private final Map<Integer, Map<Long, Integer>> owners = new HashMap<>();
    private int nextId = 1;

    public static ModuleRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(ModuleRegistry::new, ModuleRegistry::load, null), NAME);
    }

    /** Модуль занят каким-то помещением. */
    public boolean isOccupied(int mx, int mz, int floorY) {
        Map<Long, Integer> f = owners.get(floorY);
        return f != null && f.containsKey(Lattice.key(mx, mz));
    }

    /** Помещение, занимающее модуль, или null (пусто или хаб). */
    public Piece ownerOf(long module, int floorY) {
        Map<Long, Integer> f = owners.get(floorY);
        Integer id = f == null ? null : f.get(module);
        return id == null ? null : pieces.get(id);
    }

    public Piece piece(int id) {
        return pieces.get(id);
    }

    public Collection<Piece> pieces() {
        return pieces.values();
    }

    public Piece pieceAtCore(BlockPos core) {
        for (Piece p : pieces.values()) if (core.equals(p.core)) return p;
        return null;
    }

    public Piece addPiece(ResourceLocation pieceId, Node node, Map<Integer, Set<Long>> footprint, BlockPos core) {
        Piece p = new Piece(nextId++, pieceId, node, copy(footprint), core);
        pieces.put(p.id, p);
        p.footprint.forEach((floor, mods) -> {
            for (long m : mods) owners.computeIfAbsent(floor, x -> new HashMap<>()).put(m, p.id);
        });
        setDirty();
        return p;
    }

    /** Самая длинная строка названия помещения. */
    public static final int MAX_NAME = 32;

    /** Переименовать; пустое — убрать название. */
    public void rename(Piece p, String name) {
        p.customName = name == null || name.isBlank() ? null : name;
        setDirty();
    }

    public void removePiece(Piece p) {
        pieces.remove(p.id);
        p.footprint.forEach((floor, mods) -> {
            Map<Long, Integer> f = owners.get(floor);
            if (f != null) for (long m : mods) f.remove(m);
        });
        setDirty();
    }

    private static Map<Integer, Set<Long>> copy(Map<Integer, Set<Long>> fp) {
        Map<Integer, Set<Long>> c = new HashMap<>();
        fp.forEach((k, v) -> c.put(k, new HashSet<>(v)));
        return c;
    }

    private static ModuleRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        ModuleRegistry r = new ModuleRegistry();
        r.nextId = Math.max(1, tag.getInt("nextId"));
        ListTag list = tag.getList("pieces", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(t.getString("piece"));
            if (id == null) continue;
            Node node = new Node(t.getInt("nmx"), t.getInt("nmz"), Direction.from3DDataValue(t.getInt("nside")), t.getInt("nfloor"));
            Map<Integer, Set<Long>> fp = new HashMap<>();
            CompoundTag ft = t.getCompound("footprint");
            for (String k : ft.getAllKeys()) {
                Set<Long> set = new HashSet<>();
                for (long v : ft.getLongArray(k)) set.add(v);
                fp.put(Integer.parseInt(k), set);
            }
            BlockPos core = NbtUtils.readBlockPos(t, "core").orElse(null);
            Piece p = new Piece(t.getInt("id"), id, node, fp, core);
            if (t.contains("name", Tag.TAG_STRING)) p.customName = t.getString("name");
            r.pieces.put(p.id, p);
            fp.forEach((floor, mods) -> { for (long m : mods) r.owners.computeIfAbsent(floor, x -> new HashMap<>()).put(m, p.id); });
            r.nextId = Math.max(r.nextId, p.id + 1);
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("nextId", nextId);
        ListTag list = new ListTag();
        for (Piece p : pieces.values()) {
            CompoundTag t = new CompoundTag();
            t.putInt("id", p.id);
            t.putString("piece", p.pieceId.toString());
            t.putInt("nmx", p.node.mx());
            t.putInt("nmz", p.node.mz());
            t.putInt("nside", p.node.side().get3DDataValue());
            t.putInt("nfloor", p.node.floorY());
            CompoundTag ft = new CompoundTag();
            p.footprint.forEach((floor, set) -> ft.putLongArray(String.valueOf(floor), set.stream().mapToLong(Long::longValue).toArray()));
            t.put("footprint", ft);
            if (p.core != null) t.put("core", NbtUtils.writeBlockPos(p.core));
            if (p.customName != null) t.putString("name", p.customName);
            list.add(t);
        }
        tag.put("pieces", list);
        return tag;
    }
}
