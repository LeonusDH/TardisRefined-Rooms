package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Предмет интерфейса: показывает сохранённый идентификатор (настройки переносятся вместе с блоком). */
public class InterfaceItem extends BlockItem {
    public InterfaceItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Поставить можно только в ТАРДИС или вплотную к оболочке — иначе объясняем почему. */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!InterfaceBlock.canPlaceAt(context.getLevel(), context.getClickedPos())) {
            if (context.getPlayer() != null && context.getLevel().isClientSide()) {
                context.getPlayer().displayClientMessage(TRoomsText.MSG_INTERFACE_PLACE.text(), true);
            } else if (context.getPlayer() != null) {
                TardisRooms.debug("{}: interface not placed at {}: not in a TARDIS and not next to its shell",
                        TardisRooms.who(context.getPlayer()), context.getClickedPos().toShortString());
            }
            return InteractionResult.FAIL;
        }
        // Клиент знает права от сервера: без права блок даже не пытается встать (как и не в ТАРДИС).
        if (context.getLevel().isClientSide() && context.getPlayer() != null
                && !InterfaceHost.mayConfigureOnClient(context.getPlayer(), context.getClickedPos())) {
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() instanceof ServerPlayer sp
                && !InterfaceHost.mayConfigure(sp, context.getClickedPos(), "placing an interface at " + context.getClickedPos().toShortString())) {
            // Клиент уже поставил блок и списал предмет у себя: блок игра вернёт сама, а предмет — только так.
            sp.containerMenu.sendAllDataToRemote();
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        InterfaceData d = ItemData.interfaceData(stack);
        if (d != null && d.id() != 0) {
            tooltip.add(TRoomsText.IFACE_ID.text(InterfaceTools.idText(d.id())).withStyle(ChatFormatting.GRAY));
            tooltip.add(TRoomsText.IFACE_ITEM_CLEAR_HINT.text().withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
