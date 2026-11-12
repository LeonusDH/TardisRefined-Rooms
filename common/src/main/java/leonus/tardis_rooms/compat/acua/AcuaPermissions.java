package leonus.tardis_rooms.compat.acua;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.room.RoomPermissions;
import net.acuadragon100.acuatardisgadgets.AcuaTARDISGadgets;
import net.acuadragon100.acuatardisgadgets.compat.tardis_refined.TardisRefinedCompat;
import net.acuadragon100.acuatardisgadgets.registry.TARDISPermissionRegistry;
import net.acuadragon100.acuatardisgadgets.tardis.TARDISPermission;
import net.acuadragon100.acuatardisgadgets.util.helpers.TARDISHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Права аддона в реестре {@code acuatardisgadgets:tardis_permission}: меню Security Manager показывает их само.
 * Права привязаны к ТАРДИС Tardis Refined (помещения есть только у них), как у Acua «сменить консольную».
 */
final class AcuaPermissions {
    /**
     * Регистратор — с id мода Acua, хотя права наши ({@code tardis_rooms:...}): на Forge Architectury регистрирует
     * через шину событий мода с этим id, а свою шину Architectury регистрирует только Acua (аддону Architectury не нужен).
     */
    private static final DeferredRegister<TARDISPermission> PERMISSIONS =
            DeferredRegister.create(AcuaTARDISGadgets.MODID, TARDISPermissionRegistry.REGISTRY.key());

    static final RegistrySupplier<TARDISPermission.AssignablePermission> GROW = permission("rooms.grow");
    static final RegistrySupplier<TARDISPermission.AssignablePermission> MANAGE = permission("rooms.manage");
    static final RegistrySupplier<TARDISPermission.AssignablePermission> INTERFACE = permission("interface.configure");

    private AcuaPermissions() {}

    private static RegistrySupplier<TARDISPermission.AssignablePermission> permission(String path) {
        return PERMISSIONS.register(new ResourceLocation(TardisRooms.MODID, path), TARDISPermission.AssignablePermissionImpl::new);
    }

    /** Добавить права в реестр Acua (при запуске мода). */
    static void register() {
        TARDISPermissionRegistry.REGISTRY.finishRegistrations(PERMISSIONS);
        TARDISPermissionRegistry.REGISTRY.listen(() -> {
            for (RegistrySupplier<TARDISPermission.AssignablePermission> p : List.of(GROW, MANAGE, INTERFACE)) {
                p.listen(permission -> permission.registerForTARDIS(TardisRefinedCompat.class));
            }
        });
    }

    /**
     * Проверка для {@link RoomPermissions}. ТАРДИС, о которой Acua не знает, и ТАРДИС без включённой защиты (Acua сам
     * разрешает в них всё) — можно.
     */
    static boolean allowed(ServerPlayer player, ResourceKey<Level> tardis, RoomPermissions.Action action) {
        ServerLevel level = player.serverLevel().getServer().getLevel(tardis);
        if (level == null) return true;
        RegistrySupplier<TARDISPermission.AssignablePermission> permission = switch (action) {
            case GROW -> GROW;
            case MANAGE -> MANAGE;
            case CONFIGURE_INTERFACE -> INTERFACE;
        };
        return TARDISHelper.getSingleTARDIS(level, Vec3.ZERO)
                .map(t -> t.getSecurityManager().hasPermission(player, permission))
                .orElse(true);
    }
}
