package leonus.tardis_rooms.network;

import io.netty.buffer.ByteBuf;
import leonus.tardis_rooms.Screwdriver;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.room.BlockerHighlight;
import leonus.tardis_rooms.room.Connectivity;
import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.ModuleRegistry;
import leonus.tardis_rooms.room.RoomGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import leonus.tardis_rooms.platform.Platform;

import java.util.List;

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
            if (!Lattice.isTardisLevel(level) || player.distanceToSqr(r.core().getCenter()) > 64) {
                TardisRooms.debug("{}: room info for {} ignored: not in a TARDIS or too far", TardisRooms.who(player), r.core().toShortString());
                return;
            }
            ModuleRegistry.Piece piece = ModuleRegistry.get(level).pieceAtCore(r.core());
            if (piece == null) {
                TardisRooms.debug("{}: room info for {} ignored: no room there", TardisRooms.who(player), r.core().toShortString());
                return;
            }
            // Почему нельзя удалить. Мешают только вещи внутри — можно удалить вместе с ними (окно подтверждения):
            // тогда и какие это вещи (по блоку на вещь).
            TRoomsText problem = null;
            List<ResourceLocation> blockers = List.of();
            if (!Connectivity.canRemove(level, piece)) {
                problem = TRoomsText.MSG_CUTS_OFF;
            } else {
                blockers = BlockerHighlight.group(level, RoomGrowth.blockers(level, piece)).stream()
                        .map(t -> BuiltInRegistries.BLOCK.getKey(level.getBlockState(t.pos()).getBlock())).limit(256).toList();
                if (!blockers.isEmpty()) problem = TRoomsText.MSG_HAS_CONTAINERS;
            }
            BlockPos nodePos = Lattice.nodePos(piece.node.mx(), piece.node.mz(), piece.node.side(), piece.node.floorY());
            Platform.sendToPlayer(player, new Info(r.core(), piece.pieceId, piece.customName == null ? "" : piece.customName,
                    nodePos, problem == null, problem == null ? "" : problem.key(), blockers));
        }
    }

    /**
     * Сервер → клиент: сведения о помещении; {@code customName} пусто — названия нет. {@code blockers} — блоки вещей,
     * которые мешают удалить (по одному на вещь); не пусто — можно удалить вместе с ними через окно подтверждения.
     */
    public record Info(BlockPos core, ResourceLocation pieceId, String customName, BlockPos nodePos, boolean canRemove, String problem,
                       List<ResourceLocation> blockers) implements CustomPacketPayload {
        public static final Type<Info> TYPE = new Type<>(rl("manage_info"));
        private static final StreamCodec<ByteBuf, List<ResourceLocation>> IDS = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(256));
        // Полей семь, а StreamCodec.composite в 1.21.1 берёт не больше шести — пишем по порядку сами.
        public static final StreamCodec<RegistryFriendlyByteBuf, Info> STREAM_CODEC = StreamCodec.of(
                (buf, i) -> {
                    BlockPos.STREAM_CODEC.encode(buf, i.core());
                    ResourceLocation.STREAM_CODEC.encode(buf, i.pieceId());
                    ByteBufCodecs.STRING_UTF8.encode(buf, i.customName());
                    BlockPos.STREAM_CODEC.encode(buf, i.nodePos());
                    ByteBufCodecs.BOOL.encode(buf, i.canRemove());
                    ByteBufCodecs.STRING_UTF8.encode(buf, i.problem());
                    IDS.encode(buf, i.blockers());
                },
                buf -> new Info(BlockPos.STREAM_CODEC.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                        BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), IDS.decode(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** Клиент, главный поток. */
        public static void handle(Info i) {
            leonus.tardis_rooms.client.ClientManage.accept(i);
        }
    }

    /** Клиент → сервер: удалить помещение; {@code force} — вместе с тем, что мешает (из окна подтверждения). */
    public record Remove(BlockPos core, boolean force) implements CustomPacketPayload {
        public static final Type<Remove> TYPE = new Type<>(rl("manage_remove"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Remove> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Remove::core, ByteBufCodecs.BOOL, Remove::force, Remove::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(Remove r, ServerPlayer player) {
            RoomGrowth.tryRemove(player, r.core(), r.force());
        }
    }

    /**
     * Клиент → сервер: показать, что мешает удалить помещение (сообщение и подсветка). {@code confirm} — открыто окно
     * подтверждения «удалить вместе с ними»: сообщение над хотбаром, и пошёл отсчёт.
     */
    public record ShowBlockers(BlockPos core, boolean confirm) implements CustomPacketPayload {
        public static final Type<ShowBlockers> TYPE = new Type<>(rl("manage_show_blockers"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ShowBlockers> STREAM_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, ShowBlockers::core, ByteBufCodecs.BOOL, ShowBlockers::confirm, ShowBlockers::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(ShowBlockers r, ServerPlayer player) {
            RoomGrowth.showBlockers(player, r.core(), !r.confirm(), r.confirm());
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
            if (!Screwdriver.held(player) || player.distanceToSqr(r.core().getCenter()) > 64) return;
            if (!player.mayBuild() || !level.mayInteract(player, r.core())) return;
            ModuleRegistry reg = ModuleRegistry.get(level);
            ModuleRegistry.Piece piece = reg.pieceAtCore(r.core());
            if (piece == null) return;
            // Только печатные символы (без кодов форматирования §), не длиннее предела.
            String name = net.minecraft.util.StringUtil.filterText(r.name()).trim();
            if (name.length() > ModuleRegistry.MAX_NAME) name = name.substring(0, ModuleRegistry.MAX_NAME).trim();
            reg.rename(piece, name);
            TardisRooms.debug("{} renamed #{} to \"{}\"", TardisRooms.who(player), piece.id, name);
            Request.handle(new Request(r.core()), player);
        }
    }

    private ManagePayloads() {}
}
