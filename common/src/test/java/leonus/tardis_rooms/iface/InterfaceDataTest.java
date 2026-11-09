package leonus.tardis_rooms.iface;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Хранение настроек интерфейса: значения по умолчанию, стороны, переключение видов, сохранение. */
class InterfaceDataTest {
    private static final ResourceLocation TARDIS = new ResourceLocation("tardis_refined", "abc");

    private static InterfaceData load(Tag tag) {
        return InterfaceData.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
    }

    private static CompoundTag save(InterfaceData d) {
        return (CompoundTag) InterfaceData.CODEC.encodeStart(NbtOps.INSTANCE, d).result().orElseThrow();
    }

    @Test
    void defaults() {
        InterfaceData d = InterfaceData.DEFAULT;
        assertEquals(0, d.id());
        assertEquals(Flow.BOTH, d.flowMode());
        for (TransferType t : new TransferType[]{TransferType.ENERGY, TransferType.ITEMS, TransferType.FLUIDS, TransferType.CHEMICALS})
            assertTrue(d.enabled(t), t + " включён по умолчанию");
        assertFalse(d.enabled(TransferType.ME), "ME по умолчанию выключена");
        assertFalse(d.enabled(TransferType.REDSTONE), "редстоун по умолчанию выключен");
        for (Direction dir : Direction.values()) assertEquals(FaceMode.DEFAULT, d.face(dir));
    }

    @Test
    void facesAreIndependent() {
        InterfaceData d = InterfaceData.DEFAULT.withFace(Direction.DOWN, FaceMode.PUSH).withFace(Direction.EAST, FaceMode.PULL);
        assertEquals(FaceMode.PUSH, d.face(Direction.DOWN));
        assertEquals(FaceMode.PULL, d.face(Direction.EAST));
        for (Direction dir : Direction.values())
            if (dir != Direction.DOWN && dir != Direction.EAST) assertEquals(FaceMode.DEFAULT, d.face(dir), dir.toString());
        assertEquals(FaceMode.DEFAULT, d.withFace(Direction.DOWN, FaceMode.DEFAULT).face(Direction.DOWN));
    }

    @Test
    void toggleTwiceRestores() {
        InterfaceData d = InterfaceData.DEFAULT.toggle(TransferType.ME).toggle(TransferType.ITEMS);
        assertTrue(d.enabled(TransferType.ME));
        assertFalse(d.enabled(TransferType.ITEMS));
        assertEquals(InterfaceData.DEFAULT, d.toggle(TransferType.ME).toggle(TransferType.ITEMS));
    }

    @Test
    void nbtRoundTrip() {
        InterfaceData d = InterfaceData.DEFAULT.withId(42, Optional.of(TARDIS)).withFlow(Flow.OUT).toggle(TransferType.REDSTONE)
                .withFace(Direction.NORTH, FaceMode.PULL);
        assertEquals(d, load(save(d)));
        assertEquals(InterfaceData.DEFAULT, load(save(InterfaceData.DEFAULT)));
    }

    @Test
    void savedWithoutFacesLoadsDefaultFaces() {
        CompoundTag t = save(InterfaceData.DEFAULT.withFace(Direction.UP, FaceMode.PUSH));
        t.remove("faces");
        assertEquals(FaceMode.DEFAULT, load(t).face(Direction.UP));
    }

    @Test
    void hexId() {
        assertEquals("002A", InterfaceData.hex(42));
        assertEquals("FFFF", InterfaceData.hex(0xFFFF));
    }
}
