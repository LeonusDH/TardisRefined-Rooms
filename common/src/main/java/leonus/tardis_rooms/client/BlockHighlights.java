package leonus.tardis_rooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import leonus.tardis_rooms.room.BlockerHighlight;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Клиент: мигающие красные рамки вокруг мешающих блоков поверх стен — что мешает удалить помещение (как подсветка беспроводного
 * соединителя в ExtendedAE). Рамка — 12 линий вокруг коробки (блок или блок из двух половин), без проверки глубины; мигают раз в
 * полсекунды и пропадают сами. Платформа только вызывает {@link #render} из своего события отрисовки мира.
 *
 * <p>С миром рамки сверяются не каждый кадр, а раз в {@value #RECHECK_MS} мс; линии лежат в видеопамяти и пересобираются,
 * только когда рамки изменились. Так сколько угодно рамок почти ничего не стоят кадру.
 */
public final class BlockHighlights {
    /** Как часто сверяться с миром: убранный блок перестаёт обводиться не позже чем через столько. */
    private static final long RECHECK_MS = 100;
    private static final float R = 1f, G = 0.15f, B = 0.15f;

    private static List<BlockPos> blocks = List.of();
    /** Мир, для которого показ. Сам объект, а не измерение: в другом мире, на другом сервере или после смены измерения — новый. */
    private static @Nullable ClientLevel level;
    private static long until, nextCheck;
    /** Рамки, из которых собраны линии {@link #mesh}. */
    private static List<AABB> boxes = List.of();
    /** Линии всех рамок в видеопамяти — относительно {@link #anchor}, чтобы не терять точность float вдали от нуля. */
    private static @Nullable VertexBuffer mesh;
    private static BlockPos anchor = BlockPos.ZERO;

    /** Обвести блоки {@code list} (новый показ заменяет прежний; пусто — снять обводку). */
    public static void show(List<BlockPos> list, int seconds) {
        ClientLevel now = Minecraft.getInstance().level;
        clear();
        if (now == null || list.isEmpty()) return;
        blocks = List.copyOf(list);
        level = now;
        until = System.currentTimeMillis() + seconds * 1000L;
        nextCheck = 0;
    }

    private static void clear() {
        blocks = List.of();
        level = null;
        boxes = List.of();
        if (mesh != null) {
            mesh.close();
            mesh = null;
        }
    }

    /** Нарисовать рамки. Вызывать после отрисовки мира (частиц), до интерфейса. */
    public static void render(Camera camera) {
        if (level == null) return;
        long now = System.currentTimeMillis();
        if (now > until || level != Minecraft.getInstance().level) {
            clear();
            return;
        }
        if (now >= nextCheck) {
            nextCheck = now + RECHECK_MS;
            if (!recheck()) { // всё убрали — обводить больше нечего
                clear();
                return;
            }
        }
        if ((now / 500) % 2 == 1 || !camera.isInitialized() || mesh == null) return; // мигание
        // Вид строим сами, как игра (поворот камеры, затем сдвиг на неё): так не важно, что к этому моменту лежит
        // в матрицах событий разных загрузчиков.
        Vec3 cam = camera.getPosition();
        Matrix4f view = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()))
                .translate((float) (anchor.getX() - cam.x), (float) (anchor.getY() - cam.y), (float) (anchor.getZ() - cam.z));
        // Линии в 3 пикселя без проверки глубины — видны сквозь стены. Своего RenderType не заводим:
        // нужный для этого метод игры закрыт (NeoForge его открывает, Fabric — нет), а так одинаково на обоих.
        RenderSystem.lineWidth(3f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        mesh.bind();
        mesh.drawWithShader(view, RenderSystem.getProjectionMatrix(), GameRenderer.getRendertypeLinesShader());
        VertexBuffer.unbind();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1f);
    }

    /**
     * Сверить рамки с тем, что стоит в мире сейчас: убранный блок больше не обводим, у двойного сундука, ставшего
     * одинарным, рамка сжимается. Изменились — пересобрать линии. false — обводить больше нечего.
     */
    private static boolean recheck() {
        List<BlockPos> still = new ArrayList<>(blocks.size());
        for (BlockPos p : blocks) if (level.getBlockEntity(p) != null) still.add(p);
        if (still.isEmpty()) return false;
        List<AABB> now = BlockerHighlight.group(level, still).stream().map(BlockerHighlight.Target::box).toList();
        if (now.equals(boxes) && mesh != null) return true;
        boxes = now;
        anchor = still.get(0);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        float e = 0.002f; // чуть больше блока, чтобы рамка не сливалась с его гранями
        for (AABB box : boxes) {
            float x0 = (float) (box.minX - anchor.getX()) - e, y0 = (float) (box.minY - anchor.getY()) - e, z0 = (float) (box.minZ - anchor.getZ()) - e;
            float x1 = (float) (box.maxX - anchor.getX()) + e, y1 = (float) (box.maxY - anchor.getY()) + e, z1 = (float) (box.maxZ - anchor.getZ()) + e;
            for (float a : new float[]{y0, y1}) for (float b : new float[]{z0, z1}) line(buf, x0, a, b, x1, a, b);
            for (float a : new float[]{x0, x1}) for (float b : new float[]{z0, z1}) line(buf, a, y0, b, a, y1, b);
            for (float a : new float[]{x0, x1}) for (float b : new float[]{y0, y1}) line(buf, a, b, z0, a, b, z1);
        }
        if (mesh == null) mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
        mesh.bind();
        mesh.upload(buf.buildOrThrow());
        VertexBuffer.unbind();
        return true;
    }

    /** Отрезок; нормаль — его направление (так шейдер линий знает, в какую сторону их утолщать). */
    private static void line(VertexConsumer buf, float x0, float y0, float z0, float x1, float y1, float z1) {
        float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len; ny /= len; nz /= len;
        buf.addVertex(x0, y0, z0).setColor(R, G, B, 1f).setNormal(nx, ny, nz);
        buf.addVertex(x1, y1, z1).setColor(R, G, B, 1f).setNormal(nx, ny, nz);
    }

    private BlockHighlights() {}
}
