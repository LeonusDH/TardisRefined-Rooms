package leonus.tardis_rooms.iface;

/**
 * То, что у загрузчика общее для всех видов передачи одного интерфейса (NeoForge: сообщить о смене возможностей; сеть ME).
 * Сами виды — части ({@link leonus.tardis_rooms.iface.kind.KindPart}). Создаёт {@link InterfacePlatform#createTransport}.
 */
public interface InterfaceTransport {
    /** Что интерфейс принимает и отдаёт, изменилось (виды, направление, связь) — сообщить трубам и кабелям. */
    void invalidate();

    /** Раз в секунду на сервере, после пересчёта связи. */
    void tick();

    /** Блок убран или выгружен. */
    void remove();
}
