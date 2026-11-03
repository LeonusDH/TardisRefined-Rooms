package leonus.tardis_rooms.iface;

import com.google.common.math.LongMath;
import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import leonus.tardis_rooms.platform.Platform;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.List;

/**
 * Энергия через интерфейс на Fabric — Team Reborn Energy. Мод необязательный: вложенный {@link Impl} загружается,
 * только если он установлен (иначе его классы не нашлись бы).
 */
final class FabricEnergy {
    static final boolean LOADED = Platform.isModLoaded("team_reborn_energy");

    static void register() { Impl.register(); }

    static Object view(FabricInterfaceTransport transport) { return new Impl.View(transport); }

    /** Грань «вставлять»/«забирать» для энергии: сосед со своей стороны {@code face}. */
    static void moveFace(FabricInterfaceTransport transport, Direction face, boolean pull) {
        Impl.moveFace(transport, face, pull);
    }

    private static final class Impl {
        static void register() {
            EnergyStorage.SIDED.registerForBlockEntity(
                    (be, side) -> (EnergyStorage) FabricInterfaceTransport.of(be).energy(), TRoomsBlocks.INTERFACE_BE.get());
        }

        static void moveFace(FabricInterfaceTransport transport, Direction face, boolean pull) {
            if (!(transport.energy() instanceof View mine)) return;
            EnergyStorage h = mine.sides.at(face);
            if (h == null) return;
            if (pull) EnergyStorageUtil.move(h, mine, Long.MAX_VALUE, null);
            else EnergyStorageUtil.move(mine, h, Long.MAX_VALUE, null);
        }

        /** Принятое во вход отдаётся соседям выходов, забираемое из выхода — у соседей входов. Своего запаса нет. */
        static final class View implements EnergyStorage {
            private final FabricInterfaceTransport transport;
            /** Свои соседи по энергии — их берут связанные интерфейсы. */
            final ApiSides<EnergyStorage> sides;

            View(FabricInterfaceTransport transport) {
                this.transport = transport;
                this.sides = new ApiSides<>(transport.be, EnergyStorage.SIDED);
            }

            /** Хранилища энергии у соседей связанных интерфейсов. */
            private List<EnergyStorage> linked(Pick pick) {
                return transport.linked(TransferType.ENERGY, pick, t -> t.energy() instanceof View v ? v.sides : null);
            }

            @Override
            public boolean supportsInsertion() { return transport.be.canIn(TransferType.ENERGY); }

            @Override
            public boolean supportsExtraction() { return transport.be.canOut(TransferType.ENERGY); }

            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                if (maxAmount <= 0 || !supportsInsertion() || InterfaceNetwork.depth > 0) return 0;
                InterfaceNetwork.depth++;
                try {
                    long done = 0;
                    for (EnergyStorage h : linked(Pick.TO)) {
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
                    for (EnergyStorage h : linked(Pick.FROM)) {
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
            public long getAmount() { return totals()[0]; }

            /** Если связанных хранилищ нет — ёмкость условная: некоторые кабели не подключаются к хранилищу с нулевой ёмкостью. */
            @Override
            public long getCapacity() {
                long cap = totals()[1];
                return cap > 0 ? cap : Integer.MAX_VALUE;
            }

            /** Заряд и ёмкость хранилищ у связанных интерфейсов. */
            private long[] totals() {
                long[] out = new long[2];
                if (InterfaceNetwork.depth > 0) return out;
                InterfaceNetwork.depth++;
                try {
                    for (EnergyStorage h : linked(Pick.ALL)) {
                        out[0] = LongMath.saturatedAdd(out[0], h.getAmount());
                        out[1] = LongMath.saturatedAdd(out[1], h.getCapacity());
                    }
                } finally {
                    InterfaceNetwork.depth--;
                }
                return out;
            }
        }
    }

    private FabricEnergy() {}
}
