package leonus.tardis_rooms.iface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Передача ресурсов одного интерфейса — то, что у каждого загрузчика своё (NeoForge: capabilities, Fabric: Transfer API).
 * Создаёт {@link InterfacePlatform#createTransport}; настройки, связь и очередь граней — в {@code InterfaceBlockEntity}.
 */
public interface InterfaceTransport {
    /**
     * Грань «вставлять» ({@code pull = false}) или «забирать» ({@code pull = true}) с соседом в {@code neighbour}
     * (у соседа — сторона {@code side}): одна операция каждого включённого вида передачи.
     */
    void moveFace(BlockPos neighbour, Direction side, boolean pull);

    /** Что интерфейс принимает и отдаёт, изменилось (виды, направление, связь) — сообщить трубам и кабелям. */
    void invalidate();

    /** Раз в секунду на сервере, после пересчёта связи. */
    void tick();

    /** Блок убран или выгружен. */
    void remove();
}
