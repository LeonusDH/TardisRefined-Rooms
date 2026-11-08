package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.TransferKind;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Передача через интерфейс на NeoForge — capabilities. Виды передачи — части ({@link EnergyView}, {@link ItemView},
 * {@link FluidView}, химикаты Mekanism): то, что видят трубы; поданное в них сразу уходит соседям связанных интерфейсов.
 * Здесь — общее для всех видов: сообщить о смене возможностей и сеть ME.
 */
public final class NeoForgeInterfaceTransport implements InterfaceTransport {
    /** До стольких мБ жидкости или химиката за одну операцию грани. */
    static final int FLUID_PER_OP = 16000;

    private final InterfaceBlockEntity be;

    public NeoForgeInterfaceTransport(InterfaceBlockEntity be) {
        this.be = be;
    }

    /** Части встроенных видов на NeoForge по id вида (для {@link BuiltinKinds#plugin}); null — у вида здесь нет части. */
    public static TransferKind.PartFactory parts(ResourceLocation id) {
        if (id.equals(BuiltinKinds.ENERGY)) return EnergyView::new;
        if (id.equals(BuiltinKinds.ITEMS)) return ItemView::new;
        if (id.equals(BuiltinKinds.FLUIDS)) return FluidView::new;
        if (id.equals(BuiltinKinds.CHEMICALS)) return MekanismCompat.LOADED ? MekanismCompat::newPart : null;
        return null;
    }

    // ---------- для возможностей блока: null — сторона выключена или вид выключен (трубы не будут подключаться) ----------

    public static @Nullable IEnergyStorage energy(InterfaceBlockEntity be, @Nullable Direction side) {
        return be.open(side) && be.part(BuiltinKinds.energy()) instanceof EnergyView v ? v.capability() : null;
    }

    public static @Nullable IItemHandler items(InterfaceBlockEntity be, @Nullable Direction side) {
        return be.open(side) && be.part(BuiltinKinds.items()) instanceof ItemView v ? v.capability() : null;
    }

    public static @Nullable IFluidHandler fluids(InterfaceBlockEntity be, @Nullable Direction side) {
        return be.open(side) && be.part(BuiltinKinds.fluids()) instanceof FluidView v ? v.capability() : null;
    }

    @Override
    public void invalidate() {
        Level level = be.getLevel();
        if (level != null) level.invalidateCapabilities(be.getBlockPos());
        AE2Compat.update(be); // стороны, открытые для кабелей ME, — сразу, а не на ближайшем секундном такте
    }

    @Override
    public void tick() {
        AE2Compat.update(be);
    }

    @Override
    public void remove() {
        AE2Compat.destroy(be);
    }

    // ---------- соседи ----------

    /** Соседи блока по возможности {@code cap}: по BlockCapabilityCache на сторону; кэш живёт, пока блок не убран. */
    static <T> LinkedStorages.Sides<T> sides(InterfaceBlockEntity be, BlockCapability<T, Direction> cap) {
        return new SideCaches<T, BlockCapabilityCache<T, Direction>>(be,
                (level, pos, side) -> BlockCapabilityCache.create(cap, level, pos, side.getOpposite(), () -> !be.isRemoved(), () -> {}),
                (cache, side) -> cache.getCapability());
    }
}
