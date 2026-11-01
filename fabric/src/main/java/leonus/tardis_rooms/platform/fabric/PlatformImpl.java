package leonus.tardis_rooms.platform.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import leonus.tardis_rooms.network.Payload;
import leonus.tardis_rooms.network.Payloads;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;
import java.util.function.BiFunction;

/** Реализация {@link leonus.tardis_rooms.platform.Platform} для Fabric. */
public final class PlatformImpl {
    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void sendToServer(Payload payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        Payloads.encode(payload, buf);
        ClientPlayNetworking.send(payload.type().id(), buf);
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        Payloads.encode(payload, buf);
        ServerPlayNetworking.send(player, payload.type().id(), buf);
    }

    /** У Fabric нет звука с учётом мира — обычный звук блока (как у ванили). */
    @SuppressWarnings("deprecation")
    public static SoundType soundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        return state.getSoundType();
    }

    @SuppressWarnings("deprecation")
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }

    private PlatformImpl() {}
}
