package leonus.tardis_rooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.iface.Flow;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceData;
import leonus.tardis_rooms.iface.InterfaceLink;
import leonus.tardis_rooms.iface.Status;
import leonus.tardis_rooms.iface.TransferType;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.InterfacePayload;
import leonus.tardis_rooms.room.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import leonus.tardis_rooms.platform.Platform;
import org.jetbrains.annotations.NotNull;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.client.screen.components.GenericMonitorSelectionList;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;

import java.util.List;

/**
 * Настройка интерфейса ТАРДИС. На мониторе слева — состояние и ID, справа — что передавать.
 * Справа от монитора — кнопки: новый ID, очистить, вписать из отвёртки, направление; слева — «подсказки».
 */
public class InterfaceScreen extends RoomMonitorScreen {
    private static final ResourceLocation ICON_NEW = sprite("iface/new_id");
    private static final ResourceLocation ICON_TIPS = sprite("iface/tips");
    private static final ResourceLocation ICON_IN = sprite("iface/flow_in");
    private static final ResourceLocation ICON_OUT = sprite("iface/flow_out");
    private static final ResourceLocation ICON_BOTH = sprite("iface/flow_both");
    private static final ResourceLocation ICON_SIDES = sprite("iface/sides");
    /** Значок Tardis Refined: корзина — очистить. */
    private static final ResourceLocation ICON_CLEAR = ResourceLocation.fromNamespaceAndPath(TardisRefined.MODID, "trash");
    private static final ResourceLocation ICON_PASTE = sprite("iface/paste");
    private static final ResourceLocation ICON_PASTE_OFF = sprite("iface/paste_off");

    private final BlockPos pos;
    private InterfaceData data = InterfaceData.DEFAULT;
    private Status status = Status.NO_SHELL;
    private int peers;
    private int heldId;

    public InterfaceScreen(BlockPos pos) {
        super(TRoomsText.IFACE_TITLE.text(), pos, new Node(0, 0, Direction.SOUTH, 97));
        this.pos = pos;
        pull();
    }


    /** Берёт настройки из блока на клиенте (сервер присылает их после каждого изменения); true — что-то поменялось. */
    private boolean pull() {
        InterfaceLink held = heldLink();
        int h = held == null ? 0 : held.id();
        if (!(Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof InterfaceBlockEntity be)) return false;
        boolean changed = !be.data().equals(data) || be.status() != status || be.peerCount() != peers || h != heldId;
        data = be.data();
        status = be.status();
        peers = be.peerCount();
        heldId = h;
        return changed;
    }

    @Override
    public void tick() {
        super.tick();
        if (pull()) rebuildWidgets();
        if (Minecraft.getInstance().level != null && !(Minecraft.getInstance().level.getBlockEntity(pos) instanceof InterfaceBlockEntity)) onClose();
    }

    @Override
    protected void init() {
        super.init();
        this.setEvents(null, this::onClose);
        int vPos = (height - monitorHeight) / 2;
        addCancelButton(width / 2 + 5, height - vPos - 25);

        // Кнопки справа от рамки монитора — столбиком.
        int rx = rightColumn(), y = columnTop();
        boolean hasId = data.id() != 0;
        y = sideButton(rx, y, ICON_NEW, (hasId ? TRoomsText.IFACE_ID_NEW : TRoomsText.IFACE_ID_GENERATE).text(),
                TRoomsText.IFACE_ID_GENERATE_TOOLTIP.text(), () -> send(InterfacePayload.GENERATE, 0));
        if (hasId) {
            y = sideButton(rx, y, ICON_CLEAR, TRoomsText.IFACE_ID_CLEAR.text(), TRoomsText.IFACE_ID_CLEAR_TOOLTIP.text(), () -> send(InterfacePayload.CLEAR, 0));
        }
        // «Вписать» — всегда; без ID в отвёртке (или с тем же ID) — серая.
        boolean canPaste = heldId != 0 && heldId != data.id();
        MutableComponent pasteTitle = heldId != 0 ? TRoomsText.IFACE_ID_PASTE.text(hex(heldId)) : TRoomsText.IFACE_ID_PASTE_NONE.text();
        MutableComponent pasteHint = (heldId == 0 ? TRoomsText.IFACE_ID_PASTE_EMPTY : canPaste ? TRoomsText.IFACE_ID_PASTE_TOOLTIP : TRoomsText.IFACE_ID_PASTE_SAME).text();
        y = sideButton(rx, y, canPaste ? ICON_PASTE : ICON_PASTE_OFF, pasteTitle, pasteHint, () -> send(InterfacePayload.PASTE, 0));
        lastButton.active = canPaste;
        Flow f = data.flowMode();
        ResourceLocation flowIcon = switch (f) { case IN -> ICON_IN; case OUT -> ICON_OUT; case BOTH -> ICON_BOTH; };
        sideButton(rx, y + 4, flowIcon, TRoomsText.IFACE_FLOW.text(flowName(f).text().withStyle(ChatFormatting.YELLOW)),
                TRoomsText.IFACE_FLOW_TOOLTIP.text(), () -> send(InterfacePayload.FLOW, f.next().ordinal()));

        // Подсказки — слева от рамки.
        int lx = leftColumn();
        int ly = sideButton(lx, columnTop(), ICON_TIPS, TRoomsText.IFACE_TIPS.text(), TRoomsText.IFACE_HELP.text(), () -> {}) + 6;

        // Стороны — под подсказками: отдельный экран с блоком в 3D, как в EnderIO.
        sideButton(lx, ly, ICON_SIDES, TRoomsText.IFACE_SIDES.text(), TRoomsText.IFACE_FACE_TOOLTIP.text(),
                () -> Minecraft.getInstance().setScreen(new InterfaceSidesScreen(this, pos)));
    }

    private void send(int action, int value) {
        RoomManageScreen.click();
        Platform.sendToServer(new InterfacePayload(pos, action, value));
    }

    private static Component hex(int id) {
        return Component.literal(InterfaceData.hex(id)).withStyle(ChatFormatting.AQUA);
    }

    private static TRoomsText typeName(TransferType t) {
        return switch (t) {
            case ENERGY -> TRoomsText.IFACE_ENERGY;
            case ITEMS -> TRoomsText.IFACE_ITEMS;
            case FLUIDS -> TRoomsText.IFACE_FLUIDS;
            case CHEMICALS -> TRoomsText.IFACE_CHEMICALS;
            case ME -> TRoomsText.IFACE_ME;
            case REDSTONE -> TRoomsText.IFACE_REDSTONE;
        };
    }

    static TRoomsText flowName(Flow f) {
        return switch (f) {
            case IN -> TRoomsText.IFACE_FLOW_IN;
            case OUT -> TRoomsText.IFACE_FLOW_OUT;
            case BOTH -> TRoomsText.IFACE_FLOW_BOTH;
        };
    }

    /** Идентификатор, запомненный отвёрткой в руке, или null. */
    private static InterfaceLink heldLink() {
        var p = Minecraft.getInstance().player;
        if (p == null) return null;
        InterfaceLink l = p.getMainHandItem().get(TRoomsBlocks.INTERFACE_LINK.get());
        if (l == null) l = p.getOffhandItem().get(TRoomsBlocks.INTERFACE_LINK.get());
        return l != null && l.id() != 0 ? l : null;
    }

    /** Справа на мониторе — только что передавать. */
    @Override
    public ObjectSelectionList<SelectionListEntry> createSelectionList() {
        GenericMonitorSelectionList<SelectionListEntry> list = newList();
        for (TransferType t : TransferType.values()) {
            if (!t.available()) continue;
            boolean on = data.enabled(t);
            Component label = typeName(t).text().append(": ").append((on ? TRoomsText.IFACE_ON : TRoomsText.IFACE_OFF).text()
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            SelectionListEntry e = new SelectionListEntry(label, x -> send(InterfacePayload.TOGGLE, t.ordinal()), listLeft());
            TRoomsText tip = switch (t) {
                case ME -> TRoomsText.IFACE_ME_TOOLTIP;
                case REDSTONE -> TRoomsText.IFACE_REDSTONE_TOOLTIP;
                default -> TRoomsText.IFACE_TYPE_TOOLTIP;
            };
            tip(e, tip.text().withStyle(ChatFormatting.BLUE));
            list.children().add(e);
        }
        return list;
    }

    /** Слева на мониторе — состояние, ID и сколько интерфейсов связано. */
    @Override
    public void inMonitorRender(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int hPos = (width - monitorWidth) / 2, vPos = (height - monitorHeight) / 2, trim = 15;
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(backdrop, 5 + width / 2, -50 + height / 2, 110, monitorHeight, 110, 95);
        int x0 = hPos + trim - 5, y0 = vPos + trim + 5;
        g.blit(backdrop, x0 - 5, y0 - 5, 0, monitorHeight, 110, 110);
        int x = x0, y = y0, w = 96;
        y = lines(g, status.text(), x, y, w);
        y += 6;
        Component idLine = data.id() != 0 ? hex(data.id()) : TRoomsText.IFACE_ID_NONE.text().withStyle(ChatFormatting.DARK_GRAY);
        // ID крупно, как частота на туннеле P2P.
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, TRoomsText.IFACE_ID.text(idLine).withStyle(ChatFormatting.GRAY), 0, 0, 0xFFFFFF, false);
        g.pose().popPose();
        y += 22;
        if (status == Status.CONNECTED) {
            y = lines(g, TRoomsText.IFACE_PEERS.text(peers).withStyle(ChatFormatting.GRAY), x, y, w);
            y += 2;
        }
        lines(g, TRoomsText.IFACE_FLOW.text(flowName(data.flowMode()).text().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY), x, y, w);
    }

    private int lines(GuiGraphics g, Component text, int x, int y, int w) {
        List<FormattedCharSequence> ls = font.split(text, w);
        for (FormattedCharSequence l : ls) {
            g.drawString(font, l, x, y, 0xFFFFFF, false);
            y += 9;
        }
        return y;
    }
}
