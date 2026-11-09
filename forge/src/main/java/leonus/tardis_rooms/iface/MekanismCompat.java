package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
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

    /** Часть вида «химикаты» для интерфейса. Только если Mekanism установлен. */
    static KindPart newPart(InterfaceBlockEntity be, TransferKind kind) {
        return Impl.newViews(be, kind);
    }

    /** Отдельный класс, чтобы типы Mekanism не грузились без него. */
    private static final class Impl {
        static final Capability<IGasHandler> GAS = CapabilityManager.get(new CapabilityToken<>() {});
        static final Capability<IInfusionHandler> INFUSION = CapabilityManager.get(new CapabilityToken<>() {});
        static final Capability<IPigmentHandler> PIGMENT = CapabilityManager.get(new CapabilityToken<>() {});
        static final Capability<ISlurryHandler> SLURRY = CapabilityManager.get(new CapabilityToken<>() {});

        /** Часть вида: виды для четырёх возможностей и их LazyOptional. */
        static final class Views implements KindPart, ForgeInterfaceTransport.CapabilityPart {
            final GasView gas;
            final InfusionView infusion;
            final PigmentView pigment;
            final SlurryView slurry;
            LazyOptional<IGasHandler> gasOpt;
            LazyOptional<IInfusionHandler> infusionOpt;
            LazyOptional<IPigmentHandler> pigmentOpt;
            LazyOptional<ISlurryHandler> slurryOpt;

            Views(InterfaceBlockEntity be, TransferKind kind) {
                gas = new GasView(be, kind);
                infusion = new InfusionView(be, kind);
                pigment = new PigmentView(be, kind);
                slurry = new SlurryView(be, kind);
                reset();
            }

            private void reset() {
                gasOpt = LazyOptional.of(() -> gas);
                infusionOpt = LazyOptional.of(() -> infusion);
                pigmentOpt = LazyOptional.of(() -> pigment);
                slurryOpt = LazyOptional.of(() -> slurry);
            }

            /** Старые LazyOptional гасим (трубы переподключатся), новые — сразу. */
            @Override
            public void invalidate() {
                gasOpt.invalidate();
                infusionOpt.invalidate();
                pigmentOpt.invalidate();
                slurryOpt.invalidate();
                reset();
            }

            @Override
            public void unload() { invalidate(); }

            @Override
            public void remove() { invalidate(); }

            @Override
            public <T> LazyOptional<T> capability(Capability<T> cap) {
                if (cap == GAS) return gasOpt.cast();
                if (cap == INFUSION) return infusionOpt.cast();
                if (cap == PIGMENT) return pigmentOpt.cast();
                if (cap == SLURRY) return slurryOpt.cast();
                return LazyOptional.empty();
            }

            @Override
            public void moveFace(Direction face, boolean pull) {
                int max = ForgeInterfaceTransport.FLUID_PER_OP;
                move(gas.sides.at(face), gas, pull, max);
                move(infusion.sides.at(face), infusion, pull, max);
                move(pigment.sides.at(face), pigment, pull, max);
                move(slurry.sides.at(face), slurry, pull, max);
            }
        }

        static KindPart newViews(InterfaceBlockEntity be, TransferKind kind) {
            return new Views(be, kind);
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
        private final TransferKind kind;
        /** Свои соседи по этой возможности — их берут связанные интерфейсы. */
        final LinkedStorages.Sides<H> sides;
        /** Хранилища этого вида химикатов у соседей связанных интерфейсов. */
        private final LinkedStorages<H> linked;

        /** {@code select} — тот же вид химикатов у связанного интерфейса. */
        ChemicalView(InterfaceBlockEntity be, TransferKind kind, Capability<H> cap, Function<Impl.Views, ChemicalView<C, S, H>> select) {
            this.be = be;
            this.kind = kind;
            this.sides = ForgeInterfaceTransport.sides(be, cap);
            this.linked = new LinkedStorages<>(be, kind, peer -> peer.part(kind) instanceof Impl.Views v ? select.apply(v).sides : null);
        }

        private List<H> sources() {
            return be.canOut(kind) ? linked.get(LinkedStorages.Pick.FROM) : List.of();
        }

        /** Непустые баки у связанных интерфейсов (обработчик и номер бака). */
        private List<HandlerSlot<H>> filled() {
            List<HandlerSlot<H>> out = new java.util.ArrayList<>();
            for (H h : sources())
                for (int t = 0; t < h.getTanks(); t++) if (!h.getChemicalInTank(t).isEmpty()) out.add(new HandlerSlot<>(h, t));
            return out;
        }

        private HandlerSlot<H> locate(int tank) {
            if (tank < 0) return null;
            List<HandlerSlot<H>> f = filled();
            return tank < f.size() ? f.get(tank) : null;
        }

        @Override
        public int getTanks() {
            return filled().size();
        }

        @Override
        public S getChemicalInTank(int tank) {
            HandlerSlot<H> l = locate(tank);
            return l == null ? getEmptyStack() : l.from().getChemicalInTank(l.index());
        }

        @Override
        public void setChemicalInTank(int tank, S stack) {
            // Своих баков нет.
        }

        @Override
        public long getTankCapacity(int tank) {
            HandlerSlot<H> l = locate(tank);
            return l == null ? 0 : l.from().getTankCapacity(l.index());
        }

        @Override
        public boolean isValid(int tank, S stack) {
            return be.canIn(kind);
        }

        @Override
        public S insertChemical(int tank, S stack, Action action) {
            return insertChemical(stack, action);
        }

        /** Поданное — сразу в хранилища у связанных выходов. */
        @Override
        public S insertChemical(S stack, Action action) {
            if (stack.isEmpty() || !be.canIn(kind) || InterfaceNetwork.depth > 0) return stack;
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
            HandlerSlot<H> l = locate(tank);
            if (l == null) return getEmptyStack();
            InterfaceNetwork.depth++;
            try {
                return l.from().extractChemical(l.index(), amount, action);
            } finally {
                InterfaceNetwork.depth--;
            }
        }

        /** Забрать что угодно: первый химикат, какой найдётся, — и дальше только его, по всем хранилищам (как у жидкостей). */
        @Override
        @SuppressWarnings("unchecked")
        public S extractChemical(long amount, Action action) {
            if (amount <= 0 || !be.canOut(kind) || InterfaceNetwork.depth > 0) return getEmptyStack();
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
            if (stack.isEmpty() || !be.canOut(kind) || InterfaceNetwork.depth > 0) return getEmptyStack();
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
        GasView(InterfaceBlockEntity be, TransferKind kind) { super(be, kind, Impl.GAS, v -> v.gas); }
    }

    private static final class InfusionView extends ChemicalView<InfuseType, InfusionStack, IInfusionHandler> implements IInfusionHandler {
        InfusionView(InterfaceBlockEntity be, TransferKind kind) { super(be, kind, Impl.INFUSION, v -> v.infusion); }
    }

    private static final class PigmentView extends ChemicalView<Pigment, PigmentStack, IPigmentHandler> implements IPigmentHandler {
        PigmentView(InterfaceBlockEntity be, TransferKind kind) { super(be, kind, Impl.PIGMENT, v -> v.pigment); }
    }

    private static final class SlurryView extends ChemicalView<Slurry, SlurryStack, ISlurryHandler> implements ISlurryHandler {
        SlurryView(InterfaceBlockEntity be, TransferKind kind) { super(be, kind, Impl.SLURRY, v -> v.slurry); }
    }

    private MekanismCompat() {}
}
