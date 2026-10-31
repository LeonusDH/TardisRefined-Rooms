package leonus.tardis_rooms.client;

import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.ManagePayloads;
import leonus.tardis_rooms.room.Node;
import leonus.tardis_rooms.room.PieceCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import leonus.tardis_rooms.platform.Platform;
import org.jetbrains.annotations.NotNull;
import whocraft.tardis_refined.client.screen.components.GenericMonitorSelectionList;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;
import whocraft.tardis_refined.registry.TRSoundRegistry;

/** Экран блока управления: что это за помещение, заменить или удалить. */
public class RoomManageScreen extends RoomMonitorScreen {
    final BlockPos core;
    private ManagePayloads.Info info;
    /**
     * Заголовок на мониторе — название помещения. Заголовок экрана TR задаётся раз при создании,
     * а название приходит с сервера позже, поэтому заголовок экрана пустой, а название рисуем сами.
     */
    private Component heading = TRoomsText.SCREEN_LOADING.text();
    /** Тип помещения (как в каталоге) — для заголовка и экрана переименования. */
    private Component type = Component.empty();
    private static final ResourceLocation ICON_MAP = sprite("manage/map");

    public RoomManageScreen(BlockPos core) {
        super(Component.empty(), core, new Node(0, 0, net.minecraft.core.Direction.SOUTH, 97));
        this.core = core;
        Platform.sendToServer(new ManagePayloads.Request(core));
    }

    void onInfo(ManagePayloads.Info i) {
        this.info = i;
        this.nodePos = i.nodePos();
        this.node = nodeAt(i.nodePos());
        PieceCatalog.options(node, ClientCustomRooms.list()).stream().filter(o -> o.id().equals(i.pieceId())).findFirst().ifPresentOrElse(o -> {
            type = o.label();
            if (!o.preview().equals(previewNow)) changePreview(o.preview());
        }, () -> type = TRoomsText.ROOM_UNKNOWN.text(i.pieceId().toString())); // помещение из убранного датапака
        // Своё название — заголовком, тип — в скобках после него.
        heading = i.customName().isEmpty() ? type
                : Component.literal(i.customName()).append(Component.literal(" (").append(type).append(")").withStyle(ChatFormatting.GRAY));
        rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();
        this.setEvents(null, this::onClose);
        int vPos = (height - monitorHeight) / 2;
        addCancelButton(width / 2 + 5, height - vPos - 25);
        if (info != null) {
            sideButton(leftColumn(), columnTop(), ICON_MAP, TRoomsText.MAP.text(), TRoomsText.MAP_MANAGE_TOOLTIP.text(), () -> {
                click();
                switchScreenToRight(new RoomMapScreen(info.nodePos(), node, core, null, this));
            });
        }
    }

    /** Название помещения — на месте заголовка монитора (вместе с монитором трясётся и уезжает при смене экрана). */
    @Override
    public void doRender(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.doRender(g, mouseX, mouseY, partialTick);
        whocraft.tardis_refined.client.screen.ScreenHelper.renderWidthScaledText(heading.copy().withStyle(ChatFormatting.YELLOW), g, font,
                width / 2f, 5 + (height - monitorHeight) / 2f, 0xFFFFFF, 300, true);
    }

    /** «Управление помещением» — над рамкой монитора, по центру. */
    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int frameTop = (height - monitorHeight) / 2 - 13;
        g.drawCenteredString(font, TRoomsText.SCREEN_MANAGE_TITLE.text(), width / 2, frameTop - 12, 0xFFE0E0E0);
    }

    @Override
    public ObjectSelectionList<SelectionListEntry> createSelectionList() {
        GenericMonitorSelectionList<SelectionListEntry> list = newList();
        if (info == null) return list;
        SelectionListEntry rename = new SelectionListEntry(TRoomsText.SCREEN_RENAME.text(), e -> {
            click();
            switchScreenToRight(new RoomRenameScreen(this));
        }, listLeft());
        tip(rename, TRoomsText.SCREEN_RENAME_TOOLTIP.text().withStyle(ChatFormatting.BLUE));
        list.children().add(rename);
        SelectionListEntry replace = new SelectionListEntry(TRoomsText.SCREEN_REPLACE.text(), e -> {
            click();
            switchScreenToRight(new RoomCategoryScreen(info.nodePos(), core, this));
        }, listLeft());
        tip(replace, TRoomsText.SCREEN_REPLACE_TOOLTIP.text().withStyle(ChatFormatting.BLUE));
        list.children().add(replace);
        SelectionListEntry remove = new SelectionListEntry(TRoomsText.SCREEN_REMOVE.text(), e -> {
            click();
            switchScreenToRight(new RoomConfirmScreen(this));
        }, listLeft(), info.canRemove());
        tip(remove, info.canRemove()
                ? TRoomsText.SCREEN_REMOVE_TOOLTIP.text().withStyle(ChatFormatting.BLUE)
                : Component.translatable(info.problem()).withStyle(ChatFormatting.RED));
        list.children().add(remove);
        return list;
    }

    static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(TRSoundRegistry.STATIC.get(), (float) Math.random()));
    }

    /** Переименование: поле ввода вместо списка; Enter или кнопка — сохранить, пустое — убрать название. */
    static class RoomRenameScreen extends RoomMonitorScreen {
        final RoomManageScreen parent;
        private net.minecraft.client.gui.components.EditBox box;
        private String value;

        RoomRenameScreen(RoomManageScreen parent) {
            super(TRoomsText.SCREEN_RENAME_TITLE.text(), parent.nodePos, parent.node);
            this.parent = parent;
            this.previewNow = parent.previewNow;
            this.value = parent.info != null ? parent.info.customName() : "";
        }

        @Override
        protected void init() {
            super.init();
            this.setEvents(this::submit, () -> switchScreenToLeft(parent));
            int vPos = (height - monitorHeight) / 2;
            addSubmitButton(width / 2 + 25, height - vPos - 25);
            addCancelButton(width / 2 + 5, height - vPos - 25);
            box = new net.minecraft.client.gui.components.EditBox(font, listLeft(), vPos + 40, LIST_WIDTH, 16, TRoomsText.SCREEN_RENAME_TITLE.text());
            box.setMaxLength(leonus.tardis_rooms.room.ModuleRegistry.MAX_NAME);
            box.setValue(value);
            box.setHint(parent.type.copy().withStyle(ChatFormatting.DARK_GRAY));
            box.setResponder(v -> value = v);
            addRenderableWidget(box);
            setInitialFocus(box);
        }

        private void submit() {
            click();
            Platform.sendToServer(new ManagePayloads.Rename(parent.core, value.trim()));
            switchScreenToLeft(parent);
        }

        @Override
        public boolean keyPressed(int key, int scan, int mods) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                submit();
                return true;
            }
            // Пока поле в фокусе, буквы (в том числе клавиша инвентаря) — в поле.
            if (box != null && box.isFocused() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return box.keyPressed(key, scan, mods) || box.canConsumeInput();
            return super.keyPressed(key, scan, mods);
        }

        @Override
        public ObjectSelectionList<SelectionListEntry> createSelectionList() {
            GenericMonitorSelectionList<SelectionListEntry> list = newList();
            SelectionListEntry label = new SelectionListEntry(TRoomsText.SCREEN_RENAME_LABEL.text(), e -> {}, listLeft(), false);
            tip(label, TRoomsText.SCREEN_RENAME_HINT.text().withStyle(ChatFormatting.BLUE));
            list.children().add(label);
            return list;
        }
    }

    /** Подтверждение удаления. */
    static class RoomConfirmScreen extends RoomMonitorScreen {
        private final RoomManageScreen parent;

        RoomConfirmScreen(RoomManageScreen parent) {
            super(TRoomsText.SCREEN_REMOVE_CONFIRM_TITLE.text(), parent.nodePos, parent.node);
            this.parent = parent;
            this.previewNow = parent.previewNow;
        }

        @Override
        protected void init() {
            super.init();
            this.setEvents(null, () -> switchScreenToLeft(parent));
            int vPos = (height - monitorHeight) / 2;
            addCancelButton(width / 2 + 5, height - vPos - 25);
        }

        @Override
        public ObjectSelectionList<SelectionListEntry> createSelectionList() {
            GenericMonitorSelectionList<SelectionListEntry> list = newList();
            list.children().add(new SelectionListEntry(TRoomsText.SCREEN_REMOVE_WARNING.text().withStyle(ChatFormatting.GOLD), e -> {}, listLeft(), false));
            SelectionListEntry yes = new SelectionListEntry(TRoomsText.SCREEN_REMOVE_YES.text().withStyle(ChatFormatting.RED), e -> {
                click();
                Platform.sendToServer(new ManagePayloads.Remove(parent.core));
                Minecraft.getInstance().setScreen(null);
            }, listLeft());
            tip(yes, TRoomsText.SCREEN_REMOVE_WARNING_TOOLTIP.text().withStyle(ChatFormatting.RED));
            list.children().add(yes);
            list.children().add(new SelectionListEntry(TRoomsText.SCREEN_REMOVE_NO.text(), e -> {
                click();
                switchScreenToLeft(parent);
            }, listLeft()));
            return list;
        }
    }
}
