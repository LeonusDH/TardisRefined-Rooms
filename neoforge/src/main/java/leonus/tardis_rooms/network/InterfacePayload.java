package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.Flow;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.iface.TransferType;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.room.RoomGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер: действие с интерфейсом (из меню или Shift+ЛКМ отвёрткой). */
public record InterfacePayload(BlockPos pos, int action, int value) implements CustomPacketPayload {
    public static final int FLOW = 0, TOGGLE = 1, GENERATE = 2, CLEAR = 3, PASTE = 4, FACE = 5;

    public static final Type<InterfacePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "interface_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InterfacePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, InterfacePayload::pos,
            ByteBufCodecs.VAR_INT, InterfacePayload::action,
            ByteBufCodecs.VAR_INT, InterfacePayload::value,
            InterfacePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(InterfacePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player) || !RoomGrowth.holdsScrewdriver(player)) return;
            if (player.distanceToSqr(p.pos().getCenter()) > 64) return;
            if (!player.mayBuild() || !player.level().mayInteract(player, p.pos())) return;
            if (!(player.level().getBlockEntity(p.pos()) instanceof InterfaceBlockEntity be)) return;
            switch (p.action()) {
                case FLOW -> be.setFlow(Flow.of(p.value()));
                case TOGGLE -> {
                    if (p.value() >= 0 && p.value() < TransferType.values().length) be.toggle(TransferType.values()[p.value()]);
                }
                case GENERATE -> {
                    if (!be.generateId()) player.displayClientMessage(TRoomsText.MSG_INTERFACE_NO_TARDIS.text(), true);
                }
                case CLEAR -> be.clearId();
                case PASTE -> InterfaceTools.paste(player, be);
                case FACE -> {
                    // value = операция × 6 + сторона; операция: 0 — следующий режим, 1 — предыдущий, 2 — по умолчанию.
                    if (p.value() >= 0 && p.value() < 18) {
                        net.minecraft.core.Direction d = net.minecraft.core.Direction.from3DDataValue(p.value() % 6);
                        leonus.tardis_rooms.iface.FaceMode cur = be.face(d);
                        be.setFace(d, switch (p.value() / 6) {
                            case 0 -> cur.next();
                            case 1 -> cur.prev();
                            default -> leonus.tardis_rooms.iface.FaceMode.DEFAULT;
                        });
                    }
                }
                default -> {}
            }
        });
    }
}
