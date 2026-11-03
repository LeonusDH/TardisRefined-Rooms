package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Энергия через интерфейс: принятое во вход отдаётся соседям выходов, забираемое из выхода — у соседей входов. */
final class EnergyView implements IEnergyStorage {
    private final InterfaceBlockEntity be;
    private final NeoForgeInterfaceTransport transport;

    EnergyView(InterfaceBlockEntity be, NeoForgeInterfaceTransport transport) {
        this.be = be;
        this.transport = transport;
    }

    @Override
    public int receiveEnergy(int max, boolean simulate) {
        if (max <= 0 || !be.canIn(TransferType.ENERGY) || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            int done = 0;
            for (IEnergyStorage h : transport.linkedEnergy(Pick.TO)) {
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
            for (IEnergyStorage h : transport.linkedEnergy(Pick.FROM)) {
                if (done >= max) break;
                if (h.canExtract()) done += h.extractEnergy(max - done, simulate);
            }
            return done;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    /** Своего запаса нет: показываем заряд хранилищ у связанных интерфейсов (его и рисует Jade). */
    @Override
    public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, totals()[0]); }

    /** Если связанных хранилищ нет — ёмкость условная: некоторые кабели не подключаются к хранилищу с нулевой ёмкостью. */
    @Override
    public int getMaxEnergyStored() {
        long cap = totals()[1];
        return cap > 0 ? (int) Math.min(Integer.MAX_VALUE, cap) : Integer.MAX_VALUE;
    }

    /** Заряд и ёмкость хранилищ у связанных интерфейсов. */
    private long[] totals() {
        long[] out = new long[2];
        if (InterfaceNetwork.depth > 0) return out;
        InterfaceNetwork.depth++;
        try {
            for (IEnergyStorage h : transport.linkedEnergy(Pick.ALL)) {
                out[0] += h.getEnergyStored();
                out[1] += h.getMaxEnergyStored();
            }
        } finally {
            InterfaceNetwork.depth--;
        }
        return out;
    }

    @Override
    public boolean canExtract() { return be.canOut(TransferType.ENERGY); }

    @Override
    public boolean canReceive() { return be.canIn(TransferType.ENERGY); }
}
