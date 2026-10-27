package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.InterfacePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
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
    private static final java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> WRENCHES = java.util.List.of(
            net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench")),
            net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrenches")),
            net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "wrenches")));

    public static boolean isWrench(ItemStack s) {
        for (var t : WRENCHES) if (s.is(t)) return true;
        return false;
    }

    /** Ключ по интерфейсу: Shift + ПКМ — снять в инвентарь, ПКМ — переключить режим грани, по которой кликнули. */
    private static boolean wrench(PlayerInteractEvent.RightClickBlock event, InterfaceBlockEntity be) {
        Player player = event.getEntity();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) return true;
        if (!player.mayBuild() || !event.getLevel().mayInteract(player, event.getPos())) return true;
        if (player.isShiftKeyDown()) {
            be.dismantle(player);
        } else if (event.getFace() != null) {
            net.minecraft.core.Direction d = event.getFace();
            FaceMode m = be.face(d).next();
            be.setFace(d, m);
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_FACE.text(faceName(d).text(), faceModeName(m).text().withStyle(ChatFormatting.AQUA)), true);
        }
        return true;
    }

    public static TRoomsText faceName(net.minecraft.core.Direction d) {
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

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (isWrench(event.getItemStack()) && event.getLevel().getBlockEntity(event.getPos()) instanceof InterfaceBlockEntity wbe) {
            wrench(event, wbe);
            return;
        }
        if (!configMode(event.getItemStack())) return;
        boolean iface = event.getLevel().getBlockEntity(event.getPos()) instanceof InterfaceBlockEntity;
        if (!iface) {
            // Свои действия отвёртки TR в режиме настройки не выполняются; сам блок (сундук, дверь) работает как обычно.
            event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
            return;
        }
        if (!player.isShiftKeyDown()) return; // ПКМ — меню интерфейса
        InterfaceBlockEntity be = (InterfaceBlockEntity) event.getLevel().getBlockEntity(event.getPos());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) return;
        if (be.id() == 0) {
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_NO_ID.text(), true);
            return;
        }
        event.getItemStack().set(TRoomsBlocks.INTERFACE_LINK.get(), be.link());
        player.displayClientMessage(TRoomsText.MSG_INTERFACE_COPIED.text(hex(be.id())), true);
    }

    /** Shift+ЛКМ: ломать не даём, а на сервер отправляем «вписать идентификатор» (отмена на клиенте не дошла бы до сервера). */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || !configMode(player.getMainHandItem())) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof InterfaceBlockEntity)) return;
        event.setCanceled(true);
        if (event.getLevel().isClientSide() && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            PacketDistributor.sendToServer(new InterfacePayload(event.getPos(), InterfacePayload.PASTE, 0));
        }
    }

    /** ПКМ в пустоту в режиме настройки: действие отвёртки TR не выполняется; с Shift — забыть идентификатор. */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!configMode(stack)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide() || !player.isShiftKeyDown()) return;
        if (stack.remove(TRoomsBlocks.INTERFACE_LINK.get()) != null) {
            player.displayClientMessage(TRoomsText.MSG_INTERFACE_FORGOTTEN.text(), true);
        }
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
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!isScrewdriver(stack)) return;
        event.getToolTip().add(TRoomsText.SCREWDRIVER_MODE.text(modeName(mode(stack)).text().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        InterfaceLink link = stack.get(TRoomsBlocks.INTERFACE_LINK.get());
        if (link != null && link.id() != 0) {
            event.getToolTip().add(TRoomsText.IFACE_SCREWDRIVER_ID.text(hex(link.id())).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component hex(int id) {
        return Component.literal(InterfaceData.hex(id)).withStyle(ChatFormatting.AQUA);
    }

    private InterfaceTools() {}
}
