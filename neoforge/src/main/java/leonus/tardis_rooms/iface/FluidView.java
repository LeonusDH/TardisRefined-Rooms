package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

/** Жидкости через интерфейс (часть вида): залитое во вход уходит в баки у выходов, сливаемое из выхода — из баков у входов. */
final class FluidView implements IFluidHandler, KindPart {
    private final InterfaceBlockEntity be;
    private final TransferKind kind;
    /** Свои соседи по жидкостям — их берут связанные интерфейсы. */
    private final LinkedStorages.Sides<IFluidHandler> sides;
    /** Баки у соседей связанных интерфейсов. */
    private final LinkedStorages<IFluidHandler> linked;

    FluidView(InterfaceBlockEntity be, TransferKind kind) {
        this.be = be;
        this.kind = kind;
        this.sides = NeoForgeInterfaceTransport.sides(be, Capabilities.FluidHandler.BLOCK);
        this.linked = new LinkedStorages<>(be, kind, peer -> peer.part(kind) instanceof FluidView v ? v.sides : null);
    }

    /** Для возможности блока: null — вид выключен (трубы не подключатся). */
    IFluidHandler capability() { return be.enabled(kind) ? this : null; }

    @Override
    public void moveFace(Direction face, boolean pull) {
        IFluidHandler h = sides.at(face);
        if (h == null) return;
        IFluidHandler from = pull ? h : this, to = pull ? this : h;
        FluidStack probe = from.drain(NeoForgeInterfaceTransport.FLUID_PER_OP, FluidAction.SIMULATE);
        if (probe.isEmpty()) return;
        int can = to.fill(probe, FluidAction.SIMULATE);
        if (can > 0) to.fill(from.drain(probe.copyWithAmount(can), FluidAction.EXECUTE), FluidAction.EXECUTE);
    }

    private int inTanks() { return be.canIn(kind) ? 1 : 0; }

    private List<IFluidHandler> sources() {
        return be.canOut(kind) ? linked.get(Pick.FROM) : List.of();
    }

    @Override
    public int getTanks() {
        int n = inTanks();
        for (IFluidHandler h : sources()) n += h.getTanks();
        return n;
    }

    /** Где на самом деле бак {@code tank}, или null (приёмный бак / нет такого). */
    private HandlerSlot<IFluidHandler> locate(int tank) {
        int t = tank - inTanks();
        if (t < 0) return null;
        for (IFluidHandler h : sources()) {
            int n = h.getTanks();
            if (t < n) return new HandlerSlot<>(h, t);
            t -= n;
        }
        return null;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        HandlerSlot<IFluidHandler> l = locate(tank);
        return l == null ? FluidStack.EMPTY : l.from().getFluidInTank(l.index());
    }

    @Override
    public int getTankCapacity(int tank) {
        HandlerSlot<IFluidHandler> l = locate(tank);
        // Приёмный бак (всегда пуст — всё сразу уходит дальше) и пустые баки соседей — с ёмкостью 0: Jade такие баки
        // пропускает и не рисует пустыми полосками. Трубы заливают через fill() и сливают через drain(), ёмкость не смотрят;
        // из пустого бака забирать всё равно нечего.
        if (l == null) return 0;
        IFluidHandler h = l.from();
        int t = l.index();
        return h.getFluidInTank(t).isEmpty() ? 0 : h.getTankCapacity(t);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return be.canIn(kind);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !be.canIn(kind) || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            int done = 0;
            for (IFluidHandler h : linked.get(Pick.TO)) {
                if (done >= resource.getAmount()) break;
                done += h.fill(resource.copyWithAmount(resource.getAmount() - done), action);
            }
            return done;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !be.canOut(kind) || InterfaceNetwork.depth > 0) return FluidStack.EMPTY;
        InterfaceNetwork.depth++;
        try {
            FluidStack out = FluidStack.EMPTY;
            for (IFluidHandler h : sources()) {
                int need = resource.getAmount() - out.getAmount();
                if (need <= 0) break;
                FluidStack got = h.drain(resource.copyWithAmount(need), action);
                if (got.isEmpty()) continue;
                if (out.isEmpty()) out = got.copy(); else out.grow(got.getAmount());
            }
            return out;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || !be.canOut(kind) || InterfaceNetwork.depth > 0) return FluidStack.EMPTY;
        // Берём первую жидкость, какая найдётся, и дальше только её.
        FluidStack first = FluidStack.EMPTY;
        InterfaceNetwork.depth++;
        try {
            for (IFluidHandler h : sources()) {
                FluidStack probe = h.drain(maxDrain, FluidAction.SIMULATE);
                if (!probe.isEmpty()) { first = probe; break; }
            }
        } finally {
            InterfaceNetwork.depth--;
        }
        return first.isEmpty() ? FluidStack.EMPTY : drain(first.copyWithAmount(maxDrain), action);
    }
}
