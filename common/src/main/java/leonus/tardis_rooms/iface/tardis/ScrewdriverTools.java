package leonus.tardis_rooms.iface.tardis;

import leonus.tardis_rooms.Screwdriver;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceLink;
import leonus.tardis_rooms.iface.ItemData;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.iface.InterfaceTools.UseResult;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.InterfacePayload;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.room.ProtectedBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Отвёртка (Tardis Refined) — инструмент настройки интерфейсов. У отвёртки два режима (Shift + колёсико мыши):
 * «Обычный» — всё как у отвёртки TR, аддон ничего не перехватывает;
 * «Настройка» — свои действия отвёртки TR отключены, а вместо них: Shift+ПКМ по интерфейсу — запомнить его ID,
 * Shift+ЛКМ по интерфейсу — вписать запомненный, Shift+ПКМ в пустоту — забыть.
 * Меню интерфейса (ПКМ) открывается в любом режиме.
 */
public final class ScrewdriverTools {
    public static final int MODE_NORMAL = 0, MODE_CONFIG = 1, MODES = 2;

    public static int mode(ItemStack s) {
        return ItemData.screwdriverMode(s);
    }

    private static boolean configMode(ItemStack s) {
        return Screwdriver.is(s) && mode(s) == MODE_CONFIG;
    }

    /** Следующий режим отвёртки в руке (сервер; {@code step} — ±1 от колёсика). */
    public static void cycleMode(Player player, int step) {
        ItemStack stack = player.getMainHandItem();
        if (!Screwdriver.is(stack)) return;
        int m = Math.floorMod(mode(stack) + (step >= 0 ? 1 : -1), MODES);
        ItemData.setScrewdriverMode(stack, m);
        player.displayClientMessage(TRoomsText.MSG_SCREWDRIVER_MODE.text(modeName(m).text().withStyle(ChatFormatting.AQUA)), true);
    }

    public static TRoomsText modeName(int m) {
        return m == MODE_CONFIG ? TRoomsText.SCREWDRIVER_MODE_CONFIG : TRoomsText.SCREWDRIVER_MODE_NORMAL;
    }

    /** ПКМ по блоку предметом {@code stack} (после ключей — {@link leonus.tardis_rooms.iface.InterfaceTools#onUseBlock}). */
    public static UseResult onUseBlock(Player player, Level level, BlockHitResult hit, ItemStack stack) {
        BlockPos pos = hit.getBlockPos();
        if (!configMode(stack)) return UseResult.PASS;
        // Наши блоки (узел, блок управления, заглушка) отвечают на отвёртку в любом режиме.
        if (level.getBlockState(pos).getBlock() instanceof ProtectedBlock) return UseResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof InterfaceBlockEntity be)) {
            // Свои действия отвёртки TR в режиме настройки не выполняются, а сам блок (сундук, дверь) работает как обычно.
            // Блок вызываем сами и без предмета: некоторые блоки TR (астральный манипулятор) сами реагируют на отвёртку в руке.
            // С Shift игра и так не трогает блок, если в руке предмет.
            if (player.isSecondaryUseActive()) return UseResult.handled(InteractionResult.PASS);
            // На 1.20.1 у блоков нет действия «без предмета»: зовём его как будто второй рукой, если она пуста.
            if (!player.getOffhandItem().isEmpty()) return UseResult.handled(InteractionResult.PASS);
            @SuppressWarnings("deprecation")
            InteractionResult r = level.getBlockState(pos).use(level, player, InteractionHand.OFF_HAND, hit);
            return UseResult.handled(r);
        }
        if (!player.isShiftKeyDown()) return UseResult.PASS; // ПКМ — меню интерфейса
        if (level.isClientSide()) return UseResult.CONSUMED;
        if (be.id() == 0) {
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_NO_ID.text(), true);
            return UseResult.CONSUMED;
        }
        ItemData.setLink(stack, be.link());
        player.displayClientMessage(TRoomsText.MSG_INTERFACE_COPIED.text(InterfaceTools.idText(be.id())), true);
        return UseResult.CONSUMED;
    }

    /**
     * ЛКМ по блоку. {@code true} — ломать не даём. Shift+ЛКМ по интерфейсу в режиме настройки — «вписать идентификатор»:
     * это решает сервер, поэтому клиент отправляет запрос ({@link #requestPaste}).
     */
    public static boolean onAttackBlock(Player player, Level level, BlockPos pos) {
        if (!player.isShiftKeyDown() || !configMode(player.getMainHandItem())) return false;
        return level.getBlockEntity(pos) instanceof InterfaceBlockEntity;
    }

    /** Клиент: попросить сервер вписать идентификатор из отвёртки (отмена на клиенте не дошла бы до сервера). */
    public static void requestPaste(BlockPos pos) {
        Platform.sendToServer(new InterfacePayload(pos, InterfacePayload.PASTE, 0));
    }

    /**
     * ПКМ в пустоту. {@code true} — действие предмета отменяется (клик считается обработанным).
     * В режиме настройки действие отвёртки TR не выполняется; с Shift — забыть идентификатор.
     */
    public static boolean onUseItem(Player player, Level level, ItemStack stack) {
        if (!configMode(stack)) return false;
        if (level.isClientSide() || !player.isShiftKeyDown()) return true;
        if (ItemData.removeLink(stack)) {
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_FORGOTTEN.text(), true);
        }
        return true;
    }

    /** Вписать в интерфейс идентификатор из отвёртки в руке (сервер). */
    public static void paste(Player player, InterfaceBlockEntity be) {
        ItemStack stack = player.getMainHandItem();
        if (!Screwdriver.is(stack)) stack = player.getOffhandItem();
        InterfaceLink link = ItemData.link(stack);
        if (link == null || link.id() == 0) {
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_SCREWDRIVER_EMPTY.text(), true);
            return;
        }
        be.pasteId(link.id(), link.tardis());
        player.displayClientMessage(TRoomsText.MSG_INTERFACE_PASTED.text(InterfaceTools.idText(link.id())), true);
    }

    /** Подсказка отвёртки: режим и запомненный идентификатор. */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        if (!Screwdriver.is(stack)) return;
        tooltip.add(TRoomsText.SCREWDRIVER_MODE.text(modeName(mode(stack)).text().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        InterfaceLink link = ItemData.link(stack);
        if (link != null && link.id() != 0) {
            tooltip.add(TRoomsText.IFACE_SCREWDRIVER_ID.text(InterfaceTools.idText(link.id())).withStyle(ChatFormatting.GRAY));
        }
    }

    private ScrewdriverTools() {}
}
