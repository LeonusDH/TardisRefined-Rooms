package leonus.tardis_rooms.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Запреты доходят до клиента такими же, какими их отправил сервер. */
class PermissionsPayloadTest {
    private static PermissionsPayload roundTrip(PermissionsPayload p) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        PermissionsPayload.STREAM_CODEC.encode(buf, p);
        PermissionsPayload back = PermissionsPayload.STREAM_CODEC.decode(buf);
        assertEquals(0, buf.readableBytes(), "лишние байты");
        return back;
    }

    @Test
    void empty() {
        assertEquals(Map.of(), roundTrip(new PermissionsPayload(Map.of())).denied());
    }

    @Test
    void severalTardises() {
        Map<ResourceLocation, Integer> denied = Map.of(
                new ResourceLocation("tardis_refined", "a1b2c3"), 0b001,
                new ResourceLocation("tardis_refined", "d4e5f6"), 0b110);
        assertEquals(denied, roundTrip(new PermissionsPayload(denied)).denied());
    }
}
