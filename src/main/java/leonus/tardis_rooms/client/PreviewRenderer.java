package leonus.tardis_rooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import leonus.tardis_rooms.room.PieceCatalog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import whocraft.tardis_refined.client.screen.main.MonitorOS;

/**
 * Рисует план помещения (4 пикселя на блок) в квадрате 192×192, повёрнутым так,
 * чтобы вход (откуда игрок придёт из узла) был снизу, и отмечает вход рамкой.
 */
public final class PreviewRenderer {
    public static final int SIZE = 192;
    private static final int PX = 4;

    public static float rotationFor(Direction entry) {
        return switch (entry) {
            case NORTH -> 180f;
            case WEST -> -90f;
            case EAST -> 90f;
            default -> 0f;
        };
    }

    public static void draw(GuiGraphics g, PieceCatalog.Preview p, Direction entry, float alpha) {
        int w = p.widthBlocks() * PX, h = p.depthBlocks() * PX;
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(SIZE / 2f, SIZE / 2f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(rotationFor(entry)));
        pose.translate(-w / 2f, -h / 2f, 0);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        g.blit(p.texture(), 0, 0, 0, 0, w, h, w, h);
        int a = Math.round(alpha * 255) << 24;
        int x = (p.entryX() - 2) * PX, z = (p.entryZ() - 2) * PX;
        g.renderOutline(x, z, 5 * PX, 5 * PX, a | 0xFFD250);
        g.renderOutline(x + 1, z + 1, 5 * PX - 2, 5 * PX - 2, a | 0xFFD250);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.popPose();
    }

    public static void noise(GuiGraphics g, float alpha) {
        if (alpha <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        g.blit(MonitorOS.NOISE, 0, 0, (int) (Math.random() * 736), (int) (414 * (System.currentTimeMillis() % 1000) / 1000.0), SIZE, SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private PreviewRenderer() {}
}
