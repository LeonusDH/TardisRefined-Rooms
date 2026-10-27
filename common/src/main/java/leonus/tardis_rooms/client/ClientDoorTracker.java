package leonus.tardis_rooms.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import whocraft.tardis_refined.common.blockentity.door.InternalDoorBlockEntity;
import whocraft.tardis_refined.common.blockentity.door.RootShellDoorBlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Клиент не знает, где внутренняя дверь TARDIS: ищем её блок-сущность в чанках вокруг консоли
 * (не чаще раза в 2 секунды) и запоминаем. Состояние двери берём из её блокстейта, он синхронизируется сам.
 */
public final class ClientDoorTracker {
    private static final Map<ResourceKey<Level>, BlockPos> DOORS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long> LAST_SCAN = new HashMap<>();
    private static final int RADIUS_CHUNKS = 4;

    /** @return открыта ли дверь, или null, если дверь рядом не найдена. */
    public static Boolean isDoorOpen(Level level, BlockPos consolePos) {
        ResourceKey<Level> key = level.dimension();
        BlockPos cached = DOORS.get(key);
        if (cached != null && level.getBlockEntity(cached) instanceof InternalDoorBlockEntity door && !(door instanceof RootShellDoorBlockEntity)) {
            return door.isOpen();
        }
        if (LAST_SCAN.size() > 32) { DOORS.clear(); LAST_SCAN.clear(); } // у каждой TARDIS своё измерение — не копим за сессию
        long now = level.getGameTime();
        Long last = LAST_SCAN.get(key);
        if (last != null && Math.abs(now - last) < 40) return null;
        LAST_SCAN.put(key, now);
        DOORS.remove(key);

        int ccx = consolePos.getX() >> 4, ccz = consolePos.getZ() >> 4;
        for (int dx = -RADIUS_CHUNKS; dx <= RADIUS_CHUNKS; dx++) {
            for (int dz = -RADIUS_CHUNKS; dz <= RADIUS_CHUNKS; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(ccx + dx, ccz + dz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof InternalDoorBlockEntity door && !(door instanceof RootShellDoorBlockEntity)) {
                        DOORS.put(key, be.getBlockPos());
                        return door.isOpen();
                    }
                }
            }
        }
        return null;
    }

    private ClientDoorTracker() {}
}
