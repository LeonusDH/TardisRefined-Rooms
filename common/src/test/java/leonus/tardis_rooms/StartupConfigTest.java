package leonus.tardis_rooms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Чтение настройки из common.toml до загрузки конфига: значение, а если его нет или оно не читается — запасное. */
class StartupConfigTest {
    private static final String PATH = "compat.acuaTardisGadgets";

    @TempDir
    Path dir;

    private Path file(String text) throws IOException {
        Path f = dir.resolve("common.toml");
        Files.writeString(f, text);
        return f;
    }

    @Test
    void readsTheValue() throws IOException {
        Path f = file("debug = false\n\n[compat]\n\tjade = true\n\t# comment\n\tacuaTardisGadgets = false\n");
        assertFalse(StartupConfig.readBoolean(f, PATH, true));
        assertTrue(StartupConfig.readBoolean(file("[compat]\nacuaTardisGadgets = true\n"), PATH, false));
    }

    @Test
    void noFileGivesFallback() {
        assertTrue(StartupConfig.readBoolean(dir.resolve("missing.toml"), PATH, true));
        assertFalse(StartupConfig.readBoolean(dir.resolve("missing.toml"), PATH, false));
    }

    @Test
    void noValueGivesFallback() throws IOException {
        assertTrue(StartupConfig.readBoolean(file("[compat]\njade = false\n"), PATH, true));
    }

    @Test
    void wrongTypeGivesFallback() throws IOException {
        assertTrue(StartupConfig.readBoolean(file("[compat]\nacuaTardisGadgets = \"no\"\n"), PATH, true));
    }

    @Test
    void brokenFileGivesFallback() throws IOException {
        assertTrue(StartupConfig.readBoolean(file("[compat\nacuaTardisGadgets = = false\n"), PATH, true));
    }
}
