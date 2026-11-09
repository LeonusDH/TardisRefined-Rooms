package leonus.tardis_rooms.fabric;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.command.TRoomsCommands;
import leonus.tardis_rooms.iface.FabricInterfaceTransport;
import leonus.tardis_rooms.iface.tardis.TardisInterfaces;
import leonus.tardis_rooms.network.Payload;
import leonus.tardis_rooms.network.Payloads;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.registry.RegEntry;
import leonus.tardis_rooms.room.custom.CustomRooms;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;

/** Точка входа на Fabric: регистрация, конфиг, пакеты, подписки на события. */
public final class TardisRoomsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TardisInterfaces.bootstrap(FabricInterfaceTransport::parts); // до всего остального
        try {
            Files.createDirectories(Platform.configDir().resolve(TardisRooms.MODID));
        } catch (IOException e) {
            TardisRooms.LOGGER.warn("Could not create config folder: {}", e.toString());
        }
        ForgeConfigRegistry.INSTANCE.register(TardisRooms.MODID, ModConfig.Type.COMMON, TardisRoomsConfig.SPEC, TardisRooms.COMMON_CONFIG);
        ForgeConfigRegistry.INSTANCE.register(TardisRooms.MODID, ModConfig.Type.CLIENT, TardisRoomsConfig.CLIENT_SPEC, TardisRooms.CLIENT_CONFIG);
        ForgeConfigRegistry.INSTANCE.register(TardisRooms.MODID, ModConfig.Type.COMMON, TardisRoomsConfig.REMOVABLE_SPEC, TardisRooms.REMOVABLE_CONFIG);
        TardisRooms.compat("Applied Energistics 2", "ae2", TardisRoomsConfig::compatAE2);
        TardisRooms.compat("Jade", "jade", TardisRoomsConfig::compatJade);
        // Энергия на Fabric — только через Team Reborn Energy; своей настройки нет, совместимость всегда включена.
        TardisRooms.compat("Team Reborn Energy", "team_reborn_energy", () -> true);
        registerEntries();
        registerStorages();
        registerPayloads();
        addToCreativeTabs();
        FabricEvents.register();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricReloadListener(CustomRooms.ID, CustomRooms.INSTANCE));
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> CustomRooms.sync(player));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> TRoomsCommands.register(dispatcher));
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

    /** Хранилища интерфейса для труб (Transfer API) и энергия (Team Reborn Energy, если установлен). */
    private static void registerStorages() {
        var type = TRoomsBlocks.INTERFACE_BE.get();
        // С выключенной стороны трубы интерфейса не видят.
        ItemStorage.SIDED.registerForBlockEntity(FabricInterfaceTransport::items, type);
        FluidStorage.SIDED.registerForBlockEntity(FabricInterfaceTransport::fluids, type);
        FabricInterfaceTransport.registerEnergy();
        leonus.tardis_rooms.iface.AE2Compat.register();
    }

    private static final ResourceKey<CreativeModeTab> TR_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("tardis_refined", "main_tab"));

    private static void addToCreativeTabs() {
        ItemGroupEvents.modifyEntriesEvent(TR_TAB).register(entries -> entries.accept(TRoomsBlocks.INTERFACE_ITEM.get()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(TRoomsBlocks.INTERFACE_ITEM.get()));
    }

    /** Пакеты с клиента; обработчики пакетов с сервера — в TardisRoomsFabricClient. Список — общий ({@link Payloads}). */
    private static void registerPayloads() {
        for (Payloads.Entry<?> e : Payloads.ALL) if (e.toServer()) server(e);
    }

    /** Пакет с клиента: обработчик — на главном потоке сервера, с игроком, который его прислал. */
    private static <T extends Payload> void server(Payloads.Entry<T> e) {
        ServerPlayNetworking.registerGlobalReceiver(e.id(), (server, player, handler, buf, sender) -> {
            T msg = e.decode(buf);
            server.execute(() -> e.server().accept(msg, player));
        });
    }
}
