package leonus.tardis_rooms.room;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Optional;

/** Узел роста: модуль, сторона, на которой он стоит (= куда растём), и этаж. */
public record Node(int mx, int mz, Direction side, int floorY) {
    public int targetX() { return mx + side.getStepX(); }
    public int targetZ() { return mz + side.getStepZ(); }

    public static Optional<Node> locate(BlockPos pos) {
        int mx = Lattice.moduleOf(pos.getX()), mz = Lattice.moduleOf(pos.getZ());
        int floorY = pos.getY() - Lattice.NODE_DY;
        if (!Lattice.FLOORS.contains(floorY)) return Optional.empty();
        for (Direction side : Lattice.SIDES) {
            if (Lattice.nodePos(mx, mz, side, floorY).equals(pos)) return Optional.of(new Node(mx, mz, side, floorY));
        }
        return Optional.empty();
    }
}
