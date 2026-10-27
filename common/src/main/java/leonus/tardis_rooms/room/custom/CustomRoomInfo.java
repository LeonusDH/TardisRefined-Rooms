package leonus.tardis_rooms.room.custom;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.Lattice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Помещение из датапака — то, что о нём нужно меню и проверке «влезает ли» (без шаблонов). Сервер собирает его при загрузке
 * и рассылает клиентам. {@code preview} — план нижнего этажа сверху: по байту на блок, упакованный цвет карты
 * ({@link MapColor#getPackedId}), 0 — пусто; ряды по z, в каждом x (как шаблон без поворота).
 */
public record CustomRoomInfo(ResourceLocation id, Component name, Optional<Component> description, String group,
                             int width, int depth, int floors, CustomRoom.Exit entrance, List<CustomRoom.Exit> exits, byte[] preview) {

    /** Где клиент держит картинку превью (динамическая текстура, см. {@code ClientCustomRooms}). */
    public ResourceLocation previewTexture() {
        return ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "dynamic/preview/" + id.getNamespace() + "/" + id.getPath());
    }

    public int widthBlocks() { return width * Lattice.MODULE; }

    public int depthBlocks() { return depth * Lattice.MODULE; }

    /** Центр прохода входа в блоках шаблона (для рамки на превью). */
    public int[] entranceBlock() {
        CustomRoom.Exit e = entrance;
        int ox = e.x() * Lattice.MODULE, oz = e.z() * Lattice.MODULE, mid = (Lattice.OPEN_MIN + Lattice.OPEN_MAX) / 2, last = Lattice.MODULE - 1;
        return switch (e.side()) {
            case NORTH -> new int[]{ox + mid, oz};
            case SOUTH -> new int[]{ox + mid, oz + last};
            case WEST -> new int[]{ox, oz + mid};
            default -> new int[]{ox + last, oz + mid};
        };
    }

    static CustomRoomInfo of(CustomRooms.Loaded room) {
        CustomRoom def = room.def();
        return new CustomRoomInfo(room.id(), def.name(), def.description(), def.group(), room.width(), room.depth(), def.floors(),
                def.entrance(), def.exits(), preview(room));
    }

    /**
     * План сверху: в каждом столбце — первый блок сверху вниз, начиная с высоты верха прохода (так виден пол с мебелью,
     * а не потолок). Стены и всё, что выше пола, — светлее.
     */
    private static byte[] preview(CustomRooms.Loaded room) {
        int w = room.width() * Lattice.MODULE, d = room.depth() * Lattice.MODULE;
        Map<BlockPos, BlockState> blocks = CustomRooms.blocksOf(room.templates().get(0));
        int top = Math.min(Lattice.OPEN_DY_MAX, room.templates().get(0).getSize().getY() - 1);
        byte[] out = new byte[w * d];
        for (int z = 0; z < d; z++)
            for (int x = 0; x < w; x++)
                for (int y = top; y >= 0; y--) {
                    BlockState s = blocks.get(new BlockPos(x, y, z));
                    if (s == null || s.isAir()) continue;
                    MapColor c;
                    try {
                        c = s.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                    } catch (RuntimeException ex) {
                        c = MapColor.STONE; // блок, которому для цвета нужен настоящий мир
                    }
                    if (c == MapColor.NONE) continue;
                    out[z * w + x] = c.getPackedId(y >= Lattice.OPEN_DY_MIN ? MapColor.Brightness.HIGH : MapColor.Brightness.NORMAL);
                    break;
                }
        return out;
    }

    // ---------- сеть ----------

    private static final StreamCodec<RegistryFriendlyByteBuf, CustomRoom.Exit> EXIT = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CustomRoom.Exit::x,
            ByteBufCodecs.VAR_INT, CustomRoom.Exit::z,
            Direction.STREAM_CODEC, CustomRoom.Exit::side,
            ByteBufCodecs.VAR_INT, CustomRoom.Exit::floor,
            CustomRoom.Exit::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomRoomInfo> STREAM_CODEC = StreamCodec.of(
            (buf, i) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, i.id);
                ComponentSerialization.STREAM_CODEC.encode(buf, i.name);
                ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buf, i.description);
                buf.writeUtf(i.group);
                buf.writeVarInt(i.width);
                buf.writeVarInt(i.depth);
                buf.writeVarInt(i.floors);
                EXIT.encode(buf, i.entrance);
                buf.writeVarInt(i.exits.size());
                for (CustomRoom.Exit e : i.exits) EXIT.encode(buf, e);
                buf.writeByteArray(i.preview);
            },
            buf -> {
                ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                Component name = ComponentSerialization.STREAM_CODEC.decode(buf);
                Optional<Component> desc = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buf);
                String group = buf.readUtf();
                int w = buf.readVarInt(), d = buf.readVarInt(), floors = buf.readVarInt();
                CustomRoom.Exit entrance = EXIT.decode(buf);
                int n = buf.readVarInt();
                List<CustomRoom.Exit> exits = new ArrayList<>(n);
                for (int k = 0; k < n; k++) exits.add(EXIT.decode(buf));
                byte[] preview = buf.readByteArray();
                return new CustomRoomInfo(id, name, desc, group, w, d, floors, entrance, List.copyOf(exits), preview);
            });
}
