package leonus.tardis_rooms.room;

import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import whocraft.tardis_refined.common.items.ScrewdriverItem;

public class RoomNodeBlock extends ProtectedBlock {
    public RoomNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public TRoomsText breakMessage() {
        return TRoomsText.MSG_USE_SCREWDRIVER;
    }

    /** Отвёрткой — каталог помещений; без неё — подсказка (только основной рукой, чтобы не дублировать). */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof ScrewdriverItem) {
            if (level.isClientSide()) leonus.tardis_rooms.client.ClientAccess.openRoomSelector(pos);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            player.displayClientMessage(TRoomsText.MSG_USE_SCREWDRIVER.text(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
