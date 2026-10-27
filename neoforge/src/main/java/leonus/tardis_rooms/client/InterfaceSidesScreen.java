package leonus.tardis_rooms.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import leonus.tardis_rooms.iface.FaceMode;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceData;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.network.InterfacePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Настройка сторон интерфейса, как в EnderIO: блок в 3D вместе с соседями, тянуть мышью — повернуть,
 * клик по грани — сменить её режим. Цвет грани — режим: золотой — «вставлять», голубой — «забирать».
 */
public class InterfaceSidesScreen extends Screen {
    private static final int PANEL_W = 220, PANEL_H = 192, VIEW = 150;
    private static final net.minecraft.resources.ResourceLocation ICON_BACK =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(whocraft.tardis_refined.TardisRefined.MODID, "back");
    private static final net.minecraft.resources.ResourceLocation ICON_NEIGHBOURS_ON = RoomMonitorScreen.sprite("iface/neighbours_on");
    private static final net.minecraft.resources.ResourceLocation ICON_NEIGHBOURS_OFF = RoomMonitorScreen.sprite("iface/neighbours_off");
    private static final net.minecraft.resources.ResourceLocation ICON_RESET = RoomMonitorScreen.sprite("iface/reset_view");
    private static final int GOLD = 0xFFB28610, AQUA = 0xFF3FA7B8;

    private final Screen parent;
    private final BlockPos pos;
    private float yaw, pitch = 25f;
    private final float startYaw;
    private boolean neighbours = true;
    /** Сколько пикселей протащили мышь с нажатия — отличить клик от поворота. */
    private double dragged;
    private boolean pressedInView;
    private Direction hovered;
    private int left, top;

    public InterfaceSidesScreen(Screen parent, BlockPos pos) {
        super(TRoomsText.IFACE_SIDES.text());
        this.parent = parent;
        this.pos = pos;
        var p = Minecraft.getInstance().player;
        // Сначала смотрим на блок так же, как игрок: ближняя к нему грань — к нам.
        this.startYaw = p == null ? 180f : Mth.wrapDegrees(p.getYRot() + 180f);
        this.yaw = startYaw;
    }

    private InterfaceData data() {
        var l = Minecraft.getInstance().level;
        return l != null && l.getBlockEntity(pos) instanceof InterfaceBlockEntity be ? be.data() : InterfaceData.DEFAULT;
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        // Кнопки — значками слева от панели: назад, соседние блоки, исходный вид.
        int x = left - 24, y = top;
        y = icon(x, y, ICON_BACK, CommonComponents.GUI_BACK.copy(), TRoomsText.IFACE_SIDES_BACK_TOOLTIP.text(), this::onClose);
        y = icon(x, y, neighbours ? ICON_NEIGHBOURS_ON : ICON_NEIGHBOURS_OFF,
                (neighbours ? TRoomsText.IFACE_SIDES_NEIGHBOURS_ON : TRoomsText.IFACE_SIDES_NEIGHBOURS_OFF).text(),
                TRoomsText.IFACE_SIDES_NEIGHBOURS_TOOLTIP.text(), () -> { neighbours = !neighbours; rebuildWidgets(); });
        icon(x, y, ICON_RESET, TRoomsText.IFACE_SIDES_RESET.text(), TRoomsText.IFACE_SIDES_RESET_TOOLTIP.text(),
                () -> { yaw = startYaw; pitch = 25f; });
    }

    private int icon(int x, int y, net.minecraft.resources.ResourceLocation sprite, net.minecraft.network.chat.MutableComponent title,
                     net.minecraft.network.chat.MutableComponent hint, Runnable onPress) {
        var b = net.minecraft.client.gui.components.SpriteIconButton.builder(title, btn -> onPress.run(), true).width(20).sprite(sprite, 20, 20).build();
        b.setPosition(x, y);
        b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(title.copy().withStyle(ChatFormatting.GOLD)
                .append("\n").append(hint.withStyle(ChatFormatting.GRAY))));
        addRenderableWidget(b);
        return y + 22;
    }

    @Override
    public void tick() {
        var l = Minecraft.getInstance().level;
        if (l == null || !(l.getBlockEntity(pos) instanceof InterfaceBlockEntity)) Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---------- геометрия вида ----------

    private int viewX() { return left + (PANEL_W - VIEW) / 2; }
    private int viewY() { return top + 22; }

    private boolean inView(double mx, double my) {
        return mx >= viewX() && mx < viewX() + VIEW && my >= viewY() && my < viewY() + VIEW;
    }

    /** Преобразование «блок → экран»: центр блока в центре вида, масштаб под соседей. */
    private Matrix4f transform() {
        float scale = neighbours ? 34f : 60f;
        return new Matrix4f()
                .translate(viewX() + VIEW / 2f, viewY() + VIEW / 2f, 150f)
                .scale(scale, -scale, scale)
                .rotateX(pitch * Mth.DEG_TO_RAD)
                .rotateY(yaw * Mth.DEG_TO_RAD)
                .translate(-0.5f, -0.5f, -0.5f);
    }

    /** Грань блока под курсором (луч из экрана в блок), или null. */
    private Direction pick(double mx, double my) {
        if (!inView(mx, my)) return null;
        Matrix4f inv = transform().invert();
        Vector4f a = inv.transform(new Vector4f((float) mx, (float) my, 1000f, 1f));
        Vector4f b = inv.transform(new Vector4f((float) mx, (float) my, -1000f, 1f));
        Vector3f o = new Vector3f(a.x / a.w, a.y / a.w, a.z / a.w);
        Vector3f d = new Vector3f(b.x / b.w, b.y / b.w, b.z / b.w).sub(o);
        double tMin = Double.NEGATIVE_INFINITY, tMax = Double.POSITIVE_INFINITY;
        Direction face = null;
        for (int axis = 0; axis < 3; axis++) {
            double oo = o.get(axis), dd = d.get(axis);
            if (Math.abs(dd) < 1e-9) {
                if (oo < 0 || oo > 1) return null;
                continue;
            }
            double t1 = (0 - oo) / dd, t2 = (1 - oo) / dd;
            boolean nearIsMin = t1 < t2;
            double near = Math.min(t1, t2), far = Math.max(t1, t2);
            if (near > tMin) {
                tMin = near;
                Direction.Axis ax = Direction.Axis.values()[axis];
                face = Direction.fromAxisAndDirection(ax, nearIsMin ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            }
            tMax = Math.min(tMax, far);
        }
        return tMin <= tMax && tMax >= 0 ? face : null;
    }

    // ---------- мышь ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inView(mx, my)) {
            pressedInView = true;
            dragged = 0;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (pressedInView) {
            dragged += Math.abs(dx) + Math.abs(dy);
            yaw = Mth.wrapDegrees(yaw + (float) dx * 1.5f);
            pitch = Mth.clamp(pitch + (float) dy * 1.5f, -89f, 89f);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (pressedInView) {
            pressedInView = false;
            Direction f = dragged < 3 ? pick(mx, my) : null;
            if (f != null) {
                // ЛКМ — следующий режим, ПКМ — предыдущий, Shift + клик — по умолчанию.
                int op = hasShiftDown() ? 2 : button == 1 ? 1 : 0;
                RoomManageScreen.click();
                PacketDistributor.sendToServer(new InterfacePayload(pos, InterfacePayload.FACE, op * 6 + f.get3DDataValue()));
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    // ---------- рисование ----------

    /** Панель, вид и легенда — под кнопками (Screen.render рисует фон, затем виджеты). */
    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g); // без размытия — блок и кнопки должны быть чёткими
        g.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFF747370);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF2E333E);
        g.fill(viewX() - 1, viewY() - 1, viewX() + VIEW + 1, viewY() + VIEW + 1, 0xFF454D5B);
        g.fill(viewX(), viewY(), viewX() + VIEW, viewY() + VIEW, 0xFF1B1E25);
        g.drawCenteredString(font, title, width / 2, top + 7, 0xFFDDB854);

        InterfaceData data = data();
        hovered = pressedInView && dragged >= 3 ? null : pick(mouseX, mouseY);
        g.enableScissor(viewX(), viewY(), viewX() + VIEW, viewY() + VIEW);
        renderBlocks(g, data);
        g.disableScissor();

        int ly = viewY() + VIEW + 5, lx = left + 10;
        lx = legend(g, lx, ly, GOLD, InterfaceTools.faceModeName(FaceMode.PUSH).text());
        legend(g, lx + 8, ly, AQUA, InterfaceTools.faceModeName(FaceMode.PULL).text());
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hovered == null) return;
        FaceMode m = data().face(hovered);
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(InterfaceTools.faceName(hovered).text().append(": ")
                .append(InterfaceTools.faceModeName(m).text().withStyle(color(m))).getVisualOrderText());
        var l = Minecraft.getInstance().level;
        if (l != null) {
            BlockState ns = l.getBlockState(pos.relative(hovered));
            Component n = ns.isAir() ? TRoomsText.IFACE_SIDES_NOTHING.text().withStyle(ChatFormatting.DARK_GRAY)
                    : TRoomsText.IFACE_SIDES_NEIGHBOUR.text(ns.getBlock().getName().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY);
            lines.add(n.getVisualOrderText());
        }
        lines.addAll(font.split(TRoomsText.IFACE_SIDES_HINT.text().withStyle(ChatFormatting.DARK_GRAY), 200));
        g.renderTooltip(font, lines, mouseX, mouseY);
    }

    private int legend(GuiGraphics g, int x, int y, int color, Component text) {
        g.fill(x, y + 1, x + 7, y + 8, color);
        g.drawString(font, text, x + 10, y, 0xFFCCCCCC, false);
        return x + 10 + font.width(text);
    }

    private static ChatFormatting color(FaceMode m) {
        return switch (m) { case DEFAULT -> ChatFormatting.GRAY; case PUSH -> ChatFormatting.GOLD; case PULL -> ChatFormatting.AQUA; };
    }

    private void renderBlocks(GuiGraphics g, InterfaceData data) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.mulPose(transform());
        MultiBufferSource.BufferSource buf = g.bufferSource();
        Lighting.setupFor3DItems();
        var dispatcher = mc.getBlockRenderer();
        renderState(dispatcher, ps, buf, level.getBlockState(pos));
        if (neighbours) {
            for (Direction d : Direction.values()) {
                BlockState ns = level.getBlockState(pos.relative(d));
                if (ns.isAir() || ns.getRenderShape() == RenderShape.INVISIBLE) continue;
                // Соседи — уменьшенные и чуть отодвинутые, чтобы не загораживать грани интерфейса.
                ps.pushPose();
                ps.translate(0.5 + d.getStepX() * 1.25, 0.5 + d.getStepY() * 1.25, 0.5 + d.getStepZ() * 1.25);
                ps.scale(0.6f, 0.6f, 0.6f);
                ps.translate(-0.5, -0.5, -0.5);
                renderState(dispatcher, ps, buf, ns);
                ps.popPose();
            }
        }
        g.flush();

        // Цвет граней — поверх, отдельным проходом, чтобы не спорить с блоками за глубину.
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        Matrix4f m = ps.last().pose();
        for (Direction d : Direction.values()) {
            FaceMode fm = data.face(d);
            int c = fm == FaceMode.PUSH ? GOLD : fm == FaceMode.PULL ? AQUA : 0;
            int alpha = c == 0 ? 0 : 0x90;
            if (d == hovered) {
                if (c == 0) { c = 0xFFFFFF; alpha = 0x50; } else alpha = 0xC0;
            }
            if (alpha == 0) continue;
            quad(vc, m, d, (c >> 16) & 255, (c >> 8) & 255, c & 255, alpha);
        }
        g.flush();
        ps.popPose();
        Lighting.setupFor3DItems();
    }

    private static void renderState(net.minecraft.client.renderer.block.BlockRenderDispatcher dispatcher, PoseStack ps,
                                    MultiBufferSource buf, BlockState state) {
        try {
            dispatcher.renderSingleBlock(state, ps, buf, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        } catch (Exception ignored) {
            // Чужой блок, который не умеет рисоваться вне мира, — просто не показываем.
        }
    }

    /** Квадрат чуть снаружи грани {@code d} единичного куба. */
    private static void quad(VertexConsumer vc, Matrix4f m, Direction d, int r, int g, int b, int a) {
        float e = 0.003f, lo = -e, hi = 1 + e;
        float[][] v = switch (d) {
            case DOWN -> new float[][]{{0, lo, 0}, {1, lo, 0}, {1, lo, 1}, {0, lo, 1}};
            case UP -> new float[][]{{0, hi, 0}, {0, hi, 1}, {1, hi, 1}, {1, hi, 0}};
            case NORTH -> new float[][]{{0, 0, lo}, {0, 1, lo}, {1, 1, lo}, {1, 0, lo}};
            case SOUTH -> new float[][]{{0, 0, hi}, {1, 0, hi}, {1, 1, hi}, {0, 1, hi}};
            case WEST -> new float[][]{{lo, 0, 0}, {lo, 0, 1}, {lo, 1, 1}, {lo, 1, 0}};
            case EAST -> new float[][]{{hi, 0, 0}, {hi, 1, 0}, {hi, 1, 1}, {hi, 0, 1}};
        };
        for (float[] p : v) vc.addVertex(m, p[0], p[1], p[2]).setColor(r, g, b, a);
    }
}
