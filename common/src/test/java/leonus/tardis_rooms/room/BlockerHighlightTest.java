package leonus.tardis_rooms.room;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockerHighlightTest {
    private static BlockerHighlight.Target at(int x) {
        BlockPos p = new BlockPos(x, 0, 0);
        return new BlockerHighlight.Target(p, List.of(p), new AABB(p));
    }

    @Test
    void nearestFirst() {
        List<BlockerHighlight.Target> l = List.of(at(10), at(1), at(5));
        assertEquals(List.of(at(1), at(5), at(10)), BlockerHighlight.nearest(Vec3.ZERO, l));
    }

    @Test
    void cappedAtMax() {
        List<BlockerHighlight.Target> l = new ArrayList<>();
        for (int i = 0; i < 100; i++) l.add(at(i));
        List<BlockerHighlight.Target> n = BlockerHighlight.nearest(Vec3.ZERO, l);
        assertEquals(BlockerHighlight.MAX_BLOCKS, n.size());
        assertEquals(at(0), n.get(0));
    }
}
