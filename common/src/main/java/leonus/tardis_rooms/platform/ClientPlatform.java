package leonus.tardis_rooms.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/** То, что у каждого загрузчика своё, но только на клиенте (отдельно от {@link Platform}, чтобы сервер не трогал клиентские классы). */
public final class ClientPlatform {
    /**
     * Нарисовать блок целиком (все слои рендера) — для 3D-превью в окнах. Цвет (вода, трава, листва) берётся
     * для настоящей позиции {@code pos} в {@code level}: без неё такие блоки вышли бы серыми.
     */
    @ExpectPlatform
    public static void renderBlock(BlockState state, BlockAndTintGetter level, BlockPos pos, PoseStack poseStack,
                                   MultiBufferSource buffers, int light, int overlay) {
        throw new AssertionError();
    }

    private ClientPlatform() {}
}
