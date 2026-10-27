package leonus.tardis_rooms.registry;

import leonus.tardis_rooms.TardisRooms;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Объект, который регистрирует платформа: в общем коде он объявлен, а настоящий объект появляется,
 * когда платформа его зарегистрирует (NeoForge — через DeferredRegister, Fabric — сразу).
 */
public final class RegEntry<T> implements Supplier<T> {
    private static final List<RegEntry<?>> ALL = new ArrayList<>();

    private final ResourceKey<? extends Registry<? super T>> registry;
    private final ResourceLocation id;
    private final Supplier<? extends T> factory;
    private Supplier<? extends T> value;

    private RegEntry(ResourceKey<? extends Registry<? super T>> registry, String name, Supplier<? extends T> factory) {
        this.registry = registry;
        this.id = ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, name);
        this.factory = factory;
    }

    /** Объявить объект. Вызывается из статических полей классов-реестров (TRoomsBlocks). */
    public static <T> RegEntry<T> of(ResourceKey<? extends Registry<? super T>> registry, String name, Supplier<? extends T> factory) {
        RegEntry<T> e = new RegEntry<>(registry, name, factory);
        ALL.add(e);
        return e;
    }

    /** Все объявленные объекты в порядке объявления — для платформы. */
    public static List<RegEntry<?>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public ResourceKey<? extends Registry<? super T>> registry() { return registry; }

    public ResourceLocation id() { return id; }

    public Supplier<? extends T> factory() { return factory; }

    /** Платформа сообщает, где взять зарегистрированный объект. */
    public void bind(Supplier<? extends T> registered) {
        this.value = registered;
    }

    @Override
    public T get() {
        Supplier<? extends T> v = value;
        if (v == null) throw new IllegalStateException("Not registered yet: " + id);
        return v.get();
    }
}
