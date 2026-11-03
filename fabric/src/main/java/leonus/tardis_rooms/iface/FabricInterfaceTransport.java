package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.function.Function;

/**
 * Передача через интерфейс на Fabric — Transfer API (предметы, жидкости) и Team Reborn Energy (энергия, если установлен).
 * Хранилища интерфейса ({@link ForwardingStorage}, {@link FabricEnergy}) — то, что видят трубы; поданное в них сразу уходит
 * соседям связанных интерфейсов.
 */
public final class FabricInterfaceTransport implements InterfaceTransport {
    /** За одну операцию грани: до стопки предметов и до 16 вёдер жидкости (как на NeoForge). */
    static final long ITEMS_PER_OP = 64, FLUID_PER_OP = FluidConstants.BUCKET * 16;

    final InterfaceBlockEntity be;
    private final ForwardingStorage<ItemVariant> items;
    private final ForwardingStorage<FluidVariant> fluids;
    private Object energy;

    public FabricInterfaceTransport(InterfaceBlockEntity be) {
        this.be = be;
        this.items = new ForwardingStorage<>(this, TransferType.ITEMS, ItemStorage.SIDED, t -> t.items);
        this.fluids = new ForwardingStorage<>(this, TransferType.FLUIDS, FluidStorage.SIDED, t -> t.fluids);
    }

    public static FabricInterfaceTransport of(InterfaceBlockEntity be) {
        return (FabricInterfaceTransport) be.transport();
    }

    // ---------- для поиска хранилищ (BlockApiLookup) ----------

    /** Хранилища интерфейса: null — вид передачи выключен (трубы не будут подключаться). */
    public Storage<ItemVariant> items() { return be.enabled(TransferType.ITEMS) ? items : null; }

    public Storage<FluidVariant> fluids() { return be.enabled(TransferType.FLUIDS) ? fluids : null; }

    /** Энергия (Team Reborn Energy) или null — выключена или мода нет. */
    public Object energy() {
        if (!FabricEnergy.LOADED || !be.enabled(TransferType.ENERGY)) return null;
        if (energy == null) energy = FabricEnergy.view(this);
        return energy;
    }

    /** Энергия интерфейса для кабелей — если установлен Team Reborn Energy. */
    public static void registerEnergy() {
        if (FabricEnergy.LOADED) FabricEnergy.register();
    }

    /** На Fabric трубы ищут хранилища интерфейса заново при каждом обращении — сообщать некому. */
    @Override
    public void invalidate() {}

    @Override
    public void tick() {}

    @Override
    public void remove() {}

    // ---------- грани «вставлять» / «забирать» ----------

    @Override
    public void moveFace(BlockPos np, Direction side, boolean pull) {
        Direction face = side.getOpposite(); // своя сторона, к которой примыкает сосед
        if (be.enabled(TransferType.ENERGY) && FabricEnergy.LOADED) FabricEnergy.moveFace(this, face, pull);
        if (be.enabled(TransferType.ITEMS)) {
            Storage<ItemVariant> h = items.sides.at(face);
            if (h != null) move(h, items, pull, ITEMS_PER_OP);
        }
        if (be.enabled(TransferType.FLUIDS)) {
            Storage<FluidVariant> h = fluids.sides.at(face);
            if (h != null) move(h, fluids, pull, FLUID_PER_OP);
        }
    }

    /** «Забирать» — из соседа в своё хранилище (то есть дальше к выходам), «вставлять» — из своего (от входов) в соседа. */
    private static <T> void move(Storage<T> neighbour, Storage<T> mine, boolean pull, long max) {
        if (pull) StorageUtil.move(neighbour, mine, v -> true, max, null);
        else StorageUtil.move(mine, neighbour, v -> true, max, null);
    }

    // ---------- соседи связанных интерфейсов ----------

    /** Хранилища вида {@code type} у соседей связанных интерфейсов; {@code sidesOf} — где у транспорта его соседи этого вида. */
    <T> List<T> linked(TransferType type, Pick pick, Function<FabricInterfaceTransport, LinkedStorages.Sides<T>> sidesOf) {
        return LinkedStorages.collect(be, type, pick, peer -> sidesOf.apply(of(peer)));
    }
}
