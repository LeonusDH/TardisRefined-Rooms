package leonus.tardis_rooms.room;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Блоки, которые никогда не мешают удалить помещение и уходят вместе с ним, что бы в них ни было, — из отдельного
 * файла {@code config/tardis_rooms/removable_with_room.json} (строки — см. {@link BlockMatcher}). Отдельно от
 * основных настроек: сборке проще дописать свои моды. Файла нет — создаётся со списком по умолчанию. Читается
 * при загрузке датапаков ({@link #reload}): при запуске сервера и по /reload. Разбираются строки при первом
 * использовании: во время загрузки датапаков теги ещё не загружены. Неправильные строки пропускаются с
 * предупреждением в логе.
 */
public final class RemovableBlocks {
    public static final String FILE = TardisRooms.MODID + "/removable_with_room.json";

    /** По умолчанию: таблички, флаги и головы — в них не хранят вещей. */
    public static final List<String> DEFAULT = List.of("#minecraft:all_signs", "#minecraft:banners",
            "minecraft:skeleton_skull", "minecraft:skeleton_wall_skull", "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_wall_skull",
            "minecraft:zombie_head", "minecraft:zombie_wall_head", "minecraft:creeper_head", "minecraft:creeper_wall_head",
            "minecraft:piglin_head", "minecraft:piglin_wall_head", "minecraft:dragon_head", "minecraft:dragon_wall_head",
            "minecraft:player_head", "minecraft:player_wall_head");

    private static final String COMMENT = "Blocks that never keep a TARDIS room from being removed or replaced and go with it, whatever is in them. "
            + "Entries: \"minecraft:lectern\", \"#minecraft:banners\" (tag), \"mekanism:*_cable\" (* is any text), \"@mekanism\" (whole mod), "
            + "\"minecraft:player_head{profile:{name:\\\"MHF_Question\\\"}}\" (as in /execute if block). "
            + "Anything in these blocks is lost with the room: don't add blocks where players keep things. TARDIS Interfaces always count. "
            + "Re-read on /reload. See docs/block-entries.md in the addon's repository.";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Разбор строк: годные и неправильные (с причиной). */
    public record Parsed(List<BlockMatcher> matchers, List<String> bad) {}

    public static Parsed parse(List<? extends String> entries) {
        List<BlockMatcher> ok = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        for (String e : entries) {
            try {
                ok.add(BlockMatcher.parse(e));
            } catch (IllegalArgumentException ex) {
                bad.add(e + " (" + ex.getMessage() + ")");
            }
        }
        return new Parsed(List.copyOf(ok), List.copyOf(bad));
    }

    /** Строки из содержимого файла: {@code {"blocks": [...]}}. Не тот вид — {@link IllegalArgumentException}. */
    public static List<String> entries(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("blocks") || !root.getAsJsonObject().get("blocks").isJsonArray())
            throw new IllegalArgumentException("expected {\"blocks\": [ ... ]}");
        List<String> out = new ArrayList<>();
        for (JsonElement e : root.getAsJsonObject().getAsJsonArray("blocks")) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("every entry must be a string, got " + e);
            out.add(e.getAsString());
        }
        return out;
    }

    /** Файл со списком по умолчанию. */
    static String defaultFile() {
        JsonObject o = new JsonObject();
        o.addProperty("_comment", COMMENT);
        JsonArray a = new JsonArray();
        DEFAULT.forEach(a::add);
        o.add("blocks", a);
        return GSON.toJson(o) + "\n";
    }

    /** Строки из файла (null — ещё не читали) и разобранные из них (null — ещё не разбирали). */
    private static volatile List<String> entries;
    private static volatile List<BlockMatcher> matchers;

    /** Перечитать файл (при загрузке датапаков: блоки всех модов уже зарегистрированы). */
    public static void reload() {
        Path file = Platform.configDir().resolve(FILE);
        List<String> list;
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, defaultFile(), StandardCharsets.UTF_8);
            }
            list = entries(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            // Файл не трогаем — его правят руками; до исправления — список по умолчанию.
            TardisRooms.LOGGER.error("Could not read {}: {}. Using the default list until it is fixed.", file, e.getMessage());
            list = DEFAULT;
        }
        entries = List.copyOf(list);
        matchers = null; // разберём при первом использовании, когда загрузятся теги
    }

    /** Уходит ли этот блок вместе с помещением, что бы в нём ни было. */
    public static boolean matches(ServerLevel level, BlockPos pos, BlockState state) {
        List<BlockMatcher> m = matchers;
        if (m == null) {
            Parsed p = parse(entries != null ? entries : DEFAULT);
            for (String b : p.bad()) TardisRooms.LOGGER.warn("{}: skipping {}", FILE, b);
            matchers = m = p.matchers();
        }
        for (BlockMatcher x : m) if (x.test(level, pos, state)) return true;
        return false;
    }

    private RemovableBlocks() {}
}
