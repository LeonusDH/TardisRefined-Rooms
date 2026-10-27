package leonus.tardis_rooms.platform.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;

/** Реализация {@link leonus.tardis_rooms.platform.ClientPlatform} для Fabric. */
public final class ClientPlatformImpl {
    public static void renderBlock(BlockState state, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffers, light, overlay);
    }

    private ClientPlatformImpl() {}
}
