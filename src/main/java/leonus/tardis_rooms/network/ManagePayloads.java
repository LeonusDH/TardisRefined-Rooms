package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.ModuleRegistry;
import leonus.tardis_rooms.room.RoomGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Пакеты блока управления: запрос сведений, ответ, удаление. */
public final class ManagePayloads {
    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, p); }

    /** Клиент → сервер: что это за помещение и можно ли его удалить. */
    public record Request(BlockPos core) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(rl("manage_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Request::core, Request::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Request r, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
                if (!Lattice.isTardisLevel(level) || player.distanceToSqr(r.core().getCenter()) > 64) return;
                ModuleRegistry.Piece piece = ModuleRegistry.get(level).pieceAtCore(r.core());
                if (piece == null) return;
                leonus.tardis_rooms.lang.TRoomsText problem = RoomGrowth.removalProblem(level, piece);
                BlockPos nodePos = Lattice.nodePos(piece.node.mx(), piece.node.mz(), piece.node.side(), piece.node.floorY());
                PacketDistributor.sendToPlayer(player, new Info(r.core(), piece.pieceId, nodePos, problem == null, problem == null ? "" : problem.key()));
            });
        }
    }

    /** Сервер → клиент: сведения о помещении. */
    public record Info(BlockPos core, ResourceLocation pieceId, BlockPos nodePos, boolean canRemove, String problem) implements CustomPacketPayload {
        public static final Type<Info> TYPE = new Type<>(rl("manage_info"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Info> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Info::core, ResourceLocation.STREAM_CODEC, Info::pieceId, BlockPos.STREAM_CODEC, Info::nodePos,
                ByteBufCodecs.BOOL, Info::canRemove, ByteBufCodecs.STRING_UTF8, Info::problem, Info::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Info i, IPayloadContext ctx) {
            ctx.enqueueWork(() -> leonus.tardis_rooms.client.ClientManage.accept(i));
        }
    }

    /** Клиент → сервер: удалить помещение. */
    public record Remove(BlockPos core) implements CustomPacketPayload {
        public static final Type<Remove> TYPE = new Type<>(rl("manage_remove"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Remove> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Remove::core, Remove::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Remove r, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) RoomGrowth.tryRemove(player, r.core());
            });
        }
    }

    private ManagePayloads() {}
}
