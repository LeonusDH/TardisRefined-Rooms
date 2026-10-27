package leonus.tardis_rooms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Отрисовка блока для 3D-превью в окнах. Платформы отдают грани модели ({@code ClientPlatform.renderBlock}), окраска — здесь. */
public final class BlockPreview {
    /**
     * Грани модели с окраской, как при отрисовке мира: цвет — для настоящей позиции блока и по индексу окраски каждой грани.
     * Ванильная отрисовка «одного блока» берёт цвет индекса 0 для всех граней, а моды красят и другими индексами
     * (жидкость в котле Amendments — индексом 1), поэтому там она выходила бы серой.
     */
    public static void putQuads(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, BlockState state,
                                BlockAndTintGetter level, BlockPos pos, int light, int overlay) {
        BlockColors colors = Minecraft.getInstance().getBlockColors();
        int tint = Integer.MIN_VALUE;
        float r = 1f, g = 1f, b = 1f;
        for (BakedQuad quad : quads) {
            if (!quad.isTinted()) {
                consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, light, overlay);
                continue;
            }
            if (quad.getTintIndex() != tint) {
                tint = quad.getTintIndex();
                int c = colors.getColor(state, level, pos, tint);
                r = (c >> 16 & 0xFF) / 255f;
                g = (c >> 8 & 0xFF) / 255f;
                b = (c & 0xFF) / 255f;
            }
            consumer.putBulkData(pose, quad, r, g, b, 1f, light, overlay);
        }
    }

    private BlockPreview() {}
}
