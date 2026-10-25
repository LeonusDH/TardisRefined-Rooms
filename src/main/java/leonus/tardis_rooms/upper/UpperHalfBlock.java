package leonus.tardis_rooms.upper;

import leonus.tardis_rooms.TRoomsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import whocraft.tardis_refined.common.block.door.InternalDoorBlock;
import whocraft.tardis_refined.common.block.shell.ShellBaseBlock;

/**
 * Невидимая «верхушка» над дверью TARDIS и над высокой оболочкой.
 * <p>
 * У двери и оболочек TR форма на 30–32 пикселя, но игра выбирает блок под прицелом только по клетке,
 * через которую проходит взгляд, — верхняя клетка для неё воздух. Этот блок занимает её: даёт рамку
 * выделения, не даёт поставить туда другой блок и передаёт клики нижнему блоку.
 * Столкновений у него нет — их уже даёт высокая форма нижнего блока (и открытая дверь остаётся проходимой).
 */
public class UpperHalfBlock extends Block {
    private static final int CHECK_INTERVAL = 20;

    public UpperHalfBlock(Properties properties) {
        super(properties);
    }

    /** Нужна ли верхушка над этим блоком: дверь или оболочка TR, форма выше одного блока. */
    public static boolean needsUpperHalf(BlockState host, BlockGetter level, BlockPos hostPos) {
        if (!(host.getBlock() instanceof InternalDoorBlock) && !(host.getBlock() instanceof ShellBaseBlock)) return false;
        VoxelShape shape = host.getShape(level, hostPos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) > 1.0;
    }

    /** Вызывается из тика двери/оболочки: ставит верхушку, если над ней пусто. */
    public static void ensure(Level level, BlockPos hostPos, BlockState host) {
        if (level.isClientSide()) return;
        if ((level.getGameTime() + hostPos.asLong()) % CHECK_INTERVAL != 0) return;
        BlockPos above = hostPos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.isAir() && needsUpperHalf(host, level, hostPos)) {
            level.setBlock(above, TRoomsBlocks.UPPER_HALF.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (aboveState.is(TRoomsBlocks.UPPER_HALF.get()) && !needsUpperHalf(host, level, hostPos)) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean hasHost(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        return needsUpperHalf(level.getBlockState(below), level, below);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockPos below = pos.below();
        BlockState host = level.getBlockState(below);
        if (!needsUpperHalf(host, level, below)) return Shapes.empty();
        VoxelShape upper = host.getShape(level, below, context).move(0, -1, 0);
        return Shapes.join(upper, Shapes.block(), BooleanOp.AND);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasHost(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !hasHost(level, pos)) return Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    /** Подстраховка: TR иногда убирает дверь без обновления соседей — тогда верхушка уберёт себя сама. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!hasHost(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.scheduleTick(pos, this, CHECK_INTERVAL);
        }
    }

    private static BlockHitResult toHost(BlockHitResult hit, BlockPos hostPos) {
        return new BlockHitResult(hit.getLocation(), hit.getDirection(), hostPos, hit.isInside());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState host = level.getBlockState(below);
        if (!needsUpperHalf(host, level, below)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return host.useItemOn(stack, level, player, hand, toHost(hit, below));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState host = level.getBlockState(below);
        if (!needsUpperHalf(host, level, below)) return InteractionResult.PASS;
        return host.useWithoutItem(level, player, toHost(hit, below));
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        BlockPos below = pos.below();
        BlockState host = level.getBlockState(below);
        if (needsUpperHalf(host, level, below)) host.attack(level, below, player);
    }
}
