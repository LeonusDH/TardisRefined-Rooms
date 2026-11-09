package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.platform.Platform;
import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;
import java.util.function.Function;

/**
 * Химикаты Mekanism 10.4 (Minecraft 1.20.1). В отличие от 10.7, химикатов четыре вида — газы, инфузии, пигменты и
 * суспензии, — у каждого своя возможность; интерфейс передаёт все четыре одинаково.
 * Классы Mekanism здесь загружаются, только если Mekanism установлен (всё — после проверки {@link #LOADED}).
 */
public final class MekanismCompat {
    /** Установлен ли Mekanism. */
    public static final boolean LOADED = Platform.isModLoaded("mekanism");

    /** Mekanism есть и совместимость не выключена в конфиге. */
    public static boolean enabled() {
        return LOADED && leonus.tardis_rooms.TardisRoomsConfig.compatMekanism();
    }

    static Object newView(InterfaceBlockEntity be, ForgeInterfaceTransport transport) {
        return enabled() ? Impl.newViews(be, transport) : null;
    }

    static <T> LazyOptional<T> capability(InterfaceBlockEntity be, ForgeInterfaceTransport transport, Capability<T> cap) {
        if (!enabled() || !be.enabled(TransferType.CHEMICALS)) return LazyOptional.empty();
        return Impl.capability(transport, cap);
    }

    /** Старые LazyOptional химикатов гасим вместе с остальными (их хранит транспорт в виде {@code chemicals}). */
    static void invalidate(ForgeInterfaceTransport transport) {
        if (LOADED) Impl.invalidate(transport);
    }

    /** Грань «вставлять»/«забирать» для химикатов: сосед со своей стороны {@code face}. */
    static void moveFace(ForgeInterfaceTransport transport, Direction face, boolean pull, int max) {
        if (!enabled()) return;
        Impl.moveFace(transport, face, pull, max);
    }

    /** Отдельный класс, чтобы типы Mekanism не грузились без него. */
    private static final class Impl {
        static final Capability<IGasHandler> GAS = CapabilityManager.get(new CapabilityToken<>() {});
        static final Capability<IInfusionHandler> INFUSION = CapabilityManager.get(new CapabilityToken<>() {});
        static final Capability<IPigmentHandler> PIGMENT = CapabilityManager.get(new CapabilityToken<>() {});
        static final Capability<ISlurryHandler> SLURRY = CapabilityManager.get(new CapabilityToken<>() {});

        /** Виды для четырёх возможностей и их LazyOptional. */
        static final class Views {
            final GasView gas;
            final InfusionView infusion;
            final PigmentView pigment;
            final SlurryView slurry;
            LazyOptional<IGasHandler> gasOpt;
            LazyOptional<IInfusionHandler> infusionOpt;
            LazyOptional<IPigmentHandler> pigmentOpt;
            LazyOptional<ISlurryHandler> slurryOpt;

            Views(InterfaceBlockEntity be, ForgeInterfaceTransport t) {
                gas = new GasView(be);
                infusion = new InfusionView(be);
                pigment = new PigmentView(be);
                slurry = new SlurryView(be);
                reset();
            }

            void reset() {
                gasOpt = LazyOptional.of(() -> gas);
                infusionOpt = LazyOptional.of(() -> infusion);
                pigmentOpt = LazyOptional.of(() -> pigment);
                slurryOpt = LazyOptional.of(() -> slurry);
            }

            void invalidate() {
                gasOpt.invalidate();
                infusionOpt.invalidate();
                pigmentOpt.invalidate();
                slurryOpt.invalidate();
                reset();
            }
        }

        static Object newViews(InterfaceBlockEntity be, ForgeInterfaceTransport t) {
            return new Views(be, t);
        }

        static <T> LazyOptional<T> capability(ForgeInterfaceTransport t, Capability<T> cap) {
            if (!(t.chemicals() instanceof Views v)) return LazyOptional.empty();
            if (cap == GAS) return v.gasOpt.cast();
            if (cap == INFUSION) return v.infusionOpt.cast();
            if (cap == PIGMENT) return v.pigmentOpt.cast();
            if (cap == SLURRY) return v.slurryOpt.cast();
            return LazyOptional.empty();
        }

        static void invalidate(ForgeInterfaceTransport t) {
            if (t.chemicalsIfCreated() instanceof Views v) v.invalidate();
        }

        static void moveFace(ForgeInterfaceTransport t, Direction face, boolean pull, int max) {
            if (!(t.chemicals() instanceof Views v)) return;
            move(v.gas.sides.at(face), v.gas, pull, max);
            move(v.infusion.sides.at(face), v.infusion, pull, max);
            move(v.pigment.sides.at(face), v.pigment, pull, max);
            move(v.slurry.sides.at(face), v.slurry, pull, max);
        }

        private static <C extends Chemical<C>, S extends ChemicalStack<C>> void move(IChemicalHandler<C, S> h, IChemicalHandler<C, S> mine, boolean pull, long max) {
            if (h == null) return;
            IChemicalHandler<C, S> from = pull ? h : mine, to = pull ? mine : h;
            S probe = from.extractChemical(max, Action.SIMULATE);
            if (probe.isEmpty()) return;
            S rest = to.insertChemical(probe, Action.SIMULATE);
            long can = probe.getAmount() - rest.getAmount();
            if (can <= 0) return;
            S moved = from.extractChemical(can, Action.EXECUTE);
            to.insertChemical(moved, Action.EXECUTE);
        }
    }

    /**
     * Химикаты одного вида через интерфейс — так же, как жидкости. Баки — только непустые баки у связанных интерфейсов:
     * подсказка Mekanism в Jade рисует каждый бак, даже пустой и с нулевой ёмкостью, поэтому приёмного бака нет вовсе.
     * Трубы Mekanism вставляют и забирают через {@link #insertChemical(ChemicalStack, Action)} и
     * {@link #extractChemical(long, Action)} — они переопределены и баков не перебирают.
     */
    private abstract static class ChemicalView<C extends Chemical<C>, S extends ChemicalStack<C>, H extends IChemicalHandler<C, S>> implements IChemicalHandler<C, S> {
        private final InterfaceBlockEntity be;
        /** Свои соседи по этой возможности — их берут связанные интерфейсы. */
        final LinkedStorages.Sides<H> sides;
        /** Хранилища этого вида химикатов у соседей связанных интерфейсов. */
        private final LinkedStorages<H> linked;

        /** {@code select} — тот же вид химикатов у связанного интерфейса. */
        ChemicalView(InterfaceBlockEntity be, Capability<H> cap, Function<Impl.Views, ChemicalView<C, S, H>> select) {
            this.be = be;
            this.sides = ForgeInterfaceTransport.sides(be, cap);
            this.linked = new LinkedStorages<>(be, TransferType.CHEMICALS,
                    peer -> ForgeInterfaceTransport.of(peer).chemicals() instanceof Impl.Views v ? select.apply(v).sides : null);
        }

        private List<H> sources() {
            return be.canOut(TransferType.CHEMICALS) ? linked.get(LinkedStorages.Pick.FROM) : List.of();
        }

        /** Непустые баки у связанных интерфейсов (обработчик и номер бака). */
        private List<Object[]> filled() {
            List<Object[]> out = new java.util.ArrayList<>();
            for (H h : sources())
                for (int t = 0; t < h.getTanks(); t++) if (!h.getChemicalInTank(t).isEmpty()) out.add(new Object[]{h, t});
            return out;
        }

        private Object[] locate(int tank) {
            if (tank < 0) return null;
            List<Object[]> f = filled();
            return tank < f.size() ? f.get(tank) : null;
        }

        @SuppressWarnings("unchecked")
        private H at(Object[] l) { return (H) l[0]; }

        @Override
        public int getTanks() {
            return filled().size();
        }

        @Override
        public S getChemicalInTank(int tank) {
            Object[] l = locate(tank);
            return l == null ? getEmptyStack() : at(l).getChemicalInTank((int) l[1]);
        }

        @Override
        public void setChemicalInTank(int tank, S stack) {
            // Своих баков нет.
        }

        @Override
        public long getTankCapacity(int tank) {
            Object[] l = locate(tank);
            return l == null ? 0 : at(l).getTankCapacity((int) l[1]);
        }

        @Override
        public boolean isValid(int tank, S stack) {
            return be.canIn(TransferType.CHEMICALS);
        }

        @Override
        public S insertChemical(int tank, S stack, Action action) {
            return insertChemical(stack, action);
        }

        /** Поданное — сразу в хранилища у связанных выходов. */
        @Override
        public S insertChemical(S stack, Action action) {
            if (stack.isEmpty() || !be.canIn(TransferType.CHEMICALS) || InterfaceNetwork.depth > 0) return stack;
            InterfaceNetwork.depth++;
            try {
                S rest = stack;
                for (H h : linked.get(LinkedStorages.Pick.TO)) {
                    rest = h.insertChemical(rest, action);
                    if (rest.isEmpty()) break;
                }
                return rest;
            } finally {
                InterfaceNetwork.depth--;
            }
        }

        @Override
        public S extractChemical(int tank, long amount, Action action) {
            if (amount <= 0 || InterfaceNetwork.depth > 0) return getEmptyStack();
            Object[] l = locate(tank);
            if (l == null) return getEmptyStack();
            InterfaceNetwork.depth++;
            try {
                return at(l).extractChemical((int) l[1], amount, action);
            } finally {
                InterfaceNetwork.depth--;
            }
        }

        /** Забрать что угодно: первый химикат, какой найдётся, — и дальше только его, по всем хранилищам (как у жидкостей). */
        @Override
        @SuppressWarnings("unchecked")
        public S extractChemical(long amount, Action action) {
            if (amount <= 0 || !be.canOut(TransferType.CHEMICALS) || InterfaceNetwork.depth > 0) return getEmptyStack();
            S first = getEmptyStack();
            InterfaceNetwork.depth++;
            try {
                for (H h : sources()) {
                    S probe = h.extractChemical(amount, Action.SIMULATE);
                    if (!probe.isEmpty()) { first = probe; break; }
                }
            } finally {
                InterfaceNetwork.depth--;
            }
            if (first.isEmpty()) return getEmptyStack();
            S want = (S) first.copy();
            want.setAmount(amount);
            return extractChemical(want, action);
        }

        /** Забрать именно этот химикат — сколько наберётся по всем хранилищам у связанных входов. */
        @Override
        @SuppressWarnings("unchecked")
        public S extractChemical(S stack, Action action) {
            if (stack.isEmpty() || !be.canOut(TransferType.CHEMICALS) || InterfaceNetwork.depth > 0) return getEmptyStack();
            InterfaceNetwork.depth++;
            try {
                S out = getEmptyStack();
                for (H h : sources()) {
                    long need = stack.getAmount() - out.getAmount();
                    if (need <= 0) break;
                    S want = (S) stack.copy();
                    want.setAmount(need);
                    S got = h.extractChemical(want, action);
                    if (got.isEmpty()) continue;
                    if (out.isEmpty()) out = (S) got.copy(); else out.grow(got.getAmount());
                }
                return out;
            } finally {
                InterfaceNetwork.depth--;
            }
        }
    }

    private static final class GasView extends ChemicalView<Gas, GasStack, IGasHandler> implements IGasHandler {
        GasView(InterfaceBlockEntity be) { super(be, Impl.GAS, v -> v.gas); }
    }

    private static final class InfusionView extends ChemicalView<InfuseType, InfusionStack, IInfusionHandler> implements IInfusionHandler {
        InfusionView(InterfaceBlockEntity be) { super(be, Impl.INFUSION, v -> v.infusion); }
    }

    private static final class PigmentView extends ChemicalView<Pigment, PigmentStack, IPigmentHandler> implements IPigmentHandler {
        PigmentView(InterfaceBlockEntity be) { super(be, Impl.PIGMENT, v -> v.pigment); }
    }

    private static final class SlurryView extends ChemicalView<Slurry, SlurryStack, ISlurryHandler> implements ISlurryHandler {
        SlurryView(InterfaceBlockEntity be) { super(be, Impl.SLURRY, v -> v.slurry); }
    }

    private MekanismCompat() {}
}
