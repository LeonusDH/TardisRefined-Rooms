package leonus.tardis_rooms.client;

import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.FitRequestPayload;
import leonus.tardis_rooms.room.PieceCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import leonus.tardis_rooms.platform.Platform;
import whocraft.tardis_refined.client.screen.components.GenericMonitorSelectionList;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;
import whocraft.tardis_refined.registry.TRSoundRegistry;

import java.util.List;

/** Первый экран: коридоры, комнаты, блоки TR, особое. */
public class RoomCategoryScreen extends RoomMonitorScreen {
    private final List<PieceCatalog.Option> options;

    private final BlockPos replaceCore;
    /** Куда ведёт «назад»: экран блока управления при замене, иначе null (закрыть меню). */
    private final whocraft.tardis_refined.client.screen.main.MonitorOS parent;

    public RoomCategoryScreen(BlockPos nodePos, BlockPos replaceCore) {
        this(nodePos, replaceCore, null);
    }

    /** {@code replaceCore} ≠ ZERO — выбор замены для помещения с этим блоком управления. */
    public RoomCategoryScreen(BlockPos nodePos, BlockPos replaceCore, whocraft.tardis_refined.client.screen.main.MonitorOS parent) {
        super((replaceCore.equals(BlockPos.ZERO) ? TRoomsText.SCREEN_TITLE : TRoomsText.SCREEN_REPLACE_TITLE).text(), nodePos, nodeAt(nodePos));
        this.replaceCore = replaceCore;
        this.parent = parent;
        this.options = PieceCatalog.options(node);
        this.previewNow = options.get(0).preview();
        ClientFits.reset();
        Platform.sendToServer(new FitRequestPayload(nodePos, replaceCore));
    }

    @Override
    protected void init() {
        super.init();
        this.setEvents(null, () -> {
            if (parent != null) switchScreenToLeft(parent);
            else onClose();
        });
        int vPos = (height - monitorHeight) / 2;
        addCancelButton(width / 2 + 5, height - vPos - 25);
        sideButton(leftColumn(), columnTop(), sprite("manage/map"), TRoomsText.MAP.text(), TRoomsText.MAP_TOOLTIP.text(), () -> {
            RoomManageScreen.click();
            switchScreenToRight(new RoomMapScreen(nodePos, node, replaceCore, null, this));
        });
    }

    @Override
    public ObjectSelectionList<SelectionListEntry> createSelectionList() {
        GenericMonitorSelectionList<SelectionListEntry> list = newList();
        boolean known = ClientFits.known(nodePos);
        for (PieceCatalog.Group group : PieceCatalog.Group.values()) {
            List<PieceCatalog.Option> inGroup = options.stream().filter(o -> o.group() == group).toList();
            if (inGroup.isEmpty()) continue;
            long ok = inGroup.stream().filter(o -> ClientFits.available(o.id())).count();
            String count = known ? " (" + ok + "/" + inGroup.size() + ")" : " (" + inGroup.size() + ")";
            Component label = TRoomsText.group(group.name().toLowerCase()).text()
                    .append(Component.literal(count).withStyle(ChatFormatting.DARK_GRAY));
            SelectionListEntry entry = new SelectionListEntry(label, e -> {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(TRSoundRegistry.STATIC.get(), (float) Math.random()));
                switchScreenToRight(new RoomListScreen(nodePos, node, group, inGroup, replaceCore, this));
            }, listLeft(), !known || ok > 0);
            tip(entry, TRoomsText.groupTooltip(group.name().toLowerCase()).text().withStyle(ChatFormatting.BLUE));
            list.children().add(entry);
        }
        return list;
    }
}
