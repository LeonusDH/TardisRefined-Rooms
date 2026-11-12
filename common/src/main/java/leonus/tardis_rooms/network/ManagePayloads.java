package leonus.tardis_rooms.network;

import leonus.tardis_rooms.Screwdriver;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.network.codec.ByteBufCodecs;
import leonus.tardis_rooms.network.codec.StreamCodec;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.room.BlockerHighlight;
import leonus.tardis_rooms.room.Connectivity;
import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.ModuleRegistry;
import leonus.tardis_rooms.room.RoomGrowth;
import leonus.tardis_rooms.room.RoomPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Пакеты блока управления: запрос сведений, ответ, удаление. */
public final class ManagePayloads {
    private static ResourceLocation rl(String p) { return new ResourceLocation(TardisRooms.MODID, p); }

    /** Клиент → сервер: что это за помещение и можно ли его удалить. */
    public record Request(BlockPos core) implements Payload {
        public static final Type<Request> TYPE = new Type<>(rl("manage_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BLOCK_POS, Request::core, Request::new);
        @Override public Type<? extends Payload> type() { return TYPE; }

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
            // тогда и какие это вещи: вид (блок) → сколько их.
            TRoomsText problem = null;
            Map<ResourceLocation, Integer> blockers = Map.of();
            if (!Connectivity.canRemove(level, piece)) {
                problem = TRoomsText.MSG_CUTS_OFF;
            } else {
                blockers = new HashMap<>();
                for (BlockerHighlight.Target t : BlockerHighlight.group(level, RoomGrowth.blockers(level, piece)))
                    blockers.merge(BuiltInRegistries.BLOCK.getKey(level.getBlockState(t.pos()).getBlock()), 1, Integer::sum);
                if (blockers.size() > Info.MAX_KINDS) { // столько видов не бывает, но пакет не должен сломаться
                    blockers = blockers.entrySet().stream().sorted(Map.Entry.<ResourceLocation, Integer>comparingByValue().reversed())
                            .limit(Info.MAX_KINDS).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
                if (!blockers.isEmpty()) problem = TRoomsText.MSG_HAS_CONTAINERS;
            }
            BlockPos nodePos = Lattice.nodePos(piece.node.mx(), piece.node.mz(), piece.node.side(), piece.node.floorY());
            Platform.sendToPlayer(player, new Info(r.core(), piece.pieceId, piece.customName == null ? "" : piece.customName,
                    nodePos, problem == null, problem == null ? "" : problem.key(), blockers));
        }
    }

    /**
     * Сервер → клиент: сведения о помещении; {@code customName} пусто — названия нет. {@code blockers} — вещи, которые
     * мешают удалить: вид (блок) → сколько; не пусто — можно удалить вместе с ними через окно подтверждения.
     */
    public record Info(BlockPos core, ResourceLocation pieceId, String customName, BlockPos nodePos, boolean canRemove, String problem,
                       Map<ResourceLocation, Integer> blockers) implements Payload {
        public static final Type<Info> TYPE = new Type<>(rl("manage_info"));
        /** Видов вещей в пакете не больше этого (число самих вещей не ограничено). */
        static final int MAX_KINDS = 1024;
        // Полей семь, а composite берёт не больше шести — пишем по порядку сами.
        public static final StreamCodec<FriendlyByteBuf, Info> STREAM_CODEC = StreamCodec.of(
                (buf, i) -> {
                    buf.writeBlockPos(i.core());
                    buf.writeResourceLocation(i.pieceId());
                    buf.writeUtf(i.customName());
                    buf.writeBlockPos(i.nodePos());
                    buf.writeBoolean(i.canRemove());
                    buf.writeUtf(i.problem());
                    buf.writeMap(i.blockers(), FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeVarInt);
                },
                buf -> new Info(buf.readBlockPos(), buf.readResourceLocation(), buf.readUtf(), buf.readBlockPos(), buf.readBoolean(), buf.readUtf(),
                        buf.readMap(n -> new HashMap<>(Math.min(n, MAX_KINDS)), FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readVarInt)));
        @Override public Type<? extends Payload> type() { return TYPE; }

        /** Клиент, главный поток. */
        public static void handle(Info i) {
            leonus.tardis_rooms.client.ClientManage.accept(i);
        }
    }

    /** Клиент → сервер: удалить помещение; {@code force} — вместе с тем, что мешает (из окна подтверждения). */
    public record Remove(BlockPos core, boolean force) implements Payload {
        public static final Type<Remove> TYPE = new Type<>(rl("manage_remove"));
        public static final StreamCodec<FriendlyByteBuf, Remove> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BLOCK_POS, Remove::core, ByteBufCodecs.BOOL, Remove::force, Remove::new);
        @Override public Type<? extends Payload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(Remove r, ServerPlayer player) {
            RoomGrowth.tryRemove(player, r.core(), r.force());
        }
    }

    /**
     * Клиент → сервер: показать, что мешает удалить помещение (сообщение и подсветка). {@code confirm} — открыто окно
     * подтверждения «удалить вместе с ними»: сообщение над хотбаром, и пошёл отсчёт.
     */
    public record ShowBlockers(BlockPos core, boolean confirm) implements Payload {
        public static final Type<ShowBlockers> TYPE = new Type<>(rl("manage_show_blockers"));
        public static final StreamCodec<FriendlyByteBuf, ShowBlockers> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BLOCK_POS, ShowBlockers::core, ByteBufCodecs.BOOL, ShowBlockers::confirm, ShowBlockers::new);
        @Override public Type<? extends Payload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(ShowBlockers r, ServerPlayer player) {
            RoomGrowth.showBlockers(player, r.core(), !r.confirm(), r.confirm());
        }
    }

    /** Клиент → сервер: переименовать помещение (пустая строка — убрать название). В ответ — новые сведения. */
    public record Rename(BlockPos core, String name) implements Payload {
        public static final Type<Rename> TYPE = new Type<>(rl("manage_rename"));
        public static final StreamCodec<FriendlyByteBuf, Rename> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BLOCK_POS, Rename::core, ByteBufCodecs.stringUtf8(ModuleRegistry.MAX_NAME * 4), Rename::name, Rename::new);
        @Override public Type<? extends Payload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(Rename r, ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel level) || !Lattice.isTardisLevel(level)) return;
            if (!Screwdriver.held(player) || player.distanceToSqr(r.core().getCenter()) > 64) return;
            if (!player.mayBuild() || !level.mayInteract(player, r.core())) return;
            if (!RoomPermissions.allowed(player, level.dimension(), RoomPermissions.Action.MANAGE,
                    "renaming the room at " + r.core().toShortString())) return;
            ModuleRegistry reg = ModuleRegistry.get(level);
            ModuleRegistry.Piece piece = reg.pieceAtCore(r.core());
            if (piece == null) return;
            // Только печатные символы (без кодов форматирования §), не длиннее предела.
            String name = net.minecraft.SharedConstants.filterText(r.name()).trim();
            if (name.length() > ModuleRegistry.MAX_NAME) name = name.substring(0, ModuleRegistry.MAX_NAME).trim();
            reg.rename(piece, name);
            TardisRooms.debug("{} renamed #{} to \"{}\"", TardisRooms.who(player), piece.id, name);
            Request.handle(new Request(r.core()), player);
        }
    }

    private ManagePayloads() {}
}
