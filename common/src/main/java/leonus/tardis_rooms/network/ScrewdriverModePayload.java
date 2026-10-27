package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.InterfaceTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Клиент → сервер: Shift + колёсико с отвёрткой — сменить её режим ({@code step} — направление колёсика). */
public record ScrewdriverModePayload(int step) implements CustomPacketPayload {
    public static final Type<ScrewdriverModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "screwdriver_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrewdriverModePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ScrewdriverModePayload::step, ScrewdriverModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Сервер, главный поток. */
    public static void handle(ScrewdriverModePayload p, ServerPlayer player) {
        InterfaceTools.cycleMode(player, p.step());
    }
}
