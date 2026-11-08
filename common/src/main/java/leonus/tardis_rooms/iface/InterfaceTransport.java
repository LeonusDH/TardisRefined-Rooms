package leonus.tardis_rooms.iface;

/**
 * То, что у загрузчика общее для всех видов передачи одного интерфейса (NeoForge: сообщить о смене возможностей).
 * Сами виды — части ({@link leonus.tardis_rooms.iface.kind.KindPart}). Создаёт {@link InterfacePlatform#createTransport}.
 */
public interface InterfaceTransport {
    /** Что интерфейс принимает и отдаёт, изменилось (виды, направление, связь, стороны) — сообщить трубам и кабелям. */
    void invalidate();
}
