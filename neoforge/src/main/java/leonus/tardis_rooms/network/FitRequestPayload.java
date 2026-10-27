package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.ModuleRegistry;
import leonus.tardis_rooms.room.Node;
import leonus.tardis_rooms.room.RoomGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import leonus.tardis_rooms.platform.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Клиент → сервер: «что влезает за этот узел?» ({@code replaceCore} ≠ ZERO — при замене помещения). */
public record FitRequestPayload(BlockPos pos, BlockPos replaceCore) implements CustomPacketPayload {
    public static final Type<FitRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "fit_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FitRequestPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, FitRequestPayload::pos, BlockPos.STREAM_CODEC, FitRequestPayload::replaceCore, FitRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Сервер, главный поток. */
    public static void handle(FitRequestPayload payload, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!Lattice.isTardisLevel(level) || !RoomGrowth.holdsScrewdriver(player)) return;
        ModuleRegistry.Piece replacing = null;
        if (!payload.replaceCore().equals(BlockPos.ZERO)) {
            replacing = ModuleRegistry.get(level).pieceAtCore(payload.replaceCore());
            if (replacing == null || player.distanceToSqr(payload.replaceCore().getCenter()) > 64) return;
        } else if (player.distanceToSqr(payload.pos().getCenter()) > 64) return;
        ModuleRegistry.Piece rep = replacing;
        Node.locate(payload.pos()).ifPresent(node -> {
            Map<ResourceLocation, RoomGrowth.Fit> fits = RoomGrowth.checkAll(level, node, rep);
            List<ResourceLocation> blocked = new ArrayList<>(), offGrid = new ArrayList<>(), cutsOff = new ArrayList<>();
            fits.forEach((id, fit) -> {
                if (fit == RoomGrowth.Fit.BLOCKED) blocked.add(id);
                if (fit == RoomGrowth.Fit.OFF_GRID) offGrid.add(id);
                if (fit == RoomGrowth.Fit.CUTS_OFF) cutsOff.add(id);
            });
            Platform.sendToPlayer(player, new FitResultPayload(payload.pos(), blocked, offGrid, cutsOff));
        });
    }
}
