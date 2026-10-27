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
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

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
    public static final boolean LOADED = ModList.get().isLoaded("ae2");

    /** AE2 есть и совместимость не выключена в конфиге. */
    public static boolean enabled() {
        return LOADED && leonus.tardis_rooms.TardisRoomsConfig.compatAE2();
    }
    /** Ключ в настройках карты памяти: ТАРДИС, которой принадлежит идентификатор. */
    private static final String CARD_TARDIS_KEY = "tardis_rooms_tardis";

    public static void register(RegisterCapabilitiesEvent event) {
        if (LOADED) Impl.register(event);
    }

    static void update(InterfaceBlockEntity be) {
        if (LOADED) Impl.update(be);
    }

    static void destroy(InterfaceBlockEntity be) {
        if (LOADED) Impl.destroy(be);
    }

    public static boolean isMemoryCard(ItemStack stack) {
        return enabled() && Impl.isCard(stack);
    }

    /** Карта памяти по интерфейсу: с Shift — скопировать идентификатор в карту, без — вставить из карты. */
    public static ItemInteractionResult useMemoryCard(ItemStack stack, InterfaceBlockEntity be, Player player) {
        return enabled() ? Impl.useCard(stack, be, player) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static final class Impl {
        static void register(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, TRoomsBlocks.INTERFACE_BE.get(),
                    (be, ctx) -> be.me instanceof MeLink l ? l : null);
        }

        static boolean isCard(ItemStack stack) {
            return stack.getItem() instanceof IMemoryCard;
        }

        static void update(InterfaceBlockEntity be) {
            boolean wanted = be.isConnected() && be.enabled(TransferType.ME) && !be.isRemoved();
            MeLink link = be.me instanceof MeLink l ? l : null;
            if (wanted && link == null) {
                link = new MeLink(be);
                be.me = link;
                link.node.create(be.getLevel(), be.getBlockPos());
                be.getLevel().invalidateCapabilities(be.getBlockPos());
            } else if (!wanted && link != null) {
                destroy(be);
                be.getLevel().invalidateCapabilities(be.getBlockPos());
                return;
            }
            if (link != null) link.sync();
        }

        static void destroy(InterfaceBlockEntity be) {
            if (be.me instanceof MeLink l) {
                l.conns.clear();
                l.node.destroy();
            }
            be.me = null;
        }

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
            } else {
                Short freq = stack.get(AEComponents.EXPORTED_P2P_FREQUENCY);
                if (freq == null || freq == 0) {
                    card.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                    return ItemInteractionResult.SUCCESS;
                }
                Map<String, String> settings = stack.get(AEComponents.EXPORTED_SETTINGS);
                Optional<ResourceLocation> owner = Optional.ofNullable(settings == null ? null : settings.get(CARD_TARDIS_KEY)).map(ResourceLocation::tryParse);
                be.pasteId(freq & 0xFFFF, owner);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
            }
            return ItemInteractionResult.SUCCESS;
        }
    }

    /** Узел сети ME интерфейса и его соединения со связанными интерфейсами. */
    private static final class MeLink implements IInWorldGridNodeHost {
        private static final IGridNodeListener<InterfaceBlockEntity> LISTENER = (owner, node) -> owner.setChanged();

        final InterfaceBlockEntity be;
        final IManagedGridNode node;
        final Map<InterfaceBlockEntity, IGridConnection> conns = new HashMap<>();

        MeLink(InterfaceBlockEntity be) {
            this.be = be;
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
            return node.getNode();
        }

        /** Соединения — ровно со связанными интерфейсами, у которых тоже включена сеть ME (создаёт одна из сторон). */
        void sync() {
            IGridNode mine = node.getNode();
            if (mine == null) return;
            Set<InterfaceBlockEntity> wanted = new HashSet<>();
            for (InterfaceBlockEntity peer : be.linked()) {
                if (!(peer.me instanceof MeLink other) || other.node.getNode() == null) continue;
                if (order(be).compareTo(order(peer)) >= 0) continue; // соединение создаёт тот, у кого «меньше» адрес
                wanted.add(peer);
                if (!conns.containsKey(peer)) {
                    try {
                        conns.put(peer, GridHelper.createConnection(mine, other.node.getNode()));
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
