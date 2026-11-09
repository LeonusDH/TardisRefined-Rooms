package leonus.tardis_rooms.iface;

/** Слот (бак) интерфейса — на самом деле слот {@code index} хранилища {@code from} у связанного интерфейса. */
record HandlerSlot<H>(H from, int index) {}
