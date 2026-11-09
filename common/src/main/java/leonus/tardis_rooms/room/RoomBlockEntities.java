package leonus.tardis_rooms.room;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;
import whocraft.tardis_refined.TardisRefined;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Блок-сущности внутри помещения, которые мешают его удалить (сундуки, кафедры, машины модов).
 * Блоки TR и наши служебные не мешают никогда, интерфейс ТАРДИС — всегда (его ставит игрок, с настройками).
 *
 * <p>Остальные: при росте помещения запоминается отпечаток каждой блок-сущности шаблона — место, блок и данные сразу
 * после установки ({@link #fingerprint}). Блок-сущность с тем же отпечатком — нетронутая часть шаблона, не мешает;
 * положили книгу на кафедру или что-то в сундук — отпечаток другой, мешает. Чего в шаблоне не было — мешает.
 *
 * <p>У помещений, выращенных до появления отпечатков, их нет. Для них — запасное правило ({@link #hasContents}):
 * ванильная блок-сущность мешает, только если в ней что-то лежит; модовая — всегда (её содержимое не узнать).
 */
public final class RoomBlockEntities {
    /** Объём помещения: по коробке на модуль и этаж. */
    public static List<BoundingBox> boxes(Map<Integer, Set<Long>> footprint) {
        List<BoundingBox> out = new ArrayList<>();
        footprint.forEach((floor, mods) -> {
            for (long m : mods) {
                int ox = Lattice.origin(Lattice.keyX(m)), oz = Lattice.origin(Lattice.keyZ(m));
                out.add(new BoundingBox(ox, floor, oz, ox + Lattice.MODULE - 1, floor + Lattice.BAND - 1, oz + Lattice.MODULE - 1));
            }
        });
        return out;
    }

    /** Отпечатки блок-сущностей, которые иначе мешали бы удалению: место ({@link BlockPos#asLong}) → отпечаток. */
    public static Map<Long, Integer> fingerprint(ServerLevel level, List<BoundingBox> boxes) {
        Map<Long, Integer> out = new HashMap<>();
        forEach(level, boxes, be -> {
            if (!ignored(be) && !(be instanceof InterfaceBlockEntity)) out.put(be.getBlockPos().asLong(), hash(level, be));
        });
        return out;
    }

    /**
     * Места блок-сущностей, которые мешают удалить помещение; пусто — можно.
     * {@code template} — отпечатки шаблона ({@link #fingerprint}); null — помещение старое, без отпечатков.
     */
    public static List<BlockPos> blockers(ServerLevel level, List<BoundingBox> boxes, @Nullable Map<Long, Integer> template) {
        List<BlockPos> out = new ArrayList<>();
        forEach(level, boxes, be -> {
            if (blocks(level, be, template)) out.add(be.getBlockPos().immutable());
        });
        return out;
    }

    private static boolean blocks(ServerLevel level, BlockEntity be, @Nullable Map<Long, Integer> template) {
        if (be instanceof InterfaceBlockEntity) return true;
        if (ignored(be)) return false;
        if (template == null) return hasContents(be);
        Integer expected = template.get(be.getBlockPos().asLong());
        return expected == null || expected != hash(level, be);
    }

    /** Блоки TR и наши служебные — часть самой ТАРДИС. */
    private static boolean ignored(BlockEntity be) {
        String ns = blockId(be).getNamespace();
        return ns.equals(TardisRefined.MODID) || ns.equals(TardisRooms.MODID);
    }

    /**
     * Запасное правило для старых помещений: лежит ли что-то в блок-сущности. Ванильные — по содержимому
     * (предметы в контейнере, книга на кафедре, пластинка в проигрывателе, еда на костре, пчёлы в улье);
     * модовые — всегда «да»: что у них внутри, отсюда не узнать.
     */
    static boolean hasContents(BlockEntity be) {
        if (!blockId(be).getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) return true;
        // Сундук с ещё не открытой добычей: isEmpty() её бы сгенерировал, а добыча — тоже содержимое.
        if (be instanceof RandomizableContainer r && r.getLootTable() != null) return true;
        if (be instanceof Container c) return !c.isEmpty();
        if (be instanceof LecternBlockEntity l) return l.hasBook();
        if (be instanceof CampfireBlockEntity c) return c.getItems().stream().anyMatch(s -> !s.isEmpty());
        if (be instanceof BeehiveBlockEntity b) return !b.isEmpty();
        return false;
    }

    /** Отпечаток: блок и данные блок-сущности (как в сохранении, без места). */
    private static int hash(ServerLevel level, BlockEntity be) {
        return Objects.hash(blockId(be), be.saveWithoutMetadata(level.registryAccess()));
    }

    private static ResourceLocation blockId(BlockEntity be) {
        return BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock());
    }

    /** Все блок-сущности внутри коробок (каждая один раз, даже если коробки делят чанк). */
    private static void forEach(ServerLevel level, List<BoundingBox> boxes, Consumer<BlockEntity> action) {
        for (BoundingBox box : boxes) {
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    LevelChunk chunk = level.getChunk(cx, cz);
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (box.isInside(be.getBlockPos())) action.accept(be);
                    }
                }
            }
        }
    }

    private RoomBlockEntities() {}
}
