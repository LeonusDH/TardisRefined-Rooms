package leonus.tardis_rooms.iface;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Энергия через интерфейс: принятое во вход отдаётся соседям выходов, забираемое из выхода — у соседей входов. */
final class EnergyView implements IEnergyStorage {
    private final InterfaceBlockEntity be;

    EnergyView(InterfaceBlockEntity be) { this.be = be; }

    @Override
    public int receiveEnergy(int max, boolean simulate) {
        if (max <= 0 || !be.canIn(TransferType.ENERGY) || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            int done = 0;
            for (IEnergyStorage h : be.handlers(Capabilities.EnergyStorage.BLOCK, TransferType.ENERGY, false)) {
                if (done >= max) break;
                if (h.canReceive()) done += h.receiveEnergy(max - done, simulate);
            }
            return done;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    @Override
    public int extractEnergy(int max, boolean simulate) {
        if (max <= 0 || !be.canOut(TransferType.ENERGY) || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            int done = 0;
            for (IEnergyStorage h : be.handlers(Capabilities.EnergyStorage.BLOCK, TransferType.ENERGY, true)) {
                if (done >= max) break;
                if (h.canExtract()) done += h.extractEnergy(max - done, simulate);
            }
            return done;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    /** Своего запаса нет. */
    @Override
    public int getEnergyStored() { return 0; }

    /** Ёмкость условная: некоторые кабели не подключаются к хранилищу с нулевой ёмкостью. */
    @Override
    public int getMaxEnergyStored() { return Integer.MAX_VALUE; }

    @Override
    public boolean canExtract() { return be.canOut(TransferType.ENERGY); }

    @Override
    public boolean canReceive() { return be.canIn(TransferType.ENERGY); }
}
