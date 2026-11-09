package leonus.tardis_rooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Клиент: мигающие красные рамки вокруг мешающих блоков поверх стен — что мешает удалить помещение (как подсветка беспроводного
 * соединителя в ExtendedAE). Рамка — 12 линий вокруг коробки (блок или блок из двух половин), без проверки глубины; мигают раз в полсекунды и пропадают
 * сами. Платформа только вызывает {@link #render} из своего события отрисовки мира.
 */
public final class BlockHighlights {
    private static List<AABB> boxes = List.of();
    private static ResourceKey<Level> dimension;
    private static long until;

    /** Показать рамки {@code list} (новый показ заменяет прежний). */
    public static void show(List<AABB> list, int seconds) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        boxes = List.copyOf(list);
        dimension = level.dimension();
        until = System.currentTimeMillis() + seconds * 1000L;
    }

    /** Нарисовать рамки. Вызывать после отрисовки мира (частиц), до интерфейса. */
    public static void render(Camera camera) {
        if (boxes.isEmpty()) return;
        long now = System.currentTimeMillis();
        var level = Minecraft.getInstance().level;
        if (now > until || level == null || !level.dimension().equals(dimension)) {
            boxes = List.of();
            return;
        }
        if ((now / 500) % 2 == 1 || !camera.isInitialized()) return; // мигание
        // Вид строим сами, как игра (поворот камеры, затем сдвиг на неё), а общую матрицу вида на время отрисовки
        // сбрасываем: так не важно, что к этому моменту лежит в матрицах событий разных загрузчиков.
        Vec3 cam = camera.getPosition();
        Matrix4f m = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()))
                .translate((float) -cam.x, (float) -cam.y, (float) -cam.z);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        float e = 0.002f; // чуть больше блока, чтобы рамка не сливалась с его гранями
        for (AABB box : boxes) {
            float x0 = (float) box.minX - e, y0 = (float) box.minY - e, z0 = (float) box.minZ - e;
            float x1 = (float) box.maxX + e, y1 = (float) box.maxY + e, z1 = (float) box.maxZ + e;
            for (float a : new float[]{y0, y1}) for (float b : new float[]{z0, z1}) line(buf, m, x0, a, b, x1, a, b);
            for (float a : new float[]{x0, x1}) for (float b : new float[]{z0, z1}) line(buf, m, a, y0, b, a, y1, b);
            for (float a : new float[]{x0, x1}) for (float b : new float[]{y0, y1}) line(buf, m, a, b, z0, a, b, z1);
        }
        // Линии в 3 пикселя без проверки глубины — видны сквозь стены. Своего RenderType не заводим:
        // нужный для этого метод игры закрыт (NeoForge его открывает, Fabric — нет), а так одинаково на обоих.
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(3f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        BufferUploader.drawWithShader(buf.buildOrThrow());
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1f);
    }

    private static void line(VertexConsumer buf, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1) {
        float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len; ny /= len; nz /= len;
        buf.addVertex(m, x0, y0, z0).setColor(1f, 0.15f, 0.15f, 1f).setNormal(nx, ny, nz);
        buf.addVertex(m, x1, y1, z1).setColor(1f, 0.15f, 0.15f, 1f).setNormal(nx, ny, nz);
    }

    private BlockHighlights() {}
}
