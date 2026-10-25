package leonus.tardis_rooms.mixin;

import leonus.tardis_rooms.upper.UpperHalfBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import whocraft.tardis_refined.common.blockentity.door.GlobalDoorBlockEntity;
import whocraft.tardis_refined.common.blockentity.door.InternalDoorBlockEntity;

/** Внутренняя дверь TARDIS следит, чтобы над ней стояла «верхушка». */
@Mixin(value = GlobalDoorBlockEntity.class, remap = false)
public abstract class GlobalDoorBlockEntityMixin {
    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lwhocraft/tardis_refined/common/blockentity/door/InternalDoorBlockEntity;)V", at = @At("HEAD"))
    private void tardisRooms$ensureUpperHalf(Level level, BlockPos pos, BlockState state, InternalDoorBlockEntity be, CallbackInfo ci) {
        UpperHalfBlock.ensure(level, pos, state);
    }
}
