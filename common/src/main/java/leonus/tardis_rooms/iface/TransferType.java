package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TardisRooms;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Что может передавать интерфейс. Химикаты — с Mekanism, сеть ME — с Applied Energistics 2.
 * В сохранениях и по сети вид — это его {@link #id()}; номер ({@link #bit()}) живёт только в памяти.
 */
public enum TransferType {
    ENERGY("energy"), ITEMS("items"), FLUIDS("fluids"), CHEMICALS("chemicals"), ME("me"), REDSTONE("redstone");

    private final ResourceLocation id;

    TransferType(String name) { this.id = ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, name); }

    public ResourceLocation id() { return id; }

    /** Бит вида в памяти ({@link InterfaceData#types()}). Не сохраняется и по сети не передаётся. */
    public int bit() { return 1 << ordinal(); }

    /** Вид по id; null — такого вида в этой игре нет. */
    public static @Nullable TransferType byId(ResourceLocation id) {
        for (TransferType t : values()) if (t.id.equals(id)) return t;
        return null;
    }

    /** Установлен ли мод, без которого этот вид передачи не имеет смысла. */
    public boolean available() {
        return switch (this) {
            case CHEMICALS -> InterfacePlatform.chemicalsAvailable();
            case ME -> InterfacePlatform.meAvailable();
            default -> true;
        };
    }
}
