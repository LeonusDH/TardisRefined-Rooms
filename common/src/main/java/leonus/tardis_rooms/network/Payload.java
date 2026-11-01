package leonus.tardis_rooms.network;

import net.minecraft.resources.ResourceLocation;

/**
 * Пакет аддона. На 1.20.1 в игре ещё нет {@code Payload}: это его упрощённая замена с тем же видом,
 * чтобы код пакетов совпадал с основной веткой. Платформа регистрирует тип вместе с кодеком и отправляет пакет по id типа.
 */
public interface Payload {
    Type<? extends Payload> type();

    record Type<T extends Payload>(ResourceLocation id) {}
}
