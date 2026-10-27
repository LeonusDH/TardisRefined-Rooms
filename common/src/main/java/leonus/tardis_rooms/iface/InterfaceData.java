package leonus.tardis_rooms.iface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Настройки интерфейса. Хранятся в блоке и переносятся вместе с ним (предмет помнит их после разрушения);
 * сбрасываются в верстаке или кнопкой в меню.
 * {@code id} 0 — идентификатора нет; {@code tardis} — ТАРДИС, которой он принадлежит.
 */
public record InterfaceData(int id, Optional<ResourceLocation> tardis, int flow, int types, int faces) {
    /** По умолчанию: в обе стороны, всё кроме сети ME (её связь меняет устройство сетей — включается явно). */
    public static final InterfaceData DEFAULT = new InterfaceData(0, Optional.empty(), Flow.BOTH.ordinal(),
            TransferType.ENERGY.bit() | TransferType.ITEMS.bit() | TransferType.FLUIDS.bit() | TransferType.CHEMICALS.bit(), 0);

    public static final Codec<InterfaceData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(InterfaceData::id),
            ResourceLocation.CODEC.optionalFieldOf("tardis").forGetter(InterfaceData::tardis),
            Codec.INT.fieldOf("flow").forGetter(InterfaceData::flow),
            Codec.INT.fieldOf("types").forGetter(InterfaceData::types),
            Codec.INT.optionalFieldOf("faces", 0).forGetter(InterfaceData::faces)
    ).apply(i, InterfaceData::new));

    public static final StreamCodec<ByteBuf, InterfaceData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InterfaceData::id,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), InterfaceData::tardis,
            ByteBufCodecs.VAR_INT, InterfaceData::flow,
            ByteBufCodecs.VAR_INT, InterfaceData::types,
            ByteBufCodecs.VAR_INT, InterfaceData::faces,
            InterfaceData::new);

    public Flow flowMode() { return Flow.of(flow); }

    public boolean enabled(TransferType t) { return (types & t.bit()) != 0; }

    public InterfaceData withId(int id, Optional<ResourceLocation> tardis) { return new InterfaceData(id, tardis, flow, types, faces); }

    public InterfaceData withFlow(Flow f) { return new InterfaceData(id, tardis, f.ordinal(), types, faces); }

    public InterfaceData toggle(TransferType t) { return new InterfaceData(id, tardis, flow, types ^ t.bit(), faces); }

    /** Режим грани (по 2 бита на грань в {@code faces}). */
    public FaceMode face(net.minecraft.core.Direction d) { return face(faces, d); }

    public static FaceMode face(int faces, net.minecraft.core.Direction d) { return FaceMode.of((faces >> (d.get3DDataValue() * 2)) & 3); }

    public InterfaceData withFace(net.minecraft.core.Direction d, FaceMode m) {
        int shift = d.get3DDataValue() * 2;
        return new InterfaceData(id, tardis, flow, types, (faces & ~(3 << shift)) | (m.ordinal() << shift));
    }

    /** Идентификатор в виде четырёх шестнадцатеричных символов, как частота P2P в AE2. */
    public static String hex(int id) {
        return String.format("%04X", id & 0xFFFF);
    }
}
