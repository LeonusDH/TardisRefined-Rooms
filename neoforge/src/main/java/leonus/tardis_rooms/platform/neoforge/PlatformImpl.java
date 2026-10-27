package leonus.tardis_rooms.platform.neoforge;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/** Реализация {@link leonus.tardis_rooms.platform.Platform} для NeoForge. */
public final class PlatformImpl {
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    private PlatformImpl() {}
}
