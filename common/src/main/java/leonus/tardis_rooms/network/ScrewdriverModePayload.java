package leonus.tardis_rooms.network;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.tardis.ScrewdriverTools;
import leonus.tardis_rooms.network.codec.ByteBufCodecs;
import leonus.tardis_rooms.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Клиент → сервер: Shift + колёсико с отвёрткой — сменить её режим ({@code step} — направление колёсика). */
public record ScrewdriverModePayload(int step) implements Payload {
    public static final Type<ScrewdriverModePayload> TYPE = new Type<>(new ResourceLocation(TardisRooms.MODID, "screwdriver_mode"));
    public static final StreamCodec<FriendlyByteBuf, ScrewdriverModePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ScrewdriverModePayload::step, ScrewdriverModePayload::new);

    @Override
    public Type<? extends Payload> type() { return TYPE; }

    /** Сервер, главный поток. */
    public static void handle(ScrewdriverModePayload p, ServerPlayer player) {
        ScrewdriverTools.cycleMode(player, p.step());
    }
}
