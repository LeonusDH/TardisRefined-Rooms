package leonus.tardis_rooms.iface;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

/**
 * Данные аддона на предметах. На 1.20.1 компонентов предметов ещё нет — данные лежат в NBT:
 * режим отвёртки и запомненный идентификатор — в {@code tardis_rooms}, настройки интерфейса — в {@code BlockEntityTag.interface}
 * (игра сама передаёт их блоку при установке, а таблица добычи копирует их обратно при поломке).
 */
public final class ItemData {
    private static final String ROOT = "tardis_rooms";

    public static int screwdriverMode(ItemStack s) {
        CompoundTag t = s.getTagElement(ROOT);
        return t == null ? 0 : t.getInt("mode");
    }

    public static void setScrewdriverMode(ItemStack s, int mode) {
        if (mode == 0) remove(s, "mode");
        else s.getOrCreateTagElement(ROOT).putInt("mode", mode);
    }

    public static InterfaceLink link(ItemStack s) {
        CompoundTag t = s.getTagElement(ROOT);
        if (t == null || !t.contains("link")) return null;
        return InterfaceLink.CODEC.parse(NbtOps.INSTANCE, t.get("link")).result().orElse(null);
    }

    public static void setLink(ItemStack s, InterfaceLink link) {
        InterfaceLink.CODEC.encodeStart(NbtOps.INSTANCE, link).result().ifPresent(t -> s.getOrCreateTagElement(ROOT).put("link", t));
    }

    /** Убрать запомненный идентификатор; вернуть, был ли он. */
    public static boolean removeLink(ItemStack s) {
        CompoundTag t = s.getTagElement(ROOT);
        if (t == null || !t.contains("link")) return false;
        remove(s, "link");
        return true;
    }

    public static InterfaceData interfaceData(ItemStack s) {
        CompoundTag t = s.getTagElement("BlockEntityTag");
        if (t == null || !t.contains("interface")) return null;
        return InterfaceData.CODEC.parse(NbtOps.INSTANCE, t.get("interface")).result().orElse(null);
    }

    public static void setInterfaceData(ItemStack s, InterfaceData d) {
        if (d.equals(InterfaceData.DEFAULT)) return;
        InterfaceData.CODEC.encodeStart(NbtOps.INSTANCE, d).result().ifPresent(t -> s.getOrCreateTagElement("BlockEntityTag").put("interface", t));
    }

    private static void remove(ItemStack s, String key) {
        CompoundTag t = s.getTagElement(ROOT);
        if (t == null) return;
        t.remove(key);
        if (t.isEmpty()) s.removeTagKey(ROOT);
    }

    private ItemData() {}
}
