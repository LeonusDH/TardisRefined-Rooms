package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import leonus.tardis_rooms.iface.kind.TransferKind;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
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
    private final InterfaceBlockEntity be;
    private final TransferKind kind;
    /** Свои соседи этого вида — их берут связанные интерфейсы. */
    final LinkedStorages.Sides<Storage<T>> sides;
    /** Хранилища этого вида у соседей связанных интерфейсов. */
    private final LinkedStorages<Storage<T>> linked;

    /** @param sidesOf свои соседи этого вида у связанного интерфейса (null — вида у него нет) */
    ForwardingStorage(InterfaceBlockEntity be, TransferKind kind, BlockApiLookup<Storage<T>, Direction> lookup,
                      Function<InterfaceBlockEntity, LinkedStorages.Sides<Storage<T>>> sidesOf) {
        this.be = be;
        this.kind = kind;
        this.sides = FabricInterfaceTransport.sides(be, lookup);
        this.linked = new LinkedStorages<>(be, kind, sidesOf);
    }

    /** Для поиска хранилищ: null — вид выключен (трубы не подключатся). */
    Storage<T> storage() { return be.enabled(kind) ? this : null; }

    /** Грань: «забирать» — из соседа в себя (то есть дальше к выходам), «вставлять» — из себя (от входов) в соседа. */
    void moveFace(Direction face, boolean pull, long max) {
        Storage<T> h = sides.at(face);
        if (h == null) return;
        if (pull) StorageUtil.move(h, this, v -> true, max, null);
        else StorageUtil.move(this, h, v -> true, max, null);
    }

    @Override
    public boolean supportsInsertion() { return be.canIn(kind); }

    @Override
    public boolean supportsExtraction() { return be.canOut(kind); }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0 || !supportsInsertion() || InterfaceNetwork.depth > 0) return 0;
        InterfaceNetwork.depth++;
        try {
            long done = 0;
            for (Storage<T> h : linked.get(Pick.TO)) {
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
            for (Storage<T> h : linked.get(Pick.FROM)) {
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
            for (Storage<T> h : linked.get(Pick.FROM)) {
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
