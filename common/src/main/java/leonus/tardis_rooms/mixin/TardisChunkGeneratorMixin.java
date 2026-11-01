package leonus.tardis_rooms.mixin;

import leonus.tardis_rooms.TardisRoomsConfig;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import whocraft.tardis_refined.common.world.chunk.TardisChunkGenerator;

/**
 * В {@code applyBiomeDecoration} Tardis Refined сначала ставит статичные постройки
 * (Око Гармонии и части хаба коридоров), а затем заполняет остальное пространство случайными
 * ARS-комнатами и гравитационными шахтами. Вторую часть не выполняем: пространство вокруг
 * остаётся сплошным камнем, в котором позже будут вырезаться пристроенные комнаты.
 * Кроме того, над каждой частью хаба TR ставит на верхний уровень случайный коридор —
 * он никуда не ведёт и упирается в камень; его тоже не ставим, место над хабом достаётся аддону.
 * (На 1.20.1 всё это — в одном методе; после части хаба TR и так выходит из него.)
 */
@Mixin(TardisChunkGenerator.class)
public abstract class TardisChunkGeneratorMixin {

    /** Случайный коридор над частью хаба: сама часть уже поставлена, дальше метод только ставит коридор и выходит. */
    @Inject(method = "applyBiomeDecoration",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lwhocraft/tardis_refined/common/world/chunk/TardisChunkGenerator;getRandomCorridorPiece()Lwhocraft/tardis_refined/common/world/chunk/ARSStructurePiece;"),
            cancellable = true, require = 1)
    private void tardisRooms$skipCorridorAboveHub(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        if (TardisRoomsConfig.disableProceduralRooms()) ci.cancel();
    }

    /** Процедурные комнаты и шахты: начинаются с проверки «здесь гравитационная шахта?». */
    @Inject(method = "applyBiomeDecoration",
            at = @At(value = "INVOKE", remap = false,
                    target = "Lwhocraft/tardis_refined/common/world/chunk/TardisChunkGenerator;isChunkAtGravityInterval(Lnet/minecraft/world/level/ChunkPos;)Z"),
            cancellable = true, require = 1)
    private void tardisRooms$skipProceduralRooms(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        if (TardisRoomsConfig.disableProceduralRooms()) ci.cancel();
    }
}
