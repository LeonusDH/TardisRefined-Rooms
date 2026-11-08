package leonus.tardis_rooms.iface;

import java.util.List;
import java.util.Objects;

/** Текущая область связи игры. Задаётся при запуске мода; игровые тесты подставляют свою. */
public final class LinkScopes {
    /** Пока область не задана, интерфейсы нигде не работают. */
    private static volatile LinkScope current = (level, pos) -> List.of();

    private LinkScopes() {}

    public static LinkScope current() { return current; }

    public static void set(LinkScope scope) { current = Objects.requireNonNull(scope); }
}
