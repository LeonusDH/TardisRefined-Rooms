package leonus.tardis_rooms.iface.kind;

import net.minecraft.resources.ResourceLocation;

/** Регистрация видов передачи: передаётся в {@link KindPlugin#register(KindRegistration)}. */
public interface KindRegistration {
    /** Новый вид с этим id; описать и вызвать {@link TransferKind.Builder#register()}. id должен быть уникален. */
    TransferKind.Builder kind(ResourceLocation id);
}
