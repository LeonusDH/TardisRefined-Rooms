package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Предмет интерфейса: показывает сохранённый идентификатор (настройки переносятся вместе с блоком). */
public class InterfaceItem extends BlockItem {
    public InterfaceItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Поставить можно только в ТАРДИС или вплотную к оболочке — иначе объясняем почему. */
    @Override
    public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context) {
        if (!InterfaceBlock.canPlaceAt(context.getLevel(), context.getClickedPos())) {
            if (context.getPlayer() != null && context.getLevel().isClientSide()) {
                context.getPlayer().displayClientMessage(TRoomsText.MSG_INTERFACE_PLACE.text(), true);
            }
            return net.minecraft.world.InteractionResult.FAIL;
        }
        return super.place(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        InterfaceData d = stack.get(TRoomsBlocks.INTERFACE_DATA.get());
        if (d != null && d.id() != 0) {
            tooltip.add(TRoomsText.IFACE_ID.text(Component.literal(InterfaceData.hex(d.id())).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
            tooltip.add(TRoomsText.IFACE_ITEM_CLEAR_HINT.text().withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
