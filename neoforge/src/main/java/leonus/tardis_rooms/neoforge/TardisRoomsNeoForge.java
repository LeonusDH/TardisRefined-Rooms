package leonus.tardis_rooms.neoforge;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.registry.RegEntry;

import leonus.tardis_rooms.iface.AE2Compat;
import leonus.tardis_rooms.iface.InterfaceNetwork;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.iface.MekanismCompat;
import leonus.tardis_rooms.network.FitRequestPayload;
import leonus.tardis_rooms.network.FitResultPayload;
import leonus.tardis_rooms.network.GrowRoomPayload;
import leonus.tardis_rooms.network.InterfacePayload;
import leonus.tardis_rooms.network.ManagePayloads;
import leonus.tardis_rooms.network.MapPayloads;
import leonus.tardis_rooms.network.ScrewdriverModePayload;
import leonus.tardis_rooms.room.HubInitializer;
import net.minecraft.core.Registry;
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
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

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
        NeoForge.EVENT_BUS.addListener(InterfaceTools::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(InterfaceTools::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(InterfaceTools::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(InterfaceTools::onTooltip);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) leonus.tardis_rooms.client.ClientEvents.register();
        modBus.addListener(TardisRoomsNeoForge::registerCapabilities);
        modBus.addListener(TardisRoomsNeoForge::addToCreativeTabs);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> InterfaceNetwork.clear());
        modBus.addListener(TardisRoomsNeoForge::registerPayloads);
        modBus.addListener(leonus.tardis_rooms.lang.TRoomsLangProvider::gather);
        HubInitializer.register(NeoForge.EVENT_BUS);
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
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.items());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> be.fluids());
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
        event.registrar("7")
                .playToServer(GrowRoomPayload.TYPE, GrowRoomPayload.STREAM_CODEC, GrowRoomPayload::handle)
                .playToServer(FitRequestPayload.TYPE, FitRequestPayload.STREAM_CODEC, FitRequestPayload::handle)
                .playToClient(FitResultPayload.TYPE, FitResultPayload.STREAM_CODEC, FitResultPayload::handle)
                .playToServer(ManagePayloads.Request.TYPE, ManagePayloads.Request.STREAM_CODEC, ManagePayloads.Request::handle)
                .playToClient(ManagePayloads.Info.TYPE, ManagePayloads.Info.STREAM_CODEC, ManagePayloads.Info::handle)
                .playToServer(ManagePayloads.Remove.TYPE, ManagePayloads.Remove.STREAM_CODEC, ManagePayloads.Remove::handle)
                .playToServer(MapPayloads.Request.TYPE, MapPayloads.Request.STREAM_CODEC, MapPayloads.Request::handle)
                .playToClient(MapPayloads.Info.TYPE, MapPayloads.Info.STREAM_CODEC, MapPayloads.Info::handle)
                .playToServer(InterfacePayload.TYPE, InterfacePayload.STREAM_CODEC, InterfacePayload::handle)
                .playToServer(ScrewdriverModePayload.TYPE, ScrewdriverModePayload.STREAM_CODEC, ScrewdriverModePayload::handle);
    }
}
