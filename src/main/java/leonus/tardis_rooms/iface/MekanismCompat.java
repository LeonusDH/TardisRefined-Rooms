package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;

/**
 * Химикаты Mekanism. Классы Mekanism здесь загружаются, только если Mekanism установлен
 * (всё обращение к этому классу — после проверки {@link #LOADED}).
 */
public final class MekanismCompat {
    /** Установлен ли Mekanism (возможность регистрируем только тогда). */
    public static final boolean LOADED = ModList.get().isLoaded("mekanism");

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
            event.registerBlockEntity(cap(), TRoomsBlocks.INTERFACE_BE.get(), (be, side) -> (IChemicalHandler) be.chemicals());
        }
    }

    /** Грань «вставлять»/«забирать» для химикатов. */
    static void moveFace(InterfaceBlockEntity be, net.minecraft.core.BlockPos np, Direction side, boolean pull, long max) {
        if (!enabled() || be.getLevel() == null) return;
        Mover.move(be, np, side, pull, max);
    }

    private static final class Mover {
        static void move(InterfaceBlockEntity be, net.minecraft.core.BlockPos np, Direction side, boolean pull, long max) {
            IChemicalHandler h = be.getLevel().getCapability(cap(), np, side);
            if (!(be.chemicals() instanceof IChemicalHandler mine) || h == null) return;
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

    static Object newView(InterfaceBlockEntity be) {
        return enabled() ? new ChemicalView(be) : null;
    }

    /** Химикаты через интерфейс — так же, как жидкости. */
    private static final class ChemicalView implements IChemicalHandler {
        private final InterfaceBlockEntity be;

        ChemicalView(InterfaceBlockEntity be) { this.be = be; }

        private int inTanks() { return be.canIn(TransferType.CHEMICALS) ? 1 : 0; }

        private List<IChemicalHandler> sources() {
            return be.canOut(TransferType.CHEMICALS) ? be.handlers(cap(), TransferType.CHEMICALS, true) : List.of();
        }

        private Object[] locate(int tank) {
            int t = tank - inTanks();
            if (t < 0) return null;
            for (IChemicalHandler h : sources()) {
                if (t < h.getChemicalTanks()) return new Object[]{h, t};
                t -= h.getChemicalTanks();
            }
            return null;
        }

        @Override
        public int getChemicalTanks() {
            int n = inTanks();
            for (IChemicalHandler h : sources()) n += h.getChemicalTanks();
            return n;
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
            return l == null ? Long.MAX_VALUE : ((IChemicalHandler) l[0]).getChemicalTankCapacity((int) l[1]);
        }

        @Override
        public boolean isValid(int tank, ChemicalStack stack) {
            return be.canIn(TransferType.CHEMICALS);
        }

        @Override
        public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
            if (stack.isEmpty() || !be.canIn(TransferType.CHEMICALS) || InterfaceNetwork.depth > 0) return stack;
            InterfaceNetwork.depth++;
            try {
                ChemicalStack rest = stack;
                for (IChemicalHandler h : be.handlers(cap(), TransferType.CHEMICALS, false)) {
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
    }

    private MekanismCompat() {}
}
