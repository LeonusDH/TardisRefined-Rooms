package leonus.tardis_rooms.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import leonus.tardis_rooms.TardisRooms;
import leonus.tardis_rooms.lang.TRoomsText;
import leonus.tardis_rooms.room.Lattice;
import leonus.tardis_rooms.room.custom.RoomExporter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/** Команды аддона: {@code /tardis_rooms export <id>} — сохранить помещение, в котором стоишь, как помещение датапака. */
public final class TRoomsCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(TardisRooms.MODID)
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("export")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .executes(TRoomsCommands::export))));
    }

    private static int export(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        ServerLevel level = src.getLevel();
        if (!Lattice.isTardisLevel(level)) {
            src.sendFailure(TRoomsText.CMD_EXPORT_NOT_IN_ROOM.text());
            return 0;
        }
        if (id.getNamespace().equals(TardisRooms.MODID) || id.getNamespace().equals("minecraft")) {
            src.sendFailure(TRoomsText.CMD_EXPORT_NAMESPACE.text());
            return 0;
        }
        try {
            RoomExporter.Result r = RoomExporter.export(level, BlockPos.containing(src.getPosition()), id);
            String path = r.pack().toAbsolutePath().toString();
            Component link = Component.literal(path).withStyle(s -> s.withUnderlined(true).withColor(ChatFormatting.AQUA)
                    // OPEN_FILE игра с сервера не принимает — даём скопировать путь.
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, path))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TRoomsText.CMD_EXPORT_OPEN.text())));
            src.sendSuccess(() -> TRoomsText.CMD_EXPORT_DONE.text(id.toString(), link), true);
            r.note().ifPresent(n -> src.sendSuccess(() -> n.text().withStyle(ChatFormatting.YELLOW), false));
            return 1;
        } catch (RoomExporter.Failure f) {
            src.sendFailure(f.reason.text());
        } catch (Exception e) {
            TardisRooms.LOGGER.error("Room export failed", e);
            src.sendFailure(TRoomsText.CMD_EXPORT_ERROR.text(String.valueOf(e.getMessage())));
        }
        return 0;
    }

    private TRoomsCommands() {}
}
