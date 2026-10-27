package leonus.tardis_rooms.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.network.FitRequestPayload;
import leonus.tardis_rooms.network.FitResultPayload;
import leonus.tardis_rooms.network.GrowRoomPayload;
import leonus.tardis_rooms.network.InterfacePayload;
import leonus.tardis_rooms.network.ManagePayloads;
import leonus.tardis_rooms.network.MapPayloads;
import leonus.tardis_rooms.network.ScrewdriverModePayload;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.registry.RegEntry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiConsumer;

/** Точка входа на Fabric: регистрация, конфиг, пакеты, подписки на события. */
public final class TardisRoomsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        try {
            Files.createDirectories(Platform.configDir().resolve(TardisRooms.MODID));
        } catch (IOException e) {
            TardisRooms.LOGGER.warn("Could not create config folder: {}", e.toString());
        }
        NeoForgeConfigRegistry.INSTANCE.register(TardisRooms.MODID, ModConfig.Type.COMMON, TardisRoomsConfig.SPEC, TardisRooms.COMMON_CONFIG);
        NeoForgeConfigRegistry.INSTANCE.register(TardisRooms.MODID, ModConfig.Type.CLIENT, TardisRoomsConfig.CLIENT_SPEC, TardisRooms.CLIENT_CONFIG);
        registerEntries();
        registerPayloads();
        addToCreativeTabs();
        FabricEvents.register();
        TardisRooms.LOGGER.info("TARDIS Refined: Rooms loaded");
    }

    /** Всё, что объявлено через RegEntry, — сразу в реестры, в порядке объявления (блоки раньше своих предметов). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerEntries() {
        TRoomsBlocks.init();
        for (RegEntry<?> e : RegEntry.all()) {
            Registry registry = BuiltInRegistries.REGISTRY.get((ResourceKey) e.registry());
            if (registry == null) throw new IllegalStateException("Unknown registry " + e.registry().location());
            Object value = Registry.register(registry, e.id(), e.factory().get());
            e.bind((java.util.function.Supplier) () -> value);
        }
    }

    private static final ResourceKey<CreativeModeTab> TR_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("tardis_refined", "main_tab"));

    private static void addToCreativeTabs() {
        ItemGroupEvents.modifyEntriesEvent(TR_TAB).register(entries -> entries.accept(TRoomsBlocks.INTERFACE_ITEM.get()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(TRoomsBlocks.INTERFACE_ITEM.get()));
    }

    /** Типы пакетов регистрируются на обеих сторонах; обработчики с клиента — здесь, с сервера — в TardisRoomsFabricClient. */
    private static void registerPayloads() {
        server(GrowRoomPayload.TYPE, GrowRoomPayload.STREAM_CODEC, GrowRoomPayload::handle);
        server(FitRequestPayload.TYPE, FitRequestPayload.STREAM_CODEC, FitRequestPayload::handle);
        server(ManagePayloads.Request.TYPE, ManagePayloads.Request.STREAM_CODEC, ManagePayloads.Request::handle);
        server(ManagePayloads.Remove.TYPE, ManagePayloads.Remove.STREAM_CODEC, ManagePayloads.Remove::handle);
        server(MapPayloads.Request.TYPE, MapPayloads.Request.STREAM_CODEC, MapPayloads.Request::handle);
        server(InterfacePayload.TYPE, InterfacePayload.STREAM_CODEC, InterfacePayload::handle);
        server(ScrewdriverModePayload.TYPE, ScrewdriverModePayload.STREAM_CODEC, ScrewdriverModePayload::handle);
        PayloadTypeRegistry.playS2C().register(FitResultPayload.TYPE, FitResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ManagePayloads.Info.TYPE, ManagePayloads.Info.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MapPayloads.Info.TYPE, MapPayloads.Info.STREAM_CODEC);
    }

    /** Пакет с клиента: обработчик — на главном потоке сервера, с игроком, который его прислал. */
    private static <T extends CustomPacketPayload> void server(CustomPacketPayload.Type<T> type,
                                                                StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                BiConsumer<T, ServerPlayer> handler) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            player.server.execute(() -> handler.accept(payload, player));
        });
    }
}
