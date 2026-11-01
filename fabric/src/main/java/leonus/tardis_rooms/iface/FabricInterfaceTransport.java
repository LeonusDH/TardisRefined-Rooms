package leonus.tardis_rooms.iface;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Передача через интерфейс на Fabric — Transfer API (предметы, жидкости) и Team Reborn Energy (энергия, если установлен).
 * Хранилища интерфейса ({@link ForwardingStorage}, {@link FabricEnergy}) — то, что видят трубы; поданное в них сразу уходит
 * соседям связанных интерфейсов.
 */
public final class FabricInterfaceTransport implements InterfaceTransport {
    /** За одну операцию грани: до стопки предметов и до 16 вёдер жидкости (как на NeoForge). */
    static final long ITEMS_PER_OP = 64, FLUID_PER_OP = FluidConstants.BUCKET * 16;

    final InterfaceBlockEntity be;
    /** Хранилища у соседей связанных интерфейсов (общая часть, см. {@link LinkedStorages}). */
    private final LinkedStorages storages;
    private final ForwardingStorage<ItemVariant> items;
    private final ForwardingStorage<FluidVariant> fluids;
    private Object energy;

    public FabricInterfaceTransport(InterfaceBlockEntity be) {
        this.be = be;
        this.storages = new LinkedStorages(be);
        this.items = new ForwardingStorage<>(this, TransferType.ITEMS, ItemStorage.SIDED);
        this.fluids = new ForwardingStorage<>(this, TransferType.FLUIDS, FluidStorage.SIDED);
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

    /** На Fabric хранилища ищутся заново при каждом обращении — сообщать некому. */
    @Override
    public void invalidate() {}

    @Override
    public void tick() {}

    @Override
    public void remove() {}

    // ---------- грани «вставлять» / «забирать» ----------

    @Override
    public void moveFace(BlockPos np, Direction side, boolean pull) {
        Level level = be.getLevel();
        if (be.enabled(TransferType.ENERGY) && FabricEnergy.LOADED) FabricEnergy.moveFace(this, level, np, side, pull);
        if (be.enabled(TransferType.ITEMS)) {
            Storage<ItemVariant> h = ItemStorage.SIDED.find(level, np, side);
            if (h != null) move(h, items, pull, ITEMS_PER_OP);
        }
        if (be.enabled(TransferType.FLUIDS)) {
            Storage<FluidVariant> h = FluidStorage.SIDED.find(level, np, side);
            if (h != null) move(h, fluids, pull, FLUID_PER_OP);
        }
    }

    /** «Забирать» — из соседа в своё хранилище (то есть дальше к выходам), «вставлять» — из своего (от входов) в соседа. */
    private static <T> void move(Storage<T> neighbour, Storage<T> mine, boolean pull, long max) {
        if (pull) StorageUtil.move(neighbour, mine, v -> true, max, null);
        else StorageUtil.move(mine, neighbour, v -> true, max, null);
    }

    // ---------- соседи связанных интерфейсов ----------

    /** Хранилища соседей связанных интерфейсов: {@code fromInputs} — у интерфейсов со входом (откуда забирать), иначе — с выходом (куда отдавать). */
    <T> List<T> handlers(BlockApiLookup<T, Direction> lookup, TransferType type, boolean fromInputs) {
        return storages.get(lookup, lookup::find, type, fromInputs ? LinkedStorages.Pick.FROM : LinkedStorages.Pick.TO);
    }

    /** Хранилища соседей всех связанных интерфейсов, каждое один раз: сколько всего в них лежит (для подсказки). */
    <T> List<T> linked(BlockApiLookup<T, Direction> lookup, TransferType type) {
        return storages.get(lookup, lookup::find, type, LinkedStorages.Pick.ALL);
    }
}
