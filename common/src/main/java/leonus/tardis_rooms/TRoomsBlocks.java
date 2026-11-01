package leonus.tardis_rooms;

import leonus.tardis_rooms.iface.InterfaceBlock;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceItem;
import leonus.tardis_rooms.platform.Platform;
import leonus.tardis_rooms.registry.RegEntry;
import leonus.tardis_rooms.room.ProtectedBlock;
import leonus.tardis_rooms.room.RoomCoreBlock;
import leonus.tardis_rooms.room.RoomNodeBlock;
import leonus.tardis_rooms.upper.UpperHalfBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public final class TRoomsBlocks {
    /** Интерфейс ТАРДИС: передаёт энергию, предметы, жидкости и химикаты между помещениями (и оболочкой). */
    public static final RegEntry<InterfaceBlock> INTERFACE = block("tardis_interface", InterfaceBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.5F, 6.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL).lightLevel(s -> 5));
    public static final RegEntry<InterfaceItem> INTERFACE_ITEM = RegEntry.of(Registries.ITEM, "tardis_interface", () -> new InterfaceItem(INTERFACE.get(), new Item.Properties()));

    // Настройки интерфейса, режим отвёртки и запомненный идентификатор хранятся в NBT предметов (см. ItemData).
    public static final RegEntry<BlockEntityType<InterfaceBlockEntity>> INTERFACE_BE =
            RegEntry.of(Registries.BLOCK_ENTITY_TYPE, "tardis_interface", () -> Platform.blockEntityType(InterfaceBlockEntity::new, INTERFACE.get()));

    /** Заглушка прохода: панель, закрывающая тупик. Неразрушима, убирается при пристройке. */
    public static final RegEntry<ProtectedBlock> ROOM_WALL = block("room_wall", ProtectedBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(-1.0F, 3600000.0F)
                    .noLootTable().sound(SoundType.METAL));

    /** Центр панели: по нему кликают отвёрткой, чтобы выбрать комнату. */
    public static final RegEntry<RoomNodeBlock> ROOM_NODE = block("room_node", RoomNodeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F)
                    .noLootTable().sound(SoundType.METAL).lightLevel(s -> 7));

    /** Блок управления помещением в полу (заменить/удалить). */
    public static final RegEntry<RoomCoreBlock> ROOM_CORE = block("room_core", RoomCoreBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(-1.0F, 3600000.0F)
                    .noLootTable().sound(SoundType.METAL).lightLevel(s -> 9));

    /**
     * Фонарь у заглушки: закрывает ниши края прохода, за которыми камень TARDIS (иначе видно «золото дураков»).
     * Свой блок, а не морской фонарь — чтобы при открытии прохода убрать только свои.
     */
    public static final RegEntry<ProtectedBlock> PANEL_LAMP = block("panel_lamp", ProtectedBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(-1.0F, 3600000.0F)
                    .noLootTable().sound(SoundType.GLASS).lightLevel(s -> 15));

    /** Невидимая верхняя половина двери/оболочки TARDIS (см. UpperHalfBlock). */
    public static final RegEntry<UpperHalfBlock> UPPER_HALF = block("upper_half", UpperHalfBlock::new,
            BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().dynamicShape()
                    .pushReaction(PushReaction.BLOCK).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                    .isRedstoneConductor((s, l, p) -> false));

    private static <B extends Block> RegEntry<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties props) {
        return RegEntry.of(Registries.BLOCK, name, () -> factory.apply(props));
    }

    /** Загрузить класс, чтобы все объекты были объявлены до регистрации. */
    public static void init() {}

    private TRoomsBlocks() {}
}
