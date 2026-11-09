package leonus.tardis_rooms.iface;

import leonus.tardis_rooms.iface.kind.KindPart;
import leonus.tardis_rooms.iface.kind.TransferKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Редстоун через интерфейс (часть вида; общий для платформ). Сигнал передаётся от интерфейсов со входом к интерфейсам
 * с выходом. «Вход и выход» для редстоуна — только вход: иначе интерфейс ловил бы свой же сигнал через провод
 * и защёлкивался навсегда.
 */
public final class RedstonePart implements KindPart {
    private final InterfaceBlockEntity be;
    private final TransferKind kind;
    /** Сигнал, принятый от соседей (вход), и сигнал, который интерфейс выдаёт (выход). */
    private int input, output;
    /** Связанные при прошлом пересчёте: если связь сменилась, им тоже надо пересчитать выход. */
    private List<InterfaceBlockEntity> lastPeers = List.of();
    /** Стороны при прошлом пересчёте: выключенная сторона перестаёт выдавать сигнал — соседей надо оповестить. */
    private int lastFaces;
    /** Интерфейс убирают: больше не принимает (связанные пересчитывают выход без него). */
    private boolean removed;

    public RedstonePart(InterfaceBlockEntity be, TransferKind kind) {
        this.be = be;
        this.kind = kind;
    }

    /** Выдаваемый сигнал (без учёта выключенных сторон). */
    public int output() { return output; }

    private boolean reads() { return !removed && be.isConnected() && be.enabled(kind) && be.flow().in; }

    private boolean emits() { return be.isConnected() && be.enabled(kind) && be.flow() == Flow.OUT; }

    private static RedstonePart of(InterfaceBlockEntity peer, TransferKind kind) {
        return peer.part(kind) instanceof RedstonePart p ? p : null;
    }

    @Override
    public void invalidate() {
        updateInput();
        // Сменилась связь — пересчитать и тех, с кем были связаны, и тех, с кем теперь.
        List<InterfaceBlockEntity> now = be.linked();
        Set<InterfaceBlockEntity> touched = new LinkedHashSet<>(lastPeers);
        touched.addAll(now);
        lastPeers = List.copyOf(now);
        updateOutputs(touched);
        updateOutput();
        int faces = be.data().faces();
        if (faces != lastFaces) {
            lastFaces = faces;
            Level level = be.getLevel();
            if (level != null) level.updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
        }
    }

    @Override
    public void tick() {
        lastPeers = List.copyOf(be.linked());
        updateInput();
        updateOutput();
    }

    @Override
    public void neighborChanged() { updateInput(); }

    @Override
    public int signal(Direction side) { return output; }

    @Override
    public void remove() {
        removed = true;
        input = 0;
        updateOutputs(be.linked());
    }

    /** Перечитать входящий сигнал и, если он другой, сообщить связанным. */
    private void updateInput() {
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) return;
        int v = 0;
        if (reads()) {
            // Сигнал соседнего интерфейса не считаем: интерфейс с выходом рядом с интерфейсом со входом
            // той же сети иначе защёлкнул бы сигнал навсегда.
            for (Direction d : Direction.values()) {
                BlockPos np = be.getBlockPos().relative(d);
                if (!be.open(d) || level.getBlockEntity(np) instanceof InterfaceBlockEntity) continue;
                v = Math.max(v, level.getSignal(np, d));
                if (v >= 15) break;
            }
        }
        if (v == input) return;
        input = v;
        updateOutputs(be.linked());
    }

    /** Пересчитать выход у этих интерфейсов. */
    private void updateOutputs(Iterable<InterfaceBlockEntity> peers) {
        for (InterfaceBlockEntity p : peers) {
            RedstonePart rp = of(p, kind);
            if (rp != null) rp.updateOutput();
        }
    }

    /** Выдаваемый сигнал — наибольший среди связанных интерфейсов со входом. */
    private void updateOutput() {
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) return;
        int out = 0;
        if (emits()) {
            for (InterfaceBlockEntity p : be.linked()) {
                RedstonePart rp = of(p, kind);
                if (rp != null && rp.reads()) out = Math.max(out, rp.input);
            }
        }
        if (out == output) return;
        output = out;
        level.updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
    }
}
