package leonus.tardis_rooms.fabric.mixin.client;

import leonus.tardis_rooms.client.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shift + колёсико с отвёрткой — сменить её режим (на NeoForge — событие InputEvent.MouseScrollingEvent). */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void tardisRooms$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (window != Minecraft.getInstance().getWindow().getWindow()) return;
        if (ClientEvents.onScroll(yOffset)) ci.cancel();
    }
}
