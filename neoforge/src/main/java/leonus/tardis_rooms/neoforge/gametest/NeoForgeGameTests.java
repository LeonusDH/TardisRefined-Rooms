package leonus.tardis_rooms.neoforge.gametest;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.gametest.InterfaceGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Регистрация общих игровых тестов на NeoForge (класс находится по {@link GameTestHolder} только при включённых тестах).
 * Сами тесты — в {@link InterfaceGameTests}. В готовый мод не входит.
 */
@GameTestHolder(TardisRooms.MODID)
@PrefixGameTestTemplate(false)
public final class NeoForgeGameTests {
    private NeoForgeGameTests() {}

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
    public static void fluidsPull(GameTestHelper h) { InterfaceGameTests.fluidsPull(h); }

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
}
