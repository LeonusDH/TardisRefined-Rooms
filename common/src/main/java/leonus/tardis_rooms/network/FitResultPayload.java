package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import leonus.tardis_rooms.network.codec.ByteBufCodecs;
import leonus.tardis_rooms.network.codec.StreamCodec;
import leonus.tardis_rooms.network.Payload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Сервер → клиент: что не помещается, что не по сетке TR, что отрежет другие помещения (при замене). */
public record FitResultPayload(BlockPos pos, List<ResourceLocation> blocked, List<ResourceLocation> offGrid, List<ResourceLocation> cutsOff) implements Payload {
    public static final Type<FitResultPayload> TYPE = new Type<>(new ResourceLocation(TardisRooms.MODID, "fit_result"));
    public static final StreamCodec<FriendlyByteBuf, FitResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BLOCK_POS, FitResultPayload::pos,
            ByteBufCodecs.RESOURCE_LOCATION.apply(ByteBufCodecs.list()), FitResultPayload::blocked,
            ByteBufCodecs.RESOURCE_LOCATION.apply(ByteBufCodecs.list()), FitResultPayload::offGrid,
            ByteBufCodecs.RESOURCE_LOCATION.apply(ByteBufCodecs.list()), FitResultPayload::cutsOff,
            FitResultPayload::new);

    @Override
    public Type<? extends Payload> type() { return TYPE; }

    /** Клиент, главный поток. */
    public static void handle(FitResultPayload payload) {
        leonus.tardis_rooms.client.ClientFits.accept(payload);
    }
}
