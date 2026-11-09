package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.LinkedStorages.Pick;
import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * Предметы через интерфейс. Слоты: при входе — один «приёмный» слот (всегда пуст, всё сразу уходит дальше),
 * при выходе — слоты всех хранилищ рядом с интерфейсами со входом (из них можно забирать).
 */
final class ItemView implements IItemHandler, KindPart {
    private final InterfaceBlockEntity be;
    private final TransferKind kind;
    /** Свои соседи по предметам — их берут связанные интерфейсы. */
    private final LinkedStorages.Sides<IItemHandler> sides;
    /** Хранилища предметов у соседей связанных интерфейсов. */
    private final LinkedStorages<IItemHandler> linked;

    ItemView(InterfaceBlockEntity be, TransferKind kind) {
        this.be = be;
        this.kind = kind;
        this.sides = NeoForgeInterfaceTransport.sides(be, Capabilities.ItemHandler.BLOCK);
        this.linked = new LinkedStorages<>(be, kind, peer -> peer.part(kind) instanceof ItemView v ? v.sides : null);
    }

    /** Для возможности блока: null — вид выключен (трубы не подключатся). */
    IItemHandler capability() { return be.enabled(kind) ? this : null; }

    /**
     * Одна стопка за раз. «Вставлять» берёт прямо из хранилищ у связанных входов (а не через слоты интерфейса —
     * так не пересчитываются все слоты всех хранилищ на каждый слот).
     */
    @Override
    public void moveFace(Direction face, boolean pull) {
        IItemHandler h = sides.at(face);
        if (h == null) return;
        List<IItemHandler> froms = pull ? List.of(h) : linked.get(Pick.FROM);
        IItemHandler to = pull ? this : h;
        for (IItemHandler from : froms) {
            for (int slot = 0; slot < from.getSlots(); slot++) {
                ItemStack probe = from.extractItem(slot, 64, true);
                if (probe.isEmpty()) continue;
                int n = probe.getCount() - insert(to, probe, true).getCount();
                if (n <= 0) continue;
                ItemStack left = insert(to, from.extractItem(slot, n, false), false);
                // Получатель взял меньше обещанного — вернуть остаток туда, откуда взяли, а не потерять.
                if (!left.isEmpty()) left = ItemHandlerHelper.insertItemStacked(from, left, false);
                if (!left.isEmpty()) Block.popResource(be.getLevel(), be.getBlockPos(), left);
                return;
            }
        }
    }

    /** В свой вход — сразу в приёмный слот (перебор слотов вида ни к чему), в чужое хранилище — со стопками. */
    private ItemStack insert(IItemHandler to, ItemStack stack, boolean simulate) {
        return to == this ? insertItem(0, stack, simulate) : ItemHandlerHelper.insertItemStacked(to, stack, simulate);
    }

    private int inSlots() { return be.canIn(kind) ? 1 : 0; }

    private List<IItemHandler> sources() {
        return be.canOut(kind) ? linked.get(Pick.FROM) : List.of();
    }

    @Override
    public int getSlots() {
        int n = inSlots();
        for (IItemHandler h : sources()) n += h.getSlots();
        return n;
    }

    /** Источник и номер слота в нём для слота {@code slot}, или null (приёмный слот / нет такого). */
    private Object[] locate(int slot) {
        int s = slot - inSlots();
        if (s < 0) return null;
        for (IItemHandler h : sources()) {
            if (s < h.getSlots()) return new Object[]{h, s};
            s -= h.getSlots();
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        Object[] l = locate(slot);
        return l == null ? ItemStack.EMPTY : ((IItemHandler) l[0]).getStackInSlot((int) l[1]);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !be.canIn(kind) || InterfaceNetwork.depth > 0) return stack;
        InterfaceNetwork.depth++;
        try {
            ItemStack rest = stack;
            for (IItemHandler h : linked.get(Pick.TO)) {
                rest = ItemHandlerHelper.insertItemStacked(h, rest, simulate);
                if (rest.isEmpty()) break;
            }
            return rest;
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || InterfaceNetwork.depth > 0) return ItemStack.EMPTY;
        Object[] l = locate(slot);
        if (l == null) return ItemStack.EMPTY;
        InterfaceNetwork.depth++;
        try {
            return ((IItemHandler) l[0]).extractItem((int) l[1], amount, simulate);
        } finally {
            InterfaceNetwork.depth--;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        Object[] l = locate(slot);
        return l == null ? 64 : ((IItemHandler) l[0]).getSlotLimit((int) l[1]);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return be.canIn(kind);
    }
}
