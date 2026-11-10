package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Энергия через интерфейс (часть вида): принятое во вход отдаётся соседям выходов, забираемое из выхода — у соседей входов.
 */
final class EnergyView implements IEnergyStorage, KindPart, LinkedTotals {
    private final InterfaceBlockEntity be;
    private final TransferKind kind;
    /** Свои соседи по энергии — их берут связанные интерфейсы. */
    private final LinkedStorages.Sides<IEnergyStorage> sides;
    /** Хранилища энергии у соседей связанных интерфейсов. */
    private final LinkedStorages<IEnergyStorage> linked;

    EnergyView(InterfaceBlockEntity be, TransferKind kind) {
        this.be = be;
        this.kind = kind;
        this.sides = ForgeInterfaceTransport.sides(be, ForgeCapabilities.ENERGY);
        this.linked = new LinkedStorages<>(be, kind, peer -> peer.part(kind) instanceof EnergyView v ? v.sides : null);
    }

    /** Для возможности блока: null — вид выключен (кабели не подключатся). */
    IEnergyStorage capability() { return be.enabled(kind) ? this : null; }

    @Override
    public void moveFace(Direction face, boolean pull) {
        IEnergyStorage h = sides.at(face);
        if (h == null) return;
        IEnergyStorage from = pull ? h : this, to = pull ? this : h;
        int can = to.receiveEnergy(from.extractEnergy(Integer.MAX_VALUE, true), true);
        if (can > 0) to.receiveEnergy(from.extractEnergy(can, false), false);
    }

    @Override
    public int receiveEnergy(int max, boolean simulate) {
        if (max <= 0 || !be.canIn(kind) || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            int done = 0;
            for (IEnergyStorage h : linked.get(Pick.TO)) {
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
        if (max <= 0 || !be.canOut(kind) || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            int done = 0;
            for (IEnergyStorage h : linked.get(Pick.FROM)) {
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
    public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, linkedTotals()[0]); }

    /** Если связанных хранилищ нет — ёмкость условная: некоторые кабели не подключаются к хранилищу с нулевой ёмкостью. */
    @Override
    public int getMaxEnergyStored() {
        long cap = linkedTotals()[1];
        return cap > 0 ? (int) Math.min(Integer.MAX_VALUE, cap) : Integer.MAX_VALUE;
    }

    /** Заряд и ёмкость хранилищ у связанных интерфейсов (настоящие, без условной ёмкости). */
    @Override
    public long[] linkedTotals() {
        // Только накопители — то, что и принимает, и отдаёт. Машины (контроллер ME, печь) и генераторы энергию не хранят для
        // других, их буфер — не заряд сети.
        return linked.totals(h -> h.canReceive() && h.canExtract(), IEnergyStorage::getEnergyStored, IEnergyStorage::getMaxEnergyStored);
    }

    @Override
    public boolean canExtract() { return be.canOut(kind); }

    @Override
    public boolean canReceive() { return be.canIn(kind); }
}
