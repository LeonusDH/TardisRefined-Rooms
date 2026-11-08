package leonus.tardis_rooms.iface;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Хранение настроек интерфейса: перенос старого формата, неизвестные виды, сохранение и сеть. */
class InterfaceDataTest {
    private static final ResourceLocation SOULS = ResourceLocation.fromNamespaceAndPath("othermod", "souls");
    private static final ResourceLocation TARDIS = ResourceLocation.fromNamespaceAndPath("tardis_refined", "abc");

    private static InterfaceData load(Tag tag) {
        return InterfaceData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static CompoundTag save(InterfaceData d) {
        return (CompoundTag) InterfaceData.CODEC.encodeStart(NbtOps.INSTANCE, d).getOrThrow();
    }

    private static CompoundTag base() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", 0x1234);
        t.putString("tardis", TARDIS.toString());
        t.putInt("flow", Flow.OUT.ordinal());
        t.putInt("faces", 0b10_01); // down = PUSH, up = PULL
        return t;
    }

    private static ListTag ids(ResourceLocation... ids) {
        ListTag l = new ListTag();
        for (ResourceLocation id : ids) l.add(StringTag.valueOf(id.toString()));
        return l;
    }

    @Test
    void legacyBitsAreMigrated() {
        CompoundTag t = base();
        t.putInt("types", 1 | 2 | 8 | 32); // энергия, предметы, химикаты, редстоун — по старой таблице
        InterfaceData d = load(t);
        assertEquals(List.of(TransferType.ENERGY.id(), TransferType.ITEMS.id(), TransferType.CHEMICALS.id(), TransferType.REDSTONE.id()), d.kinds());
        assertFalse(d.enabled(TransferType.FLUIDS));
        assertFalse(d.enabled(TransferType.ME));
        assertTrue(d.otherKinds().isEmpty());
        // Остальные поля не тронуты.
        assertEquals(0x1234, d.id());
        assertEquals(Optional.of(TARDIS), d.tardis());
        assertEquals(Flow.OUT, d.flowMode());
        assertEquals(FaceMode.PUSH, d.face(Direction.DOWN));
        assertEquals(FaceMode.PULL, d.face(Direction.UP));
    }

    @Test
    void legacyMaskOfAllZerosStaysEmpty() {
        CompoundTag t = base();
        t.putInt("types", 0);
        assertTrue(load(t).kinds().isEmpty());
    }

    @Test
    void savedInNewFormatOnly() {
        CompoundTag t = base();
        t.putInt("types", 1 | 16);
        CompoundTag out = save(load(t));
        assertFalse(out.contains("types"), "старое поле больше не пишется");
        assertEquals(ids(TransferType.ENERGY.id(), TransferType.ME.id()), out.getList("kinds", Tag.TAG_STRING));
    }

    @Test
    void noKindsFieldGivesDefault() {
        InterfaceData d = load(base());
        assertEquals(InterfaceData.DEFAULT.kinds(), d.kinds());
    }

    @Test
    void kindsWinOverLegacy() {
        CompoundTag t = base();
        t.put("kinds", ids(TransferType.FLUIDS.id()));
        t.putInt("types", 1);
        assertEquals(List.of(TransferType.FLUIDS.id()), load(t).kinds());
    }

    @Test
    void unknownKindsArePreserved() {
        CompoundTag t = base();
        t.put("kinds", ids(SOULS, TransferType.ITEMS.id(), SOULS));
        InterfaceData d = load(t);
        assertEquals(List.of(SOULS), d.otherKinds(), "неизвестный вид хранится один раз");
        assertTrue(d.enabled(TransferType.ITEMS));

        // Переключение других видов, смена направления, ID и сторон его не теряют.
        d = d.toggle(TransferType.ENERGY).toggle(TransferType.ITEMS).withFlow(Flow.IN)
                .withId(7, Optional.empty()).withFace(Direction.NORTH, FaceMode.DISABLED);
        assertEquals(List.of(SOULS), d.otherKinds());
        assertEquals(List.of(TransferType.ENERGY.id(), SOULS), d.kinds());

        InterfaceData back = load(save(d));
        assertEquals(d, back);
        assertTrue(back.kinds().contains(SOULS));
    }

    @Test
    void nbtRoundTrip() {
        InterfaceData d = InterfaceData.DEFAULT.withId(42, Optional.of(TARDIS)).toggle(TransferType.ME)
                .withFace(Direction.EAST, FaceMode.PULL);
        assertEquals(d, load(save(d)));
        assertEquals(InterfaceData.DEFAULT, load(save(InterfaceData.DEFAULT)));
    }

    @Test
    void networkRoundTrip() {
        CompoundTag t = base();
        t.put("kinds", ids(TransferType.REDSTONE.id(), SOULS));
        InterfaceData d = load(t);
        ByteBuf buf = Unpooled.buffer();
        InterfaceData.STREAM_CODEC.encode(buf, d);
        InterfaceData back = InterfaceData.STREAM_CODEC.decode(buf);
        assertEquals(d, back);
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void transferTypeIdsAreUniqueAndResolvable() {
        for (TransferType t : TransferType.values()) assertSame(t, TransferType.byId(t.id()));
        assertNull(TransferType.byId(SOULS));
    }
}
