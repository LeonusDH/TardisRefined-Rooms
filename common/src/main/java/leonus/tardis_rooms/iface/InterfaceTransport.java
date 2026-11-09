package leonus.tardis_rooms.iface;

/**
 * То, что у загрузчика общее для всех видов передачи одного интерфейса (Forge: LazyOptional для труб и свои соседи).
 * Сами виды — части ({@link leonus.tardis_rooms.iface.kind.KindPart}). Создаёт {@link InterfacePlatform#createTransport}.
 */
public interface InterfaceTransport {
    /** Что интерфейс принимает и отдаёт, изменилось (виды, направление, связь, стороны) — сообщить трубам и кабелям. */
    void invalidate();

    /** Соседний блок изменился (сервер): запомненных соседей проверить заново. */
    default void neighborChanged() {}

    /** Блок убран или его чанк выгружается: возможностей больше не отдавать. */
    default void remove() {}
}
