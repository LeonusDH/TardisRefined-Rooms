package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Хранилище интерфейса (предметы или жидкости): своего запаса нет. Вставленное сразу уходит в хранилища у связанных
 * интерфейсов с выходом, а забирают — из хранилищ у связанных интерфейсов со входом. Транзакции ведут сами эти хранилища.
 */
final class ForwardingStorage<T> implements Storage<T> {
    private final FabricInterfaceTransport transport;
    private final TransferType type;
    /** Как найти такое же хранилище у транспорта связанного интерфейса. */
    private final Function<FabricInterfaceTransport, ForwardingStorage<T>> sameKindOf;
    /** Свои соседи этого вида — их берут связанные интерфейсы. */
    final ApiSides<Storage<T>> sides;

    ForwardingStorage(FabricInterfaceTransport transport, TransferType type, BlockApiLookup<Storage<T>, Direction> lookup,
                      Function<FabricInterfaceTransport, ForwardingStorage<T>> sameKindOf) {
        this.transport = transport;
        this.type = type;
        this.sameKindOf = sameKindOf;
        this.sides = new ApiSides<>(transport.be, lookup);
    }

    /** Хранилища у соседей связанных интерфейсов. */
    private List<Storage<T>> linked(Pick pick) {
        return transport.linked(type, pick, t -> sameKindOf.apply(t).sides);
    }

    @Override
    public boolean supportsInsertion() { return transport.be.canIn(type); }

    @Override
    public boolean supportsExtraction() { return transport.be.canOut(type); }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0 || !supportsInsertion() || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            long done = 0;
            for (Storage<T> h : linked(Pick.TO)) {
                if (done >= maxAmount) break;
                if (h.supportsInsertion()) done += h.insert(resource, maxAmount - done, transaction);
            }
            return done;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0 || !supportsExtraction() || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            long done = 0;
            for (Storage<T> h : linked(Pick.FROM)) {
                if (done >= maxAmount) break;
                if (h.supportsExtraction()) done += h.extract(resource, maxAmount - done, transaction);
            }
            return done;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    /** Содержимое хранилищ у связанных входов — чтобы трубы видели, что можно забрать. */
    @Override
    public Iterator<StorageView<T>> iterator() {
        if (!supportsExtraction() || InterfaceNetwork.depth > 0) return Collections.emptyIterator();
        InterfaceNetwork.depth++;
        try {
            List<StorageView<T>> views = new ArrayList<>();
            for (Storage<T> h : linked(Pick.FROM)) {
                if (!h.supportsExtraction()) continue;
                // Пустые места нечего забирать — и Jade не рисует их пустыми полосками.
                for (StorageView<T> v : h) if (!v.isResourceBlank() && v.getAmount() > 0) views.add(v);
            }
            return views.iterator();
        } finally {
            InterfaceNetwork.depth--;
        }
    }
}
