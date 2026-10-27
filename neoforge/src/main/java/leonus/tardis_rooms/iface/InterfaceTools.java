package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.InterfacePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import whocraft.tardis_refined.common.items.ScrewdriverItem;

/**
 * Отвёртка и интерфейсы. У отвёртки два режима (Shift + колёсико мыши):
 * «Обычный» — всё как у отвёртки TR, аддон ничего не перехватывает;
 * «Настройка» — свои действия отвёртки TR отключены, а вместо них: Shift+ПКМ по интерфейсу — запомнить его ID,
 * Shift+ЛКМ по интерфейсу — вписать запомненный, Shift+ПКМ в пустоту — забыть.
 * Меню интерфейса (ПКМ) открывается в любом режиме.
 */
public final class InterfaceTools {
    public static final int MODE_NORMAL = 0, MODE_CONFIG = 1, MODES = 2;

    public static boolean isScrewdriver(ItemStack s) {
        return s.getItem() instanceof ScrewdriverItem;
    }

    public static int mode(ItemStack s) {
        return s.getOrDefault(TRoomsBlocks.SCREWDRIVER_MODE.get(), MODE_NORMAL);
    }

    private static boolean configMode(ItemStack s) {
        return isScrewdriver(s) && mode(s) == MODE_CONFIG;
    }

    /** Следующий режим отвёртки в руке (сервер; {@code step} — ±1 от колёсика). */
    public static void cycleMode(Player player, int step) {
        ItemStack stack = player.getMainHandItem();
        if (!isScrewdriver(stack)) return;
        int m = Math.floorMod(mode(stack) + (step >= 0 ? 1 : -1), MODES);
        if (m == MODE_NORMAL) stack.remove(TRoomsBlocks.SCREWDRIVER_MODE.get());
        else stack.set(TRoomsBlocks.SCREWDRIVER_MODE.get(), m);
        player.displayClientMessage(TRoomsText.MSG_SCREWDRIVER_MODE.text(modeName(m).text().withStyle(ChatFormatting.AQUA)), true);
    }

    public static TRoomsText modeName(int m) {
        return m == MODE_CONFIG ? TRoomsText.SCREWDRIVER_MODE_CONFIG : TRoomsText.SCREWDRIVER_MODE_NORMAL;
    }

    /** Ключи других модов: метки common-тегов, которыми их помечают Mekanism, AE2, Thermal и другие. */
    private static final List<TagKey<Item>> WRENCHES = List.of(
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench")),
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrenches")),
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "wrenches")));

    public static boolean isWrench(ItemStack s) {
        for (var t : WRENCHES) if (s.is(t)) return true;
        return false;
    }

    /**
     * Что сделать с кликом по блоку. {@code handled = false} — не наше, всё как обычно; иначе событие отменяется
     * с результатом {@code result} (платформа переводит это в отмену события своего загрузчика).
     */
    public record UseResult(boolean handled, InteractionResult result) {
        public static final UseResult PASS = new UseResult(false, InteractionResult.PASS);
        public static final UseResult CONSUMED = new UseResult(true, InteractionResult.SUCCESS);

        static UseResult handled(InteractionResult result) { return new UseResult(true, result); }
    }

    /** Ключ по интерфейсу: Shift + ПКМ — снять в инвентарь, ПКМ — переключить режим грани, по которой кликнули. */
    private static UseResult wrench(Player player, Level level, BlockPos pos, Direction face, InterfaceBlockEntity be) {
        if (level.isClientSide()) return UseResult.CONSUMED;
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return UseResult.CONSUMED;
        if (player.isShiftKeyDown()) {
            be.dismantle(player);
        } else if (face != null) {
            FaceMode m = be.face(face).next();
            be.setFace(face, m);
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_FACE.text(faceName(face).text(), faceModeName(m).text().withStyle(ChatFormatting.AQUA)), true);
        }
        return UseResult.CONSUMED;
    }

    public static TRoomsText faceName(Direction d) {
        return switch (d) {
            case DOWN -> TRoomsText.FACE_DOWN;
            case UP -> TRoomsText.FACE_UP;
            case NORTH -> TRoomsText.FACE_NORTH;
            case SOUTH -> TRoomsText.FACE_SOUTH;
            case WEST -> TRoomsText.FACE_WEST;
            case EAST -> TRoomsText.FACE_EAST;
        };
    }

    public static TRoomsText faceModeName(FaceMode m) {
        return switch (m) {
            case DEFAULT -> TRoomsText.FACE_MODE_DEFAULT;
            case PUSH -> TRoomsText.FACE_MODE_PUSH;
            case PULL -> TRoomsText.FACE_MODE_PULL;
        };
    }

    /** ПКМ по блоку предметом {@code stack}. */
    public static UseResult onUseBlock(Player player, Level level, BlockHitResult hit, ItemStack stack) {
        BlockPos pos = hit.getBlockPos();
        if (isWrench(stack) && level.getBlockEntity(pos) instanceof InterfaceBlockEntity wbe) {
            return wrench(player, level, pos, hit.getDirection(), wbe);
        }
        if (!configMode(stack)) return UseResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof InterfaceBlockEntity be)) {
            // Свои действия отвёртки TR в режиме настройки не выполняются, а сам блок (сундук, дверь) работает как обычно.
            // Блок вызываем сами и без предмета: некоторые блоки TR (астральный манипулятор) сами реагируют на отвёртку в руке.
            // С Shift игра и так не трогает блок, если в руке предмет.
            if (player.isSecondaryUseActive()) return UseResult.handled(InteractionResult.PASS);
            return UseResult.handled(level.getBlockState(pos).useWithoutItem(level, player, hit));
        }
        if (!player.isShiftKeyDown()) return UseResult.PASS; // ПКМ — меню интерфейса
        if (level.isClientSide()) return UseResult.CONSUMED;
        if (be.id() == 0) {
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_NO_ID.text(), true);
            return UseResult.CONSUMED;
        }
        stack.set(TRoomsBlocks.INTERFACE_LINK.get(), be.link());
        player.displayClientMessage(TRoomsText.MSG_INTERFACE_COPIED.text(hex(be.id())), true);
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
        PacketDistributor.sendToServer(new InterfacePayload(pos, InterfacePayload.PASTE, 0));
    }

    /**
     * ПКМ в пустоту. {@code true} — действие предмета отменяется (клик считается обработанным).
     * В режиме настройки действие отвёртки TR не выполняется; с Shift — забыть идентификатор.
     */
    public static boolean onUseItem(Player player, Level level, ItemStack stack) {
        if (!configMode(stack)) return false;
        if (level.isClientSide() || !player.isShiftKeyDown()) return true;
        if (stack.remove(TRoomsBlocks.INTERFACE_LINK.get()) != null) {
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_FORGOTTEN.text(), true);
        }
        return true;
    }

    /** Вписать в интерфейс идентификатор из отвёртки в руке (сервер). */
    public static void paste(Player player, InterfaceBlockEntity be) {
        ItemStack stack = player.getMainHandItem();
        if (!isScrewdriver(stack)) stack = player.getOffhandItem();
        InterfaceLink link = stack.get(TRoomsBlocks.INTERFACE_LINK.get());
        if (link == null || link.id() == 0) {
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_SCREWDRIVER_EMPTY.text(), true);
            return;
        }
        be.pasteId(link.id(), link.tardis());
        player.displayClientMessage(TRoomsText.MSG_INTERFACE_PASTED.text(hex(link.id())), true);
    }

    /** Подсказка отвёртки: режим и запомненный идентификатор. */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        if (!isScrewdriver(stack)) return;
        tooltip.add(TRoomsText.SCREWDRIVER_MODE.text(modeName(mode(stack)).text().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        InterfaceLink link = stack.get(TRoomsBlocks.INTERFACE_LINK.get());
        if (link != null && link.id() != 0) {
            tooltip.add(TRoomsText.IFACE_SCREWDRIVER_ID.text(hex(link.id())).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component hex(int id) {
        return Component.literal(InterfaceData.hex(id)).withStyle(ChatFormatting.AQUA);
    }

    private InterfaceTools() {}
}
