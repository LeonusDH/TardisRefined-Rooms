package leonus.tardis_rooms.room;

import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.HighlightPayload;
import leonus.tardis_rooms.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Показывает игроку, что мешает удалить помещение: сообщение с ближайшим мешающим блоком и его координатами,
 * а клиент обводит мешающие блоки мигающей рамкой поверх стен на {@value #SECONDS} секунд (как подсветка
 * беспроводного соединителя в ExtendedAE; рисует {@link leonus.tardis_rooms.client.BlockHighlights}).
 */
public final class BlockerHighlight {
    /** Сколько секунд видны рамки. */
    public static final int SECONDS = 15;
    /** Больше рамок не рисуем: найти первые и так хватит, а пакет и экран не резиновые. */
    public static final int MAX_BLOCKS = 32;

    /** Сообщить игроку, что мешает, и обвести ближайшие к нему мешающие блоки. */
    public static void show(ServerPlayer player, List<BlockPos> blockers) {
        if (blockers.isEmpty()) return;
        List<BlockPos> near = nearest(player.getEyePosition(), blockers);
        BlockPos first = near.get(0);
        Component name = player.serverLevel().getBlockState(first).getBlock().getName().withStyle(ChatFormatting.WHITE);
        int more = blockers.size() - 1;
        Component msg = more == 0
                ? TRoomsText.MSG_BLOCKED_BY.text(name, first.getX(), first.getY(), first.getZ())
                : TRoomsText.MSG_BLOCKED_BY_MORE.text(name, first.getX(), first.getY(), first.getZ(), more);
        // В чат, а не над хотбаром: координаты нужны, пока ищешь блок.
        player.displayClientMessage(msg.copy().withStyle(ChatFormatting.RED), false);
        Platform.sendToPlayer(player, new HighlightPayload(near, SECONDS));
    }

    /** Мешающие блоки от ближайшего к {@code eye}, не больше {@value #MAX_BLOCKS}. */
    public static List<BlockPos> nearest(Vec3 eye, List<BlockPos> blockers) {
        List<BlockPos> sorted = new ArrayList<>(blockers);
        sorted.sort(Comparator.comparingDouble(p -> p.getCenter().distanceToSqr(eye)));
        return List.copyOf(sorted.subList(0, Math.min(MAX_BLOCKS, sorted.size())));
    }

    private BlockerHighlight() {}
}
