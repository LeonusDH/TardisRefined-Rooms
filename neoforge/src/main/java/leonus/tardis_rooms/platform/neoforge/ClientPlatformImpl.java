package leonus.tardis_rooms.platform.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Реализация {@link leonus.tardis_rooms.platform.ClientPlatform} для NeoForge. */
public final class ClientPlatformImpl {
    public static void renderBlock(BlockState state, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        // Вариант NeoForge: без данных модели и со всеми слоями рендера (null) — как устаревший ванильный.
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffers, light, overlay, ModelData.EMPTY, null);
    }

    private ClientPlatformImpl() {}
}
