package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Передача через интерфейс на Forge — capabilities (на 1.20.1 — через LazyOptional, подключаются к блоку событием). «Виды» ({@link EnergyView}, {@link ItemView}, {@link FluidView},
 * химикаты Mekanism) — то, что видят трубы; поданное в них сразу уходит соседям связанных интерфейсов.
 */
public final class ForgeInterfaceTransport implements InterfaceTransport {
    /** До стольких мБ жидкости или химиката за одну операцию грани. */
    static final int FLUID_PER_OP = 16000;

    private final InterfaceBlockEntity be;
    private final IEnergyStorage energy;
    private final IItemHandler items;
    private final IFluidHandler fluids;
    private Object chemicals;

    public ForgeInterfaceTransport(InterfaceBlockEntity be) {
        this.be = be;
        this.energy = new EnergyView(be, this);
        this.items = new ItemView(be, this);
        this.fluids = new FluidView(be, this);
    }

    public static ForgeInterfaceTransport of(InterfaceBlockEntity be) {
        return (ForgeInterfaceTransport) be.transport();
    }

    /** Копия стопки жидкости с другим количеством (в Forge 1.20.1 своего метода нет). */
    static FluidStack withAmount(FluidStack s, int amount) {
        return new FluidStack(s, amount);
    }

    /** Возможность блока по соседству, или null. */
    static <T> @Nullable T cap(Level level, BlockPos pos, Capability<T> cap, @Nullable Direction side) {
        BlockEntity nbe = level.getBlockEntity(pos);
        return nbe == null ? null : nbe.getCapability(cap, side).resolve().orElse(null);
    }

    // ---------- подключение к блоку ----------

    private static final ResourceLocation CAP_ID = ResourceLocation.tryBuild(leonus.tardis_rooms.TardisRooms.MODID, "interface");

    /** Подписка на AttachCapabilitiesEvent<BlockEntity>: возможности интерфейса. */
    public static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof InterfaceBlockEntity be) event.addCapability(CAP_ID, new Provider(be));
    }

    /**
     * Отдаёт возможности транспорта блока. Транспорт берём при запросе: событие приходит из конструктора блока-сущности,
     * когда его поля ещё не заполнены.
     */
    private record Provider(InterfaceBlockEntity be) implements ICapabilityProvider {
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (be.transport() == null) return LazyOptional.empty();
            return of(be).lazy(cap);
        }
    }

    private LazyOptional<IEnergyStorage> energyOpt = LazyOptional.empty();
    private LazyOptional<IItemHandler> itemsOpt = LazyOptional.empty();
    private LazyOptional<IFluidHandler> fluidsOpt = LazyOptional.empty();
    private boolean optsReady;

    <T> LazyOptional<T> lazy(Capability<T> cap) {
        if (!optsReady) {
            optsReady = true;
            if (energy() != null) energyOpt = LazyOptional.of(() -> energy);
            if (items() != null) itemsOpt = LazyOptional.of(() -> items);
            if (fluids() != null) fluidsOpt = LazyOptional.of(() -> fluids);
        }
        if (cap == ForgeCapabilities.ENERGY) return energyOpt.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemsOpt.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidsOpt.cast();
        LazyOptional<T> me = AE2Compat.capability(be, cap);
        if (me.isPresent()) return me;
        return MekanismCompat.capability(be, this, cap);
    }

    // ---------- для возможностей блока ----------

    /** Обработчики для возможностей блока: null — вид передачи выключен (трубы не будут подключаться). */
    public IEnergyStorage energy() { return be.enabled(TransferType.ENERGY) ? energy : null; }

    public IItemHandler items() { return be.enabled(TransferType.ITEMS) ? items : null; }

    public IFluidHandler fluids() { return be.enabled(TransferType.FLUIDS) ? fluids : null; }

    /** Виды химикатов, если уже созданы (не создавая их). */
    Object chemicalsIfCreated() { return chemicals; }

    public Object chemicals() {
        if (!be.enabled(TransferType.CHEMICALS)) return null;
        if (chemicals == null) chemicals = MekanismCompat.newView(be, this);
        return chemicals;
    }

    /** Изменились включённые виды передачи: старые LazyOptional гасим (трубы переподключатся), новые создадим при запросе. */
    @Override
    public void invalidate() {
        dropOptionals();
        Level level = be.getLevel();
        if (level != null && !level.isClientSide()) level.updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
    }

    /** Погасить LazyOptional (трубы отпустят обработчики), не трогая соседей. */
    private void dropOptionals() {
        energyOpt.invalidate();
        itemsOpt.invalidate();
        fluidsOpt.invalidate();
        energyOpt = LazyOptional.empty();
        itemsOpt = LazyOptional.empty();
        fluidsOpt = LazyOptional.empty();
        optsReady = false;
        MekanismCompat.invalidate(this);
    }

    @Override
    public void tick() {
        AE2Compat.update(be);
    }

    @Override
    public void remove() {
        AE2Compat.destroy(be);
        // Без обновления соседей: remove() зовут и при выгрузке чанка, когда игра перебирает его блоки-сущности,
        // и обновление соседей посреди этого перебора роняет сервер (ConcurrentModificationException).
        dropOptionals();
    }

    // ---------- грани «вставлять» / «забирать» ----------

    @Override
    public void moveFace(BlockPos np, Direction side, boolean pull) {
        Level level = be.getLevel();
        if (be.enabled(TransferType.ENERGY)) {
            IEnergyStorage h = cap(level, np, ForgeCapabilities.ENERGY, side);
            if (h != null) moveEnergy(h, pull);
        }
        if (be.enabled(TransferType.ITEMS)) {
            IItemHandler h = cap(level, np, ForgeCapabilities.ITEM_HANDLER, side);
            if (h != null) moveItems(h, pull);
        }
        if (be.enabled(TransferType.FLUIDS)) {
            IFluidHandler h = cap(level, np, ForgeCapabilities.FLUID_HANDLER, side);
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
        List<IItemHandler> froms = pull ? List.of(h) : handlers(ForgeCapabilities.ITEM_HANDLER, TransferType.ITEMS, true);
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
        if (can > 0) to.fill(from.drain(ForgeInterfaceTransport.withAmount(probe, can), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
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
    <T> List<T> handlers(Capability<T> cap, TransferType type, boolean fromInputs) {
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
                T h = cap(pl, np, cap, d.getOpposite());
                if (h != null) out.add(h);
            }
        }
        // По кругу: каждый раз начинаем с другого получателя, чтобы делить поровну.
        if (out.size() > 1) java.util.Collections.rotate(out, -(int) (now % out.size()));
        cache[k] = out;
        return out;
    }
}
