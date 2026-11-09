package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.FaceMode;
import leonus.tardis_rooms.iface.Flow;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceHost;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.iface.kind.TransferKinds;
import leonus.tardis_rooms.iface.tardis.ScrewdriverTools;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Клиент → сервер: действие с интерфейсом (из меню или Shift+ЛКМ отвёрткой). {@code kind} — вид передачи для TOGGLE. */
public record InterfacePayload(BlockPos pos, int action, int value, Optional<ResourceLocation> kind) implements CustomPacketPayload {
    public static final int FLOW = 0, TOGGLE = 1, GENERATE = 2, CLEAR = 3, PASTE = 4, FACE = 5;

    public static final Type<InterfacePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "interface_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InterfacePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, InterfacePayload::pos,
            ByteBufCodecs.VAR_INT, InterfacePayload::action,
            ByteBufCodecs.VAR_INT, InterfacePayload::value,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), InterfacePayload::kind,
            InterfacePayload::new);

    public InterfacePayload(BlockPos pos, int action, int value) { this(pos, action, value, Optional.empty()); }

    /** Включить или выключить вид передачи. */
    public static InterfacePayload toggle(BlockPos pos, TransferKind t) { return new InterfacePayload(pos, TOGGLE, 0, Optional.of(t.id())); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Сервер, главный поток. */
    public static void handle(InterfacePayload p, ServerPlayer player) {
        String who = player.getGameProfile().getName();
        String why = !InterfaceHost.holdsConfigurator(player) ? "no screwdriver in hand"
                : player.distanceToSqr(p.pos().getCenter()) > 64 ? "too far"
                : !player.mayBuild() || !player.level().mayInteract(player, p.pos()) ? "may not build here"
                : !(player.level().getBlockEntity(p.pos()) instanceof InterfaceBlockEntity) ? "no interface there" : null;
        if (why != null) {
            TardisRooms.debug("{}: interface action {} at {} ignored: {}", who, p.action(), p.pos().toShortString(), why);
            return;
        }
        InterfaceBlockEntity be = (InterfaceBlockEntity) player.level().getBlockEntity(p.pos());
        TardisRooms.debug("{}: interface at {}: {}", who, p.pos().toShortString(), switch (p.action()) {
            case FLOW -> "direction " + Flow.of(p.value());
            case TOGGLE -> "toggle " + p.kind().map(ResourceLocation::toString).orElse("?");
            case GENERATE -> "generate ID";
            case CLEAR -> "clear ID";
            case PASTE -> "paste ID from the screwdriver";
            case FACE -> "side " + (p.value() >= 0 && p.value() < 18 ? Direction.from3DDataValue(p.value() % 6) : "?")
                    + (p.value() / 6 == 0 ? " next" : p.value() / 6 == 1 ? " previous" : " default");
            default -> "unknown action " + p.action();
        });
        switch (p.action()) {
            case FLOW -> be.setFlow(Flow.of(p.value()));
            case TOGGLE -> {
                TransferKind t = p.kind().map(TransferKinds::byId).orElse(null);
                if (t != null) be.toggle(t);
            }
            case GENERATE -> {
                if (!be.generateId()) player.displayClientMessage(TRoomsText.MSG_INTERFACE_NO_TARDIS.text(), true);
            }
            case CLEAR -> be.clearId();
            case PASTE -> ScrewdriverTools.paste(player, be);
            case FACE -> {
                // value = операция × 6 + сторона; операция: 0 — следующий режим, 1 — предыдущий, 2 — по умолчанию.
                if (p.value() >= 0 && p.value() < 18) {
                    Direction d = Direction.from3DDataValue(p.value() % 6);
                    FaceMode cur = be.face(d);
                    be.setFace(d, switch (p.value() / 6) {
                        case 0 -> cur.next();
                        case 1 -> cur.prev();
                        default -> FaceMode.DEFAULT;
                    });
                }
            }
            default -> {}
        }
    }
}
