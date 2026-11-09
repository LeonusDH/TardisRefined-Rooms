package leonus.tardis_rooms.gametest;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.room.RoomBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        return List.of(BoundingBox.fromCorners(h.absolutePos(new BlockPos(0, 0, 0)), h.absolutePos(new BlockPos(4, 3, 3))));
    }

    /** Сундук и пустая кафедра — как в шаблоне помещения. */
    private static void furnish(GameTestHelper h) {
        h.setBlock(CHEST, Blocks.CHEST);
        h.setBlock(LECTERN, Blocks.LECTERN);
    }

    private static List<BlockPos> blockers(GameTestHelper h, Map<Long, Integer> template) {
        return RoomBlockEntities.blockers(h.getLevel(), box(h), template);
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
            CompoundTag saved = old.saveWithFullMetadata(level.registryAccess());
            BlockEntity copy = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
            h.assertTrue(copy != null && copy != old, "не удалось загрузить блок-сущность в " + rel);
            level.setBlockEntity(copy);
        }
        expect(h, blockers(h, template));
        h.succeed();
    }
}
