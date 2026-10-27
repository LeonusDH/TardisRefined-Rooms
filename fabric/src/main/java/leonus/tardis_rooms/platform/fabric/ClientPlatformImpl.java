package leonus.tardis_rooms.platform.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Реализация {@link leonus.tardis_rooms.platform.ClientPlatform} для Fabric. */
public final class ClientPlatformImpl {
    /** Как BlockRenderDispatcher.renderSingleBlock, но цвет — для настоящей позиции блока. */
    public static void renderBlock(BlockState state, BlockAndTintGetter level, BlockPos pos, PoseStack poseStack,
                                   MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        if (state.getRenderShape() != RenderShape.MODEL) {
            dispatcher.renderSingleBlock(state, poseStack, buffers, light, overlay);
            return;
        }
        BakedModel model = dispatcher.getBlockModel(state);
        int c = mc.getBlockColors().getColor(state, level, pos, 0);
        float r = (c >> 16 & 0xFF) / 255f, g = (c >> 8 & 0xFF) / 255f, b = (c & 0xFF) / 255f;
        dispatcher.getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)),
                state, model, r, g, b, light, overlay);
    }

    private ClientPlatformImpl() {}
}
