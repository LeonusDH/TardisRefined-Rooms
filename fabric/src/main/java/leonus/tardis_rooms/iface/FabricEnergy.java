package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.platform.Platform;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

/**
 * Энергия через интерфейс на Fabric — Team Reborn Energy. Мод необязательный: вложенный {@link Impl} загружается,
 * только если он установлен (иначе его классы не нашлись бы).
 */
final class FabricEnergy {
    static final boolean LOADED = Platform.isModLoaded("team_reborn_energy");

    static void register() { Impl.register(); }

    static Object view(FabricInterfaceTransport transport) { return new Impl.View(transport); }

    static void moveFace(FabricInterfaceTransport transport, Level level, BlockPos np, Direction side, boolean pull) {
        Impl.moveFace(transport, level, np, side, pull);
    }

    private static final class Impl {
        static void register() {
            EnergyStorage.SIDED.registerForBlockEntity(
                    (be, side) -> (EnergyStorage) FabricInterfaceTransport.of(be).energy(), TRoomsBlocks.INTERFACE_BE.get());
        }

        static void moveFace(FabricInterfaceTransport transport, Level level, BlockPos np, Direction side, boolean pull) {
            EnergyStorage h = EnergyStorage.SIDED.find(level, np, side);
            if (h == null || !(transport.energy() instanceof EnergyStorage mine)) return;
            if (pull) EnergyStorageUtil.move(h, mine, Long.MAX_VALUE, null);
            else EnergyStorageUtil.move(mine, h, Long.MAX_VALUE, null);
        }

        /** Принятое во вход отдаётся соседям выходов, забираемое из выхода — у соседей входов. Своего запаса нет. */
        static final class View implements EnergyStorage {
            private final FabricInterfaceTransport transport;

            View(FabricInterfaceTransport transport) { this.transport = transport; }

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
                    for (EnergyStorage h : transport.handlers(EnergyStorage.SIDED, TransferType.ENERGY, false)) {
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
                    for (EnergyStorage h : transport.handlers(EnergyStorage.SIDED, TransferType.ENERGY, true)) {
                        if (done >= maxAmount) break;
                        if (h.supportsExtraction()) done += h.extract(maxAmount - done, transaction);
                    }
                    return done;
                } finally {
                    InterfaceNetwork.depth--;
                }
            }

            @Override
            public long getAmount() { return 0; }

            /** Ёмкость условная: некоторые кабели не подключаются к хранилищу с нулевой ёмкостью. */
            @Override
            public long getCapacity() { return Integer.MAX_VALUE; }
        }
    }

    private FabricEnergy() {}
}
