package leonus.tardis_rooms.gametest;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.BlockMatcher;
import leonus.tardis_rooms.room.BlockerHighlight;
import leonus.tardis_rooms.room.RoomBlockEntities;
import leonus.tardis_rooms.room.RoomPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Игровые тесты: какие блок-сущности мешают удалить помещение ({@link RoomBlockEntities}). Вместо помещения — небольшая
 * коробка внутри места теста (помещение заняло бы модуль 24×24 и задело соседние тесты). Общие для платформ.
 *
 * <p>Раскладка: сундук — (1,1,1), кафедра — (2,1,1).
 */
public final class RoomGameTests {
    static final BlockPos CHEST = new BlockPos(1, 1, 1), LECTERN = new BlockPos(2, 1, 1), EXTRA = new BlockPos(3, 1, 1);

    private RoomGameTests() {}

    private static List<BoundingBox> box(GameTestHelper h) {
        // С (1,1,1): в (0,0,0) стоит служебный блок самого игрового теста.
        return List.of(BoundingBox.fromCorners(h.absolutePos(new BlockPos(1, 1, 1)), h.absolutePos(new BlockPos(4, 3, 3))));
    }

    /** Сундук и пустая кафедра — как в шаблоне помещения. */
    private static void furnish(GameTestHelper h) {
        h.setBlock(CHEST, Blocks.CHEST);
        h.setBlock(LECTERN, Blocks.LECTERN);
    }

    private static List<BlockPos> blockers(GameTestHelper h, Map<Long, Integer> template) {
        return blockers(h, template, Set.of(), List.of());
    }

    private static List<BlockPos> blockers(GameTestHelper h, Map<Long, Integer> template, Set<Long> marked, List<BlockMatcher> matchers) {
        return RoomBlockEntities.blockers(h.getLevel(), box(h), template, marked, matchers);
    }

    private static void expect(GameTestHelper h, List<BlockPos> actual, BlockPos... relative) {
        List<BlockPos> want = Arrays.stream(relative).map(h::absolutePos).toList();
        h.assertTrue(actual.size() == want.size() && actual.containsAll(want), "мешают " + actual + ", ожидалось " + want);
    }

    /** Нетронутые блок-сущности шаблона не мешают. */
    public static void templateUntouched(GameTestHelper h) {
        furnish(h);
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(h.getLevel(), box(h));
        h.assertTrue(template.size() == 2, "отпечатков " + template.size() + ", ожидалось 2");
        expect(h, blockers(h, template));
        h.succeed();
    }

    /** Положили что-то в сундук или книгу на кафедру — мешают. */
    public static void templateChanged(GameTestHelper h) {
        furnish(h);
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(h.getLevel(), box(h));
        ((Container) h.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.DIAMOND));
        expect(h, blockers(h, template), CHEST);
        ((LecternBlockEntity) h.getBlockEntity(LECTERN)).setBook(new ItemStack(Items.WRITABLE_BOOK));
        expect(h, blockers(h, template), CHEST, LECTERN);
        h.succeed();
    }

    /** Поставленное после роста (его не было в шаблоне) мешает, даже пустое. */
    public static void addedAfterGrowth(GameTestHelper h) {
        furnish(h);
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(h.getLevel(), box(h));
        h.setBlock(EXTRA, Blocks.BARREL);
        expect(h, blockers(h, template), EXTRA);
        h.succeed();
    }

    /** Блок на месте шаблонного заменили другим блоком с блок-сущностью — мешает. */
    public static void replacedBlock(GameTestHelper h) {
        furnish(h);
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(h.getLevel(), box(h));
        h.setBlock(CHEST, Blocks.BARREL);
        expect(h, blockers(h, template), CHEST);
        h.succeed();
    }

    /** Интерфейс мешает всегда: его ставит игрок, с настройками. */
    public static void interfaceBlocks(GameTestHelper h) {
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(h.getLevel(), box(h));
        h.setBlock(EXTRA, TRoomsBlocks.INTERFACE.get());
        expect(h, blockers(h, template), EXTRA);
        h.succeed();
    }

    /** Старое помещение (без отпечатков): пустые ванильные не мешают, с содержимым — мешают. */
    public static void oldRoomByContents(GameTestHelper h) {
        furnish(h);
        expect(h, blockers(h, null));
        ((Container) h.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.DIAMOND));
        expect(h, blockers(h, null), CHEST);
        ((LecternBlockEntity) h.getBlockEntity(LECTERN)).setBook(new ItemStack(Items.WRITABLE_BOOK));
        expect(h, blockers(h, null), CHEST, LECTERN);
        h.succeed();
    }

    /**
     * Отпечаток не меняется, если блок-сущность сохранили и загрузили заново (выгрузка чанка, перезапуск сервера):
     * иначе после перезахода любое помещение считалось бы тронутым.
     */
    public static void fingerprintSurvivesReload(GameTestHelper h) {
        furnish(h);
        ((Container) h.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.DIAMOND, 5));
        ((LecternBlockEntity) h.getBlockEntity(LECTERN)).setBook(new ItemStack(Items.WRITABLE_BOOK));
        ServerLevel level = h.getLevel();
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(level, box(h));
        for (BlockPos rel : List.of(CHEST, LECTERN)) {
            BlockPos pos = h.absolutePos(rel);
            BlockEntity old = level.getBlockEntity(pos);
            CompoundTag saved = old.saveWithFullMetadata();
            BlockEntity copy = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved);
            h.assertTrue(copy != null && copy != old, "не удалось загрузить блок-сущность в " + rel);
            level.setBlockEntity(copy);
        }
        expect(h, blockers(h, template));
        h.succeed();
    }

    /** Двойной сундук — один предмет с общей рамкой на два блока; одиночный рядом — отдельный. */
    public static void doubleChestIsOneTarget(GameTestHelper h) {
        BlockState left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockState right = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT);
        h.setBlock(CHEST, left);    // левая половина (смотрит на север) — пара к востоку
        h.setBlock(LECTERN, right); // (2,1,1)
        h.setBlock(new BlockPos(1, 1, 3), Blocks.CHEST);
        List<BlockPos> all = List.of(h.absolutePos(CHEST), h.absolutePos(LECTERN), h.absolutePos(new BlockPos(1, 1, 3)));
        List<BlockerHighlight.Target> t = BlockerHighlight.group(h.getLevel(), all);
        h.assertTrue(t.size() == 2, "предметов " + t.size() + ", ожидалось 2");
        double width = Math.max(t.get(0).box().getXsize(), t.get(1).box().getXsize());
        h.assertTrue(width > 1.5, "рамка двойного сундука шириной " + width);
        h.succeed();
    }

    /** Блоки из настройки по умолчанию (таблички, флаги) не мешают, даже поставленные после роста; интерфейс мешает. */
    public static void removableConfigIgnored(GameTestHelper h) {
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(h.getLevel(), box(h));
        h.setBlock(CHEST, Blocks.OAK_SIGN);
        h.setBlock(LECTERN, Blocks.WHITE_BANNER);
        expect(h, blockers(h, template));
        expect(h, blockers(h, null));
        h.setBlock(EXTRA, TRoomsBlocks.INTERFACE.get());
        expect(h, blockers(h, template), EXTRA);
        h.succeed();
    }

    /** Отмеченное помещением место не мешает, что бы в нём ни было; соседний блок — как обычно; интерфейс — всегда. */
    public static void removableMarkedIgnored(GameTestHelper h) {
        furnish(h);
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(h.getLevel(), box(h));
        ((Container) h.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.DIAMOND));
        ((LecternBlockEntity) h.getBlockEntity(LECTERN)).setBook(new ItemStack(Items.WRITABLE_BOOK));
        Set<Long> marked = Set.of(h.absolutePos(CHEST).asLong(), h.absolutePos(EXTRA).asLong());
        expect(h, blockers(h, template, marked, List.of()), LECTERN);
        h.setBlock(EXTRA, TRoomsBlocks.INTERFACE.get());
        expect(h, blockers(h, template, marked, List.of()), LECTERN, EXTRA);
        h.succeed();
    }

    /** Описания блоков: данные сравниваются частично, весь мод и шаблон со звёздочкой — по id. */
    public static void removableDescriptions(GameTestHelper h) {
        furnish(h);
        Map<Long, Integer> template = RoomBlockEntities.fingerprint(h.getLevel(), box(h));
        ((Container) h.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.DIAMOND));
        ((LecternBlockEntity) h.getBlockEntity(LECTERN)).setBook(new ItemStack(Items.WRITABLE_BOOK));
        List<BlockMatcher> diamondChest = List.of(BlockMatcher.parse("minecraft:chest{Items:[{id:\"minecraft:diamond\"}]}"));
        expect(h, blockers(h, template, Set.of(), diamondChest), LECTERN);
        ((Container) h.getBlockEntity(CHEST)).setItem(0, new ItemStack(Items.EMERALD));
        expect(h, blockers(h, template, Set.of(), diamondChest), CHEST, LECTERN);
        expect(h, blockers(h, template, Set.of(), List.of(BlockMatcher.parse("*:lect*"))), CHEST);
        h.setBlock(EXTRA, TRoomsBlocks.INTERFACE.get());
        expect(h, blockers(h, template, Set.of(),
                List.of(BlockMatcher.parse("@minecraft"), BlockMatcher.parse("@" + TardisRooms.MODID))), EXTRA);
        h.succeed();
    }

    // ---------- права ----------

    /**
     * Проверка прав: решает подставленная проверка; для нескольких ТАРДИС нужно право в каждой; ни одной — решать нечего;
     * сломанная проверка — отказ. Помещения в тестах не вырастить (нужно измерение ТАРДИС), поэтому — сама проверка.
     */
    public static void permissionChecks(GameTestHelper h) {
        ServerPlayer player = InterfaceGameTests.mockPlayer(h, BlockPos.ZERO);
        ResourceKey<Level> mine = h.getLevel().dimension();
        ResourceKey<Level> other = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(TardisRooms.MODID, "gametest_other"));
        RoomPermissions.Check saved = RoomPermissions.current();
        try {
            RoomPermissions.set((p, tardis, action) -> !(tardis.equals(other) && action == RoomPermissions.Action.MANAGE));
            h.assertTrue(RoomPermissions.allowed(player, mine, RoomPermissions.Action.MANAGE, "test"), "отказ там, где можно");
            h.assertTrue(!RoomPermissions.allowed(player, other, RoomPermissions.Action.MANAGE, "test"), "можно там, где отказ");
            h.assertTrue(RoomPermissions.allowed(player, other, RoomPermissions.Action.GROW, "test"), "отказ не в том действии");
            h.assertTrue(RoomPermissions.allowedInAll(player, List.of(), RoomPermissions.Action.MANAGE, "test"), "отказ без ТАРДИС");
            h.assertTrue(!RoomPermissions.allowedInAll(player, List.of(mine, other), RoomPermissions.Action.MANAGE, "test"),
                    "можно, хотя в одной из ТАРДИС отказ");
            RoomPermissions.set((p, tardis, action) -> { throw new IllegalStateException("game test: a broken permission check"); });
            h.assertTrue(!RoomPermissions.allowed(player, mine, RoomPermissions.Action.GROW, "test"), "сломанная проверка пропустила");
        } finally {
            RoomPermissions.set(saved);
        }
        h.succeed();
    }
}

