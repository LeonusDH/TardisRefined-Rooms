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

    /**
     * Игрок ломает блок: не даём. Без {@code @Override}: на NeoForge метод с этим именем есть у Block (и вызывается им
     * на клиенте и на сервере), на других платформах запрет делает событие поломки блока.
     */
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide()) player.displayClientMessage(breakMessage().text(), true);
        return false;
    }

    /**
     * Игрок начал бить блок (вызывает платформа на клиенте и на сервере). {@code true} — не даём даже начать: в творческом
     * режиме такой блок иначе ломается с одного удара (и тут же возвращается). В выживании прочность −1 и так не даёт его сломать.
     */
    public static boolean onAttack(Player player, Level level, BlockPos pos) {
        if (!player.isCreative() || !(level.getBlockState(pos).getBlock() instanceof ProtectedBlock pb)) return false;
        if (level.isClientSide()) player.displayClientMessage(pb.breakMessage().text(), true);
        return true;
    }

    /** Что сказать игроку, который пытается сломать блок. */
    public TRoomsText breakMessage() {
        return TRoomsText.MSG_PROTECTED;
    }
}
