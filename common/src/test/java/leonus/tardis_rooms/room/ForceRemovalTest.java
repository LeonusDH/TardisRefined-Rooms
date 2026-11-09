package leonus.tardis_rooms.room;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceRemovalTest {
    static {
        // На 1.20.1 ключи измерений требуют загруженных реестров игры.
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final ResourceKey<Level> T1 = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("test", "t1"));
    private static final ResourceKey<Level> T2 = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("test", "t2"));
    private static final BlockPos CORE = new BlockPos(1, 2, 3);
    private final UUID player = UUID.randomUUID();

    @BeforeEach
    void reset() { ForceRemoval.clear(); }

    @Test
    void notArmed() {
        assertFalse(ForceRemoval.take(player, T1, CORE, 100_000));
    }

    @Test
    void tooEarlyThenOk() {
        ForceRemoval.arm(player, T1, CORE, 0);
        assertFalse(ForceRemoval.take(player, T1, CORE, 5_000));
        assertTrue(ForceRemoval.take(player, T1, CORE, 10_000));
    }

    @Test
    void usedOnce() {
        ForceRemoval.arm(player, T1, CORE, 0);
        assertTrue(ForceRemoval.take(player, T1, CORE, 10_000));
        assertFalse(ForceRemoval.take(player, T1, CORE, 11_000));
    }

    @Test
    void otherRoomOrTardis() {
        ForceRemoval.arm(player, T1, CORE, 0);
        assertFalse(ForceRemoval.take(player, T1, CORE.above(), 10_000));
        assertFalse(ForceRemoval.take(player, T2, CORE, 10_000));
        assertFalse(ForceRemoval.take(UUID.randomUUID(), T1, CORE, 10_000));
    }

    @Test
    void staleForgottenOnNextArm() {
        ForceRemoval.arm(player, T1, CORE, 0);
        ForceRemoval.arm(UUID.randomUUID(), T1, CORE, 1_000);
        ForceRemoval.arm(UUID.randomUUID(), T2, CORE, ForceRemoval.EXPIRE_MS + 500);
        assertEquals(2, ForceRemoval.armedCount()); // первое (самое старое) выброшено, второе ещё живо
    }

    @Test
    void expired() {
        ForceRemoval.arm(player, T1, CORE, 0);
        assertFalse(ForceRemoval.take(player, T1, CORE, ForceRemoval.EXPIRE_MS + 1));
    }
}
