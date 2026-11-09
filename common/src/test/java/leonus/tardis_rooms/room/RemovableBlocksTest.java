package leonus.tardis_rooms.room;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Строки файла removable_with_room.json и поля removable: весь мод и шаблоны со звёздочкой (описания блоков — в игровых тестах). */
class RemovableBlocksTest {
    private static ResourceLocation id(String s) { return ResourceLocation.parse(s); }

    @Test
    void wholeMod() {
        BlockMatcher m = BlockMatcher.parse("@mekanism");
        assertTrue(m.testId(id("mekanism:basic_universal_cable")));
        assertFalse(m.testId(id("minecraft:chest")));
        assertFalse(m.testId(id("mekanismgenerators:heat_generator")));
    }

    @Test
    void starPattern() {
        BlockMatcher cables = BlockMatcher.parse("mekanism:*_cable");
        assertTrue(cables.testId(id("mekanism:basic_universal_cable")));
        assertFalse(cables.testId(id("mekanism:basic_mechanical_pipe")));
        BlockMatcher signs = BlockMatcher.parse("*:*_sign");
        assertTrue(signs.testId(id("minecraft:oak_sign")));
        assertTrue(signs.testId(id("somemod:iron_sign")));
        assertFalse(signs.testId(id("minecraft:oak_sign_post")));
        assertTrue(BlockMatcher.parse("mekanism:*").testId(id("mekanism:digital_miner")));
        assertTrue(BlockMatcher.parse("*_bed").testId(id("minecraft:red_bed"))); // без пространства — minecraft
    }

    @Test
    void badEntries() {
        assertThrows(IllegalArgumentException.class, () -> BlockMatcher.parse("@"));
        assertThrows(IllegalArgumentException.class, () -> BlockMatcher.parse("Not A Block!"));
        assertThrows(IllegalArgumentException.class, () -> BlockMatcher.parse("mekanism:*CABLE"));
        RemovableBlocks.Parsed p = RemovableBlocks.parse(List.of("Not A Block!", "@mekanism"));
        assertEquals(1, p.matchers().size());
        assertEquals(1, p.bad().size());
    }

    @Test
    void fileFormat() {
        assertEquals(List.of("@mekanism", "#minecraft:banners"),
                RemovableBlocks.entries("{\"_comment\": \"x\", \"blocks\": [\"@mekanism\", \"#minecraft:banners\"]}"));
        assertThrows(IllegalArgumentException.class, () -> RemovableBlocks.entries("[\"@mekanism\"]"));
        assertThrows(IllegalArgumentException.class, () -> RemovableBlocks.entries("{\"blocks\": [1]}"));
        // Файл по умолчанию читается обратно тем же списком.
        assertEquals(RemovableBlocks.DEFAULT, RemovableBlocks.entries(RemovableBlocks.defaultFile()));
    }
}
