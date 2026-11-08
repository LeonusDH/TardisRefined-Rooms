package leonus.tardis_rooms.iface.kind;

import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Вид передачи через интерфейс (энергия, предметы, …): запись реестра {@link TransferKinds}.
 * В сохранениях и по сети вид — его {@link #id()}; номер ({@link #index()}, {@link #bit()}) выдаётся при регистрации
 * и живёт только в памяти.
 */
public final class TransferKind {
    /** Часть вида для интерфейса; создаётся вместе с ним. */
    @FunctionalInterface
    public interface PartFactory {
        KindPart create(InterfaceBlockEntity be, TransferKind kind);
    }

    private final ResourceLocation id;
    private final int index;
    private final boolean enabledByDefault;
    private final BooleanSupplier available;
    private final Supplier<Component> name;
    private final @Nullable Supplier<Component> tooltip;
    private final @Nullable PartFactory parts;

    private TransferKind(Builder b, int index) {
        this.id = b.id;
        this.index = index;
        this.enabledByDefault = b.enabledByDefault;
        this.available = b.available;
        this.name = b.name;
        this.tooltip = b.tooltip;
        this.parts = b.parts;
    }

    public ResourceLocation id() { return id; }

    /** Номер в реестре, с нуля, в порядке регистрации. */
    public int index() { return index; }

    /** Бит вида в наборе включённых видов интерфейса (только в памяти). */
    public int bit() { return 1 << index; }

    /** Включён ли у нового интерфейса. */
    public boolean enabledByDefault() { return enabledByDefault; }

    /** Можно ли сейчас пользоваться видом: нужный мод установлен и вид не выключен в настройках. */
    public boolean available() { return available.getAsBoolean(); }

    /** Название для меню и подсказок. */
    public Component name() { return name.get(); }

    /** Пояснение в меню, или null — без пояснения. */
    public @Nullable Component tooltip() { return tooltip == null ? null : tooltip.get(); }

    /** Часть вида для интерфейса, или null — своей части у вида нет. */
    public @Nullable KindPart createPart(InterfaceBlockEntity be) {
        return parts == null ? null : parts.create(be, this);
    }

    @Override
    public String toString() { return id.toString(); }

    /** Описание вида до регистрации; получается из {@link KindRegistration#kind(ResourceLocation)}. */
    public static final class Builder {
        private final ResourceLocation id;
        private final Function<Builder, TransferKind> register;
        private boolean enabledByDefault;
        private BooleanSupplier available = () -> true;
        private Supplier<Component> name;
        private @Nullable Supplier<Component> tooltip;
        private @Nullable PartFactory parts;
        private boolean done;

        Builder(ResourceLocation id, Function<Builder, TransferKind> register) {
            this.id = id;
            this.register = register;
            this.name = () -> Component.literal(id.toString());
        }

        /** Включён у нового интерфейса (по умолчанию — нет). */
        public Builder enabledByDefault() {
            enabledByDefault = true;
            return this;
        }

        /** Когда вид доступен (по умолчанию — всегда). */
        public Builder available(BooleanSupplier available) {
            this.available = Objects.requireNonNull(available);
            return this;
        }

        public Builder name(Supplier<Component> name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /** Пояснение в меню (по умолчанию — нет). */
        public Builder tooltip(Supplier<Component> tooltip) {
            this.tooltip = Objects.requireNonNull(tooltip);
            return this;
        }

        /** Часть вида в каждом интерфейсе; null — без части (поведение вида задано в другом месте). */
        public Builder parts(@Nullable PartFactory parts) {
            this.parts = parts;
            return this;
        }

        /** Зарегистрировать вид. Только во время регистрации, один раз. */
        public TransferKind register() {
            if (done) throw new IllegalStateException("Transfer kind " + id + " is already registered");
            done = true;
            return register.apply(this);
        }

        TransferKind build(int index) { return new TransferKind(this, index); }

        ResourceLocation id() { return id; }
    }
}
