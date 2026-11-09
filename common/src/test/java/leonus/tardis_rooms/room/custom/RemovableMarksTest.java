package leonus.tardis_rooms.room.custom;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import leonus.tardis_rooms.room.Lattice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Поле removable помещения из датапака: формат и перенос отражением полосы у прохода. */
class RemovableMarksTest {
    @Test
    void placesAndDescriptions() {
        var codec = Codec.either(CustomRoom.Mark.CODEC, Codec.STRING).listOf();
        var list = codec.parse(JsonOps.INSTANCE, JsonParser.parseString("[[3, 2, 5], [3, 2, 5, 1], \"#minecraft:banners\"]")).getOrThrow();
        assertEquals(Either.left(new CustomRoom.Mark(new BlockPos(3, 2, 5), 0)), list.get(0));
        assertEquals(Either.left(new CustomRoom.Mark(new BlockPos(3, 2, 5), 1)), list.get(1));
        assertEquals(Either.right("#minecraft:banners"), list.get(2));
        assertTrue(CustomRoom.Mark.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("[3, 2]")).error().isPresent());
    }

    @Test
    void doorwayStripMirrors() {
        // Северная стена модуля (0, 0), уровень 97: блок 9 вдоль стены уезжает на 14 и обратно; вне полосы — на месте.
        int floor = 97;
        BlockPos at9 = Lattice.openingPos(0, 0, Direction.NORTH, floor, 9, 2).relative(Direction.SOUTH, 1);
        BlockPos at14 = Lattice.openingPos(0, 0, Direction.NORTH, floor, 14, 2).relative(Direction.SOUTH, 1);
        assertEquals(at14, CustomRoomPlan.mirroredInDoorway(at9, 0, 0, Direction.NORTH, floor, 10));
        assertEquals(at9, CustomRoomPlan.mirroredInDoorway(at14, 0, 0, Direction.NORTH, floor, 10));
        BlockPos outside = Lattice.openingPos(0, 0, Direction.NORTH, floor, 3, 2).relative(Direction.SOUTH, 1);
        assertEquals(outside, CustomRoomPlan.mirroredInDoorway(outside, 0, 0, Direction.NORTH, floor, 10));
    }
}
