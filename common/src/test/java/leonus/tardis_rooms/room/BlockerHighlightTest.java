package leonus.tardis_rooms.room;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockerHighlightTest {
    @Test
    void nearestFirst() {
        List<BlockPos> l = List.of(new BlockPos(10, 0, 0), new BlockPos(1, 0, 0), new BlockPos(5, 0, 0));
        assertEquals(List.of(new BlockPos(1, 0, 0), new BlockPos(5, 0, 0), new BlockPos(10, 0, 0)),
                BlockerHighlight.nearest(Vec3.ZERO, l));
    }

    @Test
    void cappedAtMax() {
        List<BlockPos> l = new ArrayList<>();
        for (int i = 0; i < 100; i++) l.add(new BlockPos(i, 0, 0));
        List<BlockPos> n = BlockerHighlight.nearest(Vec3.ZERO, l);
        assertEquals(BlockerHighlight.MAX_BLOCKS, n.size());
        assertEquals(new BlockPos(0, 0, 0), n.get(0));
    }
}
