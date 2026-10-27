package leonus.tardis_rooms.compat;

import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.iface.Flow;
import leonus.tardis_rooms.iface.InterfaceBlock;
import leonus.tardis_rooms.iface.InterfaceBlockEntity;
import leonus.tardis_rooms.iface.InterfaceData;
import leonus.tardis_rooms.iface.Status;
import leonus.tardis_rooms.iface.TransferType;
import leonus.tardis_rooms.lang.TRoomsText;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade: состояние интерфейса ТАРДИС (подключён / нет ID / нет оболочки / конфликт сети), ID, направление. Грузится только с Jade. */
@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(InterfaceInfo.INSTANCE, InterfaceBlockEntity.class);
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

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!leonus.tardis_rooms.TardisRoomsConfig.compatJade() || !(accessor.getBlockEntity() instanceof InterfaceBlockEntity be)) return;
            tag.putByte("status", (byte) be.status().ordinal());
            tag.putInt("id", be.id());
            tag.putInt("peers", be.peerCount());
            tag.putByte("flow", (byte) be.flow().ordinal());
            tag.putInt("types", be.data().types());
            tag.putInt("faces", be.data().faces());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!leonus.tardis_rooms.TardisRoomsConfig.compatJade()) return;
            CompoundTag d = accessor.getServerData();
            if (!d.contains("status")) return;
            Status st = Status.of(d.getByte("status"));
            tooltip.add(st.text());
            int id = d.getInt("id");
            if (id != 0) tooltip.add(TRoomsText.IFACE_ID.text(Component.literal(InterfaceData.hex(id)).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
            // Режимы сторон — собственная настройка интерфейса: видны и без связи.
            int faceBits = d.getInt("faces");
            MutableComponent faces = Component.empty();
            boolean first = true;
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                leonus.tardis_rooms.iface.FaceMode m = InterfaceData.face(faceBits, dir);
                if (m == leonus.tardis_rooms.iface.FaceMode.DEFAULT) continue;
                if (!first) faces.append(", ");
                faces.append(leonus.tardis_rooms.iface.InterfaceTools.faceName(dir).text()).append(": ")
                        .append(leonus.tardis_rooms.iface.InterfaceTools.faceModeName(m).text()
                                .withStyle(m == leonus.tardis_rooms.iface.FaceMode.PUSH ? ChatFormatting.GOLD : ChatFormatting.AQUA));
                first = false;
            }
            if (!first) tooltip.add(TRoomsText.JADE_INTERFACE_FACES.text(faces).withStyle(ChatFormatting.GRAY));
            if (st != Status.CONNECTED) return;
            tooltip.add(TRoomsText.IFACE_PEERS.text(d.getInt("peers")).withStyle(ChatFormatting.GRAY));
            tooltip.add(TRoomsText.IFACE_FLOW.text(flow(Flow.of(d.getByte("flow")))).withStyle(ChatFormatting.GRAY));
            int types = d.getInt("types");
            MutableComponent on = Component.empty();
            first = true;
            for (TransferType t : TransferType.values()) {
                if (!t.available() || (types & t.bit()) == 0) continue;
                if (!first) on.append(", ");
                on.append(name(t));
                first = false;
            }
            if (!first) tooltip.add(on.withStyle(ChatFormatting.DARK_GRAY));
        }

        private static MutableComponent name(TransferType t) {
            return (switch (t) {
                case ENERGY -> TRoomsText.IFACE_ENERGY;
                case ITEMS -> TRoomsText.IFACE_ITEMS;
                case FLUIDS -> TRoomsText.IFACE_FLUIDS;
                case CHEMICALS -> TRoomsText.IFACE_CHEMICALS;
                case ME -> TRoomsText.IFACE_ME;
                case REDSTONE -> TRoomsText.IFACE_REDSTONE;
            }).text();
        }

        private static Component flow(Flow f) {
            return (switch (f) {
                case IN -> TRoomsText.IFACE_FLOW_IN;
                case OUT -> TRoomsText.IFACE_FLOW_OUT;
                case BOTH -> TRoomsText.IFACE_FLOW_BOTH;
            }).text();
        }
    }
}
