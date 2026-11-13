package leonus.tardis_rooms.forge.gametest;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.gametest.InterfaceGameTests;
import leonus.tardis_rooms.gametest.RoomGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Регистрация общих игровых тестов на Forge (класс находится по {@link GameTestHolder} только при включённых тестах).
 * Сами тесты — в {@link InterfaceGameTests} и {@link RoomGameTests}. В готовый мод не входит.
 *
 * <p>Без {@code fluidsPull}: на Forge 1.20.1 у ванильного котла нет обработчика жидкостей (его добавил только NeoForge),
 * а других ванильных хранилищ жидкости нет. Жидкости проверяет тот же тест на Fabric.
 */
@GameTestHolder(TardisRooms.MODID)
@PrefixGameTestTemplate(false)
public final class ForgeGameTests {
    private ForgeGameTests() {}

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void itemsPull(GameTestHelper h) { InterfaceGameTests.itemsPull(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void itemsPush(GameTestHelper h) { InterfaceGameTests.itemsPush(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void itemsSeveralStacks(GameTestHelper h) { InterfaceGameTests.itemsSeveralStacks(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void disabledSideBlocks(GameTestHelper h) { InterfaceGameTests.disabledSideBlocks(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void kindOffBlocks(GameTestHelper h) { InterfaceGameTests.kindOffBlocks(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void flowInOnlyBlocks(GameTestHelper h) { InterfaceGameTests.flowInOnlyBlocks(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void differentIdsNotLinked(GameTestHelper h) { InterfaceGameTests.differentIdsNotLinked(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void removedPeerUnlinks(GameTestHelper h) { InterfaceGameTests.removedPeerUnlinks(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void redstone(GameTestHelper h) { InterfaceGameTests.redstone(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void redstoneDisabledSide(GameTestHelper h) { InterfaceGameTests.redstoneDisabledSide(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void settingsSurviveItem(GameTestHelper h) { InterfaceGameTests.settingsSurviveItem(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void redstoneInputRemoved(GameTestHelper h) { InterfaceGameTests.redstoneInputRemoved(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void redstoneRelinked(GameTestHelper h) { InterfaceGameTests.redstoneRelinked(h); }

    // ---------- помещения: что мешает удалению ----------

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomTemplateUntouched(GameTestHelper h) { RoomGameTests.templateUntouched(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomTemplateChanged(GameTestHelper h) { RoomGameTests.templateChanged(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomAddedAfterGrowth(GameTestHelper h) { RoomGameTests.addedAfterGrowth(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomReplacedBlock(GameTestHelper h) { RoomGameTests.replacedBlock(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomInterfaceBlocks(GameTestHelper h) { RoomGameTests.interfaceBlocks(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomOldRoomByContents(GameTestHelper h) { RoomGameTests.oldRoomByContents(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomFingerprintSurvivesReload(GameTestHelper h) { RoomGameTests.fingerprintSurvivesReload(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomDoubleChestIsOneTarget(GameTestHelper h) { RoomGameTests.doubleChestIsOneTarget(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomRemovableConfigIgnored(GameTestHelper h) { RoomGameTests.removableConfigIgnored(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomRemovableMarkedIgnored(GameTestHelper h) { RoomGameTests.removableMarkedIgnored(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void roomRemovableDescriptions(GameTestHelper h) { RoomGameTests.removableDescriptions(h); }

    // ---------- права ----------

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void noPermissionKeepsSettings(GameTestHelper h) { InterfaceGameTests.noPermissionKeepsSettings(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void permissionChecks(GameTestHelper h) { RoomGameTests.permissionChecks(h); }

    @GameTest(template = InterfaceGameTests.TEMPLATE)
    public static void permissionSyncMasks(GameTestHelper h) { RoomGameTests.permissionSyncMasks(h); }
}
