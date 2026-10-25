package leonus.tardis_rooms.room;

import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Блок аддона, который нельзя сломать даже в творческом режиме: прочность −1 защищает только в выживании.
 * Без блока управления помещением не управлять, без узла — не расти, а сломанная панель открыла бы проход в камень.
 * Убираются они только самим аддоном (пристройка, замена, удаление).
 */
public class ProtectedBlock extends Block {
    public ProtectedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide()) player.displayClientMessage(TRoomsText.MSG_PROTECTED.text(), true);
        return false;
    }
}
