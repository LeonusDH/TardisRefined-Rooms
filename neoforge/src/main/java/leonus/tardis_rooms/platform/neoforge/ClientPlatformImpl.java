package leonus.tardis_rooms.platform.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Реализация {@link leonus.tardis_rooms.platform.ClientPlatform} для NeoForge. */
public final class ClientPlatformImpl {
    /** Как BlockRenderDispatcher.renderSingleBlock, но цвет — для настоящей позиции блока. */
    public static void renderBlock(BlockState state, BlockAndTintGetter level, BlockPos pos, PoseStack poseStack,
                                   MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        if (state.getRenderShape() != RenderShape.MODEL) {
            dispatcher.renderSingleBlock(state, poseStack, buffers, light, overlay, ModelData.EMPTY, null);
            return;
        }
        BakedModel model = dispatcher.getBlockModel(state);
        int c = mc.getBlockColors().getColor(state, level, pos, 0);
        float r = (c >> 16 & 0xFF) / 255f, g = (c >> 8 & 0xFF) / 255f, b = (c & 0xFF) / 255f;
        for (RenderType rt : model.getRenderTypes(state, RandomSource.create(42), ModelData.EMPTY)) {
            dispatcher.getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(RenderTypeHelper.getEntityRenderType(rt, false)),
                    state, model, r, g, b, light, overlay, ModelData.EMPTY, rt);
        }
    }

    private ClientPlatformImpl() {}
}
