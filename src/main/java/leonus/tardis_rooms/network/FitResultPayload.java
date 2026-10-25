package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Сервер → клиент: что не помещается, что не по сетке TR, что отрежет другие помещения (при замене). */
public record FitResultPayload(BlockPos pos, List<ResourceLocation> blocked, List<ResourceLocation> offGrid, List<ResourceLocation> cutsOff) implements CustomPacketPayload {
    public static final Type<FitResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "fit_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FitResultPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FitResultPayload::pos,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), FitResultPayload::blocked,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), FitResultPayload::offGrid,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), FitResultPayload::cutsOff,
            FitResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(FitResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> leonus.tardis_rooms.client.ClientFits.accept(payload));
    }
}
