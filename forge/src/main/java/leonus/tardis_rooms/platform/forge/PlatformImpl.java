package leonus.tardis_rooms.platform.forge;

import leonus.tardis_rooms.forge.ForgeNetwork;
import leonus.tardis_rooms.network.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.BiFunction;

/** Реализация {@link leonus.tardis_rooms.platform.Platform} для Forge. */
public final class PlatformImpl {
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static void sendToServer(Payload payload) {
        ForgeNetwork.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        ForgeNetwork.sendToPlayer(player, payload);
    }

    public static SoundType soundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        return state.getSoundType(level, pos, entity);
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
    }

    private PlatformImpl() {}
}
