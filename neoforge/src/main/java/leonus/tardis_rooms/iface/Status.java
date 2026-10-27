package leonus.tardis_rooms.iface;

/** Подключён ли интерфейс. */
public enum Status {
    /** Есть идентификатор, интерфейс в своей ТАРДИС (или у её оболочки). */
    CONNECTED,
    /** Идентификатор не задан — связывать не с чем. */
    NO_ID,
    /** Идентификатор принадлежит другой ТАРДИС. */
    WRONG_TARDIS,
    /** Вне ТАРДИС, а оболочки рядом нет (например, ТАРДИС улетела). */
    NO_SHELL,
    /** Рядом оболочки двух и более ТАРДИС. */
    CONFLICT;

    /** Строка состояния для экрана и Jade. */
    public net.minecraft.network.chat.Component text() {
        return switch (this) {
            case CONNECTED -> leonus.tardis_rooms.lang.TRoomsText.IFACE_STATUS_CONNECTED.text().withStyle(net.minecraft.ChatFormatting.GREEN);
            case NO_ID -> leonus.tardis_rooms.lang.TRoomsText.IFACE_STATUS_NO_ID.text().withStyle(net.minecraft.ChatFormatting.YELLOW);
            case WRONG_TARDIS -> leonus.tardis_rooms.lang.TRoomsText.IFACE_STATUS_WRONG_TARDIS.text().withStyle(net.minecraft.ChatFormatting.RED);
            case NO_SHELL -> leonus.tardis_rooms.lang.TRoomsText.IFACE_STATUS_NO_SHELL.text().withStyle(net.minecraft.ChatFormatting.RED);
            case CONFLICT -> leonus.tardis_rooms.lang.TRoomsText.IFACE_STATUS_CONFLICT.text().withStyle(net.minecraft.ChatFormatting.RED);
        };
    }

    public static Status of(int i) { return i >= 0 && i < values().length ? values()[i] : NO_SHELL; }
}
