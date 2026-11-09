package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Блоки, которые никогда не мешают удалить помещение и уходят вместе с ним, что бы в них ни было, — из настройки
 * сервера {@code removableWithRoom} (строки — см. {@link BlockMatcher}). Строки разбираются при первом использовании,
 * а не при загрузке настройки: тогда ещё не все моды зарегистрировали блоки. Неправильные пропускаются с
 * предупреждением в логе. Разобранный список запоминается, пока настройка та же.
 */
public final class RemovableBlocks {
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

    /** Для проверки настройки: только непустая строка (разбор — позже, см. выше). */
    public static boolean valid(Object o) {
        return o instanceof String s && !s.isBlank();
    }

    private static List<? extends String> cachedFrom;
    private static List<BlockMatcher> matchers = List.of();

    /** Уходит ли этот блок вместе с помещением, что бы в нём ни было. */
    public static boolean matches(ServerLevel level, BlockPos pos, BlockState state) {
        List<? extends String> now = TardisRoomsConfig.removableWithRoom();
        if (now != cachedFrom) {
            Parsed p = parse(now);
            for (String b : p.bad()) TardisRooms.LOGGER.warn("Config removableWithRoom: skipping {}", b);
            matchers = p.matchers();
            cachedFrom = now;
        }
        for (BlockMatcher m : matchers) if (m.test(level, pos, state)) return true;
        return false;
    }

    private RemovableBlocks() {}
}
