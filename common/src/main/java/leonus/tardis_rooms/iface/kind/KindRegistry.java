package leonus.tardis_rooms.iface.kind;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Набор зарегистрированных видов. Собирается один раз из плагинов и дальше не меняется. */
public final class KindRegistry {
    /** Наибольшее число видов: включённые виды интерфейса хранятся битами в int. */
    public static final int MAX_KINDS = 32;

    private final List<TransferKind> kinds;
    private final Map<ResourceLocation, TransferKind> byId;

    private KindRegistry(List<TransferKind> kinds, Map<ResourceLocation, TransferKind> byId) {
        this.kinds = kinds;
        this.byId = byId;
    }

    /** Собрать реестр: плагины по порядку, виды — в порядке регистрации. */
    public static KindRegistry build(List<KindPlugin> plugins) {
        List<TransferKind> kinds = new ArrayList<>();
        Map<ResourceLocation, TransferKind> byId = new HashMap<>();
        boolean[] open = {true};
        KindRegistration registration = id -> new TransferKind.Builder(id, b -> {
            if (!open[0]) throw new IllegalStateException("Transfer kind " + b.id() + " registered after registration closed");
            if (byId.containsKey(b.id())) throw new IllegalArgumentException("Duplicate transfer kind " + b.id());
            if (kinds.size() >= MAX_KINDS) throw new IllegalStateException("Too many transfer kinds (max " + MAX_KINDS + ")");
            TransferKind k = b.build(kinds.size());
            kinds.add(k);
            byId.put(k.id(), k);
            return k;
        });
        for (KindPlugin p : plugins) p.register(registration);
        open[0] = false;
        return new KindRegistry(List.copyOf(kinds), Map.copyOf(byId));
    }

    /** Все виды в порядке регистрации. */
    public List<TransferKind> all() { return kinds; }

    public @Nullable TransferKind byId(ResourceLocation id) { return byId.get(id); }

    public int size() { return kinds.size(); }
}
