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
import leonus.tardis_rooms.platform.Platform;

/** Пакеты блока управления: запрос сведений, ответ, удаление. */
public final class ManagePayloads {
    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, p); }

    /** Клиент → сервер: что это за помещение и можно ли его удалить. */
    public record Request(BlockPos core) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(rl("manage_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Request::core, Request::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(Request r, ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            if (!Lattice.isTardisLevel(level) || player.distanceToSqr(r.core().getCenter()) > 64) return;
            ModuleRegistry.Piece piece = ModuleRegistry.get(level).pieceAtCore(r.core());
            if (piece == null) return;
            leonus.tardis_rooms.lang.TRoomsText problem = RoomGrowth.removalProblem(level, piece);
            BlockPos nodePos = Lattice.nodePos(piece.node.mx(), piece.node.mz(), piece.node.side(), piece.node.floorY());
            Platform.sendToPlayer(player, new Info(r.core(), piece.pieceId, piece.customName == null ? "" : piece.customName,
                    nodePos, problem == null, problem == null ? "" : problem.key()));
        }
    }

    /** Сервер → клиент: сведения о помещении; {@code customName} пусто — названия нет. */
    public record Info(BlockPos core, ResourceLocation pieceId, String customName, BlockPos nodePos, boolean canRemove, String problem) implements CustomPacketPayload {
        public static final Type<Info> TYPE = new Type<>(rl("manage_info"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Info> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Info::core, ResourceLocation.STREAM_CODEC, Info::pieceId, ByteBufCodecs.STRING_UTF8, Info::customName,
                BlockPos.STREAM_CODEC, Info::nodePos, ByteBufCodecs.BOOL, Info::canRemove, ByteBufCodecs.STRING_UTF8, Info::problem, Info::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** Клиент, главный поток. */
        public static void handle(Info i) {
            leonus.tardis_rooms.client.ClientManage.accept(i);
        }
    }

    /** Клиент → сервер: удалить помещение. */
    public record Remove(BlockPos core) implements CustomPacketPayload {
        public static final Type<Remove> TYPE = new Type<>(rl("manage_remove"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Remove> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Remove::core, Remove::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(Remove r, ServerPlayer player) {
            RoomGrowth.tryRemove(player, r.core());
        }
    }

    /** Клиент → сервер: переименовать помещение (пустая строка — убрать название). В ответ — новые сведения. */
    public record Rename(BlockPos core, String name) implements CustomPacketPayload {
        public static final Type<Rename> TYPE = new Type<>(rl("manage_rename"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Rename> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Rename::core, ByteBufCodecs.stringUtf8(ModuleRegistry.MAX_NAME * 4), Rename::name, Rename::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(Rename r, ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel level) || !Lattice.isTardisLevel(level)) return;
            if (!RoomGrowth.holdsScrewdriver(player) || player.distanceToSqr(r.core().getCenter()) > 64) return;
            if (!player.mayBuild() || !level.mayInteract(player, r.core())) return;
            ModuleRegistry reg = ModuleRegistry.get(level);
            ModuleRegistry.Piece piece = reg.pieceAtCore(r.core());
            if (piece == null) return;
            // Только печатные символы (без кодов форматирования §), не длиннее предела.
            String name = net.minecraft.util.StringUtil.filterText(r.name()).trim();
            if (name.length() > ModuleRegistry.MAX_NAME) name = name.substring(0, ModuleRegistry.MAX_NAME).trim();
            reg.rename(piece, name);
            TardisRooms.debug("{} renamed #{} to \"{}\"", player.getGameProfile().getName(), piece.id, name);
            Request.handle(new Request(r.core()), player);
        }
    }

    private ManagePayloads() {}
}
