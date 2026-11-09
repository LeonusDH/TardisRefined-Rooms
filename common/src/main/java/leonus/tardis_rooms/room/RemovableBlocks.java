package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Блоки, которые никогда не мешают удалить помещение и уходят вместе с ним, что бы в них ни было, — из отдельного
 * файла настроек {@code config/tardis_rooms/removable_with_room.toml} ({@link TardisRoomsConfig#REMOVABLE_SPEC};
 * строки — см. {@link BlockMatcher}). Отдельно от основных настроек: сборке проще дописать свои моды. Строки
 * разбираются при первом использовании (когда загружены и блоки всех модов, и теги) и заново — когда файл изменился.
 * Неправильные строки пропускаются с предупреждением в логе.
 */
public final class RemovableBlocks {
    /** По умолчанию: таблички, флаги и головы — в них не хранят вещей. */
    public static final List<String> DEFAULT = List.of("#minecraft:all_signs", "#minecraft:banners",
            "minecraft:skeleton_skull", "minecraft:skeleton_wall_skull", "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_wall_skull",
            "minecraft:zombie_head", "minecraft:zombie_wall_head", "minecraft:creeper_head", "minecraft:creeper_wall_head",
            "minecraft:piglin_head", "minecraft:piglin_wall_head", "minecraft:dragon_head", "minecraft:dragon_wall_head",
            "minecraft:player_head", "minecraft:player_wall_head");

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

    /** Строки, из которых разобраны {@link #matchers}: файл изменился — список другой, разбираем заново. */
    private static volatile List<? extends String> parsedFrom;
    private static volatile List<BlockMatcher> matchers = List.of();

    /** Уходит ли этот блок вместе с помещением, что бы в нём ни было. */
    public static boolean matches(ServerLevel level, BlockPos pos, BlockState state) {
        List<? extends String> now = TardisRoomsConfig.removableBlocks();
        if (now != parsedFrom) {
            Parsed p = parse(now);
            for (String b : p.bad()) TardisRooms.LOGGER.warn("{}: skipping {}", TardisRooms.REMOVABLE_CONFIG, b);
            TardisRooms.debug("{}: {} entr(ies) in use, {} skipped", TardisRooms.REMOVABLE_CONFIG, p.matchers().size(), p.bad().size());
            matchers = p.matchers();
            parsedFrom = now;
        }
        for (BlockMatcher x : matchers) if (x.test(level, pos, state)) return true;
        return false;
    }

    private RemovableBlocks() {}
}
