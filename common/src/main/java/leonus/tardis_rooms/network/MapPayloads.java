package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.ModuleRegistry;
import leonus.tardis_rooms.room.Node;
import leonus.tardis_rooms.room.RoomGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import leonus.tardis_rooms.platform.Platform;
import whocraft.tardis_refined.registry.TRBlockRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Схема застройки вокруг узла: что где занято на этаже. */
public final class MapPayloads {
    /** Схема — квадрат (2R+1)² модулей с модулем, куда ведёт узел, в центре. */
    public static final int RADIUS = 7;
    public static final int SIDE = 2 * RADIUS + 1;

    /** Коды клеток (положительное число — номер помещения). */
    public static final int FREE = 0, HUB = -1, FORBIDDEN = -2, UNKNOWN = -4;

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, p); }

    /** Клиент → сервер: схема этажа {@code floor} вокруг узла; {@code replaceCore} ≠ ZERO — отметить заменяемое. */
    public record Request(BlockPos nodePos, int floor, BlockPos replaceCore) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(rl("map_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Request::nodePos, ByteBufCodecs.VAR_INT, Request::floor,
                BlockPos.STREAM_CODEC, Request::replaceCore, Request::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** Сервер, главный поток. */
        public static void handle(Request r, ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel level)) return;
            if (!Lattice.isTardisLevel(level) || !RoomGrowth.holdsScrewdriver(player)) return;
            if (!Lattice.FLOORS.contains(r.floor())) return;
            BlockPos near = r.replaceCore().equals(BlockPos.ZERO) ? r.nodePos() : r.replaceCore();
            if (player.distanceToSqr(near.getCenter()) > 64) return;
            Node.locate(r.nodePos()).ifPresent(node -> Platform.sendToPlayer(player, build(level, node, r)));
        }

        private static Info build(ServerLevel level, Node node, Request r) {
            ModuleRegistry reg = ModuleRegistry.get(level);
            int cx = node.targetX(), cz = node.targetZ(), floor = r.floor();
            List<Integer> cells = new ArrayList<>(SIDE * SIDE);
            Map<Integer, PieceName> names = new LinkedHashMap<>();
            for (int dz = -RADIUS; dz <= RADIUS; dz++)
                for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                    int mx = cx + dx, mz = cz + dz;
                    long m = Lattice.key(mx, mz);
                    int code;
                    ModuleRegistry.Piece p;
                    if (Lattice.isHub(level, mx, mz, floor)) code = HUB;
                    else if ((p = reg.ownerOf(m, floor)) != null) { code = p.id; names.put(p.id, new PieceName(p.pieceId, p.customName == null ? "" : p.customName)); }
                    else if (Lattice.isForbidden(level, mx, mz, floor)) code = FORBIDDEN;
                    else code = looksBuilt(level, mx, mz, floor) ? UNKNOWN : FREE;
                    cells.add(code);
                }
            int replacing = 0;
            if (!r.replaceCore().equals(BlockPos.ZERO)) {
                ModuleRegistry.Piece p = reg.pieceAtCore(r.replaceCore());
                if (p != null) replacing = p.id;
            }
            return new Info(r.nodePos(), floor, cells, new ArrayList<>(names.keySet()), new ArrayList<>(names.values()), replacing);
        }

        /**
         * Модуль без записи, но не камень: чужая постройка (например, сгенерированная TR до аддона).
         * Смотрим только загруженные чанки — схема не должна генерировать мир.
         */
        private static boolean looksBuilt(ServerLevel level, int mx, int mz, int floor) {
            Block stone = TRBlockRegistry.FOOLS_STONE.get();
            for (Direction side : Lattice.SIDES) {
                BlockPos pos = Lattice.nodePos(mx, mz, side, floor);
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                if (chunk == null) return false;
                if (!chunk.getBlockState(pos).is(stone)) return true;
            }
            return false;
        }
    }

    /** Тип помещения и название, данное игроком (пусто — нет). */
    public record PieceName(ResourceLocation id, String custom) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PieceName> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, PieceName::id, ByteBufCodecs.STRING_UTF8, PieceName::custom, PieceName::new);
    }

    /** Сервер → клиент: клетки схемы построчно (z, затем x) и названия помещений на ней. */
    public record Info(BlockPos nodePos, int floor, List<Integer> cells, List<Integer> pieceIds,
                       List<PieceName> pieceNames, int replacingId) implements CustomPacketPayload {
        public static final Type<Info> TYPE = new Type<>(rl("map_info"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Info> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Info::nodePos,
                ByteBufCodecs.VAR_INT, Info::floor,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Info::cells,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Info::pieceIds,
                PieceName.STREAM_CODEC.apply(ByteBufCodecs.list()), Info::pieceNames,
                ByteBufCodecs.VAR_INT, Info::replacingId,
                Info::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public int cell(int dx, int dz) {
            if (Math.abs(dx) > RADIUS || Math.abs(dz) > RADIUS || cells.size() != SIDE * SIDE) return FREE;
            return cells.get((dz + RADIUS) * SIDE + dx + RADIUS);
        }

        public PieceName nameOf(int id) {
            int i = pieceIds.indexOf(id);
            return i < 0 ? null : pieceNames.get(i);
        }

        /** Клиент, главный поток. */
        public static void handle(Info i) {
            leonus.tardis_rooms.client.RoomMapScreen.accept(i);
        }
    }

    private MapPayloads() {}
}
