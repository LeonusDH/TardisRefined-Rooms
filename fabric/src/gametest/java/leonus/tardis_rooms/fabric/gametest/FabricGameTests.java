package leonus.tardis_rooms.fabric.gametest;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.gametest.InterfaceGameTests;
import leonus.tardis_rooms.gametest.RoomGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Регистрация общих игровых тестов на Fabric (точка входа fabric-gametest отдельного тестового мода).
 * Сами тесты — в {@link InterfaceGameTests} и {@link RoomGameTests}.
 */
public final class FabricGameTests {
    private static final String TEMPLATE = TardisRooms.MODID + ":" + InterfaceGameTests.TEMPLATE;

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

    @GameTest(template = TEMPLATE)
    public static void redstoneInputRemoved(GameTestHelper h) { InterfaceGameTests.redstoneInputRemoved(h); }

    @GameTest(template = TEMPLATE)
    public static void redstoneRelinked(GameTestHelper h) { InterfaceGameTests.redstoneRelinked(h); }

    // ---------- помещения: что мешает удалению ----------

    @GameTest(template = TEMPLATE)
    public static void roomTemplateUntouched(GameTestHelper h) { RoomGameTests.templateUntouched(h); }

    @GameTest(template = TEMPLATE)
    public static void roomTemplateChanged(GameTestHelper h) { RoomGameTests.templateChanged(h); }

    @GameTest(template = TEMPLATE)
    public static void roomAddedAfterGrowth(GameTestHelper h) { RoomGameTests.addedAfterGrowth(h); }

    @GameTest(template = TEMPLATE)
    public static void roomReplacedBlock(GameTestHelper h) { RoomGameTests.replacedBlock(h); }

    @GameTest(template = TEMPLATE)
    public static void roomInterfaceBlocks(GameTestHelper h) { RoomGameTests.interfaceBlocks(h); }

    @GameTest(template = TEMPLATE)
    public static void roomOldRoomByContents(GameTestHelper h) { RoomGameTests.oldRoomByContents(h); }

    @GameTest(template = TEMPLATE)
    public static void roomFingerprintSurvivesReload(GameTestHelper h) { RoomGameTests.fingerprintSurvivesReload(h); }

    @GameTest(template = TEMPLATE)
    public static void roomDoubleChestIsOneTarget(GameTestHelper h) { RoomGameTests.doubleChestIsOneTarget(h); }

    @GameTest(template = TEMPLATE)
    public static void roomRemovableConfigIgnored(GameTestHelper h) { RoomGameTests.removableConfigIgnored(h); }

    @GameTest(template = TEMPLATE)
    public static void roomRemovableMarkedIgnored(GameTestHelper h) { RoomGameTests.removableMarkedIgnored(h); }

    @GameTest(template = TEMPLATE)
    public static void roomRemovableDescriptions(GameTestHelper h) { RoomGameTests.removableDescriptions(h); }
}
