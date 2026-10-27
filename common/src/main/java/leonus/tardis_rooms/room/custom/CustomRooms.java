package leonus.tardis_rooms.room.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.mixin.StructureTemplateAccessor;
import leonus.tardis_rooms.room.Lattice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Помещения из датапаков: читает {@code data/<ns>/tardis_rooms/rooms/*.json} при загрузке мира и по /reload,
 * проверяет их вместе с шаблонами и хранит годные. Сломанные пропускаются, причина — в логе (с именем файла).
 */
public final class CustomRooms extends SimpleJsonResourceReloadListener {
    // GSON — раньше INSTANCE: статические поля создаются по порядку, а конструктору он уже нужен.
    private static final Gson GSON = new GsonBuilder().create();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "rooms");
    public static final CustomRooms INSTANCE = new CustomRooms();
    /** Группы меню, в которые можно положить помещение. */
    private static final Set<String> GROUPS = Set.of("corridor", "room", "special", CustomRoom.DEFAULT_GROUP);

    /** Проверенное помещение: описание, размер в модулях и шаблоны по этажам. */
    public record Loaded(ResourceLocation id, CustomRoom def, int width, int depth, List<StructureTemplate> templates) {}

    private volatile Map<ResourceLocation, Loaded> rooms = Map.of();

    private CustomRooms() {
        super(GSON, "tardis_rooms/rooms");
    }

    /** Все годные помещения (в порядке id). */
    public Map<ResourceLocation, Loaded> all() {
        return rooms;
    }

    public Optional<Loaded> get(ResourceLocation id) {
        return Optional.ofNullable(rooms.get(id));
    }

    /** Своя ошибка не должна ломать загрузку датапаков (а с ней и мира): что бы ни случилось — в лог, помещений нет. */
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        try {
            load(files, resources);
        } catch (Exception e) {
            rooms = Map.of();
            TardisRooms.LOGGER.error("Could not load datapack rooms", e);
        }
    }

    private void load(Map<ResourceLocation, JsonElement> files, ResourceManager resources) {
        Map<ResourceLocation, Loaded> out = new LinkedHashMap<>();
        List<ResourceLocation> ids = new ArrayList<>(files.keySet());
        Collections.sort(ids);
        for (ResourceLocation id : ids) {
            Optional<CustomRoom> parsed = CustomRoom.CODEC.parse(JsonOps.INSTANCE, files.get(id))
                    .resultOrPartial(err -> TardisRooms.LOGGER.error("Datapack room {}: {}", id, err));
            if (parsed.isEmpty()) continue;
            List<String> problems = new ArrayList<>();
            Loaded room = check(id, parsed.get(), resources, problems);
            if (room == null) {
                for (String p : problems) TardisRooms.LOGGER.error("Datapack room {}: {}", id, p);
                continue;
            }
            out.put(id, room);
        }
        rooms = Collections.unmodifiableMap(out);
        TardisRooms.LOGGER.info("Loaded {} datapack room(s){}", out.size(), out.isEmpty() ? "" : ": " + out.keySet());
    }

    // ---------- проверка ----------

    private static Loaded check(ResourceLocation id, CustomRoom def, ResourceManager resources, List<String> problems) {
        if (!GROUPS.contains(def.group())) problems.add("unknown group \"" + def.group() + "\" (use one of " + GROUPS + ")");
        if (def.floors() < 1 || def.floors() > 2) problems.add("needs one or two structures (one per floor), got " + def.floors());
        if (!problems.isEmpty()) return null;

        List<StructureTemplate> templates = new ArrayList<>();
        for (ResourceLocation s : def.structures()) {
            StructureTemplate t = readTemplate(s, resources, problems);
            if (t == null) return null;
            templates.add(t);
        }
        Vec3i size = templates.get(0).getSize();
        if (size.getX() % Lattice.MODULE != 0 || size.getZ() % Lattice.MODULE != 0 || size.getX() == 0 || size.getZ() == 0) {
            problems.add("structure " + def.structures().get(0) + " is " + size.getX() + "×" + size.getZ()
                    + " blocks; width and depth must be multiples of " + Lattice.MODULE + " (one module)");
            return null;
        }
        for (int f = 0; f < templates.size(); f++) {
            Vec3i s = templates.get(f).getSize();
            if (s.getX() != size.getX() || s.getZ() != size.getZ())
                problems.add("all floors must have the same width and depth; " + def.structures().get(f) + " is " + s.getX() + "×" + s.getZ());
            if (s.getY() > Lattice.BAND - 1)
                problems.add("structure " + def.structures().get(f) + " is " + s.getY() + " blocks high; a floor fits at most " + (Lattice.BAND - 1));
        }
        int w = size.getX() / Lattice.MODULE, d = size.getZ() / Lattice.MODULE;

        List<CustomRoom.Exit> all = new ArrayList<>();
        all.add(def.entrance());
        all.addAll(def.exits());
        if (def.entrance().floor() != 0) problems.add("the entrance must be on the bottom floor (floor 0)");
        for (CustomRoom.Exit e : all) checkExit(e, w, d, def, templates, problems);
        def.controlBlock().ifPresent(p -> {
            if (p.getX() < 0 || p.getZ() < 0 || p.getX() >= size.getX() || p.getZ() >= size.getZ() || p.getY() < 0 || p.getY() >= templates.get(0).getSize().getY())
                problems.add("control_block " + p.toShortString() + " is outside the bottom floor's structure");
        });
        return problems.isEmpty() ? new Loaded(id, def, w, d, List.copyOf(templates)) : null;
    }

    /** Выход: на краю шаблона и с проходом — воздухом на блоках 8..10 стороны модуля, высотой 2..5 от пола. */
    private static void checkExit(CustomRoom.Exit e, int w, int d, CustomRoom def, List<StructureTemplate> templates, List<String> problems) {
        String where = "exit (" + e.x() + ", " + e.z() + ", " + e.side().getName() + ", floor " + e.floor() + ")";
        if (!e.side().getAxis().isHorizontal()) { problems.add(where + ": side must be north, south, west or east"); return; }
        if (e.floor() < 0 || e.floor() >= templates.size()) { problems.add(where + ": no such floor"); return; }
        if (e.x() < 0 || e.z() < 0 || e.x() >= w || e.z() >= d) { problems.add(where + ": module is outside the structure (" + w + "×" + d + " modules)"); return; }
        boolean onEdge = switch (e.side()) {
            case NORTH -> e.z() == 0;
            case SOUTH -> e.z() == d - 1;
            case WEST -> e.x() == 0;
            default -> e.x() == w - 1;
        };
        if (!onEdge) { problems.add(where + ": that side of the module is inside the room, not on its outer wall"); return; }

        Map<BlockPos, BlockState> blocks = blocksOf(templates.get(e.floor()));
        int ox = e.x() * Lattice.MODULE, oz = e.z() * Lattice.MODULE, last = Lattice.MODULE - 1;
        for (int a = Lattice.OPEN_MIN; a <= Lattice.OPEN_MAX; a++)
            for (int dy = Lattice.OPEN_DY_MIN; dy <= Lattice.OPEN_DY_MAX; dy++) {
                BlockPos p = switch (e.side()) {
                    case NORTH -> new BlockPos(ox + a, dy, oz);
                    case SOUTH -> new BlockPos(ox + a, dy, oz + last);
                    case WEST -> new BlockPos(ox, dy, oz + a);
                    default -> new BlockPos(ox + last, dy, oz + a);
                };
                BlockState s = blocks.get(p);
                if (s == null || !s.isAir()) {
                    problems.add(where + ": no passage — the structure must have air at " + p.toShortString()
                            + " (blocks " + Lattice.OPEN_MIN + ".." + Lattice.OPEN_MAX + " along the wall, " + Lattice.OPEN_DY_MIN + ".." + Lattice.OPEN_DY_MAX + " above the floor)"
                            + (s == null ? "; that spot is empty in the structure — save it with air included" : ", found " + BuiltInRegistries.BLOCK.getKey(s.getBlock())));
                    return;
                }
            }
    }

    static Map<BlockPos, BlockState> blocksOf(StructureTemplate t) {
        Map<BlockPos, BlockState> out = new HashMap<>();
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) t).tardisRooms$getPalettes();
        if (palettes.isEmpty()) return out;
        for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) out.put(info.pos(), info.state());
        return out;
    }

    /** Шаблон из ресурсов датапака ({@code data/<ns>/structure/<путь>.nbt}), как его читает сама игра. */
    private static StructureTemplate readTemplate(ResourceLocation id, ResourceManager resources, List<String> problems) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "structure/" + id.getPath() + ".nbt");
        Optional<Resource> res = resources.getResource(file);
        if (res.isEmpty()) {
            problems.add("structure " + id + " not found (expected data/" + id.getNamespace() + "/structure/" + id.getPath() + ".nbt)");
            return null;
        }
        try (InputStream in = res.get().open()) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            tag = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), tag, NbtUtils.getDataVersion(tag, 500));
            StructureTemplate t = new StructureTemplate();
            t.load(BuiltInRegistries.BLOCK.asLookup(), tag);
            return t;
        } catch (Exception ex) {
            problems.add("structure " + id + " could not be read: " + ex);
            return null;
        }
    }
}
