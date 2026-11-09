package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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
    public Component text() {
        return switch (this) {
            case CONNECTED -> TRoomsText.IFACE_STATUS_CONNECTED.text().withStyle(ChatFormatting.GREEN);
            case NO_ID -> TRoomsText.IFACE_STATUS_NO_ID.text().withStyle(ChatFormatting.YELLOW);
            case WRONG_TARDIS -> TRoomsText.IFACE_STATUS_WRONG_TARDIS.text().withStyle(ChatFormatting.RED);
            case NO_SHELL -> TRoomsText.IFACE_STATUS_NO_SHELL.text().withStyle(ChatFormatting.RED);
            case CONFLICT -> TRoomsText.IFACE_STATUS_CONFLICT.text().withStyle(ChatFormatting.RED);
        };
    }

    public static Status of(int i) { return i >= 0 && i < values().length ? values()[i] : NO_SHELL; }
}
