package leonus.tardis_rooms.gametest;

import com.mojang.authlib.GameProfile;
import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.FaceMode;
import leonus.tardis_rooms.iface.Flow;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceHost;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.iface.ItemData;
import leonus.tardis_rooms.iface.RedstonePart;
import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.network.InterfacePayload;
import leonus.tardis_rooms.room.RoomPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Игровые тесты интерфейса — общие для платформ; платформа только регистрирует их (аннотации у каждой свои).
 * Запуск: gradlew :forge:runGameTestServer и gradlew :fabric:runGameTestServer. В готовый мод не входят.
 *
 * <p>Раскладка: сосед A — (0,1,1), интерфейс A — (1,1,1), интерфейс B — (4,1,1), сосед B — (5,1,1).
 * A и B не касаются друг друга, связаны одинаковым ID.
 */
public final class InterfaceGameTests {
    /** Шаблон: пустое место. */
    public static final String TEMPLATE = "gametest/empty";
    /** В тестах нет ТАРДИС: все интерфейсы относятся к одной выдуманной. */
    private static final ResourceKey<Level> TEST_TARDIS =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(TardisRooms.MODID, "gametest"));
    /** Тесты идут одновременно в одном мире: у каждого свой ID, чтобы не связаться с чужими. */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(100);

    static final BlockPos SRC = new BlockPos(0, 1, 1), A = new BlockPos(1, 1, 1), B = new BlockPos(4, 1, 1), DST = new BlockPos(5, 1, 1);

    private InterfaceGameTests() {}

    // ---------- раскладка ----------

    private record Pair(InterfaceBlockEntity a, InterfaceBlockEntity b) {}

    private static InterfaceBlockEntity iface(GameTestHelper h, BlockPos pos, int id) {
        // Область — здесь, а не при загрузке тестов: на Fabric тесты загружаются раньше, чем мод задаёт свою область.
        InterfaceHost.set(new InterfaceHost((level, p) -> List.of(TEST_TARDIS), InterfaceHost.current().configurator(), InterfaceHost.current().access()));
        h.setBlock(pos, TRoomsBlocks.INTERFACE.get());
        InterfaceBlockEntity be = (InterfaceBlockEntity) h.getBlockEntity(pos);
        be.pasteId(id, Optional.empty());
        return be;
    }

    private static Pair pair(GameTestHelper h) {
        int id = NEXT_ID.getAndIncrement();
        Pair p = new Pair(iface(h, A, id), iface(h, B, id));
        h.assertTrue(p.a.isConnected() && p.b.isConnected(), "интерфейсы не подключились: " + p.a.status() + ", " + p.b.status());
        h.assertTrue(p.a.linked().size() == 1 && p.b.linked().size() == 1, "интерфейсы не связались");
        return p;
    }

    private static void chests(GameTestHelper h, int count) {
        h.setBlock(SRC, Blocks.CHEST);
        h.setBlock(DST, Blocks.CHEST);
        container(h, SRC).setItem(0, new ItemStack(Items.COBBLESTONE, count));
    }

    private static Container container(GameTestHelper h, BlockPos pos) {
        BlockEntity e = h.getBlockEntity(pos);
        return (Container) e;
    }

    private static int count(GameTestHelper h, BlockPos pos) {
        Container c = container(h, pos);
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) n += c.getItem(i).getCount();
        return n;
    }

    private static void moved(GameTestHelper h, int count) {
        h.succeedWhen(() -> {
            h.assertTrue(count(h, DST) == count, "в приёмнике " + count(h, DST) + " из " + count);
            h.assertTrue(count(h, SRC) == 0, "в источнике осталось " + count(h, SRC));
        });
    }

    /** Через 2 секунды ничего не сдвинулось. */
    private static void nothingMoved(GameTestHelper h, int count) {
        h.runAfterDelay(40, () -> {
            h.assertTrue(count(h, SRC) == count, "из источника ушло: осталось " + count(h, SRC));
            h.assertTrue(count(h, DST) == 0, "в приёмник пришло " + count(h, DST));
            h.succeed();
        });
    }

    // ---------- предметы ----------

    /** A забирает из сундука, B (по умолчанию) отдаёт в свой сундук. */
    public static void itemsPull(GameTestHelper h) {
        chests(h, 10);
        Pair p = pair(h);
        p.a.setFace(Direction.WEST, FaceMode.PULL);
        moved(h, 10);
    }

    /** B сам вставляет в свой сундук, беря из сундука у A. */
    public static void itemsPush(GameTestHelper h) {
        chests(h, 10);
        Pair p = pair(h);
        p.b.setFace(Direction.EAST, FaceMode.PUSH);
        moved(h, 10);
    }

    /** Больше стопки — уходит за несколько раз. */
    public static void itemsSeveralStacks(GameTestHelper h) {
        h.setBlock(SRC, Blocks.CHEST);
        h.setBlock(DST, Blocks.CHEST);
        Container c = container(h, SRC);
        for (int i = 0; i < 3; i++) c.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        Pair p = pair(h);
        p.a.setFace(Direction.WEST, FaceMode.PULL);
        moved(h, 192);
    }

    /** Выключенная сторона у приёмника — ничего не идёт. */
    public static void disabledSideBlocks(GameTestHelper h) {
        chests(h, 10);
        Pair p = pair(h);
        p.a.setFace(Direction.WEST, FaceMode.PULL);
        p.b.setFace(Direction.EAST, FaceMode.DISABLED);
        nothingMoved(h, 10);
    }

    /** Предметы выключены у одного из интерфейсов — ничего не идёт. */
    public static void kindOffBlocks(GameTestHelper h) {
        chests(h, 10);
        Pair p = pair(h);
        p.a.setFace(Direction.WEST, FaceMode.PULL);
        p.b.toggle(BuiltinKinds.items());
        nothingMoved(h, 10);
    }

    /** У приёмника только вход — ничего не идёт. */
    public static void flowInOnlyBlocks(GameTestHelper h) {
        chests(h, 10);
        Pair p = pair(h);
        p.a.setFace(Direction.WEST, FaceMode.PULL);
        p.b.setFlow(Flow.IN);
        nothingMoved(h, 10);
    }

    /** Разные ID — не связаны, ничего не идёт. */
    public static void differentIdsNotLinked(GameTestHelper h) {
        chests(h, 10);
        InterfaceBlockEntity a = iface(h, A, NEXT_ID.getAndIncrement());
        iface(h, B, NEXT_ID.getAndIncrement());
        a.setFace(Direction.WEST, FaceMode.PULL);
        h.assertTrue(a.linked().isEmpty(), "связались интерфейсы с разными ID");
        nothingMoved(h, 10);
    }

    /** Связь рвётся, когда интерфейс убирают. */
    public static void removedPeerUnlinks(GameTestHelper h) {
        Pair p = pair(h);
        h.setBlock(B, Blocks.AIR);
        h.assertTrue(p.a.linked().isEmpty(), "убранный интерфейс остался в связи");
        h.succeed();
    }

    // ---------- жидкости ----------

    /** Котёл с водой у A (забирать) → пустой котёл у B. Только Fabric: на Forge 1.20.1 котёл не хранилище жидкости. */
    public static void fluidsPull(GameTestHelper h) {
        h.setBlock(SRC, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        h.setBlock(DST, Blocks.CAULDRON);
        Pair p = pair(h);
        p.a.setFace(Direction.WEST, FaceMode.PULL);
        h.succeedWhen(() -> {
            h.assertBlockPresent(Blocks.WATER_CAULDRON, DST);
            h.assertBlockPresent(Blocks.CAULDRON, SRC);
        });
    }

    // ---------- редстоун ----------

    /** Лампа у B; редстоун включён у обоих, A — вход (по умолчанию «вход и выход»), B — выход. */
    private static Pair redstonePair(GameTestHelper h) {
        h.setBlock(DST, Blocks.REDSTONE_LAMP);
        Pair p = pair(h);
        p.a.toggle(BuiltinKinds.redstone());
        p.b.toggle(BuiltinKinds.redstone());
        p.b.setFlow(Flow.OUT);
        return p;
    }

    /** Сигнал у A (вход) зажигает лампу у B (выход) и гаснет вместе с ним. */
    public static void redstone(GameTestHelper h) {
        Pair p = redstonePair(h);
        h.startSequence()
                .thenExecute(() -> h.setBlock(SRC, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> h.assertBlockProperty(DST, RedstoneLampBlock.LIT, true))
                .thenExecute(() -> h.setBlock(SRC, Blocks.AIR))
                .thenWaitUntil(() -> h.assertBlockProperty(DST, RedstoneLampBlock.LIT, false))
                .thenSucceed();
    }

    /** Интерфейс со входом убрали — лампа у выхода гаснет. */
    public static void redstoneInputRemoved(GameTestHelper h) {
        Pair p = redstonePair(h);
        h.startSequence()
                .thenExecute(() -> h.setBlock(SRC, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> h.assertBlockProperty(DST, RedstoneLampBlock.LIT, true))
                .thenExecute(() -> h.setBlock(A, Blocks.AIR))
                .thenWaitUntil(() -> h.assertBlockProperty(DST, RedstoneLampBlock.LIT, false))
                .thenSucceed();
    }

    /** У входа сменили ID — связь с выходом рвётся, лампа гаснет. */
    public static void redstoneRelinked(GameTestHelper h) {
        Pair p = redstonePair(h);
        h.startSequence()
                .thenExecute(() -> h.setBlock(SRC, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> h.assertBlockProperty(DST, RedstoneLampBlock.LIT, true))
                .thenExecute(() -> p.a.pasteId(NEXT_ID.getAndIncrement(), Optional.empty()))
                .thenWaitUntil(() -> h.assertBlockProperty(DST, RedstoneLampBlock.LIT, false))
                .thenSucceed();
    }

    /** Выключенная сторона не выдаёт сигнал. */
    public static void redstoneDisabledSide(GameTestHelper h) {
        Pair p = redstonePair(h);
        p.b.setFace(Direction.EAST, FaceMode.DISABLED);
        h.setBlock(SRC, Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(20, () -> {
            h.assertTrue(p.b.part(BuiltinKinds.redstone()) instanceof RedstonePart rp && rp.output() == 15, "интерфейс не получил сигнал");
            h.assertBlockProperty(DST, RedstoneLampBlock.LIT, false);
            h.succeed();
        });
    }

    // ---------- настройки ----------

    /** Снятый интерфейс уносит настройки в предмет и ставится с ними же. */
    public static void settingsSurviveItem(GameTestHelper h) {
        InterfaceBlockEntity a = iface(h, A, NEXT_ID.getAndIncrement());
        a.setFlow(Flow.OUT);
        a.toggle(BuiltinKinds.fluids());
        a.toggle(BuiltinKinds.redstone());
        a.setFace(Direction.NORTH, FaceMode.PUSH);
        a.setFace(Direction.UP, FaceMode.DISABLED);
        ItemStack stack = new ItemStack(TRoomsBlocks.INTERFACE_ITEM.get());
        ItemData.setInterfaceData(stack, a.data()); // как в dismantle и при обычной поломке
        h.setBlock(B, TRoomsBlocks.INTERFACE.get());
        // Как при установке блока из предмета: игра передаёт блоку BlockEntityTag.
        BlockItem.updateCustomBlockEntityTag(h.getLevel(), null, h.absolutePos(B), stack);
        InterfaceBlockEntity b = (InterfaceBlockEntity) h.getBlockEntity(B);
        h.assertTrue(a.data().equals(b.data()), "настройки не совпали: " + a.data() + " → " + b.data());
        h.succeed();
    }

    // ---------- права ----------

    /**
     * Игрок для проверки прав: стоит над {@code pos}, в мир не входит (в списке игроков его нет). Соединение без канала:
     * пакеты (сообщения над хотбаром) просто копятся в нём.
     */
    static ServerPlayer mockPlayer(GameTestHelper h, BlockPos pos) {
        ServerPlayer player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "permission-test"));
        new ServerGamePacketListenerImpl(h.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player);
        BlockPos at = h.absolutePos(pos).above();
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    /** Без права настраивать интерфейс меню ничего не меняет и сломать интерфейс нельзя; с правом — можно. */
    public static void noPermissionKeepsSettings(GameTestHelper h) {
        InterfaceBlockEntity be = iface(h, A, NEXT_ID.getAndIncrement());
        be.setFlow(Flow.IN);
        BlockPos pos = h.absolutePos(A);
        ServerPlayer player = mockPlayer(h, A);
        InterfaceHost host = InterfaceHost.current();
        RoomPermissions.Check check = RoomPermissions.current();
        List<String> asked = new ArrayList<>();
        try {
            // Инструмент настройки — палка: отвёртка Tardis Refined тестам не нужна.
            InterfaceHost.set(new InterfaceHost(host.scope(), s -> s.is(Items.STICK), host.access()));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            RoomPermissions.set((p, tardis, action) -> {
                asked.add(tardis.location() + " " + action);
                return false;
            });
            InterfacePayload.handle(new InterfacePayload(pos, InterfacePayload.FLOW, Flow.OUT.ordinal()), player);
            h.assertTrue(be.flow() == Flow.IN, "без права направление сменилось: " + be.flow());
            h.assertTrue(asked.equals(List.of(TEST_TARDIS.location() + " CONFIGURE_INTERFACE")), "спросили не то: " + asked);
            h.assertTrue(!InterfaceTools.onBreakBlock(player, h.getLevel(), pos), "без права интерфейс можно сломать");

            RoomPermissions.set(RoomPermissions.ALLOW_ALL);
            InterfacePayload.handle(new InterfacePayload(pos, InterfacePayload.FLOW, Flow.OUT.ordinal()), player);
            h.assertTrue(be.flow() == Flow.OUT, "с правом направление не сменилось: " + be.flow());
            h.assertTrue(InterfaceTools.onBreakBlock(player, h.getLevel(), pos), "с правом интерфейс нельзя сломать");
        } finally {
            InterfaceHost.set(host);
            RoomPermissions.set(check);
        }
        h.succeed();
    }
}

