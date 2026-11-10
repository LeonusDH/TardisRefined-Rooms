package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.platform.Platform;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;


/**
 * Энергия через интерфейс на Fabric — Team Reborn Energy. Мод необязательный: вложенный {@link Impl} загружается,
 * только если он установлен (иначе его классы не нашлись бы).
 */
final class FabricEnergy {
    static final boolean LOADED = Platform.isModLoaded("team_reborn_energy");

    static void register() { Impl.register(); }

    /** Часть вида «энергия» для интерфейса. Только если Team Reborn Energy установлен. */
    static KindPart newPart(InterfaceBlockEntity be, TransferKind kind) { return Impl.newPart(be, kind); }

    private static final class Impl {
        static void register() {
            EnergyStorage.SIDED.registerForBlockEntity(
                    (be, side) -> be.open(side) && be.part(BuiltinKinds.energy()) instanceof View v ? v.capability() : null,
                    TRoomsBlocks.INTERFACE_BE.get());
        }

        static KindPart newPart(InterfaceBlockEntity be, TransferKind kind) { return new View(be, kind); }

        /** Принятое во вход отдаётся соседям выходов, забираемое из выхода — у соседей входов. Своего запаса нет. */
        static final class View implements EnergyStorage, KindPart, LinkedTotals {
            private final InterfaceBlockEntity be;
            private final TransferKind kind;
            /** Свои соседи по энергии — их берут связанные интерфейсы. */
            private final LinkedStorages.Sides<EnergyStorage> sides;
            /** Хранилища энергии у соседей связанных интерфейсов. */
            private final LinkedStorages<EnergyStorage> linked;

            View(InterfaceBlockEntity be, TransferKind kind) {
                this.be = be;
                this.kind = kind;
                this.sides = FabricInterfaceTransport.sides(be, EnergyStorage.SIDED);
                this.linked = new LinkedStorages<>(be, kind, peer -> peer.part(kind) instanceof View v ? v.sides : null);
            }

            /** Для поиска хранилищ: null — вид выключен (кабели не подключатся). */
            EnergyStorage capability() { return be.enabled(kind) ? this : null; }

            /** Грань «вставлять»/«забирать»: сосед со своей стороны {@code face}. */
            @Override
            public void moveFace(Direction face, boolean pull) {
                EnergyStorage h = sides.at(face);
                if (h == null) return;
                if (pull) EnergyStorageUtil.move(h, this, Long.MAX_VALUE, null);
                else EnergyStorageUtil.move(this, h, Long.MAX_VALUE, null);
            }

            @Override
            public boolean supportsInsertion() { return be.canIn(kind); }

            @Override
            public boolean supportsExtraction() { return be.canOut(kind); }

            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                if (maxAmount <= 0 || !supportsInsertion() || InterfaceNetwork.depth > 0) return 0;
                InterfaceNetwork.depth++;
                try {
                    long done = 0;
                    for (EnergyStorage h : linked.get(Pick.TO)) {
                        if (done >= maxAmount) break;
                        if (h.supportsInsertion()) done += h.insert(maxAmount - done, transaction);
                    }
                    return done;
                } finally {
                    InterfaceNetwork.depth--;
                }
            }

            @Override
            public long extract(long maxAmount, TransactionContext transaction) {
                if (maxAmount <= 0 || !supportsExtraction() || InterfaceNetwork.depth > 0) return 0;
                InterfaceNetwork.depth++;
                try {
                    long done = 0;
                    for (EnergyStorage h : linked.get(Pick.FROM)) {
                        if (done >= maxAmount) break;
                        if (h.supportsExtraction()) done += h.extract(maxAmount - done, transaction);
                    }
                    return done;
                } finally {
                    InterfaceNetwork.depth--;
                }
            }

            /** Своего запаса нет: показываем заряд хранилищ у связанных интерфейсов (его и рисует Jade). */
            @Override
            public long getAmount() { return linkedTotals()[0]; }

            /** Если связанных хранилищ нет — ёмкость условная: некоторые кабели не подключаются к хранилищу с нулевой ёмкостью. */
            @Override
            public long getCapacity() {
                long cap = linkedTotals()[1];
                return cap > 0 ? cap : Integer.MAX_VALUE;
            }

            /** Заряд и ёмкость хранилищ у связанных интерфейсов (настоящие, без условной ёмкости). */
            @Override
            public long[] linkedTotals() {
                // Только накопители — то, что и принимает, и отдаёт. Машины (контроллер ME, печь) и генераторы энергию не хранят
                // для других, их буфер — не заряд сети.
                return linked.totals(s -> s.supportsInsertion() && s.supportsExtraction(), EnergyStorage::getAmount, EnergyStorage::getCapacity);
            }
        }
    }

    private FabricEnergy() {}
}
