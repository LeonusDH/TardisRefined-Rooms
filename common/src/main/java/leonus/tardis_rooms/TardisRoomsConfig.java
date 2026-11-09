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
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REMOVABLE_WITH_ROOM;
    /** По умолчанию: таблички, флаги и головы — в них не хранят вещей. */
    private static final List<String> REMOVABLE_DEFAULT = List.of("#minecraft:all_signs", "#minecraft:banners",
            "minecraft:skeleton_skull", "minecraft:skeleton_wall_skull", "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_wall_skull",
            "minecraft:zombie_head", "minecraft:zombie_wall_head", "minecraft:creeper_head", "minecraft:creeper_wall_head",
            "minecraft:piglin_head", "minecraft:piglin_wall_head", "minecraft:dragon_head", "minecraft:dragon_wall_head",
            "minecraft:player_head", "minecraft:player_wall_head");

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
        REMOVABLE_WITH_ROOM = b
                .comment("Блоки, которые не мешают убрать или заменить помещение и уходят вместе с ним, что бы в них ни было:",
                        "id блоков и метки через #, например \"#minecraft:all_signs\". Содержимое таких блоков пропадает,",
                        "поэтому не добавляйте блоки, в которых игроки хранят вещи. Интерфейс ТАРДИС мешает всегда.",
                        "Помещения из датапаков могут отметить такие блоки у себя сами (поле removable в JSON помещения).")
                .defineListAllowEmpty("removableWithRoom", REMOVABLE_DEFAULT, () -> "", RemovableBlocks::valid);
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

    public static List<? extends String> removableWithRoom() {
        return SPEC.isLoaded() ? REMOVABLE_WITH_ROOM.get() : REMOVABLE_DEFAULT;
    }

    public static boolean debug() {
        return SPEC.isLoaded() ? DEBUG.get() : DEBUG.getDefault();
    }

    public static double doorLeverClosedAngle() {
        return CLIENT_SPEC.isLoaded() ? DOOR_LEVER_CLOSED_ANGLE.get() : DOOR_LEVER_CLOSED_ANGLE.getDefault();
    }

    private TardisRoomsConfig() {}
}
