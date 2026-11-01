package leonus.tardis_rooms.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Квадратная кнопка со значком. На 1.20.1 нет {@code SpriteIconButton} и атласа спрайтов интерфейса:
 * рисуем обычную кнопку, а поверх — значок из {@code textures/gui/sprites/<путь>.png} (те же картинки, что в основной ветке).
 */
public class IconButton extends Button {
    private final ResourceLocation texture;

    public IconButton(int size, ResourceLocation sprite, Component message, OnPress onPress) {
        super(0, 0, size, size, message, onPress, DEFAULT_NARRATION);
        this.texture = new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(g, mouseX, mouseY, partialTick);
        g.blit(texture, getX(), getY(), 0, 0, width, height, width, height);
    }

    /** Только значок, без подписи (подпись — в подсказке). */
    @Override
    public void renderString(GuiGraphics g, Font font, int color) {}
}
