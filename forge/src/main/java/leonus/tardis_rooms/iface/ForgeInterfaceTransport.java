package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.iface.kind.TransferKinds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Передача через интерфейс на Forge — capabilities (на 1.20.1 — через LazyOptional, подключаются к блоку событием).
 * Виды передачи — части ({@link EnergyView}, {@link ItemView}, {@link FluidView}, химикаты Mekanism, сеть ME): то, что
 * видят трубы и кабели; поданное сразу уходит соседям связанных интерфейсов. Здесь — общее для всех видов: LazyOptional
 * для труб, их гашение при смене настроек и уходе блока, и свои соседи интерфейса ({@link CapCache}).
 */
public final class ForgeInterfaceTransport implements InterfaceTransport {
    /** До стольких мБ жидкости или химиката за одну операцию грани. */
    static final int FLUID_PER_OP = 16000;

    private final InterfaceBlockEntity be;

    public ForgeInterfaceTransport(InterfaceBlockEntity be) {
        this.be = be;
    }

    public static ForgeInterfaceTransport of(InterfaceBlockEntity be) {
        return (ForgeInterfaceTransport) be.transport();
    }

    /** Части встроенных видов на Forge по id вида (для {@link BuiltinKinds#plugin}); null — у вида здесь нет части. */
    public static TransferKind.PartFactory parts(ResourceLocation id) {
        if (id.equals(BuiltinKinds.ENERGY)) return EnergyView::new;
        if (id.equals(BuiltinKinds.ITEMS)) return ItemView::new;
        if (id.equals(BuiltinKinds.FLUIDS)) return FluidView::new;
        if (id.equals(BuiltinKinds.CHEMICALS)) return MekanismCompat.LOADED ? MekanismCompat::newPart : null;
        if (id.equals(BuiltinKinds.ME)) return AE2Compat.LOADED ? AE2Compat::newPart : null;
        return null;
    }

    /** Копия стопки жидкости с другим количеством (в Forge 1.20.1 своего метода нет). */
    static FluidStack withAmount(FluidStack s, int amount) {
        return new FluidStack(s, amount);
    }

    /** Часть вида, которая сама отдаёт трубам свои возможности (химикаты Mekanism, узел сети ME). */
    interface CapabilityPart {
        /** Возможность {@code cap}, если она этой части; иначе пусто. Вызывается, только если вид включён. */
        <T> LazyOptional<T> capability(Capability<T> cap);
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
            if (be.part(BuiltinKinds.energy()) instanceof EnergyView v && v.capability() != null) energyOpt = LazyOptional.of(() -> v);
            if (be.part(BuiltinKinds.items()) instanceof ItemView v && v.capability() != null) itemsOpt = LazyOptional.of(() -> v);
            if (be.part(BuiltinKinds.fluids()) instanceof FluidView v && v.capability() != null) fluidsOpt = LazyOptional.of(() -> v);
        }
        if (cap == ForgeCapabilities.ENERGY) return energyOpt.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemsOpt.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidsOpt.cast();
        for (TransferKind k : TransferKinds.all()) {
            if (!be.enabled(k) || !(be.part(k) instanceof CapabilityPart p)) continue;
            LazyOptional<T> o = p.capability(cap);
            if (o.isPresent()) return o;
        }
        return LazyOptional.empty();
    }

    /** Изменились включённые виды передачи: старые LazyOptional гасим (трубы переподключатся), новые создадим при запросе. */
    @Override
    public void invalidate() {
        dropOptionals();
        Level level = be.getLevel();
        if (level != null && !level.isClientSide()) level.updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
    }

    /** Погасить LazyOptional (трубы отпустят обработчики), не трогая соседей. Свои гасят сами части ({@link KindPart#invalidate}). */
    private void dropOptionals() {
        energyOpt.invalidate();
        itemsOpt.invalidate();
        fluidsOpt.invalidate();
        energyOpt = LazyOptional.empty();
        itemsOpt = LazyOptional.empty();
        fluidsOpt = LazyOptional.empty();
        optsReady = false;
    }

    @Override
    public void remove() {
        dead = true;
        // Без обновления соседей: remove() зовут и при выгрузке чанка, когда игра перебирает его блоки-сущности,
        // и обновление соседей посреди этого перебора роняет сервер (ConcurrentModificationException).
        dropOptionals();
    }

    // ---------- свои соседи ----------

    /** Сосед изменился: запомненные возможности соседей проверить заново (см. {@link CapCache}). */
    @Override
    public void neighborChanged() {
        neighbours++;
    }

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
