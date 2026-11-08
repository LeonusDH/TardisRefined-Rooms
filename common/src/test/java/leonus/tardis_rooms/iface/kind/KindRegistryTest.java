package leonus.tardis_rooms.iface.kind;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Реестр видов передачи: порядок, номера, поиск по id и ошибки регистрации. */
class KindRegistryTest {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    @Test
    void builtinsInOrderWithSequentialBits() {
        KindRegistry r = KindRegistry.build(List.of(BuiltinKinds.plugin(id -> null)));
        assertEquals(List.of(BuiltinKinds.ENERGY, BuiltinKinds.ITEMS, BuiltinKinds.FLUIDS, BuiltinKinds.CHEMICALS, BuiltinKinds.ME, BuiltinKinds.REDSTONE),
                r.all().stream().map(TransferKind::id).toList());
        for (int i = 0; i < r.size(); i++) {
            TransferKind k = r.all().get(i);
            assertEquals(i, k.index());
            assertEquals(1 << i, k.bit());
            assertSame(k, r.byId(k.id()));
        }
        assertNull(r.byId(id("missing")));
    }

    @Test
    void builtinDefaults() {
        KindRegistry r = KindRegistry.build(List.of(BuiltinKinds.plugin(id -> null)));
        assertEquals(List.of(BuiltinKinds.ENERGY, BuiltinKinds.ITEMS, BuiltinKinds.FLUIDS, BuiltinKinds.CHEMICALS),
                r.all().stream().filter(TransferKind::enabledByDefault).map(TransferKind::id).toList());
    }

    @Test
    void pluginsKeepTheirOrder() {
        KindRegistry r = KindRegistry.build(List.of(
                reg -> reg.kind(id("a")).register(),
                reg -> {
                    reg.kind(id("b")).enabledByDefault().register();
                    reg.kind(id("c")).register();
                }));
        assertEquals(List.of(id("a"), id("b"), id("c")), r.all().stream().map(TransferKind::id).toList());
        assertTrue(r.byId(id("b")).enabledByDefault());
        assertFalse(r.byId(id("a")).enabledByDefault());
        assertTrue(r.byId(id("a")).available(), "по умолчанию вид доступен");
    }

    @Test
    void duplicateIdRejected() {
        assertThrows(IllegalArgumentException.class, () -> KindRegistry.build(List.of(
                reg -> reg.kind(id("a")).register(),
                reg -> reg.kind(id("a")).register())));
    }

    @Test
    void builderRegistersOnce() {
        assertThrows(IllegalStateException.class, () -> KindRegistry.build(List.of(reg -> {
            TransferKind.Builder b = reg.kind(id("a"));
            b.register();
            b.register();
        })));
    }

    @Test
    void lateRegistrationRejected() {
        KindRegistration[] kept = new KindRegistration[1];
        KindRegistry.build(List.of(reg -> kept[0] = reg));
        assertThrows(IllegalStateException.class, () -> kept[0].kind(id("late")).register());
    }

    @Test
    void tooManyKindsRejected() {
        assertThrows(IllegalStateException.class, () -> KindRegistry.build(List.of(reg -> {
            for (int i = 0; i <= KindRegistry.MAX_KINDS; i++) reg.kind(id("k" + i)).register();
        })));
    }

    @Test
    void partFactoryGetsItsKind() {
        TransferKind[] seen = new TransferKind[1];
        KindRegistry r = KindRegistry.build(List.of(reg -> reg.kind(id("a")).parts((be, kind) -> {
            seen[0] = kind;
            return new KindPart() {};
        }).register()));
        assertNotNull(r.all().get(0).createPart(null));
        assertSame(r.all().get(0), seen[0]);
        assertNull(KindRegistry.build(List.of(reg -> reg.kind(id("b")).register())).all().get(0).createPart(null));
    }
}
