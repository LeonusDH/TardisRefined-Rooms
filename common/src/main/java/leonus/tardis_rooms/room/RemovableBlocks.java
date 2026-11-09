package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TardisRoomsConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Блоки, которые никогда не мешают удалить помещение и уходят вместе с ним, что бы в них ни было, — из настройки
 * сервера {@code removableWithRoom}: id блоков и метки через {@code #}. Разобранный список запоминается, пока
 * настройка та же.
 */
public final class RemovableBlocks {
    /** Разобранная настройка: id блоков и метки. Неправильные строки пропускаются (их отсекает и проверка настройки). */
    public record Parsed(Set<ResourceLocation> blocks, List<ResourceLocation> tags) {}

    public static Parsed parse(List<? extends String> entries) {
        Set<ResourceLocation> blocks = new HashSet<>();
        List<ResourceLocation> tags = new ArrayList<>();
        for (String e : entries) {
            String s = e.trim();
            boolean tag = s.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? s.substring(1) : s);
            if (id == null) continue;
            if (tag) tags.add(id); else blocks.add(id);
        }
        return new Parsed(Set.copyOf(blocks), List.copyOf(tags));
    }

    /** Подходит ли строка для настройки: id блока или {@code #}метка. */
    public static boolean valid(Object o) {
        if (!(o instanceof String s)) return false;
        s = s.trim();
        return ResourceLocation.tryParse(s.startsWith("#") ? s.substring(1) : s) != null;
    }

    private static List<? extends String> cachedFrom;
    private static Set<ResourceLocation> blocks = Set.of();
    private static List<TagKey<Block>> tags = List.of();

    /** Уходит ли этот блок вместе с помещением, что бы в нём ни было. */
    public static boolean matches(BlockState state) {
        List<? extends String> now = TardisRoomsConfig.removableWithRoom();
        if (now != cachedFrom) {
            Parsed p = parse(now);
            blocks = p.blocks();
            tags = p.tags().stream().map(id -> TagKey.create(Registries.BLOCK, id)).toList();
            cachedFrom = now;
        }
        if (blocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return true;
        for (TagKey<Block> t : tags) if (state.is(t)) return true;
        return false;
    }

    private RemovableBlocks() {}
}
