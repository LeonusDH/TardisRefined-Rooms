package leonus.tardis_rooms.iface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Идентификатор интерфейса, запомненный отвёрткой. */
public record InterfaceLink(int id, Optional<ResourceLocation> tardis) {
    public static final Codec<InterfaceLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(InterfaceLink::id),
            ResourceLocation.CODEC.optionalFieldOf("tardis").forGetter(InterfaceLink::tardis)
    ).apply(i, InterfaceLink::new));

    public static final StreamCodec<ByteBuf, InterfaceLink> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InterfaceLink::id,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), InterfaceLink::tardis,
            InterfaceLink::new);
}
