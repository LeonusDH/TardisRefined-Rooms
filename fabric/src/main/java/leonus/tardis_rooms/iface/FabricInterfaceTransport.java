package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Передача через интерфейс на Fabric — Transfer API (предметы, жидкости) и Team Reborn Energy (энергия, если установлен).
 * Виды передачи — части ({@link ItemsPart}, {@link FluidsPart}, энергия в {@link FabricEnergy}); их хранилища
 * ({@link ForwardingStorage}) — то, что видят трубы: поданное сразу уходит соседям связанных интерфейсов.
 */
public final class FabricInterfaceTransport implements InterfaceTransport {
    /** За одну операцию грани: до стопки предметов и до 16 вёдер жидкости (как на NeoForge). */
    static final long ITEMS_PER_OP = 64, FLUID_PER_OP = FluidConstants.BUCKET * 16;

    /** Части встроенных видов на Fabric по id вида (для {@link BuiltinKinds#plugin}); null — у вида здесь нет части. */
    public static TransferKind.PartFactory parts(ResourceLocation id) {
        if (id.equals(BuiltinKinds.ENERGY)) return FabricEnergy.LOADED ? FabricEnergy::newPart : null;
        if (id.equals(BuiltinKinds.ITEMS)) return ItemsPart::new;
        if (id.equals(BuiltinKinds.FLUIDS)) return FluidsPart::new;
        if (id.equals(BuiltinKinds.ME)) return AE2Compat.LOADED ? AE2Compat::newPart : null;
        return null;
    }

    /** Предметы: хранилище с пересылкой. */
    static final class ItemsPart implements KindPart {
        final ForwardingStorage<ItemVariant> storage;

        ItemsPart(InterfaceBlockEntity be, TransferKind kind) {
            storage = new ForwardingStorage<>(be, kind, ItemStorage.SIDED, peer -> peer.part(kind) instanceof ItemsPart p ? p.storage.sides : null);
        }

        @Override
        public void moveFace(Direction face, boolean pull) { storage.moveFace(face, pull, ITEMS_PER_OP); }
    }

    /** Жидкости: хранилище с пересылкой. */
    static final class FluidsPart implements KindPart {
        final ForwardingStorage<FluidVariant> storage;

        FluidsPart(InterfaceBlockEntity be, TransferKind kind) {
            storage = new ForwardingStorage<>(be, kind, FluidStorage.SIDED, peer -> peer.part(kind) instanceof FluidsPart p ? p.storage.sides : null);
        }

        @Override
        public void moveFace(Direction face, boolean pull) { storage.moveFace(face, pull, FLUID_PER_OP); }
    }

    // ---------- для поиска хранилищ (BlockApiLookup): null — сторона или вид выключены (трубы не подключатся) ----------

    public static @Nullable Storage<ItemVariant> items(InterfaceBlockEntity be, @Nullable Direction side) {
        return be.open(side) && be.part(BuiltinKinds.items()) instanceof ItemsPart p ? p.storage.storage() : null;
    }

    public static @Nullable Storage<FluidVariant> fluids(InterfaceBlockEntity be, @Nullable Direction side) {
        return be.open(side) && be.part(BuiltinKinds.fluids()) instanceof FluidsPart p ? p.storage.storage() : null;
    }

    /** Энергия интерфейса для кабелей — если установлен Team Reborn Energy. */
    public static void registerEnergy() {
        if (FabricEnergy.LOADED) FabricEnergy.register();
    }

    /** На Fabric трубы ищут хранилища интерфейса заново при каждом обращении — сообщать некому. */
    @Override
    public void invalidate() {}

    // ---------- соседи ----------

    /** Соседи блока по {@code lookup}: по BlockApiCache на сторону. */
    static <T> LinkedStorages.Sides<T> sides(InterfaceBlockEntity be, BlockApiLookup<T, Direction> lookup) {
        return new SideCaches<T, BlockApiCache<T, Direction>>(be,
                (level, pos, side) -> BlockApiCache.create(lookup, level, pos),
                (cache, side) -> cache.find(side.getOpposite()));
    }
}
