package leonus.tardis_rooms.mixin;

import leonus.tardis_rooms.TardisRoomsConfig;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import whocraft.tardis_refined.common.world.chunk.TardisChunkGenerator;

/**
 * В {@code applyBiomeDecoration} Tardis Refined сначала ставит статичные постройки
 * (Око Гармонии и хаб коридоров), а затем заполняет остальное пространство случайными
 * ARS-комнатами. Обрываем метод ровно перед второй частью: пространство вокруг
 * остаётся сплошным камнем, в котором позже будут вырезаться пристроенные комнаты.
 * Кроме того, над каждой частью хаба TR ставит на верхний уровень случайный коридор —
 * он никуда не ведёт и упирается в камень; его тоже не ставим, место над хабом достаётся аддону.
 */
@Mixin(value = TardisChunkGenerator.class, remap = false)
public abstract class TardisChunkGeneratorMixin {

    @Inject(
            method = "applyBiomeDecoration",
            at = @At(
                    value = "INVOKE",
                    target = "Lwhocraft/tardis_refined/common/world/chunk/TardisChunkGenerator;get3x3ARSReferencePos(Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/world/level/ChunkPos;"
            ),
            cancellable = true
    )
    private void tardisRooms$skipProceduralRooms(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        if (TardisRoomsConfig.disableProceduralRooms()) {
            ci.cancel();
        }
    }

    /** Часть хаба уже поставлена; случайный коридор над ней — нет (метод возвращает «основной этаж поставлен», как и TR). */
    @Inject(
            method = "placeIfCorrectChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lwhocraft/tardis_refined/common/world/chunk/TardisChunkGenerator;getRandomCorridorPiece(Lnet/minecraft/util/RandomSource;)Lwhocraft/tardis_refined/common/world/chunk/ARSStructurePiece;"
            ),
            cancellable = true
    )
    private void tardisRooms$skipCorridorAboveHub(CallbackInfoReturnable<Boolean> cir) {
        if (TardisRoomsConfig.disableProceduralRooms()) {
            cir.setReturnValue(true);
        }
    }
}
