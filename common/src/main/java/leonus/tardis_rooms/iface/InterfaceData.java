package leonus.tardis_rooms.iface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.iface.kind.TransferKinds;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Настройки интерфейса. Хранятся в блоке и переносятся вместе с ним (предмет помнит их после разрушения);
 * сбрасываются в верстаке или кнопкой в меню.
 * {@code id} 0 — идентификатора нет; {@code tardis} — ТАРДИС, которой он принадлежит.
 * {@code types} — включённые виды передачи, по биту на вид ({@link TransferKind#bit()}), только в памяти;
 * {@code otherKinds} — включённые виды, которых в этой игре нет (мод убрали): хранятся как есть, чтобы вернуться с модом.
 * В сохранении и по сети виды — список id ({@code kinds}).
 */
public record InterfaceData(int id, Optional<ResourceLocation> tardis, int flow, int types, int faces, List<ResourceLocation> otherKinds) {
    private static volatile InterfaceData defaults;

    /** Новый интерфейс: в обе стороны, виды — те, что включены по умолчанию ({@link TransferKind#enabledByDefault()}). */
    public static InterfaceData defaults() {
        InterfaceData d = defaults;
        if (d == null) {
            int bits = 0;
            for (TransferKind k : TransferKinds.all()) if (k.enabledByDefault()) bits |= k.bit();
            defaults = d = new InterfaceData(0, Optional.empty(), Flow.BOTH.ordinal(), bits, 0, List.of());
        }
        return d;
    }

    /**
     * Старый формат хранил виды битами в {@code types}: бит → id. Таблица постоянная — с порядком видов в игре
     * её не связывать, иначе старые миры прочитаются не так.
     */
    private static final List<ResourceLocation> LEGACY_BITS = Stream.of("energy", "items", "fluids", "chemicals", "me", "redstone")
            .map(n -> ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, n)).toList();

    public InterfaceData {
        otherKinds = List.copyOf(otherKinds);
    }

    /** Как записано: виды — список id; {@code legacyTypes} — только при чтении старого формата. */
    private record Stored(int id, Optional<ResourceLocation> tardis, int flow, Optional<List<ResourceLocation>> kinds,
                          Optional<Integer> legacyTypes, int faces) {}

    private static final Codec<Stored> STORED = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(Stored::id),
            ResourceLocation.CODEC.optionalFieldOf("tardis").forGetter(Stored::tardis),
            Codec.INT.fieldOf("flow").forGetter(Stored::flow),
            ResourceLocation.CODEC.listOf().optionalFieldOf("kinds").forGetter(Stored::kinds),
            Codec.INT.optionalFieldOf("types").forGetter(Stored::legacyTypes),
            Codec.INT.optionalFieldOf("faces", 0).forGetter(Stored::faces)
    ).apply(i, Stored::new));

    public static final Codec<InterfaceData> CODEC = STORED.xmap(InterfaceData::fromStored, InterfaceData::toStored);

    public static final StreamCodec<ByteBuf, InterfaceData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InterfaceData::id,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), InterfaceData::tardis,
            ByteBufCodecs.VAR_INT, InterfaceData::flow,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(64)), InterfaceData::kinds,
            ByteBufCodecs.VAR_INT, InterfaceData::faces,
            InterfaceData::fromKinds);

    private static InterfaceData fromStored(Stored s) {
        if (s.kinds().isPresent()) return fromKinds(s.id(), s.tardis(), s.flow(), s.kinds().get(), s.faces());
        if (s.legacyTypes().isPresent()) {
            List<ResourceLocation> kinds = new ArrayList<>();
            int bits = s.legacyTypes().get();
            for (int b = 0; b < LEGACY_BITS.size(); b++) if ((bits & (1 << b)) != 0) kinds.add(LEGACY_BITS.get(b));
            return fromKinds(s.id(), s.tardis(), s.flow(), kinds, s.faces());
        }
        return new InterfaceData(s.id(), s.tardis(), s.flow(), defaults().types, s.faces(), List.of());
    }

    private Stored toStored() {
        return new Stored(id, tardis, flow, Optional.of(kinds()), Optional.empty(), faces);
    }

    /** Из списка id: известные — в биты, неизвестные — как есть. */
    private static InterfaceData fromKinds(int id, Optional<ResourceLocation> tardis, int flow, List<ResourceLocation> kinds, int faces) {
        int bits = 0;
        List<ResourceLocation> other = new ArrayList<>();
        for (ResourceLocation k : kinds) {
            TransferKind t = TransferKinds.byId(k);
            if (t != null) bits |= t.bit();
            else if (!other.contains(k)) other.add(k);
        }
        return new InterfaceData(id, tardis, flow, bits, faces, other);
    }

    /** Включённые виды списком id: известные по порядку видов, затем неизвестные. */
    public List<ResourceLocation> kinds() {
        List<ResourceLocation> out = new ArrayList<>();
        for (TransferKind t : TransferKinds.all()) if ((types & t.bit()) != 0) out.add(t.id());
        out.addAll(otherKinds);
        return out;
    }

    public Flow flowMode() { return Flow.of(flow); }

    public boolean enabled(TransferKind t) { return (types & t.bit()) != 0; }

    public InterfaceData withId(int id, Optional<ResourceLocation> tardis) { return new InterfaceData(id, tardis, flow, types, faces, otherKinds); }

    public InterfaceData withFlow(Flow f) { return new InterfaceData(id, tardis, f.ordinal(), types, faces, otherKinds); }

    public InterfaceData toggle(TransferKind t) { return new InterfaceData(id, tardis, flow, types ^ t.bit(), faces, otherKinds); }

    /** Режим грани (по 2 бита на грань в {@code faces}). */
    public FaceMode face(Direction d) { return face(faces, d); }

    public static FaceMode face(int faces, Direction d) { return FaceMode.of((faces >> (d.get3DDataValue() * 2)) & 3); }

    public InterfaceData withFace(Direction d, FaceMode m) {
        int shift = d.get3DDataValue() * 2;
        return new InterfaceData(id, tardis, flow, types, (faces & ~(3 << shift)) | (m.ordinal() << shift), otherKinds);
    }

    /** Идентификатор в виде четырёх шестнадцатеричных символов, как частота P2P в AE2. */
    public static String hex(int id) {
        return String.format("%04X", id & 0xFFFF);
    }
}
