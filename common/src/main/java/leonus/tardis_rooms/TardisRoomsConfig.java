package leonus.tardis_rooms;

import leonus.tardis_rooms.room.RemovableBlocks;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;


public final class TardisRoomsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DISABLE_PROCEDURAL_ROOMS;
    public static final ModConfigSpec.BooleanValue DEBUG;
    public static final ModConfigSpec.BooleanValue SIMPLIFY_HUB;
    public static final ModConfigSpec.BooleanValue COMPAT_MEKANISM, COMPAT_AE2, COMPAT_JADE;

    /** Отдельный файл: блоки, которые уходят вместе с помещением (сборке проще дописать свои моды). */
    public static final ModConfigSpec REMOVABLE_SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REMOVABLE_BLOCKS;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.DoubleValue DOOR_LEVER_CLOSED_ANGLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        DISABLE_PROCEDURAL_ROOMS = b
                .comment("Не генерировать процедурные ARS-комнаты и коридоры вокруг интерьера TARDIS.",
                        "Консольная комната, Око Гармонии и центральный хаб коридоров остаются.",
                        "Влияет только на ещё не сгенерированные чанки.")
                .define("disableProceduralRooms", true);
        SIMPLIFY_HUB = b
                .comment("Упростить хаб TR: оставить центр со шлюзом, смотровую Ока и два коротких перехода к узлам роста,",
                        "а остальные коридоры и комнаты хаба убрать — на их месте растут свои помещения.",
                        "Делается один раз на ТАРДИС при входе. Если в хабе что-то стоит (сундуки, механизмы, рамки)",
                        "или хаб не совпадает с шаблонами TR, хаб не трогается.")
                .define("simplifyHub", true);
        DEBUG = b
                .comment("Отладка: писать в лог подробности работы аддона — кто что пристроил, заменил или удалил,",
                        "почему действие отклонено, граф связности при удалении и замене, расстановку узлов в хабе.",
                        "Выключено — в лог попадает только строка о загрузке мода.")
                .define("debug", false);
        b.comment("Совместимость с другими модами. Если мода нет, пункт ни на что не влияет.").push("compat");
        COMPAT_MEKANISM = b.comment("Mekanism: передача химикатов через интерфейс ТАРДИС.").define("mekanism", true);
        COMPAT_AE2 = b.comment("Applied Energistics 2: сеть ME через интерфейс ТАРДИС и карта памяти для ID.").define("ae2", true);
        COMPAT_JADE = b.comment("Jade: состояние интерфейса ТАРДИС в подсказке.").define("jade", true);
        b.pop();
        SPEC = b.build();

        ModConfigSpec.Builder c = new ModConfigSpec.Builder();
        DOOR_LEVER_CLOSED_ANGLE = c
                .comment("Консоль Factory: на сколько градусов поворачивается рычаг двери, когда дверь закрыта.",
                        "Если рычаг уходит не в ту сторону, поменяй знак. 0 отключает анимацию.")
                .defineInRange("doorLeverClosedAngle", 60.0, -180.0, 180.0);
        CLIENT_SPEC = c.build();

        ModConfigSpec.Builder r = new ModConfigSpec.Builder();
        REMOVABLE_BLOCKS = r
                .comment("Блоки, которые не мешают убрать или заменить помещение ТАРДИС и уходят вместе с ним, что бы в них ни было.",
                        "",
                        "Записи:",
                        "  \"minecraft:lectern\"            — блок",
                        "  \"#minecraft:banners\"           — блоки с тегом",
                        "  \"mekanism:*_cable\", \"*:*_sign\" — id, где * — любой текст (без пространства имён — minecraft)",
                        "  \"@mekanism\"                    — все блоки мода (альтернативно: \"mekanism:*\")",
                        "  \"minecraft:player_head{profile:{name:\\\"MHF_Question\\\"}}\", \"#minecraft:candles[lit=false]\"",
                        "                                  — как в /execute if block: свойства и данные (данные — частично)",
                        "",
                        "ВНИМАНИЕ: всё, что лежит в этих блоках, пропадает вместе с помещением. Не добавляйте блоки, в которых игроки",
                        "хранят вещи; с \"@мод\" будьте особенно осторожны — у модов техники почти каждый блок что-то хранит.",
                        "Интерфейс ТАРДИС мешает всегда. Неправильные записи пропускаются с предупреждением в логе.",
                        "Изменения подхватываются сами, перезапуск не нужен. Помещения из датапаков отмечают такие блоки у себя сами",
                        "(поле removable в JSON помещения). Подробнее: docs/block-entries.ru.md в репозитории аддона.")
                .defineListAllowEmpty("blocks", RemovableBlocks.DEFAULT, () -> "", o -> o instanceof String s && !s.isBlank());
        REMOVABLE_SPEC = r.build();
    }

    /** Безопасно читает значение: до загрузки конфига используется значение по умолчанию. */
    public static boolean disableProceduralRooms() {
        return SPEC.isLoaded() ? DISABLE_PROCEDURAL_ROOMS.get() : DISABLE_PROCEDURAL_ROOMS.getDefault();
    }

    public static boolean simplifyHub() {
        return SPEC.isLoaded() ? SIMPLIFY_HUB.get() : SIMPLIFY_HUB.getDefault();
    }

    public static boolean compatMekanism() {
        return SPEC.isLoaded() ? COMPAT_MEKANISM.get() : COMPAT_MEKANISM.getDefault();
    }

    public static boolean compatAE2() {
        return SPEC.isLoaded() ? COMPAT_AE2.get() : COMPAT_AE2.getDefault();
    }

    public static boolean compatJade() {
        return SPEC.isLoaded() ? COMPAT_JADE.get() : COMPAT_JADE.getDefault();
    }

    public static boolean debug() {
        return SPEC.isLoaded() ? DEBUG.get() : DEBUG.getDefault();
    }

    /** Строки списка блоков, уходящих вместе с помещением (до загрузки файла — по умолчанию). */
    public static List<? extends String> removableBlocks() {
        return REMOVABLE_SPEC.isLoaded() ? REMOVABLE_BLOCKS.get() : RemovableBlocks.DEFAULT;
    }

    public static double doorLeverClosedAngle() {
        return CLIENT_SPEC.isLoaded() ? DOOR_LEVER_CLOSED_ANGLE.get() : DOOR_LEVER_CLOSED_ANGLE.getDefault();
    }

    private TardisRoomsConfig() {}
}
