package leonus.tardis_rooms.client;

import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.GrowRoomPayload;
import leonus.tardis_rooms.room.Node;
import leonus.tardis_rooms.room.PieceCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import leonus.tardis_rooms.platform.Platform;
import whocraft.tardis_refined.client.screen.components.GenericListSelection;
import whocraft.tardis_refined.client.screen.components.GenericMonitorSelectionList;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;
import whocraft.tardis_refined.registry.TRSoundRegistry;

import java.util.List;

/** Второй экран: варианты категории; что не влезает — серым с причиной, но выбрать его можно ради превью (строить — нет). */
public class RoomListScreen extends RoomMonitorScreen {
    private final List<PieceCatalog.Option> options;
    private PieceCatalog.Option current;
    /** Игрок сам выбрал вариант — не перескакивать на доступный при обновлении списка. */
    private boolean picked;
    private net.minecraft.client.gui.components.AbstractWidget submitButton;

    private final BlockPos replaceCore;
    private final RoomCategoryScreen parent;

    public RoomListScreen(BlockPos nodePos, Node node, PieceCatalog.Group group, List<PieceCatalog.Option> options, BlockPos replaceCore, RoomCategoryScreen parent) {
        super(TRoomsText.group(group.name().toLowerCase()).text(), nodePos, node);
        this.replaceCore = replaceCore;
        this.parent = parent;
        this.options = options;
        this.current = options.stream().filter(o -> ClientFits.available(o.id())).findFirst().orElse(options.get(0));
        this.previewNow = current.preview();
    }

    @Override
    protected void init() {
        super.init();
        this.setEvents(this::submit, () -> switchScreenToLeft(parent));
        int vPos = (height - monitorHeight) / 2;
        addSubmitButton(width / 2 + 25, height - vPos - 25);
        // Метод TR кнопку не возвращает: она — последний добавленный элемент.
        if (children().get(children().size() - 1) instanceof net.minecraft.client.gui.components.AbstractWidget w) submitButton = w;
        updateSubmit();
        addCancelButton(width / 2 + 5, height - vPos - 25);
    }

    private void updateSubmit() {
        if (submitButton != null) submitButton.active = ClientFits.available(current.id());
    }

    private void submit() {
        if (!ClientFits.available(current.id())) return;
        Platform.sendToServer(new GrowRoomPayload(nodePos, current.id(), replaceCore));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public ObjectSelectionList<SelectionListEntry> createSelectionList() {
        GenericMonitorSelectionList<SelectionListEntry> list = newList();
        if (!picked && !ClientFits.available(current.id())) {
            current = options.stream().filter(o -> ClientFits.available(o.id())).findFirst().orElse(current);
            previewNow = current.preview();
        }
        updateSubmit();
        SelectionListEntry map = new SelectionListEntry(TRoomsText.MAP_SHOW.text().withStyle(ChatFormatting.YELLOW), e -> {
            RoomManageScreen.click();
            switchScreenToRight(new RoomMapScreen(nodePos, node, replaceCore, current, this));
        }, listLeft());
        tip(map, TRoomsText.MAP_SHOW_TOOLTIP.text().withStyle(ChatFormatting.BLUE));
        list.children().add(map);
        for (PieceCatalog.Option option : options) {
            boolean ok = ClientFits.available(option.id());
            GenericListSelection select = selected -> {
                current = option;
                picked = true;
                changePreview(option.preview());
                for (SelectionListEntry child : list.children()) if (child != map) child.setChecked(false);
                selected.setChecked(true);
                updateSubmit();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(TRSoundRegistry.STATIC.get(), (float) Math.random()));
            };
            // Недоступный пункт TR рисует серым и не нажимает — нажатие (выбор ради превью) делаем сами.
            SelectionListEntry entry = new SelectionListEntry(option.label(), select, listLeft(), ok) {
                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    if (!ok) select.onClick(this);
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            };
            Component tip = ok ? option.tooltip().copy().withStyle(ChatFormatting.BLUE)
                    : ClientFits.reason(option.id(), node).text().withStyle(ChatFormatting.RED);
            tip(entry, tip);
            if (option == current) entry.setChecked(true);
            list.children().add(entry);
        }
        return list;
    }
}
