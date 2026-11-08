package leonus.tardis_rooms.iface.kind;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Реестр видов передачи в игре. Платформа заполняет его один раз при запуске мода ({@link #bootstrap(List)}),
 * раньше, чем появится первый интерфейс.
 */
public final class TransferKinds {
    private static @Nullable KindRegistry registry;

    private TransferKinds() {}

    public static synchronized void bootstrap(List<KindPlugin> plugins) {
        if (registry != null) throw new IllegalStateException("Transfer kinds are already registered");
        KindRegistry r = KindRegistry.build(plugins);
        BuiltinKinds.bind(r);
        registry = r;
    }

    public static boolean ready() { return registry != null; }

    private static KindRegistry registry() {
        KindRegistry r = registry;
        if (r == null) throw new IllegalStateException("Transfer kinds are not registered yet");
        return r;
    }

    /** Все виды в порядке регистрации (в нём же они в меню). */
    public static List<TransferKind> all() { return registry().all(); }

    /** Вид по id; null — такого вида в этой игре нет. */
    public static @Nullable TransferKind byId(ResourceLocation id) { return registry().byId(id); }
}
