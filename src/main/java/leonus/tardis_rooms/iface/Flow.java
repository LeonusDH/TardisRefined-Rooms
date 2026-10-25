package leonus.tardis_rooms.iface;

/**
 * Направление интерфейса. Вход — принимает в себя (поданное сразу уходит связанным интерфейсам с выходом),
 * выход — отдаёт (забирает у соседей связанных интерфейсов со входом), или и то и другое.
 * На сеть ME не влияет: она связывается в обе стороны.
 */
public enum Flow {
    IN(true, false), OUT(false, true), BOTH(true, true);

    public final boolean in, out;

    Flow(boolean in, boolean out) { this.in = in; this.out = out; }

    public Flow next() { return values()[(ordinal() + 1) % values().length]; }

    public static Flow of(int i) { return i >= 0 && i < values().length ? values()[i] : BOTH; }
}
