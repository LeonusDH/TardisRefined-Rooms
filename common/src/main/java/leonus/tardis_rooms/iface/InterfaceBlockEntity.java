package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.TRoomsBlocks;
import leonus.tardis_rooms.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import whocraft.tardis_refined.common.blockentity.shell.ShellBaseBlockEntity;
import whocraft.tardis_refined.registry.TRDimensionTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Интерфейс ТАРДИС. Связан с интерфейсами той же ТАРДИС, у которых тот же идентификатор (как туннели P2P в AE2).
 * Ничего не хранит: поданное во вход сразу передаётся соседям связанных интерфейсов с выходом,
 * а забираемое из выхода берётся у соседей связанных интерфейсов со входом. Скорость задают трубы и механизмы.
 */
public class InterfaceBlockEntity extends BlockEntity {
    private static final int CHECK_INTERVAL = 20;

    private InterfaceData data = InterfaceData.DEFAULT;
    private Status status = Status.NO_SHELL;
    /** ТАРДИС, в которой (или у оболочки которой) стоит интерфейс. */
    private ResourceKey<Level> tardis;
    /** Сколько ещё интерфейсов связано (для экрана и Jade). */
    private int peers;
    private InterfaceNetwork.Key joinedKey;

    /** Передача ресурсов — своя у каждого загрузчика. */
    private final InterfaceTransport transport = InterfacePlatform.createTransport(this);
    /** Узел сети ME (с AE2), иначе null. */
    Object me;
    /** Редстоун: сигнал, принятый от соседей (вход), и сигнал, который интерфейс выдаёт (выход). */
    private int redstoneIn, redstoneOut;

    public InterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(TRoomsBlocks.INTERFACE_BE.get(), pos, state);
    }

    // ---------- настройки ----------

    public InterfaceData data() { return data; }

    public int id() { return data.id(); }

    public Flow flow() { return data.flowMode(); }

    public boolean enabled(TransferType t) { return t.available() && data.enabled(t); }

    public Status status() { return status; }

    public int peerCount() { return peers; }

    public ResourceKey<Level> tardis() { return tardis; }

    public boolean isConnected() { return status == Status.CONNECTED; }

    public InterfaceTransport transport() { return transport; }

    InterfaceNetwork.Key linkKey() { return isConnected() && tardis != null ? new InterfaceNetwork.Key(tardis, data.id()) : null; }

    void setData(InterfaceData d) {
        if (d.equals(data)) return;
        Iterable<InterfaceBlockEntity> before = linked();
        data = d;
        if (level != null && !level.isClientSide()) {
            refresh();
            for (InterfaceBlockEntity p : before) p.updateRedstoneOutput();
        }
        changed();
    }

    public void setFlow(Flow f) { setData(data.withFlow(f)); }

    public FaceMode face(Direction d) { return data.face(d); }

    public void setFace(Direction d, FaceMode m) { setData(data.withFace(d, m)); }

    public void toggle(TransferType t) { if (t.available()) setData(data.toggle(t)); }

    public void clearId() { setData(data.withId(0, Optional.empty())); }

    /** Вставляет идентификатор (с отвёртки, карты памяти). Без ТАРДИС — считается идентификатором здешней. */
    public void pasteId(int id, Optional<ResourceLocation> owner) {
        if (id <= 0 || id > 0xFFFF) return;
        Optional<ResourceLocation> t = owner.isPresent() ? owner : Optional.ofNullable(tardis).map(ResourceKey::location);
        setData(data.withId(id, t));
    }

    /** Новый идентификатор от этой ТАРДИС. false — интерфейс не подключён ни к какой ТАРДИС. */
    public boolean generateId() {
        if (!(level instanceof ServerLevel sl) || tardis == null) return false;
        ServerLevel tl = sl.getServer().getLevel(tardis);
        if (tl == null) return false;
        int id = InterfaceIds.get(tl).allocate(tl);
        if (id == 0) return false;
        setData(data.withId(id, Optional.of(tardis.location())));
        return true;
    }

    public InterfaceLink link() {
        return new InterfaceLink(data.id(), data.tardis());
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            transport.invalidate();
            BlockState s = getBlockState();
            level.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
        }
    }

    // ---------- связь ----------

    /** Связь нужно пересчитать на ближайшем тике (блок только что загружен). */
    private boolean loadPending;

    /**
     * Блок загружен в мир. Без {@code @Override}: на NeoForge метод с этим именем есть у BlockEntity (и вызывается им сам),
     * на других платформах его вызывает событие загрузки блока-сущности — а моды-мосты (Mekanism Fabricated) вызывают его
     * и сами, прямо посреди загрузки чанка. Поэтому здесь только отметка: пересчёт обращается к миру и посреди загрузки
     * чанка повесил бы сервер, ожидающий этот же чанк. Пересчитаем на ближайшем тике.
     */
    public void onLoad() {
        if (level != null && !level.isClientSide()) loadPending = true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        leave();
        transport.remove();
    }

    /** Чанк выгружен. Без {@code @Override} — по той же причине, что и {@link #onLoad()}. */
    public void onChunkUnloaded() {
        leave();
        transport.remove();
    }

    private void leave() {
        if (joinedKey != null) InterfaceNetwork.leave(joinedKey, this);
        joinedKey = null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InterfaceBlockEntity be) {
        boolean periodic = (level.getGameTime() + pos.asLong()) % CHECK_INTERVAL == 0;
        if (be.loadPending || periodic) {
            be.loadPending = false;
            be.refresh();
        }
        if (periodic) be.transport.tick();
        if (be.data.faces() != 0 && (level.getGameTime() + pos.asLong()) % FACE_INTERVAL == 0) be.tickFaces();
    }

    // ---------- грани «вставлять» / «забирать» ----------

    /**
     * Грани работают раз в {@value #FACE_INTERVAL} тика: за раз — стопка предметов, порция жидкости или химиката,
     * энергии — сколько примут. Чаще незачем: поиск соседей стоит дороже самой передачи.
     */
    private static final int FACE_INTERVAL = 4;

    private void tickFaces() {
        if (!isConnected() || InterfaceNetwork.depth > 0) return;
        for (Direction d : Direction.values()) {
            FaceMode m = face(d);
            if (m == FaceMode.DEFAULT) continue;
            boolean pull = m == FaceMode.PULL;
            if (!(pull ? flow().in : flow().out)) continue;
            BlockPos np = worldPosition.relative(d);
            if (!level.isLoaded(np) || level.getBlockEntity(np) instanceof InterfaceBlockEntity) continue;
            transport.moveFace(np, d.getOpposite(), pull);
        }
    }

    /** Снять интерфейс ключом: блок — сразу в инвентарь, с настройками. */
    public void dismantle(Player player) {
        ItemStack stack = new ItemStack(TRoomsBlocks.INTERFACE_ITEM.get());
        stack.applyComponents(collectComponents()); // те же компоненты, что при обычной поломке блока
        Level l = level;
        BlockPos p = worldPosition;
        l.playSound(null, p, Platform.soundType(getBlockState(), l, p, player).getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
        l.removeBlock(p, false);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /** Пересчитывает, к какой ТАРДИС подключён интерфейс и с кем связан. */
    void refresh() {
        ResourceKey<Level> t = null;
        Status st;
        if (TRDimensionTypes.isTARDISDimension(level)) {
            t = level.dimension();
            st = Status.CONNECTED;
        } else {
            List<ResourceKey<Level>> found = nearbyTardises(level, worldPosition);
            if (found.isEmpty()) st = Status.NO_SHELL;
            else if (found.size() > 1) st = Status.CONFLICT;
            else { t = found.get(0); st = Status.CONNECTED; }
        }
        if (st == Status.CONNECTED) {
            if (data.id() == 0) st = Status.NO_ID;
            else if (data.tardis().isPresent() && !data.tardis().get().equals(t.location())) st = Status.WRONG_TARDIS;
        }
        boolean changedState = st != status || !Objects.equals(t, tardis);
        status = st;
        tardis = t;
        InterfaceNetwork.Key key = linkKey();
        if (!Objects.equals(key, joinedKey)) {
            leave();
            if (key != null) InterfaceNetwork.join(key, this);
            joinedKey = key;
        }
        int p = key != null ? InterfaceNetwork.peers(this).size() : 0;
        if (changedState || p != peers) {
            peers = p;
            changed();
        }
        updateRedstoneInput();
        updateRedstoneOutput();
    }

    // ---------- редстоун ----------

    /**
     * Сигнал передаётся от интерфейсов со входом к интерфейсам с выходом. «Вход и выход» для редстоуна — только вход:
     * иначе интерфейс ловил бы свой же сигнал через провод и защёлкивался навсегда.
     */
    private boolean redstoneReads() { return isConnected() && enabled(TransferType.REDSTONE) && flow().in; }

    private boolean redstoneEmits() { return isConnected() && enabled(TransferType.REDSTONE) && flow() == Flow.OUT; }

    public int redstoneOut() { return redstoneOut; }

    /** Сосед изменился (или настройки): перечитать входящий сигнал и, если он другой, сообщить связанным. */
    public void updateRedstoneInput() {
        if (level == null || level.isClientSide()) return;
        int v = 0;
        if (redstoneReads()) {
            // Сигнал соседнего интерфейса не считаем: интерфейс с выходом рядом с интерфейсом со входом
            // той же сети иначе защёлкнул бы сигнал навсегда.
            for (Direction d : Direction.values()) {
                BlockPos np = worldPosition.relative(d);
                if (level.getBlockEntity(np) instanceof InterfaceBlockEntity) continue;
                v = Math.max(v, level.getSignal(np, d));
                if (v >= 15) break;
            }
        }
        if (v == redstoneIn) return;
        redstoneIn = v;
        for (InterfaceBlockEntity p : linked()) p.updateRedstoneOutput();
    }

    /** Выдаваемый сигнал — наибольший среди связанных интерфейсов со входом. */
    public void updateRedstoneOutput() {
        if (level == null || level.isClientSide()) return;
        int out = 0;
        if (redstoneEmits()) {
            for (InterfaceBlockEntity p : linked()) {
                if (p.redstoneReads()) out = Math.max(out, p.redstoneIn);
            }
        }
        if (out == redstoneOut) return;
        redstoneOut = out;
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    /** Разные ТАРДИС, чьи оболочки стоят вплотную к блоку (куб 3×3×3 и на блок ниже — оболочка высотой 2). */
    public static List<ResourceKey<Level>> nearbyTardises(Level level, BlockPos pos) {
        List<ResourceKey<Level>> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -2, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockEntity(p) instanceof ShellBaseBlockEntity shell) {
                ResourceKey<Level> id = shell.getTardisId();
                if (id != null && !out.contains(id)) out.add(id);
            }
        }
        return out;
    }

    /** Связанные интерфейсы (сервер). */
    public List<InterfaceBlockEntity> linked() {
        return InterfaceNetwork.peers(this);
    }

    // ---------- передача ----------

    boolean canIn(TransferType t) { return isConnected() && enabled(t) && flow().in; }

    boolean canOut(TransferType t) { return isConnected() && enabled(t) && flow().out; }

    // ---------- сохранение и перенос с предметом ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        InterfaceData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().ifPresent(t -> tag.put("interface", t));
        tag.putByte("status", (byte) status.ordinal());
        tag.putInt("peers", peers);
        if (tardis != null) tag.putString("in_tardis", tardis.location().toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("interface")) data = InterfaceData.CODEC.parse(NbtOps.INSTANCE, tag.get("interface")).result().orElse(InterfaceData.DEFAULT);
        // На сервере это пересчитает refresh(); на клиенте — показываем присланное.
        status = Status.of(tag.getByte("status"));
        peers = tag.getInt("peers");
        ResourceLocation t = tag.contains("in_tardis") ? ResourceLocation.tryParse(tag.getString("in_tardis")) : null;
        if (joinedKey == null) tardis = t == null ? null : ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, t);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        InterfaceData d = input.get(TRoomsBlocks.INTERFACE_DATA.get());
        if (d != null) data = d;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (!data.equals(InterfaceData.DEFAULT)) builder.set(TRoomsBlocks.INTERFACE_DATA.get(), data);
    }

    // Mojang пометила метод устаревшим, но замены в 1.21.1 нет: без него настройки попадут в предмет дважды (тегом и компонентом).
    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("interface");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
