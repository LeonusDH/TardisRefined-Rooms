package leonus.tardis_rooms.room;

import leonus.tardis_rooms.Screwdriver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Защищённый блок, которым управляют отвёрткой (узел роста, блок управления): отвёрткой — свой экран
 * ({@link #openScreen}), без неё — подсказка, что нужна отвёртка ({@link #breakMessage}).
 */
public abstract class ScrewdriverBlock extends ProtectedBlock {
    protected ScrewdriverBlock(Properties properties) {
        super(properties);
    }

    /** Открыть экран блока (клиент). */
    protected abstract void openScreen(BlockPos pos);

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!Screwdriver.is(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) openScreen(pos);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) player.displayClientMessage(breakMessage().text(), true);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
