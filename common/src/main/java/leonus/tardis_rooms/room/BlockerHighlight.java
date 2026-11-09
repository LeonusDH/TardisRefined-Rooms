package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.HighlightPayload;
import leonus.tardis_rooms.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Показывает игроку, что мешает удалить помещение: сообщение с ближайшим мешающим предметом и его координатами
 * (нажатие на него обводит снова), а клиент обводит мешающие предметы мигающей рамкой поверх стен на
 * {@value #SECONDS} секунд (как подсветка беспроводного соединителя в ExtendedAE; рисует
 * {@link leonus.tardis_rooms.client.BlockHighlights}).
 *
 * <p>Предмет — один блок или блок из двух половин (двойной сундук, кровать): такой обводится одной рамкой и
 * считается один раз. Рамка — по форме блока, а не по полному кубу.
 */
public final class BlockerHighlight {
    /** Сколько секунд видны рамки. */
    public static final int SECONDS = 15;
    /** Больше рамок не рисуем: найти первые и так хватит, а пакет и экран не резиновые. */
    public static final int MAX_BLOCKS = 32;

    /** Мешающий предмет: блок, по которому он назван, и рамка вокруг него (с половинами). */
    public record Target(BlockPos pos, AABB box) {}

    /**
     * Сообщить игроку, что мешает удалить помещение с блоком управления {@code core}, и обвести ближайшие к нему
     * мешающие предметы. {@code inChat} — сообщение в чат (нажатие обводит снова); иначе — над хотбаром,
     * чтобы повторный показ не засорял чат.
     */
    public static void show(ServerPlayer player, BlockPos core, List<BlockPos> blockers, boolean inChat) {
        if (blockers.isEmpty()) return;
        ServerLevel level = player.serverLevel();
        List<Target> targets = group(level, blockers);
        List<Target> near = nearest(player.getEyePosition(), targets);
        BlockPos first = near.get(0).pos();
        Component name = level.getBlockState(first).getBlock().getName().withStyle(ChatFormatting.WHITE);
        int more = targets.size() - 1;
        MutableComponent msg = (more == 0
                ? TRoomsText.MSG_BLOCKED_BY.text(name, first.getX(), first.getY(), first.getZ())
                : TRoomsText.MSG_BLOCKED_BY_MORE.text(name, first.getX(), first.getY(), first.getZ(), more))
                .withStyle(ChatFormatting.RED);
        if (inChat) {
            String cmd = "/" + TardisRooms.MODID + " show_blockers " + core.getX() + " " + core.getY() + " " + core.getZ();
            msg.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TRoomsText.MSG_BLOCKED_BY_AGAIN.text())));
        }
        // В чат: координаты нужны, пока ищешь блок.
        player.displayClientMessage(msg, !inChat);
        Platform.sendToPlayer(player, new HighlightPayload(near.stream().map(Target::box).toList(), SECONDS));
    }

    /** Мешающие блоки → предметы: половины двойного сундука или кровати — один предмет с общей рамкой. */
    public static List<Target> group(ServerLevel level, List<BlockPos> blockers) {
        Set<BlockPos> done = new HashSet<>();
        List<Target> out = new ArrayList<>();
        for (BlockPos p : blockers) {
            if (!done.add(p)) continue;
            BlockState s = level.getBlockState(p);
            AABB box = box(level, p);
            BlockPos other = otherHalf(s, p);
            if (other != null && level.getBlockState(other).is(s.getBlock())) {
                done.add(other);
                box = box.minmax(box(level, other));
            }
            out.add(new Target(p, box));
        }
        return out;
    }

    /** Вторая половина блока из двух (двойной сундук, кровать), или null. */
    private static @Nullable BlockPos otherHalf(BlockState s, BlockPos p) {
        if (s.getBlock() instanceof ChestBlock && s.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return p.relative(ChestBlock.getConnectedDirection(s));
        }
        if (s.getBlock() instanceof BedBlock) return p.relative(BedBlock.getConnectedDirection(s));
        return null;
    }

    /** Рамка блока — по его форме (сундук чуть меньше полного куба); без формы — полный куб. */
    private static AABB box(ServerLevel level, BlockPos p) {
        VoxelShape shape = level.getBlockState(p).getShape(level, p);
        return shape.isEmpty() ? new AABB(p) : shape.bounds().move(p);
    }

    /** Предметы от ближайшего к {@code eye}, не больше {@value #MAX_BLOCKS}. */
    public static List<Target> nearest(Vec3 eye, List<Target> targets) {
        List<Target> sorted = new ArrayList<>(targets);
        sorted.sort(Comparator.comparingDouble(t -> t.box().getCenter().distanceToSqr(eye)));
        return List.copyOf(sorted.subList(0, Math.min(MAX_BLOCKS, sorted.size())));
    }

    private BlockerHighlight() {}
}
