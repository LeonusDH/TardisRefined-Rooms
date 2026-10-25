package leonus.tardis_rooms;

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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

@Mod(TardisRooms.MODID)
public final class TardisRooms {
    public static final String MODID = "tardis_rooms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public TardisRooms(IEventBus modBus, ModContainer container) {
        try {
            Files.createDirectories(FMLPaths.CONFIGDIR.get().resolve(MODID));
        } catch (IOException e) {
            LOGGER.warn("Could not create config folder: {}", e.toString());
        }
        container.registerConfig(ModConfig.Type.COMMON, TardisRoomsConfig.SPEC, COMMON_CONFIG);
        container.registerConfig(ModConfig.Type.CLIENT, TardisRoomsConfig.CLIENT_SPEC, CLIENT_CONFIG);
        TRoomsBlocks.BLOCKS.register(modBus);
        TRoomsBlocks.ITEMS.register(modBus);
        TRoomsBlocks.BLOCK_ENTITIES.register(modBus);
        TRoomsBlocks.COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(InterfaceTools::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(InterfaceTools::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(InterfaceTools::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(InterfaceTools::onTooltip);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) leonus.tardis_rooms.client.ClientEvents.register();
        modBus.addListener(TardisRooms::registerCapabilities);
        modBus.addListener(TardisRooms::addToCreativeTabs);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> InterfaceNetwork.clear());
        modBus.addListener(TardisRooms::registerPayloads);
        modBus.addListener(leonus.tardis_rooms.lang.TRoomsLangProvider::gather);
        HubInitializer.register(NeoForge.EVENT_BUS);
        LOGGER.info("TARDIS Refined: Rooms loaded");
    }

    /** Конфиги лежат в своей папке: config/tardis_rooms/. */
    private static final String COMMON_CONFIG = MODID + "/common.toml";
    private static final String CLIENT_CONFIG = MODID + "/client.toml";

    /** Отладочная запись в лог — только при debug = true в config/tardis_rooms/common.toml. */
    public static void debug(String message, Object... args) {
        if (TardisRoomsConfig.debug()) LOGGER.info("[debug] " + message, args);
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
