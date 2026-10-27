package leonus.tardis_rooms.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;

/** То, что у каждого загрузчика своё, но только на клиенте (отдельно от {@link Platform}, чтобы сервер не трогал клиентские классы). */
public final class ClientPlatform {
    /** Нарисовать блок целиком, как в мире (все слои рендера), — для 3D-превью в окнах. */
    @ExpectPlatform
    public static void renderBlock(BlockState state, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        throw new AssertionError();
    }

    private ClientPlatform() {}
}
