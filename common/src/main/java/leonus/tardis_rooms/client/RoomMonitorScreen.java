package leonus.tardis_rooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import leonus.tardis_rooms.room.Node;
import leonus.tardis_rooms.room.PieceCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import whocraft.tardis_refined.TardisRefined;
import whocraft.tardis_refined.client.screen.components.GenericMonitorSelectionList;
import whocraft.tardis_refined.client.screen.components.SelectionListEntry;
import whocraft.tardis_refined.client.screen.main.MonitorOS;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Общая часть экранов выбора: монитор TR, слева план, справа список. */
public abstract class RoomMonitorScreen extends MonitorOS {
    protected BlockPos nodePos;
    protected Node node;
    protected PieceCatalog.Preview previewNow;
    protected PieceCatalog.Preview previewBefore;

    protected RoomMonitorScreen(Component title, BlockPos nodePos, Node node) {
        super(title, ResourceLocation.fromNamespaceAndPath(TardisRefined.MODID, "textures/gui/monitor/backdrop.png"));
        this.nodePos = nodePos;
        this.node = node;
    }

    protected static Node nodeAt(BlockPos pos) {
        return Node.locate(pos).orElse(new Node(0, 0, Direction.SOUTH, 97));
    }

    protected Direction entry() {
        return node.side().getOpposite();
    }

    /** Подсказки строк: рисуем сами поверх рамки монитора, иначе TR обрезает их по краю экрана. */
    private final Map<SelectionListEntry, Component> tips = new LinkedHashMap<>();

    protected void tip(SelectionListEntry entry, Component text) {
        tips.put(entry, text);
    }

    @Override
    protected void init() {
        tips.clear();
        sideButtons.clear();
        super.init();
    }

    // ---------- кнопки вокруг рамки ----------

    /** Ширина рамки монитора TR. */
    protected static final int FRAME_W = 256;

    /** Кнопки вокруг рамки монитора: принимают клики как обычно, а рисуются в {@link #render} поверх рамки. */
    private final List<SpriteIconButton> sideButtons = new ArrayList<>();
    /** Последняя добавленная кнопка (чтобы, например, сделать её неактивной). */
    protected SpriteIconButton lastButton;

    protected static ResourceLocation sprite(String path) {
        return ResourceLocation.fromNamespaceAndPath(leonus.tardis_rooms.TardisRooms.MODID, path);
    }

    /** Левая колонка кнопок — вплотную слева от рамки. */
    protected int leftColumn() { return (width - FRAME_W) / 2 - 24; }

    /** Правая колонка кнопок — вплотную справа от рамки. */
    protected int rightColumn() { return (width + FRAME_W) / 2 + 4; }

    /** Верх колонок кнопок — на уровне экрана монитора. */
    protected int columnTop() { return (height - monitorHeight) / 2 + 2; }

    /**
     * Квадратная кнопка 20×20 со значком; подсказка — заголовок и пояснение. Возвращает y следующей.
     * Монитор TR рисует свои виджеты только в пределах экрана монитора, поэтому эти рисуем сами.
     */
    protected int sideButton(int x, int y, ResourceLocation icon, MutableComponent title, MutableComponent hint, Runnable onPress) {
        SpriteIconButton b = SpriteIconButton.builder(title, btn -> onPress.run(), true).width(20).sprite(icon, 20, 20).build();
        b.setPosition(x, y);
        b.setTooltip(Tooltip.create(title.copy().withStyle(ChatFormatting.GOLD).append("\n").append(hint.withStyle(ChatFormatting.GRAY))));
        addWidget(b);
        sideButtons.add(b);
        lastButton = b;
        return y + 22;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        for (SpriteIconButton b : sideButtons) b.render(g, mouseX, mouseY, partialTick);
        for (Map.Entry<SelectionListEntry, Component> e : tips.entrySet()) {
            if (overList(mouseX, mouseY) && e.getKey().isMouseOver(mouseX, mouseY)) {
                g.renderTooltip(font, font.split(e.getValue(), 180), mouseX, mouseY);
                break;
            }
        }
    }

    /** Пришли данные с сервера — перестроить список. */
    public void onDataChanged() {
        rebuildWidgets();
    }

    protected GenericMonitorSelectionList<SelectionListEntry> newList() {
        int topPos = (height - monitorHeight) / 2;
        // Ширина строки по умолчанию 220: зона наведения/клика строк вылезала бы на превью слева.
        GenericMonitorSelectionList<SelectionListEntry> list = new GenericMonitorSelectionList<>(this.minecraft, 100, 80, listLeft(), topPos + 15, topPos + monitorHeight - 30, 12) {
            @Override
            public int getRowWidth() {
                return LIST_WIDTH;
            }
        };
        list.setRenderBackground(false);
        return list;
    }

    protected static final int LIST_WIDTH = 100;

    /** Мышь над списком справа (а не над превью/схемой). */
    protected boolean overList(double mouseX, double mouseY) {
        return mouseX >= listLeft() && mouseX < listLeft() + LIST_WIDTH;
    }

    protected int listLeft() {
        return 5 + width / 2;
    }

    // ---------- общий вид ----------

    /**
     * События и кнопки под монитором: «назад» и, если {@code submit} не null, «выбрать».
     * Возвращает кнопку «выбрать» (или null): метод TR её не возвращает, она — последний добавленный элемент.
     */
    protected @Nullable AbstractWidget monitorButtons(@Nullable MonitorOSRun submit, MonitorOSRun back) {
        setEvents(submit, back);
        int y = height - (height - monitorHeight) / 2 - 25;
        AbstractWidget ok = null;
        if (submit != null) {
            addSubmitButton(width / 2 + 25, y);
            if (children().get(children().size() - 1) instanceof AbstractWidget w) ok = w;
        }
        addCancelButton(width / 2 + 5, y);
        return ok;
    }

    /** Отступ содержимого от края экрана монитора. */
    protected static final int TRIM = 15;

    /** Левый верхний угол левой панели (план, схема, сведения). */
    protected int panelX() { return (width - monitorWidth) / 2 + TRIM - 5; }

    protected int panelY() { return (height - monitorHeight) / 2 + TRIM + 5; }

    /** Масштаб плана в левой панели. */
    protected float panelScale() { return (monitorHeight - 2 * TRIM) / (float) PreviewRenderer.SIZE; }

    /** Фон обеих панелей монитора: справа — под список, слева — под план или сведения. */
    protected void drawPanels(GuiGraphics g) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(backdrop, 5 + width / 2, -50 + height / 2, 110, monitorHeight, 110, 95);
        g.blit(backdrop, panelX() - 5, panelY() - 5, 0, monitorHeight, 110, 110);
    }

    protected void changePreview(PieceCatalog.Preview next) {
        previewBefore = previewNow;
        previewNow = next;
        age = 0;
    }

    @Override
    public void inMonitorRender(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PoseStack pose = g.pose();
        drawPanels(g);
        pose.pushPose();
        pose.translate(panelX(), panelY(), 0);
        float scale = panelScale();
        pose.scale(scale, scale, scale);
        if (previewNow != null) PreviewRenderer.draw(g, previewNow, entry(), 1f);
        float fade = (float) Math.max(0, (100.0D - this.age * 3.0D) / 100.0D);
        if (fade > 0 && previewBefore != null) PreviewRenderer.draw(g, previewBefore, entry(), fade);
        PreviewRenderer.noise(g, fade);
        RenderSystem.disableBlend();
        pose.popPose();
    }
}
