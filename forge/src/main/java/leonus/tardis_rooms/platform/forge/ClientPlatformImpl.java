package leonus.tardis_rooms.platform.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import leonus.tardis_rooms.client.BlockPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;

/** Реализация {@link leonus.tardis_rooms.platform.ClientPlatform} для Forge. */
public final class ClientPlatformImpl {
    private static final Direction[] SIDES_AND_NONE = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

    /**
     * Как BlockRenderDispatcher.renderSingleBlock, но с данными модели для настоящей позиции блока (их берут модели, которые
     * рисуют содержимое блока-сущности, — котёл с жидкостью из Amendments) и с окраской, как в мире ({@link BlockPreview}).
     */
    public static void renderBlock(BlockState state, BlockAndTintGetter level, BlockPos pos, PoseStack poseStack,
                                   MultiBufferSource buffers, int light, int overlay) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        if (state.getRenderShape() != RenderShape.MODEL) {
            dispatcher.renderSingleBlock(state, poseStack, buffers, light, overlay, ModelData.EMPTY, null);
            return;
        }
        BakedModel model = dispatcher.getBlockModel(state);
        var manager = level.getModelDataManager();
        ModelData beData = manager != null ? manager.getAt(pos) : null;
        ModelData data = model.getModelData(level, pos, state, beData != null ? beData : ModelData.EMPTY);
        RandomSource random = RandomSource.create();
        for (RenderType rt : model.getRenderTypes(state, RandomSource.create(42), data)) {
            VertexConsumer consumer = buffers.getBuffer(RenderTypeHelper.getEntityRenderType(rt, false));
            for (Direction side : SIDES_AND_NONE) {
                random.setSeed(42L);
                BlockPreview.putQuads(poseStack.last(), consumer, model.getQuads(state, side, random, data, rt), state, level, pos, light, overlay);
            }
        }
    }

    private ClientPlatformImpl() {}
}
