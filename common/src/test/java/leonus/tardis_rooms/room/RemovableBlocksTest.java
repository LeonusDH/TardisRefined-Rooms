package leonus.tardis_rooms.room;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemovableBlocksTest {
    @Test
    void blocksAndTags() {
        RemovableBlocks.Parsed p = RemovableBlocks.parse(List.of("minecraft:lectern", " #minecraft:banners ", "mymod:shelf"));
        assertEquals(Set.of(ResourceLocation.parse("minecraft:lectern"), ResourceLocation.parse("mymod:shelf")), p.blocks());
        assertEquals(List.of(ResourceLocation.parse("minecraft:banners")), p.tags());
    }

    @Test
    void badEntriesSkipped() {
        assertFalse(RemovableBlocks.valid("Not A Block!"));
        assertFalse(RemovableBlocks.valid(42));
        assertTrue(RemovableBlocks.valid("#c:chests"));
        RemovableBlocks.Parsed p = RemovableBlocks.parse(List.of("Not A Block!", "minecraft:lectern"));
        assertEquals(Set.of(ResourceLocation.parse("minecraft:lectern")), p.blocks());
    }
}
