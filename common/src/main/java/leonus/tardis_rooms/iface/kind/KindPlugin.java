package leonus.tardis_rooms.iface.kind;

/** Источник видов передачи. Встроенные виды регистрируются тем же путём ({@link BuiltinKinds}). */
@FunctionalInterface
public interface KindPlugin {
    void register(KindRegistration registration);
}
