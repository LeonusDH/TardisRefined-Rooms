package leonus.tardis_rooms.iface;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

/**
 * Предметы через интерфейс. Слоты: при входе — один «приёмный» слот (всегда пуст, всё сразу уходит дальше),
 * при выходе — слоты всех хранилищ рядом с интерфейсами со входом (из них можно забирать).
 */
final class ItemView implements IItemHandler {
    private final InterfaceBlockEntity be;
    private final ForgeInterfaceTransport transport;

    ItemView(InterfaceBlockEntity be, ForgeInterfaceTransport transport) {
        this.be = be;
        this.transport = transport;
    }

    private int inSlots() { return be.canIn(TransferType.ITEMS) ? 1 : 0; }

    private List<IItemHandler> sources() {
        return be.canOut(TransferType.ITEMS) ? transport.linkedItems.get(LinkedStorages.Pick.FROM) : List.of();
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
        if (stack.isEmpty() || !be.canIn(TransferType.ITEMS) || InterfaceNetwork.depth > 0) return stack;
        InterfaceNetwork.depth++;
        try {
            ItemStack rest = stack;
            for (IItemHandler h : transport.linkedItems.get(LinkedStorages.Pick.TO)) {
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
        return be.canIn(TransferType.ITEMS);
    }
}
