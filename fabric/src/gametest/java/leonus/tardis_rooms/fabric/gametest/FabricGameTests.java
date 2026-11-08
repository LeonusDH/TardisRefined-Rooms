package leonus.tardis_rooms.fabric.gametest;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.gametest.InterfaceGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Регистрация общих игровых тестов на Fabric (точка входа fabric-gametest отдельного тестового мода).
 * Сами тесты — в {@link InterfaceGameTests}.
 */
public final class FabricGameTests {
    private static final String TEMPLATE = TardisRooms.MODID + ":" + InterfaceGameTests.TEMPLATE;

    static {
        InterfaceGameTests.setUp();
    }

    @GameTest(template = TEMPLATE)
    public static void itemsPull(GameTestHelper h) { InterfaceGameTests.itemsPull(h); }

    @GameTest(template = TEMPLATE)
    public static void itemsPush(GameTestHelper h) { InterfaceGameTests.itemsPush(h); }

    @GameTest(template = TEMPLATE)
    public static void itemsSeveralStacks(GameTestHelper h) { InterfaceGameTests.itemsSeveralStacks(h); }

    @GameTest(template = TEMPLATE)
    public static void disabledSideBlocks(GameTestHelper h) { InterfaceGameTests.disabledSideBlocks(h); }

    @GameTest(template = TEMPLATE)
    public static void kindOffBlocks(GameTestHelper h) { InterfaceGameTests.kindOffBlocks(h); }

    @GameTest(template = TEMPLATE)
    public static void flowInOnlyBlocks(GameTestHelper h) { InterfaceGameTests.flowInOnlyBlocks(h); }

    @GameTest(template = TEMPLATE)
    public static void differentIdsNotLinked(GameTestHelper h) { InterfaceGameTests.differentIdsNotLinked(h); }

    @GameTest(template = TEMPLATE)
    public static void removedPeerUnlinks(GameTestHelper h) { InterfaceGameTests.removedPeerUnlinks(h); }

    @GameTest(template = TEMPLATE)
    public static void fluidsPull(GameTestHelper h) { InterfaceGameTests.fluidsPull(h); }

    @GameTest(template = TEMPLATE)
    public static void redstone(GameTestHelper h) { InterfaceGameTests.redstone(h); }

    @GameTest(template = TEMPLATE)
    public static void redstoneDisabledSide(GameTestHelper h) { InterfaceGameTests.redstoneDisabledSide(h); }

    @GameTest(template = TEMPLATE)
    public static void settingsSurviveItem(GameTestHelper h) { InterfaceGameTests.settingsSurviveItem(h); }
}
