package leonus.tardis_rooms.room.custom;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Помещение из датапака: {@code data/<ns>/tardis_rooms/rooms/<имя>.json}. Строится из шаблонов (по одному на этаж),
 * поворачивается под направление узла. Координаты — в шаблоне, как он сохранён (без поворота); модуль — 24×24 блока.
 *
 * <pre>{@code
 * {
 *   "name": "Library",                       // текст или {"translate": "ключ"}
 *   "description": "Shelves and reading desks", // необязательно
 *   "group": "room",                         // room / corridor / special; по умолчанию — «Из датапаков»
 *   "structure": "example:rooms/library",     // или список — по шаблону на этаж, снизу вверх
 *   "entrance": {"x": 0, "z": 0, "side": "south"},   // выход, которым помещение пристраивается к узлу
 *   "exits": [{"x": 0, "z": 0, "side": "north"}],    // остальные выходы; "floor": 1 — на втором этаже
 *   "control_block": [11, 0, 11],            // необязательно; блок управления (y = 0 — пол нижнего этажа)
 *   "removable": [[3, 2, 5], [3, 2, 5, 1]],   // необязательно; блоки, которые уходят вместе с помещением, что бы в них
 *                                            // ни было: x, y, z в шаблоне и этаж (по умолчанию 0)
 *   "rotatable": false                       // необязательно (по умолчанию true): false — строится только от узла,
 *                                            // смотрящего туда же, куда вход шаблона, без поворота
 * }
 * }</pre>
 */
public record CustomRoom(Component name, Optional<Component> description, String group, List<ResourceLocation> structures,
                         Exit entrance, List<Exit> exits, Optional<BlockPos> controlBlock, List<Mark> removable, boolean rotatable) {

    /** Отмеченный блок: место в шаблоне этажа {@code floor} (0 — нижний). В JSON — [x, y, z] или [x, y, z, этаж]. */
    public record Mark(BlockPos pos, int floor) {
        public static final Codec<Mark> CODEC = Codec.INT.listOf().comapFlatMap(
                l -> l.size() == 3 || l.size() == 4
                        ? DataResult.success(new Mark(new BlockPos(l.get(0), l.get(1), l.get(2)), l.size() == 4 ? l.get(3) : 0))
                        : DataResult.error(() -> "removable: each entry is [x, y, z] or [x, y, z, floor], got " + l),
                m -> m.floor() == 0 ? List.of(m.pos().getX(), m.pos().getY(), m.pos().getZ())
                        : List.of(m.pos().getX(), m.pos().getY(), m.pos().getZ(), m.floor()));
    }

    /** Выход: модуль шаблона (x, z), сторона и этаж (0 — нижний). */
    public record Exit(int x, int z, Direction side, int floor) {
        public static final Codec<Exit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Exit::x),
                Codec.INT.fieldOf("z").forGetter(Exit::z),
                Direction.CODEC.fieldOf("side").forGetter(Exit::side),
                Codec.INT.optionalFieldOf("floor", 0).forGetter(Exit::floor)
        ).apply(i, Exit::new));
    }

    public static final String DEFAULT_GROUP = "datapack";

    private static final Codec<List<ResourceLocation>> STRUCTURES = Codec.either(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf())
            .xmap(e -> e.map(List::of, l -> l), l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l));

    public static final Codec<CustomRoom> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(CustomRoom::name),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(CustomRoom::description),
            Codec.STRING.optionalFieldOf("group", DEFAULT_GROUP).forGetter(CustomRoom::group),
            STRUCTURES.fieldOf("structure").forGetter(CustomRoom::structures),
            Exit.CODEC.fieldOf("entrance").forGetter(CustomRoom::entrance),
            Exit.CODEC.listOf().optionalFieldOf("exits", List.of()).forGetter(CustomRoom::exits),
            BlockPos.CODEC.optionalFieldOf("control_block").forGetter(CustomRoom::controlBlock),
            Mark.CODEC.listOf().optionalFieldOf("removable", List.of()).forGetter(CustomRoom::removable),
            Codec.BOOL.optionalFieldOf("rotatable", true).forGetter(CustomRoom::rotatable)
    ).apply(i, CustomRoom::new));

    public int floors() { return structures.size(); }
}
