package leonus.tardis_rooms.forge;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.TardisRoomsConfig;
import leonus.tardis_rooms.iface.ForgeInterfaceTransport;
import leonus.tardis_rooms.iface.tardis.TardisInterfaces;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.registry.RegEntry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** Точка входа на Forge: регистрация, конфиг, подписки на события. */
@Mod(TardisRooms.MODID)
public final class TardisRoomsForge {
    // ModLoadingContext.get() и FMLJavaModLoadingContext.get() в Forge 47.4 помечены «к удалению», но замена (контекст в конструкторе)
    // есть не во всех сборках Forge 47 — оставляем старый способ, чтобы аддон работал на любой из них.
    @SuppressWarnings("removal")
    public TardisRoomsForge() {
        TardisInterfaces.bootstrap(ForgeInterfaceTransport::parts); // до всего остального
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        try {
            Files.createDirectories(Platform.configDir().resolve(TardisRooms.MODID));
        } catch (IOException e) {
            TardisRooms.LOGGER.warn("Could not create config folder: {}", e.toString());
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TardisRoomsConfig.SPEC, TardisRooms.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TardisRoomsConfig.CLIENT_SPEC, TardisRooms.CLIENT_CONFIG);
        registerEntries(modBus);
        ForgeNetwork.register();
        ForgeEvents.register(MinecraftForge.EVENT_BUS);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ForgeInterfaceTransport::attach);
        if (FMLEnvironment.dist.isClient()) leonus.tardis_rooms.forge.client.ForgeClientEvents.register();
        modBus.addListener(TardisRoomsForge::addToCreativeTabs);
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

    private static final ResourceKey<CreativeModeTab> TR_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.tryBuild("tardis_refined", "main_tab"));

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(TR_TAB) || event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(TRoomsBlocks.INTERFACE_ITEM.get());
        }
    }
}
