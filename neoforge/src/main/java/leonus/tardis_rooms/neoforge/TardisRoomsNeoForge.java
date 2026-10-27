package leonus.tardis_rooms.neoforge;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.registry.RegEntry;

import leonus.tardis_rooms.iface.AE2Compat;
import leonus.tardis_rooms.iface.MekanismCompat;
import leonus.tardis_rooms.iface.NeoForgeInterfaceTransport;
import leonus.tardis_rooms.network.CustomRoomsPayload;
import leonus.tardis_rooms.network.FitRequestPayload;
import leonus.tardis_rooms.network.FitResultPayload;
import leonus.tardis_rooms.network.GrowRoomPayload;
import leonus.tardis_rooms.network.InterfacePayload;
import leonus.tardis_rooms.network.ManagePayloads;
import leonus.tardis_rooms.network.MapPayloads;
import leonus.tardis_rooms.network.ScrewdriverModePayload;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Точка входа на NeoForge: регистрация, конфиг, подписки на события. */
@Mod(TardisRooms.MODID)
public final class TardisRoomsNeoForge {
    public TardisRoomsNeoForge(IEventBus modBus, ModContainer container) {
        try {
            Files.createDirectories(Platform.configDir().resolve(TardisRooms.MODID));
        } catch (IOException e) {
            TardisRooms.LOGGER.warn("Could not create config folder: {}", e.toString());
        }
        container.registerConfig(ModConfig.Type.COMMON, TardisRoomsConfig.SPEC, TardisRooms.COMMON_CONFIG);
        container.registerConfig(ModConfig.Type.CLIENT, TardisRoomsConfig.CLIENT_SPEC, TardisRooms.CLIENT_CONFIG);
        registerEntries(modBus);
        NeoForgeEvents.register(NeoForge.EVENT_BUS);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) leonus.tardis_rooms.neoforge.client.NeoForgeClientEvents.register();
        modBus.addListener(TardisRoomsNeoForge::registerCapabilities);
        modBus.addListener(TardisRoomsNeoForge::addToCreativeTabs);
        modBus.addListener(TardisRoomsNeoForge::registerPayloads);
        modBus.addListener(leonus.tardis_rooms.lang.TRoomsLangProvider::gather);
        TardisRooms.LOGGER.info("TARDIS Refined: Rooms loaded");
    }

    /** Всё, что объявлено через RegEntry, — в DeferredRegister своего реестра (по одному на реестр). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerEntries(IEventBus modBus) {
        TRoomsBlocks.init();
        Map<ResourceKey<?>, DeferredRegister> registers = new LinkedHashMap<>();
        for (RegEntry<?> e : RegEntry.all()) {
            DeferredRegister dr = registers.computeIfAbsent(e.registry(),
                    k -> DeferredRegister.create((ResourceKey<? extends Registry<Object>>) k, TardisRooms.MODID));
            e.bind((java.util.function.Supplier) dr.register(e.id().getPath(), (java.util.function.Supplier) e.factory()));
        }
        registers.values().forEach(dr -> dr.register(modBus));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        var type = TRoomsBlocks.INTERFACE_BE.get();
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> NeoForgeInterfaceTransport.of(be).energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> NeoForgeInterfaceTransport.of(be).items());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> NeoForgeInterfaceTransport.of(be).fluids());
        MekanismCompat.register(event);
        AE2Compat.register(event);
    }

    private static final ResourceKey<CreativeModeTab> TR_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("tardis_refined", "main_tab"));

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(TR_TAB) || event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(TRoomsBlocks.INTERFACE_ITEM.get());
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("8")
                .playToServer(GrowRoomPayload.TYPE, GrowRoomPayload.STREAM_CODEC, server(GrowRoomPayload::handle))
                .playToServer(FitRequestPayload.TYPE, FitRequestPayload.STREAM_CODEC, server(FitRequestPayload::handle))
                .playToClient(FitResultPayload.TYPE, FitResultPayload.STREAM_CODEC, client(FitResultPayload::handle))
                .playToServer(ManagePayloads.Request.TYPE, ManagePayloads.Request.STREAM_CODEC, server(ManagePayloads.Request::handle))
                .playToClient(ManagePayloads.Info.TYPE, ManagePayloads.Info.STREAM_CODEC, client(ManagePayloads.Info::handle))
                .playToServer(ManagePayloads.Remove.TYPE, ManagePayloads.Remove.STREAM_CODEC, server(ManagePayloads.Remove::handle))
                .playToServer(MapPayloads.Request.TYPE, MapPayloads.Request.STREAM_CODEC, server(MapPayloads.Request::handle))
                .playToClient(MapPayloads.Info.TYPE, MapPayloads.Info.STREAM_CODEC, client(MapPayloads.Info::handle))
                .playToClient(CustomRoomsPayload.TYPE, CustomRoomsPayload.STREAM_CODEC, client(CustomRoomsPayload::handle))
                .playToServer(InterfacePayload.TYPE, InterfacePayload.STREAM_CODEC, server(InterfacePayload::handle))
                .playToServer(ScrewdriverModePayload.TYPE, ScrewdriverModePayload.STREAM_CODEC, server(ScrewdriverModePayload::handle));
    }

    /** Обработчик пакета с клиента: на главном потоке сервера, с игроком, который его прислал. */
    private static <T extends CustomPacketPayload> IPayloadHandler<T> server(BiConsumer<T, ServerPlayer> handler) {
        return (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) handler.accept(payload, player);
        });
    }

    /** Обработчик пакета с сервера: на главном потоке клиента. */
    private static <T extends CustomPacketPayload> IPayloadHandler<T> client(Consumer<T> handler) {
        return (payload, ctx) -> ctx.enqueueWork(() -> handler.accept(payload));
    }
}
