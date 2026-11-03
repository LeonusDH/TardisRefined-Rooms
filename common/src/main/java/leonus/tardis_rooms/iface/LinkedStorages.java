package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Хранилища у соседей связанных интерфейсов — общее для всех платформ. Каждый интерфейс сам помнит своих соседей
 * ({@link Sides}: на NeoForge — BlockCapabilityCache, на Fabric — BlockApiCache, их обновляет сама игра), а здесь
 * только обходятся связанные интерфейсы и отбираются нужные стороны.
 */
public final class LinkedStorages {
    /** Какие соседи нужны. */
    public enum Pick {
        /** У интерфейсов с выходом — куда отдавать. Грани «забирать» пропускаются: они сами вынимают из соседа (гоняли бы по кругу). */
        TO,
        /** У интерфейсов со входом — откуда забирать. Грани «вставлять» пропускаются: они сами кладут в соседа. */
        FROM,
        /** Все, каждый один раз (у интерфейса «вход и выход» сосед попал бы и в TO, и в FROM) — для подсказки. */
        ALL
    }

    /** Соседи интерфейса одного вида (энергия, предметы…): хранилище соседа с данной стороны, или null. */
    @FunctionalInterface
    public interface Sides<T> {
        @Nullable T at(Direction side);
    }

    /**
     * Хранилища у соседей связанных с {@code self} интерфейсов, у которых включён вид передачи {@code type}.
     * {@code sidesOf} — соседи этого вида у связанного интерфейса (null — вид у него недоступен). Соседние интерфейсы
     * пропускаются: передача из интерфейса в интерфейс шла бы по кругу.
     */
    public static <T> List<T> collect(InterfaceBlockEntity self, TransferType type, Pick pick, Function<InterfaceBlockEntity, Sides<T>> sidesOf) {
        Level level = self.getLevel();
        if (level == null || !self.isConnected()) return List.of();
        List<T> out = new ArrayList<>();
        for (InterfaceBlockEntity peer : InterfaceNetwork.peers(self)) {
            if (!peer.enabled(type) || (pick == Pick.TO && !peer.flow().out) || (pick == Pick.FROM && !peer.flow().in)) continue;
            Level pl = peer.getLevel();
            Sides<T> sides = sidesOf.apply(peer);
            if (pl == null || sides == null) continue;
            for (Direction d : Direction.values()) {
                FaceMode fm = peer.face(d);
                if ((pick == Pick.TO && fm == FaceMode.PULL) || (pick == Pick.FROM && fm == FaceMode.PUSH)) continue;
                BlockPos np = peer.getBlockPos().relative(d);
                if (!pl.isLoaded(np) || pl.getBlockEntity(np) instanceof InterfaceBlockEntity) continue;
                T h = sides.at(d);
                if (h != null) out.add(h);
            }
        }
        // По кругу: каждый раз начинаем с другого получателя, чтобы делить поровну.
        if (pick != Pick.ALL && out.size() > 1) Collections.rotate(out, -(int) (level.getGameTime() % out.size()));
        return out;
    }

    private LinkedStorages() {}
}
