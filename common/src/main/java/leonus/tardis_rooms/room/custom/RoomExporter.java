package leonus.tardis_rooms.room.custom;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.ModuleRegistry;
import leonus.tardis_rooms.room.Node;
import leonus.tardis_rooms.room.PieceCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import whocraft.tardis_refined.registry.TRBlockRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Сохраняет построенное помещение как помещение для датапака: шаблоны по этажам и JSON, в готовый датапак
 * {@code <мир>/tardis_rooms_export}. Помещение сохраняется как стоит (без поворота): вход — сторона, которой
 * оно пристроено к узлу. Служебные блоки аддона (узлы, заглушки, фонари, интерфейсы) не попадают в шаблон.
 */
public final class RoomExporter {
    /** Итог: путь к датапаку и, если вход пришлось сменить, предупреждение. */
    public record Result(Path pack, Optional<TRoomsText> note) {}

    public static final class Failure extends Exception {
        public final TRoomsText reason;
        Failure(TRoomsText reason) { this.reason = reason; }
    }

    public static Result export(ServerLevel level, BlockPos at, ResourceLocation id) throws Failure, IOException {
        Integer floorHere = null;
        for (int f : Lattice.FLOORS) if (at.getY() >= f && at.getY() < f + Lattice.BAND) floorHere = f;
        if (floorHere == null) throw new Failure(TRoomsText.CMD_EXPORT_NOT_IN_ROOM);
        ModuleRegistry.Piece piece = ModuleRegistry.get(level).ownerOf(Lattice.key(Lattice.moduleOf(at.getX()), Lattice.moduleOf(at.getZ())), floorHere);
        if (piece == null) throw new Failure(TRoomsText.CMD_EXPORT_NOT_IN_ROOM);

        // Форма: прямоугольник модулей, одинаковый на всех уровнях; не больше двух уровней подряд.
        List<Integer> floors = new ArrayList<>(new TreeSet<>(piece.footprint.keySet()));
        if (floors.size() > 2) throw new Failure(TRoomsText.CMD_EXPORT_SHAPE);
        if (floors.size() == 2 && Lattice.above(floors.get(0)) != floors.get(1)) throw new Failure(TRoomsText.CMD_EXPORT_SHAPE);
        Set<Long> mods = piece.footprint.get(floors.get(0));
        for (int f : floors) if (!piece.footprint.get(f).equals(mods)) throw new Failure(TRoomsText.CMD_EXPORT_SHAPE);
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long m : mods) {
            minX = Math.min(minX, Lattice.keyX(m)); maxX = Math.max(maxX, Lattice.keyX(m));
            minZ = Math.min(minZ, Lattice.keyZ(m)); maxZ = Math.max(maxZ, Lattice.keyZ(m));
        }
        int w = maxX - minX + 1, d = maxZ - minZ + 1;
        if (mods.size() != w * d) throw new Failure(TRoomsText.CMD_EXPORT_SHAPE);
        int ox = Lattice.origin(minX), oz = Lattice.origin(minZ);

        // Выходы: по плану постройки, а если его нет — по проходам в стенах.
        List<CustomRoom.Exit> exits = new ArrayList<>();
        Map<Integer, Set<PieceCatalog.ModSide>> planned = piece.plannedExits();
        for (int fi = 0; fi < floors.size(); fi++) {
            int f = floors.get(fi);
            for (long m : mods)
                for (Direction side : Lattice.SIDES) {
                    long n = Lattice.key(Lattice.keyX(m) + side.getStepX(), Lattice.keyZ(m) + side.getStepZ());
                    if (mods.contains(n)) continue;
                    boolean exit = planned != null
                            ? planned.getOrDefault(f, Set.of()).contains(new PieceCatalog.ModSide(m, side))
                            : isOpening(level.getBlockState(Lattice.nodePos(Lattice.keyX(m), Lattice.keyZ(m), side, f)));
                    if (exit) exits.add(new CustomRoom.Exit(Lattice.keyX(m) - minX, Lattice.keyZ(m) - minZ, side, fi));
                }
        }
        exits.sort((a, b) -> a.floor() != b.floor() ? a.floor() - b.floor() : a.z() != b.z() ? a.z() - b.z() : a.x() != b.x() ? a.x() - b.x() : a.side().compareTo(b.side()));

        // Вход: сторона, которой помещение пристроено к узлу. Если она не на нижнем уровне — первый выход нижнего.
        Node node = piece.node;
        CustomRoom.Exit entrance = new CustomRoom.Exit(node.targetX() - minX, node.targetZ() - minZ, node.side().getOpposite(), floors.indexOf(node.floorY()));
        Optional<TRoomsText> note = Optional.empty();
        if (entrance.floor() != 0 || !exits.contains(entrance)) {
            CustomRoom.Exit first = exits.stream().filter(e -> e.floor() == 0).findFirst().orElseThrow(() -> new Failure(TRoomsText.CMD_EXPORT_NO_ENTRANCE));
            if (!first.equals(entrance)) note = Optional.of(TRoomsText.CMD_EXPORT_ENTRANCE_MOVED);
            entrance = first;
        }
        CustomRoom.Exit ent = entrance;
        exits.removeIf(e -> e.equals(ent));

        // Шаблоны: от пола уровня до верха постройки (выше — нетронутый камень, его не берём).
        Path pack = level.getServer().getWorldPath(LevelResource.ROOT).resolve("tardis_rooms_export").normalize();
        List<ResourceLocation> structures = new ArrayList<>();
        for (int fi = 0; fi < floors.size(); fi++) {
            int f = floors.get(fi);
            int h = Math.max(Lattice.OPEN_DY_MAX + 1, builtHeight(level, ox, oz, w, d, f));
            StructureTemplate t = new StructureTemplate();
            t.fillFromWorld(level, new BlockPos(ox, f, oz), new Vec3i(w * Lattice.MODULE, h, d * Lattice.MODULE), true, null);
            BlockPos core = piece.core != null && piece.core.getY() >= f && piece.core.getY() < f + h ? piece.core.offset(-ox, -f, -oz) : null;
            CompoundTag tag = clean(t.save(new CompoundTag()), core);
            ResourceLocation sid = floors.size() == 1 ? id : id.withSuffix("_" + (fi + 1));
            structures.add(sid);
            Path file = pack.resolve("data").resolve(sid.getNamespace()).resolve("structure").resolve(sid.getPath() + ".nbt");
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(tag, file);
        }

        JsonObject json = new JsonObject();
        json.add("name", roomName(level, piece, id));
        json.addProperty("group", "room");
        if (structures.size() == 1) json.addProperty("structure", structures.get(0).toString());
        else {
            JsonArray a = new JsonArray();
            structures.forEach(s -> a.add(s.toString()));
            json.add("structure", a);
        }
        json.add("entrance", exitJson(entrance));
        if (!exits.isEmpty()) {
            JsonArray a = new JsonArray();
            exits.forEach(e -> a.add(exitJson(e)));
            json.add("exits", a);
        }
        if (piece.core != null && piece.core.getY() >= floors.get(0) && piece.core.getY() < floors.get(0) + Lattice.BAND) {
            JsonArray a = new JsonArray();
            a.add(piece.core.getX() - ox); a.add(piece.core.getY() - floors.get(0)); a.add(piece.core.getZ() - oz);
            json.add("control_block", a);
        }
        Path jsonFile = pack.resolve("data").resolve(id.getNamespace()).resolve("tardis_rooms").resolve("rooms").resolve(id.getPath() + ".json");
        Files.createDirectories(jsonFile.getParent());
        Files.writeString(jsonFile, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json) + "\n", StandardCharsets.UTF_8);

        Path meta = pack.resolve("pack.mcmeta");
        if (!Files.exists(meta))
            Files.writeString(meta, "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"Rooms exported by TARDIS Refined: Rooms\"\n  }\n}\n", StandardCharsets.UTF_8);
        TardisRooms.LOGGER.info("Exported room #{} ({}) as {} to {}", piece.id, piece.pieceId, id, pack);
        return new Result(pack, note);
    }

    /** Название: данное игроком, иначе как в меню (ключ перевода аддона или текст из датапака), у неизвестного помещения — путь id. */
    private static JsonElement roomName(ServerLevel level, ModuleRegistry.Piece piece, ResourceLocation id) {
        if (piece.customName != null) return new com.google.gson.JsonPrimitive(piece.customName);
        Component name = PieceCatalog.options(piece.node, CustomRooms.INSTANCE.infos()).stream()
                .filter(o -> o.id().equals(piece.pieceId)).findFirst().map(PieceCatalog.Option::label)
                .orElse(Component.literal(id.getPath().substring(id.getPath().lastIndexOf('/') + 1)));
        return ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), name)
                .result().orElseGet(() -> new com.google.gson.JsonPrimitive(name.getString()));
    }

    private static JsonObject exitJson(CustomRoom.Exit e) {
        JsonObject o = new JsonObject();
        o.addProperty("x", e.x());
        o.addProperty("z", e.z());
        o.addProperty("side", e.side().getName());
        if (e.floor() != 0) o.addProperty("floor", e.floor());
        return o;
    }

    private static boolean isOpening(BlockState s) {
        return s.isAir() || Lattice.isPanelBlock(s);
    }

    /** Высота постройки над полом уровня: до последнего слоя, где в модулях есть что-то кроме камня TARDIS. */
    private static int builtHeight(ServerLevel level, int ox, int oz, int w, int d, int floor) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = Lattice.BAND - 2; dy >= 0; dy--)
            for (int x = ox; x < ox + w * Lattice.MODULE; x++)
                for (int z = oz; z < oz + d * Lattice.MODULE; z++) {
                    BlockState s = level.getBlockState(p.set(x, floor + dy, z));
                    if (!s.is(TRBlockRegistry.FOOLS_STONE.get()) && !s.is(Blocks.BEDROCK)) return dy + 1;
                }
        return 0;
    }

    /**
     * Убирает из шаблона блоки аддона: блок управления заменяется соседним блоком пола (место для него задаёт JSON),
     * остальные (панели проходов, фонари, интерфейсы) — воздухом.
     */
    private static CompoundTag clean(CompoundTag tag, BlockPos core) {
        ListTag palette = tag.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) states.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(i)));
        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        Map<BlockPos, Integer> at = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) at.put(posOf(blocks.getCompound(i)), blocks.getCompound(i).getInt("state"));

        int air = index(states, palette, Blocks.AIR.defaultBlockState());
        Integer coreFill = null;
        if (core != null)
            for (Direction dir : Lattice.SIDES) {
                Integer s = at.get(core.relative(dir));
                if (s != null && !ours(states.get(s)) && !states.get(s).isAir()) { coreFill = s; break; }
            }
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompound(i);
            if (!ours(states.get(b.getInt("state")))) continue;
            b.putInt("state", core != null && core.equals(posOf(b)) && coreFill != null ? coreFill : air);
            b.remove("nbt");
        }
        return tag;
    }

    private static boolean ours(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace().equals(TardisRooms.MODID);
    }

    private static int index(List<BlockState> states, ListTag palette, BlockState s) {
        int i = states.indexOf(s);
        if (i >= 0) return i;
        states.add(s);
        palette.add(NbtUtils.writeBlockState(s));
        return states.size() - 1;
    }

    private static BlockPos posOf(CompoundTag block) {
        var p = block.getList("pos", Tag.TAG_INT);
        return new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2));
    }

    private RoomExporter() {}
}
