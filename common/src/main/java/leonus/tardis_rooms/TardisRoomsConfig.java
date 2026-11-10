package leonus.tardis_rooms;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import leonus.tardis_rooms.room.RemovableBlocks;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;


public final class TardisRoomsConfig {
    /** Документация на GitHub — ссылки в комментариях конфигов. */
    private static final String DOCS = "https://github.com/LeonusDH/TardisRefined-Rooms/blob/1.20.1/docs/";

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DISABLE_PROCEDURAL_ROOMS;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.BooleanValue SIMPLIFY_HUB;
    public static final ForgeConfigSpec.BooleanValue COMPAT_MEKANISM, COMPAT_AE2, COMPAT_JADE;

    /** Отдельный файл: блоки, которые уходят вместе с помещением (сборке проще дописать свои моды). */
    public static final ForgeConfigSpec REMOVABLE_SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REMOVABLE_BLOCKS;

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue DOOR_LEVER_CLOSED_ANGLE;

    static {
        // Комментарии конфигов — по-английски: файл пишется один раз на сервере, перевести его под язык игрока нельзя.
        // По-русски то же описано в docs/configuration.ru.md и docs/block-entries.ru.md.
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        // Второй comment() подряд заменил бы первый — ссылка на описание всех параметров идёт в комментарий первого.
        DISABLE_PROCEDURAL_ROOMS = b
                .comment("All options: " + DOCS + "configuration.md",
                        "",
                        "Don't generate Tardis Refined's procedural ARS rooms and corridors around the TARDIS interior.",
                        "The console room, the Eye of Harmony and the central corridor hub stay.",
                        "Affects only chunks that haven't been generated yet.")
                .define("disableProceduralRooms", true);
        SIMPLIFY_HUB = b
                .comment("Simplify the Tardis Refined hub: keep the airlock centre, the Eye of Harmony viewing chamber and two short",
                        "passages to growth nodes, and remove the rest of the hub's corridors and rooms to make room for your own.",
                        "Done once per TARDIS, when a player enters it. If something stands in the hub (chests, machines, item frames)",
                        "or the hub doesn't match the Tardis Refined templates, the hub is left as it is.")
                .define("simplifyHub", true);
        DEBUG = b
                .comment("Debug: also write the addon's details to the main log (latest.log): players' actions with rooms, interfaces",
                        "and the screwdriver, why an action was refused, interface status changes, the connectivity graph when removing",
                        "or replacing. The details are always logged at DEBUG level, but only some loaders keep such lines: Forge writes",
                        "them to logs/debug.log, Fabric by default doesn't keep them at all. Rooms grown, replaced or removed and other",
                        "changes to the world always go to latest.log.")
                .define("debug", false);
        b.comment("Compatibility with other mods. An option does nothing if that mod isn't installed.").push("compat");
        COMPAT_MEKANISM = b.comment("Mekanism: chemicals through the TARDIS Interface.").define("mekanism", true);
        COMPAT_AE2 = b.comment("Applied Energistics 2: ME networks through the TARDIS Interface and the Memory Card for IDs.").define("ae2", true);
        COMPAT_JADE = b.comment("Jade: TARDIS Interface status in the tooltip.").define("jade", true);
        b.pop();
        SPEC = b.build();

        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        DOOR_LEVER_CLOSED_ANGLE = c
                .comment("Factory console: how many degrees the door lever turns when the door is closed.",
                        "If the lever turns the wrong way, change the sign. 0 turns the animation off.")
                .defineInRange("doorLeverClosedAngle", 60.0, -180.0, 180.0);
        CLIENT_SPEC = c.build();

        ForgeConfigSpec.Builder r = new ForgeConfigSpec.Builder();
        REMOVABLE_BLOCKS = r
                .comment("Blocks that never keep a TARDIS room from being removed or replaced and go with it, whatever is in them.",
                        "",
                        "Entries:",
                        "  \"minecraft:lectern\"            - a block",
                        "  \"#minecraft:banners\"           - blocks with the tag",
                        "  \"mekanism:*_cable\", \"*:*_sign\" - block ids where * is any text (without a namespace: minecraft)",
                        "  \"@mekanism\"                    - every block of the mod (alternatively: \"mekanism:*\")",
                        "  \"minecraft:player_head{SkullOwner:{Name:\\\"MHF_Question\\\"}}\", \"#minecraft:candles[lit=false]\"",
                        "                                  - as in /execute if block: block states and data (data matches partially)",
                        "",
                        "WARNING: anything in these blocks is lost together with the room. Don't add blocks where players keep things;",
                        "be especially careful with \"@mod\": in tech mods almost every block holds something.",
                        "TARDIS Interfaces always count. Wrong entries are skipped with a warning in the log.",
                        "Changes are usually picked up by themselves; if not, run /reload. Datapack rooms mark such blocks for themselves",
                        "(the removable field of the room JSON). More: " + DOCS + "block-entries.md")
                .defineListAllowEmpty(List.of("blocks"), () -> RemovableBlocks.DEFAULT, o -> o instanceof String s && !s.isBlank());
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

    /**
     * Перечитывает removable_with_room.toml по /reload. Следилка за файлами в NightConfig 1.20.1 пропускает
     * изменение, если Windows склеила уведомления в одно (count > 1), поэтому правка может не подхватиться сама.
     * Делает то же, что следилка: загрузить, исправить неверное, сбросить кэш значений.
     */
    public static void reloadRemovable(Object data) {
        if (!(data instanceof CommentedFileConfig file)) return;
        file.load();
        if (!REMOVABLE_SPEC.isCorrect(file)) {
            REMOVABLE_SPEC.correct(file);
            file.save();
        }
        REMOVABLE_SPEC.afterReload();
        TardisRooms.LOGGER.info("{} reloaded: {} entr(ies) in use", TardisRooms.REMOVABLE_CONFIG, RemovableBlocks.current().size());
    }

    public static double doorLeverClosedAngle() {
        return CLIENT_SPEC.isLoaded() ? DOOR_LEVER_CLOSED_ANGLE.get() : DOOR_LEVER_CLOSED_ANGLE.getDefault();
    }

    private TardisRoomsConfig() {}
}
