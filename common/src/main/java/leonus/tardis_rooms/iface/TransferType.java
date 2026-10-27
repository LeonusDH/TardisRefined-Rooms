package leonus.tardis_rooms.iface;

/** Что может передавать интерфейс. Химикаты — с Mekanism, сеть ME — с Applied Energistics 2. Новые — только в конец (биты). */
public enum TransferType {
    ENERGY, ITEMS, FLUIDS, CHEMICALS, ME, REDSTONE;

    public int bit() { return 1 << ordinal(); }

    /** Установлен ли мод, без которого этот вид передачи не имеет смысла. */
    public boolean available() {
        return switch (this) {
            case CHEMICALS -> InterfacePlatform.chemicalsAvailable();
            case ME -> InterfacePlatform.meAvailable();
            default -> true;
        };
    }
}
