package leonus.tardis_rooms.iface;

import appeng.api.AECapabilities;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardColors;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AEColor;
import appeng.items.tools.MemoryCardItem;
import appeng.util.Platform;
import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
 */
public final class AE2Compat {
    /** Установлен ли AE2 (возможность регистрируем только тогда). */
    public static final boolean LOADED = leonus.tardis_rooms.platform.Platform.isModLoaded("ae2");

    /** AE2 есть и совместимость не выключена в конфиге. */
    public static boolean enabled() {
        return LOADED && leonus.tardis_rooms.TardisRoomsConfig.compatAE2();
    }
    /** Ключ в настройках карты памяти: ТАРДИС, которой принадлежит идентификатор. */
    private static final String CARD_TARDIS_KEY = "tardis_rooms_tardis";

    public static void register(RegisterCapabilitiesEvent event) {
        if (LOADED) Impl.register(event);
    }

    /** Часть вида «сеть ME» для интерфейса. Только если AE2 установлен. */
    static KindPart newPart(InterfaceBlockEntity be, TransferKind kind) {
        return Impl.newPart(be, kind);
    }

    private static final class Impl {
        static void register(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, TRoomsBlocks.INTERFACE_BE.get(),
                    (be, ctx) -> be.part(BuiltinKinds.me()) instanceof MePart p ? p.link : null);
        }

        static KindPart newPart(InterfaceBlockEntity be, TransferKind kind) { return new MePart(be, kind); }

        static ItemInteractionResult useCard(ItemStack stack, InterfaceBlockEntity be, Player player) {
            if (player.level().isClientSide()) return ItemInteractionResult.SUCCESS;
            IMemoryCard card = (IMemoryCard) stack.getItem();
            if (player.isShiftKeyDown()) {
                if (be.id() == 0) {
                    player.displayClientMessage(TRoomsText.MSG_INTERFACE_NO_ID.text(), true);
                    return ItemInteractionResult.SUCCESS;
                }
                MemoryCardItem.clearCard(stack);
                short freq = (short) be.id();
                stack.set(AEComponents.EXPORTED_SETTINGS_SOURCE, TRoomsBlocks.INTERFACE.get().getName());
                stack.set(AEComponents.EXPORTED_P2P_FREQUENCY, freq);
                AEColor[] c = Platform.p2p().toColors(freq);
                stack.set(AEComponents.MEMORY_CARD_COLORS, new MemoryCardColors(c[0], c[0], c[1], c[1], c[2], c[2], c[3], c[3]));
                be.data().tardis().ifPresent(t -> stack.set(AEComponents.EXPORTED_SETTINGS, Map.of(CARD_TARDIS_KEY, t.toString())));
                card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
                TardisRooms.debug("{}: memory card copied ID {} from the interface at {}", player.getGameProfile().getName(),
                        InterfaceData.hex(be.id()), be.getBlockPos().toShortString());
            } else {
                Short freq = stack.get(AEComponents.EXPORTED_P2P_FREQUENCY);
                if (freq == null || freq == 0) {
                    card.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                    return ItemInteractionResult.SUCCESS;
                }
                Map<String, String> settings = stack.get(AEComponents.EXPORTED_SETTINGS);
                Optional<ResourceLocation> owner = Optional.ofNullable(settings == null ? null : settings.get(CARD_TARDIS_KEY)).map(ResourceLocation::tryParse);
                TardisRooms.debug("{}: memory card pasted ID {} into the interface at {}", player.getGameProfile().getName(),
                        InterfaceData.hex(freq & 0xFFFF), be.getBlockPos().toShortString());
                be.pasteId(freq & 0xFFFF, owner);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
            }
            return ItemInteractionResult.SUCCESS;
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
        public @Nullable ItemInteractionResult useItem(ItemStack stack, Player player) {
            return stack.getItem() instanceof IMemoryCard ? Impl.useCard(stack, be, player) : null;
        }

        private void update() {
            boolean wanted = be.isConnected() && be.enabled(kind) && !be.isRemoved();
            if (wanted && link == null) {
                link = new MeLink(be, this);
                link.node.create(be.getLevel(), be.getBlockPos());
                be.getLevel().invalidateCapabilities(be.getBlockPos());
            } else if (!wanted && link != null) {
                destroy();
                be.getLevel().invalidateCapabilities(be.getBlockPos());
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
                try {
                    e.getValue().destroy();
                } catch (Exception ex) {
                    TardisRooms.debug("ME link {} -> {} could not be removed: {}", be.getBlockPos(), e.getKey().getBlockPos(), ex.toString());
                }
                it.remove();
            }
        }

        private static String order(InterfaceBlockEntity b) {
            return (b.getLevel() == null ? "" : b.getLevel().dimension().location().toString()) + "|" + b.getBlockPos().asLong();
        }
    }

    private AE2Compat() {}
}
