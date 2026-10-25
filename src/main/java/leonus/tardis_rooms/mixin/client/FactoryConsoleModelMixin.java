package leonus.tardis_rooms.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jeryn.frame.tardis.Frame;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.client.ClientDoorTracker;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import whocraft.tardis_refined.client.model.blockentity.console.FactoryConsoleModel;
import whocraft.tardis_refined.common.blockentity.console.GlobalConsoleBlockEntity;

/**
 * У консоли Factory рычаг двери (деталь модели "lever" на северной панели, ровно там же,
 * где кнопка DOOR_TOGGLE) нигде не анимируется. Двигаем его по состоянию двери прямо перед отрисовкой.
 */
@Mixin(value = FactoryConsoleModel.class, remap = false)
public abstract class FactoryConsoleModelMixin {
    @Unique
    private ModelPart tardisRooms$doorLever;

    @Inject(method = "renderConsole",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void tardisRooms$animateDoorLever(GlobalConsoleBlockEntity console, float partialTick, Level level, PoseStack poseStack,
                                             VertexConsumer consumer, int light, int overlay, int color, CallbackInfo ci) {
        if (console == null || level == null) return;
        double angle = TardisRoomsConfig.doorLeverClosedAngle();
        if (angle == 0) return;
        if (tardisRooms$doorLever == null) {
            tardisRooms$doorLever = Frame.findPart((HierarchicalModel<?>) (Object) this, "lever");
        }
        Boolean open = ClientDoorTracker.isDoorOpen(level, console.getBlockPos());
        if (open == null || open) return; // открыта: рычаг в исходном положении модели
        ModelPart lever = tardisRooms$doorLever;
        lever.xRot = lever.getInitialPose().xRot + (float) Math.toRadians(angle);
    }
}
