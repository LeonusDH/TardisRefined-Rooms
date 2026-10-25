package leonus.tardis_rooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.MapPayloads;
import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.Node;
import leonus.tardis_rooms.room.PieceCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import whocraft.tardis_refined.client.screen.components.GenericMonitorSelectionList;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;
import whocraft.tardis_refined.client.screen.main.MonitorOS;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Схема застройки этажа вокруг узла: помещения с границами, хаб, запретные зоны.
 * Повёрнута как превью — узел снизу, рост «вверх». Если открыта из списка вариантов —
 * поверх показано, куда встанет выбранный вариант (красным — где он упрётся).
 */
public class RoomMapScreen extends RoomMonitorScreen {
    private static final int C = 12;
    private static final int OFF = (PreviewRenderer.SIZE - MapPayloads.SIDE * C) / 2;

    private static final int COL_FREE = 0x28FFFFFF, COL_HUB = 0xFFC9A227, COL_FORBIDDEN = 0xFF5A1E1E,
            COL_UNKNOWN = 0xFF555555, COL_TARGET = 0xFFFFD250,
            COL_PLAN = 0xA0FFD250, COL_PLAN_BAD = 0xC0FF3030, COL_REPLACING = 0xFFD07020, COL_EDGE = 0xFFE8F4FF;
    private static final int[] PIECE_COLORS = {0xFF2E8B9A, 0xFF3A6FB0, 0xFF4F9E5A, 0xFF7A5BB0, 0xFF3AA0A0, 0xFF5470C0, 0xFF6E9A3A};

    private final MonitorOS parent;
    private final BlockPos replaceCore;
    private final PieceCatalog.Option option;
    private final Map<Integer, Set<Long>> plan;
    private final Map<ResourceLocation, Component> labels = new HashMap<>();
    private int floor;
    private MapPayloads.Info info;

    /** {@code option} — показать поверх, куда встанет вариант (или null); {@code replaceCore} ≠ ZERO — заменяемое помещение. */
    public RoomMapScreen(BlockPos nodePos, Node node, BlockPos replaceCore, PieceCatalog.Option option, MonitorOS parent) {
        super(TRoomsText.MAP_TITLE.text(), nodePos, node);
        this.parent = parent;
        this.replaceCore = replaceCore;
        this.option = option;
        this.floor = node.floorY();
        this.plan = option == null ? Map.of() : PieceCatalog.plan(option.id(), node).map(PieceCatalog.Plan::footprint).orElse(Map.of());
        for (PieceCatalog.Option o : PieceCatalog.options(node)) labels.put(o.id(), o.label());
        request();
    }

    private void request() {
        info = null;
        PacketDistributor.sendToServer(new MapPayloads.Request(nodePos, floor, replaceCore));
    }

    public static void accept(MapPayloads.Info i) {
        if (Minecraft.getInstance().screen instanceof RoomMapScreen s && s.nodePos.equals(i.nodePos()) && s.floor == i.floor()) {
            s.info = i;
            s.rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.setEvents(null, () -> switchScreenToLeft(parent));
        int vPos = (height - monitorHeight) / 2;
        addCancelButton(width / 2 + 5, height - vPos - 25);
        sideButton(rightColumn(), columnTop(), sprite("iface/tips"), TRoomsText.MAP_LEGEND.text(), legend(), () -> {});
    }

    /** Обозначения схемы — в подсказке кнопки справа от монитора. */
    private net.minecraft.network.chat.MutableComponent legend() {
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        java.util.List<Component> rows = new java.util.ArrayList<>();
        if (!plan.isEmpty()) {
            rows.add(swatch(COL_PLAN, TRoomsText.MAP_PLAN));
            rows.add(swatch(COL_PLAN_BAD, TRoomsText.MAP_PLAN_BAD));
        }
        if (info != null && info.replacingId() > 0) rows.add(swatch(COL_REPLACING, TRoomsText.MAP_REPLACING));
        rows.add(swatch(COL_TARGET, TRoomsText.MAP_TARGET));
        rows.add(swatch(PIECE_COLORS[0], TRoomsText.MAP_PIECE));
        rows.add(swatch(COL_HUB, TRoomsText.MAP_HUB));
        rows.add(swatch(COL_UNKNOWN, TRoomsText.MAP_UNKNOWN));
        rows.add(swatch(COL_FORBIDDEN, TRoomsText.MAP_FORBIDDEN));
        rows.add(swatch(0xFF909090, TRoomsText.MAP_FREE));
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) out.append("\n");
            out.append(rows.get(i));
        }
        return out.append("\n").append(TRoomsText.MAP_LEGEND_HINT.text().withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component swatch(int color, TRoomsText name) {
        return Component.literal("■ ").withStyle(s -> s.withColor(TextColor.fromRgb(color & 0xFFFFFF)))
                .append(name.text().withStyle(ChatFormatting.WHITE));
    }

    @Override
    public ObjectSelectionList<SelectionListEntry> createSelectionList() {
        GenericMonitorSelectionList<SelectionListEntry> list = newList();
        if (option != null) list.children().add(new SelectionListEntry(option.label().copy().withStyle(ChatFormatting.GOLD), e -> {}, listLeft(), false));
        for (int i = Lattice.FLOORS.size() - 1; i >= 0; i--) {
            int f = Lattice.FLOORS.get(i);
            SelectionListEntry entry = new SelectionListEntry(TRoomsText.floor(f).text(), e -> {
                if (floor == f) return;
                RoomManageScreen.click();
                floor = f;
                request();
                rebuildWidgets();
            }, listLeft());
            entry.setChecked(floor == f);
            list.children().add(entry);
        }
        if (info == null) list.children().add(new SelectionListEntry(TRoomsText.SCREEN_LOADING.text(), e -> {}, listLeft(), false));
        return list;
    }

    // ---------- рисование ----------

    private int colorOf(int code) {
        if (code > 0) return code == info.replacingId() ? COL_REPLACING : PIECE_COLORS[code % PIECE_COLORS.length];
        return switch (code) {
            case MapPayloads.HUB -> COL_HUB;
            case MapPayloads.FORBIDDEN -> COL_FORBIDDEN;
            case MapPayloads.UNKNOWN -> COL_UNKNOWN;
            default -> COL_FREE;
        };
    }

    private static boolean sameOwner(int a, int b) {
        return a == b;
    }

    private int cellX(int dx) { return OFF + (dx + MapPayloads.RADIUS) * C; }

    @Override
    public void inMonitorRender(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PoseStack pose = g.pose();
        int hPos = (width - monitorWidth) / 2, vPos = (height - monitorHeight) / 2, trim = 15;
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(backdrop, 5 + width / 2, -50 + height / 2, 110, monitorHeight, 110, 95);
        pose.pushPose();
        pose.translate(hPos + trim - 5, vPos + trim + 5, 0);
        g.blit(backdrop, -5, -5, 0, monitorHeight, 110, 110);
        float scale = (monitorHeight - 2 * trim) / (float) PreviewRenderer.SIZE;
        pose.scale(scale, scale, scale);
        pose.translate(PreviewRenderer.SIZE / 2f, PreviewRenderer.SIZE / 2f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(PreviewRenderer.rotationFor(entry())));
        pose.translate(-PreviewRenderer.SIZE / 2f, -PreviewRenderer.SIZE / 2f, 0);
        if (info != null) drawMap(g);
        pose.popPose();
        float fade = (float) Math.max(0, (100.0D - this.age * 3.0D) / 100.0D);
        if (info == null || fade > 0) {
            pose.pushPose();
            pose.translate(hPos + trim - 5, vPos + trim + 5, 0);
            pose.scale(scale, scale, scale);
            PreviewRenderer.noise(g, info == null ? 0.6f : fade);
            pose.popPose();
        }
        RenderSystem.disableBlend();
    }

    private void drawMap(GuiGraphics g) {
        int r = MapPayloads.RADIUS;
        for (int dz = -r; dz <= r; dz++)
            for (int dx = -r; dx <= r; dx++) {
                int code = info.cell(dx, dz), x = cellX(dx), y = cellX(dz);
                if (code == MapPayloads.FREE) { g.fill(x + 2, y + 2, x + C - 2, y + C - 2, COL_FREE); continue; }
                g.fill(x, y, x + C, y + C, colorOf(code));
                // Границы: там, где сосед принадлежит другому.
                if (!sameOwner(code, info.cell(dx, dz - 1))) g.fill(x, y, x + C, y + 1, COL_EDGE);
                if (!sameOwner(code, info.cell(dx, dz + 1))) g.fill(x, y + C - 1, x + C, y + C, COL_EDGE);
                if (!sameOwner(code, info.cell(dx - 1, dz))) g.fill(x, y, x + 1, y + C, COL_EDGE);
                if (!sameOwner(code, info.cell(dx + 1, dz))) g.fill(x + C - 1, y, x + C, y + C, COL_EDGE);
            }

        Set<Long> fp = plan.getOrDefault(floor, Set.of());
        int cx = node.targetX(), cz = node.targetZ();
        for (long m : fp) {
            int dx = Lattice.keyX(m) - cx, dz = Lattice.keyZ(m) - cz;
            if (Math.abs(dx) > r || Math.abs(dz) > r) continue;
            int code = info.cell(dx, dz);
            boolean ok = code == MapPayloads.FREE || (info.replacingId() > 0 && code == info.replacingId());
            int x = cellX(dx), y = cellX(dz);
            g.fill(x + 2, y + 2, x + C - 2, y + C - 2, ok ? COL_PLAN : COL_PLAN_BAD);
        }

        // Узел: рамка вокруг модуля, куда он ведёт, и метка на стыке с модулем, где он стоит.
        if (floor == node.floorY()) {
            int x = cellX(0), y = cellX(0);
            g.renderOutline(x, y, C, C, COL_TARGET);
            g.renderOutline(x + 1, y + 1, C - 2, C - 2, COL_TARGET);
            int sx = -node.side().getStepX(), sz = -node.side().getStepZ();
            int mx = x + C / 2 + sx * C / 2, my = y + C / 2 + sz * C / 2;
            g.fill(mx - 2, my - 2, mx + 2, my + 2, COL_TARGET);
        }
    }

    // ---------- подсказка клетки ----------

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (info == null || overList(mouseX, mouseY)) return;
        int hPos = (width - monitorWidth) / 2, vPos = (height - monitorHeight) / 2, trim = 15;
        float scale = (monitorHeight - 2 * trim) / (float) PreviewRenderer.SIZE;
        double lx = (mouseX - (hPos + trim - 5)) / scale - PreviewRenderer.SIZE / 2.0;
        double ly = (mouseY - (vPos + trim + 5)) / scale - PreviewRenderer.SIZE / 2.0;
        double a = Math.toRadians(PreviewRenderer.rotationFor(entry()));
        double ux = Math.cos(a) * lx + Math.sin(a) * ly + PreviewRenderer.SIZE / 2.0;
        double uy = -Math.sin(a) * lx + Math.cos(a) * ly + PreviewRenderer.SIZE / 2.0;
        int dx = (int) Math.floor((ux - OFF) / C) - MapPayloads.RADIUS, dz = (int) Math.floor((uy - OFF) / C) - MapPayloads.RADIUS;
        if (Math.abs(dx) > MapPayloads.RADIUS || Math.abs(dz) > MapPayloads.RADIUS || ux < OFF || uy < OFF) return;
        int code = info.cell(dx, dz);
        net.minecraft.network.chat.MutableComponent text = describe(code).copy();
        if (dx == 0 && dz == 0 && floor == node.floorY()) note(text, TRoomsText.MAP_TARGET, TRoomsText.MAP_TARGET_TOOLTIP, ChatFormatting.YELLOW);
        if (plan.getOrDefault(floor, Set.of()).contains(Lattice.key(node.targetX() + dx, node.targetZ() + dz))) {
            boolean ok = code == MapPayloads.FREE || (info.replacingId() > 0 && code == info.replacingId());
            if (ok) note(text, TRoomsText.MAP_PLAN, TRoomsText.MAP_PLAN_TOOLTIP, ChatFormatting.YELLOW);
            else note(text, TRoomsText.MAP_PLAN_BAD, TRoomsText.MAP_PLAN_BAD_TOOLTIP, ChatFormatting.RED);
        }
        g.renderTooltip(font, font.split(text, 180), mouseX, mouseY);
    }

    /** Дописать к подсказке клетки строку-пометку и её пояснение. */
    private static void note(net.minecraft.network.chat.MutableComponent text, TRoomsText name, TRoomsText hint, ChatFormatting color) {
        text.append("\n").append(name.text().withStyle(color)).append("\n").append(hint.text().withStyle(ChatFormatting.GRAY));
    }

    /** Что в клетке: название и пояснение. */
    private Component describe(int code) {
        if (code > 0) {
            ResourceLocation id = info.nameOf(code);
            Component name = id == null ? null : labels.get(id);
            net.minecraft.network.chat.MutableComponent out = (name != null ? name.copy() : TRoomsText.MAP_PIECE.text()).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(" #" + code).withStyle(ChatFormatting.DARK_GRAY));
            if (code == info.replacingId()) note(out, TRoomsText.MAP_REPLACING, TRoomsText.MAP_REPLACING_TOOLTIP, ChatFormatting.GOLD);
            else out.append("\n").append(TRoomsText.MAP_PIECE_TOOLTIP.text().withStyle(ChatFormatting.GRAY));
            return out;
        }
        TRoomsText[] t = switch (code) {
            case MapPayloads.HUB -> new TRoomsText[]{TRoomsText.MAP_HUB, TRoomsText.MAP_HUB_TOOLTIP};
            case MapPayloads.FORBIDDEN -> new TRoomsText[]{TRoomsText.MAP_FORBIDDEN, TRoomsText.MAP_FORBIDDEN_TOOLTIP};
            case MapPayloads.UNKNOWN -> new TRoomsText[]{TRoomsText.MAP_UNKNOWN, TRoomsText.MAP_UNKNOWN_TOOLTIP};
            default -> new TRoomsText[]{TRoomsText.MAP_FREE, TRoomsText.MAP_FREE_TOOLTIP};
        };
        return t[0].text().withStyle(ChatFormatting.WHITE).append("\n").append(t[1].text().withStyle(ChatFormatting.GRAY));
    }
}
