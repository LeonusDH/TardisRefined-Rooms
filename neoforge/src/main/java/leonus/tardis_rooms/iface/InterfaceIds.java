package leonus.tardis_rooms.iface;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Выданные идентификаторы интерфейсов одной ТАРДИС (хранится в её измерении): новый всегда не занят. */
public final class InterfaceIds extends SavedData {
    private static final String NAME = "tardis_rooms_interface_ids";
    private final Set<Integer> used = new HashSet<>();

    public static InterfaceIds get(ServerLevel tardis) {
        return tardis.getDataStorage().computeIfAbsent(new SavedData.Factory<>(InterfaceIds::new, InterfaceIds::load, null), NAME);
    }

    /** Новый идентификатор 0001..FFFF, или 0, если все заняты. */
    public int allocate(ServerLevel level) {
        if (used.size() >= 0xFFFF) return 0;
        int id;
        do { id = 1 + level.getRandom().nextInt(0xFFFF); } while (used.contains(id));
        used.add(id);
        setDirty();
        return id;
    }

    private static InterfaceIds load(CompoundTag tag, HolderLookup.Provider provider) {
        InterfaceIds d = new InterfaceIds();
        for (int v : tag.getIntArray("used")) d.used.add(v);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putIntArray("used", used.stream().mapToInt(Integer::intValue).toArray());
        return tag;
    }
}
