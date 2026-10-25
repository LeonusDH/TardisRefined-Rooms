package leonus.tardis_rooms.iface;

/**
 * Режим грани интерфейса.
 * По умолчанию — грань ничего не делает сама: трубы и механизмы сами подают и забирают.
 * Вставлять — интерфейс сам кладёт в соседа с этой стороны то, что лежит у связанных интерфейсов со входом (нужен выход).
 * Забирать — интерфейс сам вынимает из соседа с этой стороны и отдаёт связанным интерфейсам с выходом (нужен вход).
 * Так интерфейс работает и с обычными сундуками, без труб.
 */
public enum FaceMode {
    DEFAULT, PUSH, PULL;

    public FaceMode next() { return values()[(ordinal() + 1) % values().length]; }

    public FaceMode prev() { return values()[(ordinal() + values().length - 1) % values().length]; }

    public static FaceMode of(int i) { return i >= 0 && i < values().length ? values()[i] : DEFAULT; }
}
