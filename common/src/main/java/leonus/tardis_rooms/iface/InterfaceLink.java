package leonus.tardis_rooms.iface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Идентификатор интерфейса, запомненный отвёрткой. */
public record InterfaceLink(int id, Optional<ResourceLocation> tardis) {
    public static final Codec<InterfaceLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(InterfaceLink::id),
            ResourceLocation.CODEC.optionalFieldOf("tardis").forGetter(InterfaceLink::tardis)
    ).apply(i, InterfaceLink::new));
}
