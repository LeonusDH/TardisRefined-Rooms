package leonus.tardis_rooms.room;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceRemovalTest {
    private static final ResourceKey<Level> T1 = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("test", "t1"));
    private static final ResourceKey<Level> T2 = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("test", "t2"));
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
    void expired() {
        ForceRemoval.arm(player, T1, CORE, 0);
        assertFalse(ForceRemoval.take(player, T1, CORE, ForceRemoval.EXPIRE_MS + 1));
    }
}
