package leonus.tardis_rooms.iface.kind;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.InterfacePlatform;
import leonus.tardis_rooms.iface.RedstonePart;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Встроенные виды передачи. Регистрируются плагином ({@link #plugin(Function)}) — как и любые другие;
 * платформа даёт части видов, остальное (id, порядок, что включено по умолчанию, когда доступно, редстоун) — здесь.
 * Химикаты — с Mekanism, сеть ME — с Applied Energistics 2.
 */
public final class BuiltinKinds {
    public static final ResourceLocation ENERGY = id("energy"), ITEMS = id("items"), FLUIDS = id("fluids"),
            CHEMICALS = id("chemicals"), ME = id("me"), REDSTONE = id("redstone");

    private static TransferKind energy, items, fluids, chemicals, me, redstone;

    private BuiltinKinds() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, path);
    }

    /**
     * Плагин встроенных видов. {@code parts} — части видов на этой платформе по id (null — у вида нет части здесь).
     * Новый интерфейс передаёт всё, кроме сети ME (её связь меняет устройство сетей) и редстоуна.
     */
    public static KindPlugin plugin(Function<ResourceLocation, TransferKind.PartFactory> parts) {
        return reg -> {
            reg.kind(ENERGY).enabledByDefault().name(() -> TRoomsText.IFACE_ENERGY.text())
                    .tooltip(BuiltinKinds::storageTooltip).parts(parts.apply(ENERGY)).register();
            reg.kind(ITEMS).enabledByDefault().name(() -> TRoomsText.IFACE_ITEMS.text())
                    .tooltip(BuiltinKinds::storageTooltip).parts(parts.apply(ITEMS)).register();
            reg.kind(FLUIDS).enabledByDefault().name(() -> TRoomsText.IFACE_FLUIDS.text())
                    .tooltip(BuiltinKinds::storageTooltip).parts(parts.apply(FLUIDS)).register();
            reg.kind(CHEMICALS).enabledByDefault().available(InterfacePlatform::chemicalsAvailable).name(() -> TRoomsText.IFACE_CHEMICALS.text())
                    .tooltip(BuiltinKinds::storageTooltip).parts(parts.apply(CHEMICALS)).register();
            reg.kind(ME).available(InterfacePlatform::meAvailable).name(() -> TRoomsText.IFACE_ME.text())
                    .tooltip(() -> TRoomsText.IFACE_ME_TOOLTIP.text()).parts(parts.apply(ME)).register();
            // Редстоун — один на все платформы.
            reg.kind(REDSTONE).name(() -> TRoomsText.IFACE_REDSTONE.text())
                    .tooltip(() -> TRoomsText.IFACE_REDSTONE_TOOLTIP.text()).parts(RedstonePart::new).register();
        };
    }

    /** Пояснение у видов хранилищ: энергии, предметов, жидкостей, химикатов. */
    private static Component storageTooltip() { return TRoomsText.IFACE_TYPE_TOOLTIP.text(); }

    /** Ссылки на встроенные виды из реестра игры (вызывает {@link TransferKinds#bootstrap}). */
    static void bind(KindRegistry r) {
        energy = r.byId(ENERGY);
        items = r.byId(ITEMS);
        fluids = r.byId(FLUIDS);
        chemicals = r.byId(CHEMICALS);
        me = r.byId(ME);
        redstone = r.byId(REDSTONE);
    }

    public static TransferKind energy() { return require(energy); }

    public static TransferKind items() { return require(items); }

    public static TransferKind fluids() { return require(fluids); }

    public static TransferKind chemicals() { return require(chemicals); }

    public static TransferKind me() { return require(me); }

    public static TransferKind redstone() { return require(redstone); }

    private static TransferKind require(@Nullable TransferKind k) {
        if (k == null) throw new IllegalStateException("Built-in transfer kind is not registered");
        return k;
    }
}
