package leonus.tardis_rooms.client;

import com.mojang.blaze3d.platform.NativeImage;
import leonus.tardis_rooms.room.custom.CustomRoomInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

/** Помещения из датапаков, присланные сервером, и их картинки превью (собираются здесь из цветов карты). */
public final class ClientCustomRooms {
    private static List<CustomRoomInfo> rooms = List.of();
    private static final List<ResourceLocation> textures = new ArrayList<>();

    public static List<CustomRoomInfo> list() {
        return rooms;
    }

    /** Главный поток клиента. Заменяет прежний список и его текстуры. */
    public static void accept(List<CustomRoomInfo> list) {
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation t : textures) tm.release(t);
        textures.clear();
        for (CustomRoomInfo r : list) {
            int w = r.widthBlocks(), d = r.depthBlocks();
            if (r.preview().length != w * d) continue;
            NativeImage img = new NativeImage(w, d, true);
            for (int z = 0; z < d; z++)
                for (int x = 0; x < w; x++)
                    img.setPixelRGBA(x, z, MapColor.getColorFromPackedId(r.preview()[z * w + x] & 0xFF));
            ResourceLocation id = r.previewTexture();
            tm.register(id, new DynamicTexture(img));
            textures.add(id);
        }
        rooms = List.copyOf(list);
    }

    private ClientCustomRooms() {}
}
