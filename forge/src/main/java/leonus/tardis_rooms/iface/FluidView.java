package leonus.tardis_rooms.iface;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/** Жидкости через интерфейс: залитое во вход уходит в баки у выходов, сливаемое из выхода — из баков у входов. */
final class FluidView implements IFluidHandler {
    private final InterfaceBlockEntity be;
    private final ForgeInterfaceTransport transport;

    FluidView(InterfaceBlockEntity be, ForgeInterfaceTransport transport) {
        this.be = be;
        this.transport = transport;
    }

    private int inTanks() { return be.canIn(TransferType.FLUIDS) ? 1 : 0; }

    private List<IFluidHandler> sources() {
        return be.canOut(TransferType.FLUIDS) ? transport.handlers(ForgeCapabilities.FLUID_HANDLER, TransferType.FLUIDS, true) : List.of();
    }

    /**
     * Непустые баки у связанных интерфейсов (обработчик и номер бака). Пустые не показываем: забирать из них нечего,
     * а Jade рисовал бы их пустыми полосками.
     */
    private List<Object[]> filled() {
        List<Object[]> out = new java.util.ArrayList<>();
        for (IFluidHandler h : sources())
            for (int t = 0; t < h.getTanks(); t++) if (!h.getFluidInTank(t).isEmpty()) out.add(new Object[]{h, t});
        return out;
    }

    @Override
    public int getTanks() {
        return inTanks() + filled().size();
    }

    private Object[] locate(int tank) {
        int t = tank - inTanks();
        if (t < 0) return null;
        List<Object[]> f = filled();
        return t < f.size() ? f.get(t) : null;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        Object[] l = locate(tank);
        return l == null ? FluidStack.EMPTY : ((IFluidHandler) l[0]).getFluidInTank((int) l[1]);
    }

    @Override
    public int getTankCapacity(int tank) {
        Object[] l = locate(tank);
        // Приёмный бак всегда пуст (всё сразу уходит дальше): ёмкость 0, чтобы Jade не рисовал его пустой полоской.
        // Трубы заливают через fill(), ёмкость при этом не смотрят.
        return l == null ? 0 : ((IFluidHandler) l[0]).getTankCapacity((int) l[1]);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return be.canIn(TransferType.FLUIDS);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !be.canIn(TransferType.FLUIDS) || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            int done = 0;
            for (IFluidHandler h : transport.handlers(ForgeCapabilities.FLUID_HANDLER, TransferType.FLUIDS, false)) {
                if (done >= resource.getAmount()) break;
                done += h.fill(ForgeInterfaceTransport.withAmount(resource, resource.getAmount() - done), action);
            }
            return done;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !be.canOut(TransferType.FLUIDS) || InterfaceNetwork.depth > 0) return FluidStack.EMPTY;
        InterfaceNetwork.depth++;
        try {
            FluidStack out = FluidStack.EMPTY;
            for (IFluidHandler h : sources()) {
                int need = resource.getAmount() - out.getAmount();
                if (need <= 0) break;
                FluidStack got = h.drain(ForgeInterfaceTransport.withAmount(resource, need), action);
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
        if (maxDrain <= 0 || !be.canOut(TransferType.FLUIDS) || InterfaceNetwork.depth > 0) return FluidStack.EMPTY;
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
        return first.isEmpty() ? FluidStack.EMPTY : drain(ForgeInterfaceTransport.withAmount(first, maxDrain), action);
    }
}
