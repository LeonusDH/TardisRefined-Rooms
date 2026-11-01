package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import whocraft.tardis_refined.common.items.ScrewdriverItem;

/**
 * Интерфейс ТАРДИС: ставится в любом помещении ТАРДИС, а вне её — только вплотную к оболочке
 * (тогда подключается к сети этой ТАРДИС). Настраивается отвёрткой.
 */
public class InterfaceBlock extends BaseEntityBlock {
    public InterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Можно ли поставить здесь: в ТАРДИС — везде, снаружи — только рядом с оболочкой. */
    public static boolean canPlaceAt(Level level, BlockPos pos) {
        return leonus.tardis_rooms.TardisRooms.isTardisDimension(level) || !InterfaceBlockEntity.nearbyTardises(level, pos).isEmpty();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Без сообщения: этот метод зовут и другие моды (предпросмотр постройки, раздатчики и т. п.).
        // Сообщение — только когда игрок сам ставит интерфейс ({@link InterfaceItem#place}).
        return canPlaceAt(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    // ---------- редстоун ----------

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return level.getBlockEntity(pos) instanceof InterfaceBlockEntity be ? be.redstoneOut() : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof InterfaceBlockEntity be) be.updateRedstoneInput();
    }

    /** Убрали интерфейс — связанные пересчитывают свой сигнал без него. */
    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        java.util.List<InterfaceBlockEntity> peers = !level.isClientSide() && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof InterfaceBlockEntity be ? be.linked() : java.util.List.of();
        super.onRemove(state, level, pos, newState, moving);
        for (InterfaceBlockEntity p : peers) p.updateRedstoneOutput();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InterfaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TRoomsBlocks.INTERFACE_BE.get(), InterfaceBlockEntity::serverTick);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (InterfacePlatform.isMemoryCard(stack) && level.getBlockEntity(pos) instanceof InterfaceBlockEntity be) {
            return InterfacePlatform.useMemoryCard(stack, be, player);
        }
        if (stack.getItem() instanceof ScrewdriverItem) {
            if (level.isClientSide()) leonus.tardis_rooms.client.ClientAccess.openInterface(pos);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level.isClientSide()) player.displayClientMessage(TRoomsText.MSG_INTERFACE_USE_SCREWDRIVER.text(), true);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
