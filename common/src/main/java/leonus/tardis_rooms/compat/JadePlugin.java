package leonus.tardis_rooms.compat;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.Flow;
import leonus.tardis_rooms.iface.InterfaceBlock;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceData;
import leonus.tardis_rooms.iface.InterfaceTools;
import leonus.tardis_rooms.iface.LinkedTotals;
import leonus.tardis_rooms.iface.Status;
import leonus.tardis_rooms.iface.kind.BuiltinKinds;
import leonus.tardis_rooms.iface.kind.TransferKind;
import leonus.tardis_rooms.iface.kind.TransferKinds;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Jade: состояние интерфейса ТАРДИС (подключён / нет ID / нет оболочки / конфликт сети), ID, направление. Грузится только с Jade:
 * на NeoForge он находит плагин по {@link WailaPlugin}, на Fabric — по точке входа {@code jade} в fabric.mod.json.
 */
@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(InterfaceInfo.INSTANCE, InterfaceBlockEntity.class);
        registration.registerEnergyStorage(InterfaceEnergy.INSTANCE, InterfaceBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(InterfaceInfo.INSTANCE, InterfaceBlock.class);
    }

    public enum InterfaceInfo implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "interface");

        @Override
        public ResourceLocation getUid() { return UID; }

        /** Ключи с префиксом: данные с сервера у всех плагинов Jade лежат в одном теге, а «id» и «status» там могут быть заняты. */
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!leonus.tardis_rooms.TardisRoomsConfig.compatJade() || !(accessor.getBlockEntity() instanceof InterfaceBlockEntity be)) return;
            tag.putByte("tardis_rooms_status", (byte) be.status().ordinal());
            tag.putInt("tardis_rooms_id", be.id());
            tag.putInt("tardis_rooms_peers", be.peerCount());
            tag.putByte("tardis_rooms_flow", (byte) be.flow().ordinal());
            // Виды — по id: номера видов у сервера и клиента могут не совпасть.
            ListTag kinds = new ListTag();
            for (ResourceLocation k : be.data().kinds()) kinds.add(StringTag.valueOf(k.toString()));
            tag.put("tardis_rooms_kinds", kinds);
            tag.putInt("tardis_rooms_faces", be.data().faces());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!leonus.tardis_rooms.TardisRoomsConfig.compatJade()) return;
            CompoundTag d = accessor.getServerData();
            if (!d.contains("tardis_rooms_status")) return;
            Status st = Status.of(d.getByte("tardis_rooms_status"));
            tooltip.add(st.text());
            int id = d.getInt("tardis_rooms_id");
            if (id != 0) tooltip.add(TRoomsText.IFACE_ID.text(InterfaceTools.idText(id)).withStyle(ChatFormatting.GRAY));
            // Режимы сторон — собственная настройка интерфейса: видны и без связи.
            int faceBits = d.getInt("tardis_rooms_faces");
            MutableComponent faces = Component.empty();
            boolean first = true;
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                leonus.tardis_rooms.iface.FaceMode m = InterfaceData.face(faceBits, dir);
                if (m == leonus.tardis_rooms.iface.FaceMode.DEFAULT) continue;
                if (!first) faces.append(", ");
                faces.append(InterfaceTools.faceName(dir).text()).append(": ")
                        .append(InterfaceTools.faceModeName(m).text()
                                .withStyle(switch (m) {
                                    case PUSH -> ChatFormatting.GOLD;
                                    case PULL -> ChatFormatting.AQUA;
                                    default -> ChatFormatting.RED;
                                }));
                first = false;
            }
            if (!first) tooltip.add(TRoomsText.JADE_INTERFACE_FACES.text(faces).withStyle(ChatFormatting.GRAY));
            if (st != Status.CONNECTED) return;
            tooltip.add(TRoomsText.IFACE_PEERS.text(d.getInt("tardis_rooms_peers")).withStyle(ChatFormatting.GRAY));
            tooltip.add(TRoomsText.IFACE_FLOW.text(InterfaceTools.flowName(Flow.of(d.getByte("tardis_rooms_flow"))).text()).withStyle(ChatFormatting.GRAY));
            Set<String> kinds = new HashSet<>();
            for (Tag k : d.getList("tardis_rooms_kinds", Tag.TAG_STRING)) kinds.add(k.getAsString());
            MutableComponent on = Component.empty();
            first = true;
            for (TransferKind t : TransferKinds.all()) {
                if (!t.available() || !kinds.contains(t.id().toString())) continue;
                if (!first) on.append(", ");
                on.append(t.name());
                first = false;
            }
            if (!first) tooltip.add(on.withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Полоса энергии в подсказке. Без настоящих хранилищ у связанных интерфейсов интерфейс называет кабелям условную
     * ёмкость (иначе часть кабелей к нему не подключается), и встроенный показ Jade рисовал бы «0 / 2,1 G». Здесь Jade
     * спрашивает раньше встроенного показа: нет настоящей ёмкости — пустой ответ, полосы нет; есть — null, и Jade
     * показывает как обычно (настоящие цифры). То, что видят кабели, не меняется.
     */
    public enum InterfaceEnergy implements IServerExtensionProvider<CompoundTag> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(TardisRooms.MODID, "interface_energy");

        @Override
        public ResourceLocation getUid() { return UID; }

        @Override
        public @Nullable List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            if (accessor.getTarget() instanceof InterfaceBlockEntity be
                    && be.part(BuiltinKinds.energy()) instanceof LinkedTotals t && t.linkedTotals()[1] > 0) return null;
            return List.of();
        }
    }
}
