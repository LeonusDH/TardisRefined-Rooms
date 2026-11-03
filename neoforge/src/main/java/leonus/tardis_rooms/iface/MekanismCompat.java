package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import leonus.tardis_rooms.platform.Platform;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Химикаты Mekanism. Классы Mekanism здесь загружаются, только если Mekanism установлен
 * (всё обращение к этому классу — после проверки {@link #LOADED}).
 */
public final class MekanismCompat {
    /** Установлен ли Mekanism (возможность регистрируем только тогда). */
    public static final boolean LOADED = Platform.isModLoaded("mekanism");

    /** Mekanism есть и совместимость не выключена в конфиге. */
    public static boolean enabled() {
        return LOADED && leonus.tardis_rooms.TardisRoomsConfig.compatMekanism();
    }

    /** Та же возможность, что у Mekanism (возможности NeoForge совпадают по имени и типу). */
    private static BlockCapability<IChemicalHandler, Direction> cap() {
        return Holder.CAP;
    }

    private static final class Holder {
        static final BlockCapability<IChemicalHandler, Direction> CAP =
                BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"), IChemicalHandler.class);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        if (!LOADED) return;
        Registration.register(event);
    }

    /** Отдельный класс, чтобы лямбда с типами Mekanism не грузилась без него. */
    private static final class Registration {
        static void register(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(cap(), TRoomsBlocks.INTERFACE_BE.get(), (be, side) -> be.open(side) ? (IChemicalHandler) NeoForgeInterfaceTransport.of(be).chemicals() : null);
        }
    }

    /** Грань «вставлять»/«забирать» для химикатов: сосед со своей стороны {@code face}. */
    static void moveFace(NeoForgeInterfaceTransport transport, Direction face, boolean pull, long max) {
        if (!enabled()) return;
        Mover.move(transport, face, pull, max);
    }

    private static final class Mover {
        static void move(NeoForgeInterfaceTransport transport, Direction face, boolean pull, long max) {
            if (!(transport.chemicals() instanceof ChemicalView mine)) return;
            IChemicalHandler h = mine.sides.at(face);
            if (h == null) return;
            IChemicalHandler from = pull ? h : mine, to = pull ? mine : h;
            ChemicalStack probe = from.extractChemical(max, Action.SIMULATE);
            if (probe.isEmpty()) return;
            ChemicalStack rest = to.insertChemical(probe, Action.SIMULATE);
            long can = probe.getAmount() - rest.getAmount();
            if (can <= 0) return;
            ChemicalStack moved = from.extractChemical(probe.copyWithAmount(can), Action.EXECUTE);
            to.insertChemical(moved, Action.EXECUTE);
        }
    }

    static Object newView(InterfaceBlockEntity be, NeoForgeInterfaceTransport transport) {
        return enabled() ? new ChemicalView(be, transport) : null;
    }

    /**
     * Химикаты через интерфейс — так же, как жидкости. Баки — только непустые баки у связанных интерфейсов:
     * подсказка Mekanism в Jade рисует каждый бак, даже пустой и с нулевой ёмкостью, поэтому приёмного бака нет вовсе.
     * Трубы Mekanism вставляют и забирают через {@link #insertChemical(ChemicalStack, Action)} и
     * {@link #extractChemical(long, Action)} — они переопределены и баков не перебирают.
     */
    private static final class ChemicalView implements IChemicalHandler {
        private final InterfaceBlockEntity be;
        private final NeoForgeInterfaceTransport transport;
        /** Свои соседи по химикатам — их берут связанные интерфейсы. */
        final LinkedStorages.Sides<IChemicalHandler> sides;
        /** Хранилища химикатов у соседей связанных интерфейсов (у кого химикаты выключены, вида нет — пропускаются). */
        private final LinkedStorages<IChemicalHandler> linked;

        ChemicalView(InterfaceBlockEntity be, NeoForgeInterfaceTransport transport) {
            this.be = be;
            this.transport = transport;
            this.sides = NeoForgeInterfaceTransport.sides(be, cap());
            this.linked = new LinkedStorages<>(be, TransferType.CHEMICALS,
                    peer -> NeoForgeInterfaceTransport.of(peer).chemicals() instanceof ChemicalView v ? v.sides : null);
        }

        private List<IChemicalHandler> sources() {
            return be.canOut(TransferType.CHEMICALS) ? linked.get(Pick.FROM) : List.of();
        }

        /** Непустые баки у связанных интерфейсов (обработчик и номер бака). */
        private List<Object[]> filled() {
            List<Object[]> out = new ArrayList<>();
            for (IChemicalHandler h : sources())
                for (int t = 0; t < h.getChemicalTanks(); t++) if (!h.getChemicalInTank(t).isEmpty()) out.add(new Object[]{h, t});
            return out;
        }

        private Object[] locate(int tank) {
            if (tank < 0) return null;
            List<Object[]> f = filled();
            return tank < f.size() ? f.get(tank) : null;
        }

        @Override
        public int getChemicalTanks() {
            return filled().size();
        }

        @Override
        public ChemicalStack getChemicalInTank(int tank) {
            Object[] l = locate(tank);
            return l == null ? ChemicalStack.EMPTY : ((IChemicalHandler) l[0]).getChemicalInTank((int) l[1]);
        }

        @Override
        public void setChemicalInTank(int tank, ChemicalStack stack) {
            // Своих баков нет.
        }

        @Override
        public long getChemicalTankCapacity(int tank) {
            Object[] l = locate(tank);
            return l == null ? 0 : ((IChemicalHandler) l[0]).getChemicalTankCapacity((int) l[1]);
        }

        @Override
        public boolean isValid(int tank, ChemicalStack stack) {
            return be.canIn(TransferType.CHEMICALS);
        }

        @Override
        public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
            return insertChemical(stack, action);
        }

        /** Поданное — сразу в хранилища у связанных выходов. */
        @Override
        public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
            if (stack.isEmpty() || !be.canIn(TransferType.CHEMICALS) || InterfaceNetwork.depth > 0) return stack;
            InterfaceNetwork.depth++;
            try {
                ChemicalStack rest = stack;
                for (IChemicalHandler h : linked.get(Pick.TO)) {
                    rest = h.insertChemical(rest, action);
                    if (rest.isEmpty()) break;
                }
                return rest;
            } finally {
                InterfaceNetwork.depth--;
            }
        }

        @Override
        public ChemicalStack extractChemical(int tank, long amount, Action action) {
            if (amount <= 0 || InterfaceNetwork.depth > 0) return ChemicalStack.EMPTY;
            Object[] l = locate(tank);
            if (l == null) return ChemicalStack.EMPTY;
            InterfaceNetwork.depth++;
            try {
                return ((IChemicalHandler) l[0]).extractChemical((int) l[1], amount, action);
            } finally {
                InterfaceNetwork.depth--;
            }
        }

        /** Забрать что угодно: первый химикат, какой найдётся, — и дальше только его, по всем хранилищам (как у жидкостей). */
        @Override
        public ChemicalStack extractChemical(long amount, Action action) {
            if (amount <= 0 || !be.canOut(TransferType.CHEMICALS) || InterfaceNetwork.depth > 0) return ChemicalStack.EMPTY;
            ChemicalStack first = ChemicalStack.EMPTY;
            InterfaceNetwork.depth++;
            try {
                for (IChemicalHandler h : sources()) {
                    ChemicalStack probe = h.extractChemical(amount, Action.SIMULATE);
                    if (!probe.isEmpty()) { first = probe; break; }
                }
            } finally {
                InterfaceNetwork.depth--;
            }
            return first.isEmpty() ? ChemicalStack.EMPTY : extractChemical(first.copyWithAmount(amount), action);
        }

        /** Забрать именно этот химикат — сколько наберётся по всем хранилищам у связанных входов. */
        @Override
        public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
            if (stack.isEmpty() || !be.canOut(TransferType.CHEMICALS) || InterfaceNetwork.depth > 0) return ChemicalStack.EMPTY;
            InterfaceNetwork.depth++;
            try {
                ChemicalStack out = ChemicalStack.EMPTY;
                for (IChemicalHandler h : sources()) {
                    long need = stack.getAmount() - out.getAmount();
                    if (need <= 0) break;
                    ChemicalStack got = h.extractChemical(stack.copyWithAmount(need), action);
                    if (got.isEmpty()) continue;
                    if (out.isEmpty()) out = got.copy(); else out.grow(got.getAmount());
                }
                return out;
            } finally {
                InterfaceNetwork.depth--;
            }
        }
    }

    private MekanismCompat() {}
}
