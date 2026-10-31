package leonus.tardis_rooms.lang;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Все строки мода: ключ перевода и английский текст рядом (как {@code LocalizationEnum} в Applied Energistics 2).
 * Английский {@code en_us.json} генерирует датаген ({@code gradlew :neoforge:runData}, см. {@link TRoomsLangProvider});
 * остальные языки — обычные файлы в {@code assets/tardis_rooms/lang/}.
 * Новую строку добавляй сюда и используй в коде через {@link #text()}.
 */
public enum TRoomsText {
    // ---------- Mod list (NeoForge берёт описание мода отсюда, текст в mods.toml — запасной) ----------
    MOD_DESCRIPTION("fml.menu.mods.info.description.tardis_rooms", "A hand-built TARDIS interior: grow rooms and corridors with the sonic screwdriver on a grid, instead of Tardis Refined's endless procedural ARS corridors. The TARDIS Interface carries items, fluids, energy and redstone between rooms and to the outside, plus Mekanism chemicals and AE2 ME networks."),
    // ---------- Blocks ----------
    BLOCK_PANEL_LAMP("block.tardis_rooms.panel_lamp", "Panel Lamp"),
    BLOCK_ROOM_CORE("block.tardis_rooms.room_core", "Room Control Block"),
    BLOCK_ROOM_NODE("block.tardis_rooms.room_node", "Room Growth Node"),
    BLOCK_ROOM_WALL("block.tardis_rooms.room_wall", "Passage Seal"),
    BLOCK_UPPER_HALF("block.tardis_rooms.upper_half", "TARDIS Door Top"),
    // ---------- Messages (action bar) ----------
    MSG_CUTS_OFF("message.tardis_rooms.cuts_off", "Not possible: other spaces connect through this one"),
    MSG_CUTS_OFF_REPLACE("message.tardis_rooms.cuts_off_replace", "Not possible: the replacement would cut other spaces off"),
    MSG_FORBIDDEN("message.tardis_rooms.forbidden", "The TARDIS cannot grow here"),
    MSG_GROWN("message.tardis_rooms.grown", "The TARDIS has grown a new space"),
    MSG_HAS_CONTAINERS("message.tardis_rooms.has_containers", "Take your things first: there are containers or machines inside"),
    MSG_INVALID("message.tardis_rooms.invalid", "This node is not on a passage"),
    MSG_NEED_SCREWDRIVER("message.tardis_rooms.need_screwdriver", "Hold a screwdriver"),
    MSG_OCCUPIED("message.tardis_rooms.occupied", "The space behind this wall is already taken"),
    MSG_OFF_GRID("message.tardis_rooms.off_grid", "This block only fits the Tardis Refined grid"),
    MSG_CUSTOM_NO_HEIGHT("message.tardis_rooms.custom_no_height", "This room is too tall for this level"),
    MSG_CUSTOM_DIRECTION("message.tardis_rooms.custom_direction", "This room can't be turned to face this node"),
    MSG_PROTECTED("message.tardis_rooms.protected", "This block is part of the TARDIS and can't be broken"),
    MSG_REMOVED("message.tardis_rooms.removed", "Space removed"),
    MSG_REPLACED("message.tardis_rooms.replaced", "Space replaced"),
    MSG_UNKNOWN_PIECE("message.tardis_rooms.unknown_piece", "No such room"),
    MSG_USE_SCREWDRIVER("message.tardis_rooms.use_screwdriver", "Use a screwdriver to choose a room"),
    MSG_USE_SCREWDRIVER_CORE("message.tardis_rooms.use_screwdriver_core", "Use a screwdriver to manage this room"),
    // ---------- Menu: categories ----------
    GROUP_CORRIDOR("screen.tardis_rooms.group.corridor", "Corridors"),
    GROUP_CORRIDOR_TOOLTIP("screen.tardis_rooms.group.corridor.tooltip", "Plain corridors: straight, turns, forks"),
    GROUP_LIFT("screen.tardis_rooms.group.lift", "Lifts"),
    GROUP_LIFT_TOOLTIP("screen.tardis_rooms.group.lift.tooltip", "A shaft to the neighbouring level, with passages like corridors"),
    GROUP_PRESET("screen.tardis_rooms.group.preset", "Tardis Refined blocks"),
    GROUP_PRESET_TOOLTIP("screen.tardis_rooms.group.preset.tooltip", "Ready-made Tardis Refined blocks: rooms with corridors"),
    GROUP_ROOM("screen.tardis_rooms.group.room", "Rooms"),
    GROUP_ROOM_TOOLTIP("screen.tardis_rooms.group.room.tooltip", "Empty dead-end rooms"),
    GROUP_SPECIAL("screen.tardis_rooms.group.special", "Special"),
    GROUP_SPECIAL_TOOLTIP("screen.tardis_rooms.group.special.tooltip", "Tardis Refined Gravity Well"),
    GROUP_DATAPACK("screen.tardis_rooms.group.datapack", "From datapacks"),
    GROUP_DATAPACK_TOOLTIP("screen.tardis_rooms.group.datapack.tooltip", "Rooms added by datapacks"),
    CUSTOM_TOOLTIP("piece.tardis_rooms.custom.tooltip", "From datapack: %s"),
    ROOM_UNKNOWN("piece.tardis_rooms.unknown", "Unknown room (%s)"),
    CMD_EXPORT_NOT_IN_ROOM("command.tardis_rooms.export.not_in_room", "Stand inside a grown room in a TARDIS"),
    CMD_EXPORT_SHAPE("command.tardis_rooms.export.shape", "Only rectangular rooms on one level or two neighbouring levels can be exported"),
    CMD_EXPORT_NO_ENTRANCE("command.tardis_rooms.export.no_entrance", "The room has no passage on its bottom level to use as the entrance"),
    CMD_EXPORT_ENTRANCE_MOVED("command.tardis_rooms.export.entrance_moved", "The room was grown from its upper level; the entrance is set to the first passage on the bottom level"),
    CMD_EXPORT_NAMESPACE("command.tardis_rooms.export.namespace", "Use your own namespace, e.g. mypack:rooms/library"),
    CMD_EXPORT_DONE("command.tardis_rooms.export.done", "Room exported as %s to the datapack %s — move it into the world's datapacks folder"),
    CMD_EXPORT_OPEN("command.tardis_rooms.export.open", "Click to copy the path"),
    CMD_EXPORT_ERROR("command.tardis_rooms.export.error", "Export failed: %s"),
    // ---------- Menu: room map ----------
    MAP("screen.tardis_rooms.map", "Map"),
    MAP_FLOOR_125("screen.tardis_rooms.map.floor_125", "Top level"),
    MAP_FLOOR_13("screen.tardis_rooms.map.floor_13", "Level 4"),
    MAP_FLOOR_41("screen.tardis_rooms.map.floor_41", "Level 3"),
    MAP_FLOOR_69("screen.tardis_rooms.map.floor_69", "Level 2"),
    MAP_FLOOR_97("screen.tardis_rooms.map.floor_97", "Level 1 — hub"),
    MAP_FORBIDDEN("screen.tardis_rooms.map.forbidden", "No building"),
    MAP_FORBIDDEN_TOOLTIP("screen.tardis_rooms.map.forbidden.tooltip", "Desktop zone (wiped by desktop reconfiguration), the Eye of Harmony spot, and below the hub the area around the hub and the Eye"),
    MAP_FREE("screen.tardis_rooms.map.free", "Free"),
    MAP_LEGEND("screen.tardis_rooms.map.legend", "Legend"),
    MAP_LEGEND_HINT("screen.tardis_rooms.map.legend.hint", "Hover a cell of the map for details"),
    MAP_FREE_TOOLTIP("screen.tardis_rooms.map.free.tooltip", "Untouched TARDIS stone (Fool's Stone)"),
    MAP_HUB("screen.tardis_rooms.map.hub", "TARDIS hub"),
    MAP_HUB_TOOLTIP("screen.tardis_rooms.map.hub.tooltip", "The central corridor hub everything grows from"),
    MAP_MANAGE_TOOLTIP("screen.tardis_rooms.map.manage_tooltip", "Where this room is and what surrounds it"),
    MAP_PIECE("screen.tardis_rooms.map.piece", "Built room"),
    MAP_PIECE_TOOLTIP("screen.tardis_rooms.map.piece.tooltip", "A built room; the white line is the border between rooms"),
    MAP_PLAN("screen.tardis_rooms.map.plan", "New room"),
    MAP_PLAN_TOOLTIP("screen.tardis_rooms.map.plan.tooltip", "The selected option goes here"),
    MAP_PLAN_BAD("screen.tardis_rooms.map.plan_bad", "Blocked"),
    MAP_PLAN_BAD_TOOLTIP("screen.tardis_rooms.map.plan_bad.tooltip", "Already taken here — the option will not fit"),
    MAP_REPLACING("screen.tardis_rooms.map.replacing", "This room"),
    MAP_REPLACING_TOOLTIP("screen.tardis_rooms.map.replacing.tooltip", "The room you are managing (its space frees up on replace)"),
    MAP_SHOW("screen.tardis_rooms.map.show", "▣ Show on map"),
    MAP_SHOW_TOOLTIP("screen.tardis_rooms.map.show.tooltip", "Where the selected option will go and what it runs into"),
    MAP_TARGET("screen.tardis_rooms.map.target", "Node leads here"),
    MAP_TARGET_TOOLTIP("screen.tardis_rooms.map.target.tooltip", "Frame: the module behind the node; mark: the node itself"),
    MAP_TITLE("screen.tardis_rooms.map.title", "Floor map"),
    MAP_TOOLTIP("screen.tardis_rooms.map.tooltip", "What is already taken around: rooms and their borders, the hub, forbidden zones"),
    MAP_UNKNOWN("screen.tardis_rooms.map.unknown", "Foreign build"),
    MAP_UNKNOWN_TOOLTIP("screen.tardis_rooms.map.unknown.tooltip", "Not TARDIS stone, but not an extension either: e.g. generated by Tardis Refined"),
    // ---------- Menu: screens ----------
    SCREEN_BLOCKED("screen.tardis_rooms.blocked", "Doesn't fit: space is taken"),
    SCREEN_CUTS_OFF("screen.tardis_rooms.cuts_off", "Would cut other spaces off the TARDIS"),
    SCREEN_LOADING("screen.tardis_rooms.loading", "Contacting the TARDIS…"),
    SCREEN_MANAGE_TITLE("screen.tardis_rooms.manage_title", "Space control"),
    SCREEN_OFF_GRID("screen.tardis_rooms.off_grid", "Only on the Tardis Refined grid — pick another node"),
    SCREEN_CUSTOM_NO_HEIGHT("screen.tardis_rooms.custom_no_height", "Too tall for this level — pick a node on a lower level"),
    SCREEN_CUSTOM_DIRECTION("screen.tardis_rooms.custom_direction", "Can't be turned to face this node — pick a node facing another way"),
    SCREEN_RENAME("screen.tardis_rooms.rename", "Rename"),
    SCREEN_RENAME_TOOLTIP("screen.tardis_rooms.rename.tooltip", "Give this space its own name"),
    SCREEN_RENAME_TITLE("screen.tardis_rooms.rename.title", "Rename"),
    SCREEN_RENAME_LABEL("screen.tardis_rooms.rename.label", "New name:"),
    SCREEN_RENAME_HINT("screen.tardis_rooms.rename.hint", "Leave empty to remove the name"),
    SCREEN_REMOVE("screen.tardis_rooms.remove", "Remove"),
    SCREEN_REMOVE_TOOLTIP("screen.tardis_rooms.remove.tooltip", "Return TARDIS stone to this place"),
    SCREEN_REMOVE_CONFIRM_TITLE("screen.tardis_rooms.remove_confirm_title", "Remove this space?"),
    SCREEN_REMOVE_NO("screen.tardis_rooms.remove_no", "Cancel"),
    SCREEN_REMOVE_WARNING("screen.tardis_rooms.remove_warning", "Everything inside will vanish"),
    SCREEN_REMOVE_WARNING_TOOLTIP("screen.tardis_rooms.remove_warning.tooltip", "Blocks inside will be erased for good"),
    SCREEN_REMOVE_YES("screen.tardis_rooms.remove_yes", "Yes, remove"),
    SCREEN_REPLACE("screen.tardis_rooms.replace", "Replace…"),
    SCREEN_REPLACE_TOOLTIP("screen.tardis_rooms.replace.tooltip", "Put a different space here"),
    SCREEN_REPLACE_TITLE("screen.tardis_rooms.replace_title", "Replace space"),
    SCREEN_TITLE("screen.tardis_rooms.title", "Grow the TARDIS"),
    // ---------- Corridors ----------
    CORRIDOR_CROSS("piece.tardis_rooms.corridor.cross", "Crossroads"),
    CORRIDOR_LEFT("piece.tardis_rooms.corridor.left", "Turn left"),
    CORRIDOR_RIGHT("piece.tardis_rooms.corridor.right", "Turn right"),
    CORRIDOR_STRAIGHT("piece.tardis_rooms.corridor.straight", "Straight"),
    CORRIDOR_T_FL("piece.tardis_rooms.corridor.t_fl", "Fork: straight and left"),
    CORRIDOR_T_FR("piece.tardis_rooms.corridor.t_fr", "Fork: straight and right"),
    CORRIDOR_T_LR("piece.tardis_rooms.corridor.t_lr", "Fork: left and right"),
    CORRIDOR_TOOLTIP("piece.tardis_rooms.corridor.tooltip", "Corridor only, no rooms"),
    // ---------- Rooms ----------
    ROOM_HALL("piece.tardis_rooms.room.hall", "Hall with exits"),
    ROOM_HALL_TOOLTIP("piece.tardis_rooms.room.hall.tooltip", "2×2 modules, doors on every side, height %s"),
    ROOM_HALL_2F("piece.tardis_rooms.room.hall_2f", "Two-floor hall"),
    ROOM_HALL_2F_TOOLTIP("piece.tardis_rooms.room.hall_2f.tooltip", "Spans both floors: doors below, a round viewing gallery above"),
    ROOM_LARGE("piece.tardis_rooms.room.large", "Large"),
    ROOM_LARGE_TOOLTIP("piece.tardis_rooms.room.large.tooltip", "Dead-end room, 2×2 modules, height %s"),
    ROOM_LONG("piece.tardis_rooms.room.long", "Long"),
    ROOM_LONG_TOOLTIP("piece.tardis_rooms.room.long.tooltip", "Dead-end room, 1×2 modules, height %s"),
    ROOM_SMALL("piece.tardis_rooms.room.small", "Small"),
    ROOM_SMALL_TOOLTIP("piece.tardis_rooms.room.small.tooltip", "Dead-end room, 1 module, height %s"),
    // ---------- Lifts ----------
    LIFT_DOWN("piece.tardis_rooms.lift.down", "Down: %s"),
    LIFT_DOWN_TOOLTIP("piece.tardis_rooms.lift.down.tooltip", "Lift to the level below. Chosen passages here, a crossroads below. Jump to rise, sneak to descend"),
    LIFT_END_BACK("piece.tardis_rooms.lift.end.back", "dead end, exit behind"),
    LIFT_END_DOWN_TOOLTIP("piece.tardis_rooms.lift.end.down.tooltip", "Lift between levels only: entrance here, one exit below in the given direction. Jump to rise, sneak to descend"),
    LIFT_END_FORWARD("piece.tardis_rooms.lift.end.forward", "dead end, exit ahead"),
    LIFT_END_LEFT("piece.tardis_rooms.lift.end.left", "dead end, exit left"),
    LIFT_END_RIGHT("piece.tardis_rooms.lift.end.right", "dead end, exit right"),
    LIFT_END_UP_TOOLTIP("piece.tardis_rooms.lift.end.up.tooltip", "Lift between levels only: entrance here, one exit above in the given direction. Jump to rise, sneak to descend"),
    LIFT_UP("piece.tardis_rooms.lift.up", "Up: %s"),
    LIFT_UP_TOOLTIP("piece.tardis_rooms.lift.up.tooltip", "Lift to the level above. Chosen passages here, a crossroads above. Jump to rise, sneak to descend"),
    // ---------- Tardis Refined pieces ----------
    GRAVITY_WELL("piece.tardis_rooms.gravity_well", "Gravity Well"),
    GRAVITY_WELL_TOOLTIP("piece.tardis_rooms.gravity_well.tooltip", "Tardis Refined shaft spanning two levels (Tardis Refined grid only)"),
    PRESET_TOOLTIP("piece.tardis_rooms.preset.tooltip", "Ready-made Tardis Refined block: %s"),
    PRESET_CORRIDOR("piece.tardis_rooms.preset_corridor", "Corridor block %s"),
    PRESET_ROOM("piece.tardis_rooms.preset_room", "Room block %s"),
    // ---------- TARDIS Interface ----------
    BLOCK_TARDIS_INTERFACE("block.tardis_rooms.tardis_interface", "TARDIS Interface"),
    MSG_INTERFACE_PLACE("message.tardis_rooms.interface_place", "The interface can only be placed inside a TARDIS or right next to its shell"),
    MSG_INTERFACE_USE_SCREWDRIVER("message.tardis_rooms.interface_use_screwdriver", "Use a screwdriver to configure the interface"),
    IFACE_TITLE("screen.tardis_rooms.interface.title", "TARDIS Interface"),
    IFACE_ENERGY("screen.tardis_rooms.interface.energy", "Energy"),
    IFACE_ITEMS("screen.tardis_rooms.interface.items", "Items"),
    IFACE_FLUIDS("screen.tardis_rooms.interface.fluids", "Fluids"),
    IFACE_CHEMICALS("screen.tardis_rooms.interface.chemicals", "Chemicals"),
    JADE_INTERFACE("config.jade.plugin_tardis_rooms.interface", "TARDIS Interface"),
    IFACE_ME("screen.tardis_rooms.interface.me", "ME network"),
    IFACE_ON("screen.tardis_rooms.interface.on", "on"),
    IFACE_OFF("screen.tardis_rooms.interface.off", "off"),
    IFACE_TYPE_TOOLTIP("screen.tardis_rooms.interface.type.tooltip", "Click to allow or forbid passing this through the interface"),
    IFACE_ME_TOOLTIP("screen.tardis_rooms.interface.me.tooltip", "Joins the ME networks next to linked interfaces into one (a dense connection, like a quantum bridge). Works both ways regardless of direction."),
    IFACE_FLOW("screen.tardis_rooms.interface.flow", "Direction: %s"),
    IFACE_FLOW_IN("screen.tardis_rooms.interface.flow.in", "input"),
    IFACE_FLOW_OUT("screen.tardis_rooms.interface.flow.out", "output"),
    IFACE_FLOW_BOTH("screen.tardis_rooms.interface.flow.both", "input and output"),
    IFACE_FLOW_TOOLTIP("screen.tardis_rooms.interface.flow.tooltip", "Click to switch. Input: takes in — whatever is fed here goes to linked output interfaces. Output: gives — takes from the neighbours of linked input interfaces."),
    IFACE_ID("screen.tardis_rooms.interface.id", "ID: %s"),
    IFACE_ID_NONE("screen.tardis_rooms.interface.id.none", "none"),
    IFACE_ID_GENERATE("screen.tardis_rooms.interface.id.generate", "Generate ID"),
    IFACE_ID_NEW("screen.tardis_rooms.interface.id.new", "New ID"),
    IFACE_ID_GENERATE_TOOLTIP("screen.tardis_rooms.interface.id.generate.tooltip", "Gives this interface a new unique ID. Interfaces with the same ID are linked, like P2P tunnels."),
    IFACE_ID_PASTE("screen.tardis_rooms.interface.id.paste", "Paste %s from the screwdriver"),
    IFACE_ID_PASTE_TOOLTIP("screen.tardis_rooms.interface.id.paste.tooltip", "Same as Shift + left click on the interface with the screwdriver in Setup mode"),
    IFACE_ID_CLEAR("screen.tardis_rooms.interface.id.clear", "Clear ID"),
    IFACE_ID_CLEAR_TOOLTIP("screen.tardis_rooms.interface.id.clear.tooltip", "Removes the current ID from the interface: it stops being linked. Putting it into a crafting grid does the same."),
    IFACE_PEERS("screen.tardis_rooms.interface.peers", "Linked interfaces: %s"),
    IFACE_HELP("screen.tardis_rooms.interface.help", "• To link interfaces, give them the same ID.\n• The interface stores nothing and passes everything on at once: the speed depends on the pipes and machines.\n• Sides set to Insert or Extract work with plain chests, no pipes needed. Set them up with the cube button.\n• Wrench: right click switches a side, Shift + right click picks the interface up.\n• Works only while its chunk is loaded."),
    IFACE_STATUS_CONNECTED("screen.tardis_rooms.interface.status.connected", "Connected"),
    IFACE_STATUS_NO_ID("screen.tardis_rooms.interface.status.no_id", "No ID: not linked to anything"),
    IFACE_STATUS_WRONG_TARDIS("screen.tardis_rooms.interface.status.wrong_tardis", "Disconnected: the ID belongs to another TARDIS"),
    IFACE_STATUS_NO_SHELL("screen.tardis_rooms.interface.status.no_shell", "Disconnected: no shell nearby"),
    IFACE_STATUS_CONFLICT("screen.tardis_rooms.interface.status.conflict", "Disconnected: network conflict"),
    IFACE_ITEM_CLEAR_HINT("item.tardis_rooms.tardis_interface.clear_hint", "Put into a crafting grid to clear"),
    IFACE_SCREWDRIVER_ID("item.tardis_rooms.screwdriver.interface_id", "Interface ID: %s"),
    MSG_INTERFACE_NO_ID("message.tardis_rooms.interface_no_id", "This interface has no ID yet — generate one in its menu"),
    MSG_INTERFACE_NO_TARDIS("message.tardis_rooms.interface_no_tardis", "The interface is not connected to any TARDIS"),
    MSG_INTERFACE_COPIED("message.tardis_rooms.interface_copied", "ID %s copied to the screwdriver"),
    MSG_INTERFACE_PASTED("message.tardis_rooms.interface_pasted", "ID %s written to the interface"),
    MSG_INTERFACE_FORGOTTEN("message.tardis_rooms.interface_forgotten", "The screwdriver forgot the interface ID"),
    MSG_INTERFACE_SCREWDRIVER_EMPTY("message.tardis_rooms.interface_screwdriver_empty", "The screwdriver remembers no ID — Shift + right click an interface in Setup mode first"),
    MSG_SCREWDRIVER_MODE("message.tardis_rooms.screwdriver_mode", "Screwdriver mode: %s"),
    SCREWDRIVER_MODE("item.tardis_rooms.screwdriver.mode", "Mode: %s (Shift + mouse wheel)"),
    SCREWDRIVER_MODE_NORMAL("item.tardis_rooms.screwdriver.mode.normal", "Normal"),
    SCREWDRIVER_MODE_CONFIG("item.tardis_rooms.screwdriver.mode.config", "Setup"),
    IFACE_REDSTONE("screen.tardis_rooms.interface.redstone", "Redstone"),
    IFACE_REDSTONE_TOOLTIP("screen.tardis_rooms.interface.redstone.tooltip", "Click to toggle. The signal next to input interfaces is given off by linked output interfaces (the strongest one). With \"input and output\" the interface only reads the signal — otherwise it would catch its own signal and latch forever."),
    IFACE_TIPS("screen.tardis_rooms.interface.tips", "Tips"),
    IFACE_ID_PASTE_NONE("screen.tardis_rooms.interface.id.paste.none", "Paste the ID from the screwdriver"),
    IFACE_ID_PASTE_EMPTY("screen.tardis_rooms.interface.id.paste.empty", "The screwdriver in your hand has no ID — copy one with Shift + right click on another interface (Setup mode)"),
    IFACE_ID_PASTE_SAME("screen.tardis_rooms.interface.id.paste.same", "This ID is already set on the interface"),
    IFACE_FACE_TOOLTIP("screen.tardis_rooms.interface.face.tooltip", "Shows the interface with its neighbours: click a side to switch it. Insert puts into the neighbour what linked inputs have (needs output); Extract takes from the neighbour and sends to linked outputs (needs input). A wrench switches the clicked side too."),
    IFACE_SIDES("screen.tardis_rooms.interface.sides", "Side settings"),
    IFACE_SIDES_RESET("screen.tardis_rooms.interface.sides.reset", "Default view"),
    IFACE_SIDES_NEIGHBOURS_ON("screen.tardis_rooms.interface.sides.neighbours_on", "Neighbouring blocks: shown"),
    IFACE_SIDES_NEIGHBOURS_OFF("screen.tardis_rooms.interface.sides.neighbours_off", "Neighbouring blocks: hidden"),
    IFACE_SIDES_NEIGHBOUR("screen.tardis_rooms.interface.sides.neighbour", "Neighbour: %s"),
    IFACE_SIDES_NOTHING("screen.tardis_rooms.interface.sides.nothing", "Nothing adjacent"),
    IFACE_SIDES_BACK_TOOLTIP("screen.tardis_rooms.interface.sides.back.tooltip", "Back to the interface settings"),
    IFACE_SIDES_NEIGHBOURS_TOOLTIP("screen.tardis_rooms.interface.sides.neighbours.tooltip", "Show or hide the blocks right next to the interface"),
    IFACE_SIDES_RESET_TOOLTIP("screen.tardis_rooms.interface.sides.reset.tooltip", "Turn the block back the way you were looking at it"),
    IFACE_SIDES_HINT("screen.tardis_rooms.interface.sides.hint", "Left click — next mode, right click — previous, Shift + click — default. Drag to rotate."),
    FACE_DOWN("tardis_rooms.face.down", "Bottom"),
    FACE_UP("tardis_rooms.face.up", "Top"),
    FACE_NORTH("tardis_rooms.face.north", "North"),
    FACE_SOUTH("tardis_rooms.face.south", "South"),
    FACE_WEST("tardis_rooms.face.west", "West"),
    FACE_EAST("tardis_rooms.face.east", "East"),
    FACE_MODE_DEFAULT("tardis_rooms.face_mode.default", "Default"),
    FACE_MODE_PUSH("tardis_rooms.face_mode.push", "Insert"),
    FACE_MODE_PULL("tardis_rooms.face_mode.pull", "Extract"),
    MSG_INTERFACE_FACE("message.tardis_rooms.interface_face", "%s side: %s"),
    JADE_INTERFACE_FACES("tardis_rooms.jade.interface.faces", "Sides: %s");

    private static final Map<String, TRoomsText> BY_KEY = new HashMap<>();
    static {
        for (TRoomsText t : values()) BY_KEY.put(t.key, t);
    }

    private final String key;
    private final String english;

    TRoomsText(String key, String english) {
        this.key = key;
        this.english = english;
    }

    public String key() { return key; }

    public String english() { return english; }

    public MutableComponent text() {
        return Component.translatable(key);
    }

    public MutableComponent text(Object... args) {
        return Component.translatable(key, args);
    }

    /** Строка по ключу (для ключей, собираемых из id: форма коридора, тип комнаты, уровень). Нет — ошибка. */
    public static TRoomsText of(String key) {
        TRoomsText t = BY_KEY.get(key);
        if (t == null) throw new IllegalArgumentException("No TRoomsText for key " + key);
        return t;
    }

    public static TRoomsText corridor(String shapeId) { return of("piece.tardis_rooms.corridor." + shapeId); }
    public static TRoomsText room(String typeId) { return of("piece.tardis_rooms.room." + typeId); }
    public static TRoomsText roomTooltip(String typeId) { return of("piece.tardis_rooms.room." + typeId + ".tooltip"); }
    public static TRoomsText liftEnd(String exitId) { return of("piece.tardis_rooms.lift.end." + exitId); }
    public static TRoomsText group(String groupId) { return of("screen.tardis_rooms.group." + groupId); }
    public static TRoomsText groupTooltip(String groupId) { return of("screen.tardis_rooms.group." + groupId + ".tooltip"); }
    public static TRoomsText floor(int floorY) { return of("screen.tardis_rooms.map.floor_" + floorY); }
}
