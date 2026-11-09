package leonus.tardis_rooms.iface;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AEColor;
import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Applied Energistics 2: интерфейсы как туннели P2P. Со включённой «Сетью ME» связанные интерфейсы соединяют
 * свои сети ME (плотное соединение, как у квантового моста), а карта памяти копирует и вставляет идентификатор.
 * Классы AE2 загружаются, только если AE2 установлен (всё — через {@link Impl} после проверки {@link #LOADED}).
 * На Fabric AE2 находит узел через BlockApiLookup IInWorldGridNodeHost.LOOKUP.
 */
public final class AE2Compat {
    /** Установлен ли AE2. */
    public static final boolean LOADED = leonus.tardis_rooms.platform.Platform.isModLoaded("ae2");

    /** AE2 есть и совместимость не выключена в конфиге. */
    public static boolean enabled() {
        return LOADED && leonus.tardis_rooms.TardisRoomsConfig.compatAE2();
    }

    /** Ключи в данных карты памяти: частота (как у P2P) и ТАРДИС, которой принадлежит идентификатор. */
    private static final String CARD_FREQ_KEY = "p2pFreq", CARD_TARDIS_KEY = "tardis_rooms_tardis";

    /** Регистрирует для интерфейса поиск узла сети ME (только если AE2 установлен). */
    public static void register() {
        if (LOADED) Impl.register();
    }

    /** Часть вида «сеть ME» для интерфейса. Только если AE2 установлен. */
    static KindPart newPart(InterfaceBlockEntity be, TransferKind kind) {
        return Impl.newPart(be, kind);
    }

    private static final class Impl {
        static void register() {
            IInWorldGridNodeHost.LOOKUP.registerForBlockEntity(
                    (be, ctx) -> be.part(BuiltinKinds.me()) instanceof MePart p ? p.link : null, TRoomsBlocks.INTERFACE_BE.get());
        }

        static KindPart newPart(InterfaceBlockEntity be, TransferKind kind) { return new MePart(be, kind); }

        static InteractionResult useCard(ItemStack stack, InterfaceBlockEntity be, Player player) {
            if (player.level().isClientSide()) return InteractionResult.SUCCESS;
            IMemoryCard card = (IMemoryCard) stack.getItem();
            if (player.isShiftKeyDown()) {
                if (be.id() == 0) {
                    player.displayClientMessage(TRoomsText.MSG_INTERFACE_NO_ID.text(), true);
                    return InteractionResult.SUCCESS;
                }
                short freq = (short) be.id();
                CompoundTag data = new CompoundTag();
                data.putShort(CARD_FREQ_KEY, freq);
                AEColor[] c = appeng.util.Platform.p2p().toColors(freq);
                data.putIntArray(IMemoryCard.NBT_COLOR_CODE, new int[]{c[0].ordinal(), c[0].ordinal(), c[1].ordinal(), c[1].ordinal(),
                        c[2].ordinal(), c[2].ordinal(), c[3].ordinal(), c[3].ordinal()});
                be.data().tardis().ifPresent(t -> data.putString(CARD_TARDIS_KEY, t.toString()));
                card.setMemoryCardContents(stack, TRoomsBlocks.INTERFACE.get().getDescriptionId(), data);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
            } else {
                CompoundTag data = card.getData(stack);
                short freq = data.getShort(CARD_FREQ_KEY);
                if (freq == 0) {
                    card.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                    return InteractionResult.SUCCESS;
                }
                Optional<ResourceLocation> owner = data.contains(CARD_TARDIS_KEY) ? Optional.ofNullable(ResourceLocation.tryParse(data.getString(CARD_TARDIS_KEY))) : Optional.empty();
                be.pasteId(freq & 0xFFFF, owner);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * Сеть ME (часть вида): узел AE2 есть, пока вид включён и интерфейс подключён; при выгрузке и удалении узел убирается.
     * Карта памяти работает, пока AE2 доступен, даже если у этого интерфейса сеть ME выключена.
     */
    private static final class MePart implements KindPart {
        private final InterfaceBlockEntity be;
        private final TransferKind kind;
        /** Узел и соединения, или null — узла сейчас нет. */
        @Nullable MeLink link;

        MePart(InterfaceBlockEntity be, TransferKind kind) {
            this.be = be;
            this.kind = kind;
        }

        /** Стороны, открытые для кабелей ME, — сразу, а не на ближайшем секундном такте. */
        @Override
        public void invalidate() { update(); }

        @Override
        public void tick() { update(); }

        @Override
        public void unload() { destroy(); }

        @Override
        public void remove() { destroy(); }

        /** Карта памяти по интерфейсу: с Shift — скопировать идентификатор в карту, без — вставить из карты. */
        @Override
        public @Nullable InteractionResult useItem(ItemStack stack, Player player) {
            return stack.getItem() instanceof IMemoryCard ? Impl.useCard(stack, be, player) : null;
        }

        private void update() {
            boolean wanted = be.isConnected() && be.enabled(kind) && !be.isRemoved();
            if (wanted && link == null) {
                link = new MeLink(be, this);
                link.node.create(be.getLevel(), be.getBlockPos());
                be.getLevel().updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
            } else if (!wanted && link != null) {
                destroy();
                be.getLevel().updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
                return;
            }
            if (link != null) {
                link.exposeOpenSides();
                link.sync();
            }
        }

        private void destroy() {
            if (link == null) return;
            link.conns.clear();
            link.node.destroy();
            link = null;
        }

        /** Узел этой части у связанного интерфейса, если он там есть. */
        @Nullable IGridNode nodeOf(InterfaceBlockEntity peer) {
            return peer.part(kind) instanceof MePart p && p.link != null ? p.link.node.getNode() : null;
        }
    }

    /** Узел сети ME интерфейса и его соединения со связанными интерфейсами. */
    private static final class MeLink implements IInWorldGridNodeHost {
        private static final IGridNodeListener<InterfaceBlockEntity> LISTENER = (owner, node) -> owner.setChanged();

        final InterfaceBlockEntity be;
        final MePart part;
        final IManagedGridNode node;
        final Map<InterfaceBlockEntity, IGridConnection> conns = new HashMap<>();

        MeLink(InterfaceBlockEntity be, MePart part) {
            this.be = be;
            this.part = part;
            this.node = GridHelper.createManagedNode(be, LISTENER)
                    .setFlags(GridFlags.DENSE_CAPACITY)
                    .setIdlePowerUsage(1.0)
                    .setInWorldNode(true)
                    .setExposedOnSides(EnumSet.allOf(Direction.class))
                    .setVisualRepresentation(TRoomsBlocks.INTERFACE_ITEM.get())
                    .setTagName("tardis_interface");
        }

        @Override
        public IGridNode getGridNode(Direction dir) {
            return be.open(dir) ? node.getNode() : null;
        }

        /** Кабели ME подключаются только к открытым сторонам (выключенная сторона — как будто её нет). */
        private Set<Direction> exposed = EnumSet.allOf(Direction.class);

        void exposeOpenSides() {
            Set<Direction> open = EnumSet.noneOf(Direction.class);
            for (Direction d : Direction.values()) if (be.open(d)) open.add(d);
            if (open.equals(exposed)) return;
            exposed = open;
            node.setExposedOnSides(open);
        }

        /** Соединения — ровно со связанными интерфейсами, у которых тоже включена сеть ME (создаёт одна из сторон). */
        void sync() {
            IGridNode mine = node.getNode();
            if (mine == null) return;
            Set<InterfaceBlockEntity> wanted = new HashSet<>();
            for (InterfaceBlockEntity peer : be.linked()) {
                IGridNode other = part.nodeOf(peer);
                if (other == null) continue;
                if (order(be).compareTo(order(peer)) >= 0) continue; // соединение создаёт тот, у кого «меньше» адрес
                wanted.add(peer);
                if (!conns.containsKey(peer)) {
                    try {
                        conns.put(peer, GridHelper.createConnection(mine, other));
                    } catch (Exception e) {
                        TardisRooms.debug("ME link {} -> {} failed: {}", be.getBlockPos(), peer.getBlockPos(), e.toString());
                    }
                }
            }
            for (Iterator<Map.Entry<InterfaceBlockEntity, IGridConnection>> it = conns.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<InterfaceBlockEntity, IGridConnection> e = it.next();
                if (wanted.contains(e.getKey()) && !e.getKey().isRemoved()) continue;
                try { e.getValue().destroy(); } catch (Exception ignored) {}
                it.remove();
            }
        }

        private static String order(InterfaceBlockEntity b) {
            return (b.getLevel() == null ? "" : b.getLevel().dimension().location().toString()) + "|" + b.getBlockPos().asLong();
        }
    }

    private AE2Compat() {}
}
