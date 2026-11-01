package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Передача через интерфейс на NeoForge — capabilities. «Виды» ({@link EnergyView}, {@link ItemView}, {@link FluidView},
 * химикаты Mekanism) — то, что видят трубы; поданное в них сразу уходит соседям связанных интерфейсов.
 */
public final class NeoForgeInterfaceTransport implements InterfaceTransport {
    /** До стольких мБ жидкости или химиката за одну операцию грани. */
    static final int FLUID_PER_OP = 16000;

    private final InterfaceBlockEntity be;
    private final IEnergyStorage energy;
    private final IItemHandler items;
    private final IFluidHandler fluids;
    private Object chemicals;

    public NeoForgeInterfaceTransport(InterfaceBlockEntity be) {
        this.be = be;
        this.energy = new EnergyView(be, this);
        this.items = new ItemView(be, this);
        this.fluids = new FluidView(be, this);
    }

    public static NeoForgeInterfaceTransport of(InterfaceBlockEntity be) {
        return (NeoForgeInterfaceTransport) be.transport();
    }

    // ---------- для возможностей блока ----------

    /** Обработчики для возможностей блока: null — вид передачи выключен (трубы не будут подключаться). */
    public IEnergyStorage energy() { return be.enabled(TransferType.ENERGY) ? energy : null; }

    public IItemHandler items() { return be.enabled(TransferType.ITEMS) ? items : null; }

    public IFluidHandler fluids() { return be.enabled(TransferType.FLUIDS) ? fluids : null; }

    public Object chemicals() {
        if (!be.enabled(TransferType.CHEMICALS)) return null;
        if (chemicals == null) chemicals = MekanismCompat.newView(be, this);
        return chemicals;
    }

    @Override
    public void invalidate() {
        Level level = be.getLevel();
        if (level != null) level.invalidateCapabilities(be.getBlockPos());
    }

    @Override
    public void tick() {
        AE2Compat.update(be);
    }

    @Override
    public void remove() {
        AE2Compat.destroy(be);
    }

    // ---------- грани «вставлять» / «забирать» ----------

    @Override
    public void moveFace(BlockPos np, Direction side, boolean pull) {
        Level level = be.getLevel();
        if (be.enabled(TransferType.ENERGY)) {
            IEnergyStorage h = level.getCapability(Capabilities.EnergyStorage.BLOCK, np, side);
            if (h != null) moveEnergy(h, pull);
        }
        if (be.enabled(TransferType.ITEMS)) {
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, np, side);
            if (h != null) moveItems(h, pull);
        }
        if (be.enabled(TransferType.FLUIDS)) {
            IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, np, side);
            if (h != null) moveFluids(h, pull);
        }
        if (be.enabled(TransferType.CHEMICALS)) MekanismCompat.moveFace(be, this, np, side, pull, FLUID_PER_OP);
    }

    private void moveEnergy(IEnergyStorage h, boolean pull) {
        IEnergyStorage from = pull ? h : energy, to = pull ? energy : h;
        int can = to.receiveEnergy(from.extractEnergy(Integer.MAX_VALUE, true), true);
        if (can > 0) to.receiveEnergy(from.extractEnergy(can, false), false);
    }

    /**
     * Одна стопка за раз. «Вставлять» берёт прямо из хранилищ у связанных входов (а не через слоты интерфейса —
     * так не пересчитываются все слоты всех хранилищ на каждый слот).
     */
    private void moveItems(IItemHandler h, boolean pull) {
        List<IItemHandler> froms = pull ? List.of(h) : handlers(Capabilities.ItemHandler.BLOCK, TransferType.ITEMS, true);
        IItemHandler to = pull ? items : h;
        for (IItemHandler from : froms) {
            for (int slot = 0; slot < from.getSlots(); slot++) {
                ItemStack probe = from.extractItem(slot, 64, true);
                if (probe.isEmpty()) continue;
                int n = probe.getCount() - insert(to, probe, true).getCount();
                if (n <= 0) continue;
                ItemStack left = insert(to, from.extractItem(slot, n, false), false);
                // Получатель взял меньше обещанного — вернуть остаток туда, откуда взяли, а не потерять.
                if (!left.isEmpty()) left = ItemHandlerHelper.insertItemStacked(from, left, false);
                if (!left.isEmpty()) Block.popResource(be.getLevel(), be.getBlockPos(), left);
                return;
            }
        }
    }

    /** В свой вход — сразу в приёмный слот (перебор слотов вида ни к чему), в чужое хранилище — со стопками. */
    private ItemStack insert(IItemHandler to, ItemStack stack, boolean simulate) {
        return to == items ? items.insertItem(0, stack, simulate) : ItemHandlerHelper.insertItemStacked(to, stack, simulate);
    }

    private void moveFluids(IFluidHandler h, boolean pull) {
        IFluidHandler from = pull ? h : fluids, to = pull ? fluids : h;
        FluidStack probe = from.drain(FLUID_PER_OP, IFluidHandler.FluidAction.SIMULATE);
        if (probe.isEmpty()) return;
        int can = to.fill(probe, IFluidHandler.FluidAction.SIMULATE);
        if (can > 0) to.fill(from.drain(probe.copyWithAmount(can), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
    }

    // ---------- соседи связанных интерфейсов ----------

    private long cacheTick = -1;
    /** Кэш на тик: по два списка (откуда / куда) на каждый вид передачи. */
    private final List<?>[] cache = new List<?>[TransferType.values().length * 2];

    /**
     * Обработчики соседей связанных интерфейсов: {@code fromInputs} — у интерфейсов со входом (откуда забирать),
     * иначе — у интерфейсов с выходом (куда отдавать). Соседние интерфейсы пропускаются. Кэш на тик.
     */
    @SuppressWarnings("unchecked")
    <T> List<T> handlers(BlockCapability<T, Direction> cap, TransferType type, boolean fromInputs) {
        Level level = be.getLevel();
        if (level == null || !be.isConnected()) return List.of();
        long now = level.getGameTime();
        if (now != cacheTick) { java.util.Arrays.fill(cache, null); cacheTick = now; }
        int k = type.ordinal() * 2 + (fromInputs ? 1 : 0);
        List<?> cached = cache[k];
        if (cached != null) return (List<T>) cached;
        List<T> out = new ArrayList<>();
        for (InterfaceBlockEntity peer : InterfaceNetwork.peers(be)) {
            if (!peer.enabled(type) || (fromInputs ? !peer.flow().in : !peer.flow().out)) continue;
            Level pl = peer.getLevel();
            if (pl == null) continue;
            for (Direction d : Direction.values()) {
                // Грань «забирать» сама вынимает из соседа — класть туда же нельзя (гоняли бы по кругу); и наоборот.
                FaceMode fm = peer.face(d);
                if (fromInputs ? fm == FaceMode.PUSH : fm == FaceMode.PULL) continue;
                BlockPos np = peer.getBlockPos().relative(d);
                if (!pl.isLoaded(np) || pl.getBlockEntity(np) instanceof InterfaceBlockEntity) continue;
                T h = pl.getCapability(cap, np, d.getOpposite());
                if (h != null) out.add(h);
            }
        }
        // По кругу: каждый раз начинаем с другого получателя, чтобы делить поровну.
        if (out.size() > 1) java.util.Collections.rotate(out, -(int) (now % out.size()));
        cache[k] = out;
        return out;
    }
}
