package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TardisRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Передача через интерфейс на Forge — capabilities (на 1.20.1 — через LazyOptional, подключаются к блоку событием). «Виды» ({@link EnergyView}, {@link ItemView}, {@link FluidView},
 * химикаты Mekanism) — то, что видят трубы; поданное в них сразу уходит соседям связанных интерфейсов.
 */
public final class ForgeInterfaceTransport implements InterfaceTransport {
    /** До стольких мБ жидкости или химиката за одну операцию грани. */
    static final int FLUID_PER_OP = 16000;

    private final InterfaceBlockEntity be;
    /** Свои соседи по видам — их берут связанные интерфейсы (см. {@link LinkedStorages}). */
    private final LinkedStorages.Sides<IEnergyStorage> energySides;
    private final LinkedStorages.Sides<IItemHandler> itemSides;
    private final LinkedStorages.Sides<IFluidHandler> fluidSides;
    /** Хранилища у соседей связанных интерфейсов — по виду. */
    final LinkedStorages<IEnergyStorage> linkedEnergy;
    final LinkedStorages<IItemHandler> linkedItems;
    final LinkedStorages<IFluidHandler> linkedFluids;
    private final IEnergyStorage energy;
    private final IItemHandler items;
    private final IFluidHandler fluids;
    private Object chemicals;

    public ForgeInterfaceTransport(InterfaceBlockEntity be) {
        this.be = be;
        this.energySides = sides(be, ForgeCapabilities.ENERGY);
        this.itemSides = sides(be, ForgeCapabilities.ITEM_HANDLER);
        this.fluidSides = sides(be, ForgeCapabilities.FLUID_HANDLER);
        this.linkedEnergy = new LinkedStorages<>(be, TransferType.ENERGY, peer -> of(peer).energySides);
        this.linkedItems = new LinkedStorages<>(be, TransferType.ITEMS, peer -> of(peer).itemSides);
        this.linkedFluids = new LinkedStorages<>(be, TransferType.FLUIDS, peer -> of(peer).fluidSides);
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


    // ---------- подключение к блоку ----------

    private static final ResourceLocation CAP_ID = ResourceLocation.tryBuild(TardisRooms.MODID, "interface");

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
            // С выключенной стороны трубы интерфейса не видят.
            if (be.transport() == null || !be.open(side)) return LazyOptional.empty();
            return of(be).lazy(cap);
        }
    }

    private LazyOptional<IEnergyStorage> energyOpt = LazyOptional.empty();
    private LazyOptional<IItemHandler> itemsOpt = LazyOptional.empty();
    private LazyOptional<IFluidHandler> fluidsOpt = LazyOptional.empty();
    private boolean optsReady;
    /** Блок снят или его чанк выгружается: возможностей больше не отдаём (иначе трубы, получив сигнал «погашено», тут же берут новые). */
    private boolean dead;

    <T> LazyOptional<T> lazy(Capability<T> cap) {
        if (dead) return LazyOptional.empty();
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
        AE2Compat.update(be); // стороны, открытые для кабелей ME, — сразу, а не на ближайшем секундном такте
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

    /** Сосед изменился: запомненные возможности соседей проверить заново (см. {@link CapCache}). */
    @Override
    public void neighborChanged() {
        neighbours++;
    }

    @Override
    public void tick() {
        AE2Compat.update(be);
    }

    @Override
    public void remove() {
        dead = true;
        AE2Compat.destroy(be);
        // Без обновления соседей: remove() зовут и при выгрузке чанка, когда игра перебирает его блоки-сущности,
        // и обновление соседей посреди этого перебора роняет сервер (ConcurrentModificationException).
        dropOptionals();
    }

    // ---------- грани «вставлять» / «забирать» ----------

    @Override
    public void moveFace(BlockPos np, Direction side, boolean pull) {
        Direction face = side.getOpposite(); // своя сторона, к которой примыкает сосед
        if (be.enabled(TransferType.ENERGY)) {
            IEnergyStorage h = energySides.at(face);
            if (h != null) moveEnergy(h, pull);
        }
        if (be.enabled(TransferType.ITEMS)) {
            IItemHandler h = itemSides.at(face);
            if (h != null) moveItems(h, pull);
        }
        if (be.enabled(TransferType.FLUIDS)) {
            IFluidHandler h = fluidSides.at(face);
            if (h != null) moveFluids(h, pull);
        }
        if (be.enabled(TransferType.CHEMICALS)) MekanismCompat.moveFace(this, face, pull, FLUID_PER_OP);
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
        List<IItemHandler> froms = pull ? List.of(h) : linkedItems.get(LinkedStorages.Pick.FROM);
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

    // ---------- свои соседи ----------

    /** Растёт, когда меняется соседний блок: запомненные возможности соседей тогда проверяются заново. */
    private int neighbours;

    /** Соседи блока по возможности {@code cap}: по {@link CapCache} на сторону; кэш живёт, пока блок не убран. */
    static <T> LinkedStorages.Sides<T> sides(InterfaceBlockEntity be, Capability<T> cap) {
        return new SideCaches<T, CapCache<T>>(be, (level, pos, side) -> new CapCache<>(of(be), level, pos, side.getOpposite(), cap),
                (cache, side) -> cache.get());
    }

    /**
     * Сосед с одной стороны по одной возможности (на Forge 1.20.1 нет BlockCapabilityCache — свой, такой же по смыслу).
     * Возможность запоминается и спрашивается у соседа заново, только если её LazyOptional погас или соседний блок
     * изменился (поставили, сломали, заменили). Каждый раз её у соседа не спрашиваем: у некоторых модов это дорого.
     */
    static final class CapCache<T> {
        private final ForgeInterfaceTransport owner;
        private final ServerLevel level;
        private final BlockPos pos;
        /** Сторона соседа, которой он примыкает к интерфейсу. */
        private final Direction side;
        private final Capability<T> cap;
        private @Nullable BlockEntity source;
        private @Nullable T value;
        private boolean valid;
        private int seen;

        CapCache(ForgeInterfaceTransport owner, ServerLevel level, BlockPos pos, Direction side, Capability<T> cap) {
            this.owner = owner;
            this.level = level;
            this.pos = pos;
            this.side = side;
            this.cap = cap;
        }

        @Nullable T get() {
            if (valid && seen == owner.neighbours) return value;
            seen = owner.neighbours;
            BlockEntity nbe = level.getBlockEntity(pos);
            // Сосед тот же и его возможность не погасла — она та же.
            if (valid && nbe == source) return value;
            source = nbe;
            value = null;
            valid = true;
            if (nbe != null) {
                LazyOptional<T> o = nbe.getCapability(cap, side);
                if (o.isPresent()) {
                    value = o.resolve().orElse(null);
                    o.addListener(x -> valid = false);
                }
            }
            return value;
        }
    }
}
