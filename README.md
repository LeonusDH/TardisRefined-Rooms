# TARDIS Refined: Rooms

**English** · [Русский](README.ru.md)

An addon for [Tardis Refined](https://github.com/Whocraft/TardisRefined) on Minecraft 1.21.1 (NeoForge and Fabric) that replaces its endless procedural ARS corridors with an interior you build yourself, room by room, the way the classic 1.7.10 TARDIS mod worked.

## Features

### Growing the interior

- The interior sits on a fixed grid of modules across five floors: the hub floor, three below it and one above.
- **Room Growth Nodes** sit in the passages. Right-click one with the sonic screwdriver to open a catalogue of rooms and corridors. Pick one and the TARDIS grows it behind the wall. Growing is free.
- The catalogue shows a top-down plan of each option and a **floor map** of what's already taken: built rooms, the hub, zones where nothing can be built, and where the new space would go.
- Every room has a **Room Control Block**. Use the screwdriver on it to replace or remove the room. A room is never removed while it still has containers or machines inside, or if other rooms connect through it.
- Where a passage leads nowhere, a **Passage Seal** closes it. When a neighbouring space is free, the seal becomes a new growth node.
- Datapacks can add their own rooms, and any built room can be exported as one. See [Custom rooms from datapacks](#custom-rooms-from-datapacks).

### TARDIS Interface

A block that moves resources between linked interfaces, either between rooms or between the interior and the outside, next to the TARDIS shell.

- Give interfaces the same ID to link them. The screwdriver can copy an ID from one interface and paste it into another.
- Each interface has a direction (input or output) and a choice of what it passes: items, fluids, energy, redstone, Mekanism chemicals and AE2 ME networks (the last two on NeoForge only).
- The interface stores nothing. It passes everything on at once, so throughput depends on the pipes and machines connected to it.
- Each side can be set to **Default**, **Insert** or **Extract**, either in a 3D side screen or with a wrench from any mod. Insert and Extract sides work with plain chests, no pipes needed.
- Shift + right-click with a wrench picks the interface up straight into your inventory, settings included.
- Wrenches are recognised by the `tardis_rooms:wrenches` item tag: the common wrench tags plus the Tech Reborn wrench. A modpack can add its own with a datapack.

### Other changes to Tardis Refined

- Procedural ARS rooms and corridors are switched off, including the random corridors above the hub.
- The TR hub is simplified: only the airlock centre, the Eye of Harmony viewing chamber and two short passages to the first growth nodes stay. The rest becomes buildable space. This happens only in a hub nobody has built in yet.
- Internal doors and tall TARDIS shells get an invisible top half. You can aim at and click the upper part of the door, and nothing can be placed into it.

## Requirements

| | NeoForge | Fabric |
|---|---|---|
| Minecraft | 1.21.1 | 1.21.1 |
| Loader | NeoForge 21.1.235 or newer | Fabric Loader 0.16.9 or newer, Fabric API 0.110 or newer |
| Tardis Refined | 2.1.6 built from [PR #618](https://github.com/Whocraft/TardisRefined/pull/618) | the same, Fabric build |
| Forge Config API Port | — | 21.1 or newer (bundled with Tardis Refined) |

> [!IMPORTANT]
> The addon relies on changes from Tardis Refined PR #618, which isn't in any public release yet. Until it ships, you need a Tardis Refined build from that PR. See [Building](#building).

Optional integrations. Each one turns on only if that mod is installed:

| Mod | NeoForge | Fabric | What it adds |
|---|---|---|---|
| Mekanism 10.7+ | ✓ | — | Chemicals through the interface |
| Applied Energistics 2 19.2+ | ✓ | — | ME networks through the interface; a Memory Card copies and pastes the interface ID |
| Jade 15.10+ | ✓ | ✓ | Interface status in the tooltip |
| Team Reborn Energy 4.1+ | — | ✓ | Energy through the interface. Most Fabric tech mods bundle it; on NeoForge energy works out of the box |

Mekanism, AE2 and Jade support can also be turned off in the config.

## Custom rooms from datapacks

A datapack can add its own rooms to the catalogue. Each room is one or two structure templates (one per level) and a JSON file. A ready-made example is in [`examples/datapack/tardis_rooms_example`](examples/datapack/tardis_rooms_example): copy the folder into a world's `datapacks` folder and the Library appears among the rooms.

### Files

- `data/<namespace>/structure/<path>.nbt` holds the templates, saved with a structure block or with the export command below.
- `data/<namespace>/tardis_rooms/rooms/<name>.json` describes the room.

The `tardis_rooms` namespace is reserved for the built-in rooms, so use your own.

```json
{
  "name": "Library",
  "description": "Bookshelves along the walls",
  "group": "room",
  "structure": "example:rooms/library",
  "entrance": {"x": 0, "z": 0, "side": "south"},
  "exits": [{"x": 0, "z": 0, "side": "north"}],
  "control_block": [11, 1, 15],
  "rotatable": true
}
```

| Field | Required | Meaning |
|---|---|---|
| `name` | yes | Name in the catalogue: plain text or a text component such as `{"translate": "..."}` |
| `description` | no | Tooltip in the catalogue |
| `group` | no | `corridor`, `room` or `special`. Without it the room goes to the "From datapacks" group, which is hidden while it is empty |
| `structure` | yes | Template id, or a list of two ids for a two-level room (bottom level first) |
| `entrance` | yes | The passage the room is attached to the growth node with. Must be on the bottom level |
| `exits` | no | Other passages. Each gets a growth node, joins a neighbour's passage or is sealed |
| `control_block` | no | Where the Room Control Block goes, in template coordinates. Without it, it goes near the middle of the floor |
| `rotatable` | no | `true` by default. `false` means the room is never turned: it can only be grown from nodes it already faces |

A passage is `{"x": ..., "z": ..., "side": ..., "floor": ...}`: the module inside the template (counted from 0), the wall it is in (`north`, `south`, `west` or `east`) and the level (`0` is the bottom one, the default).

### Template requirements

- Width and depth are multiples of 24 blocks (one module). A level is at most 27 blocks high. Both levels of a two-level room have the same footprint.
- Each passage is a hole in the outer wall at blocks 8–10 along the wall, counted from the module's north or west corner, and 2–4 blocks above the bottom of the template. These blocks must be air. The row above, at 5, may hold an arch.
- Save the template with air included. Empty spots in a template leave whatever was there before.
- The template is placed with its bottom at the level's floor: layers 0–1 are the floor, as in the built-in rooms.

The passages at 8–10 are not centred in the wall, so when a room is turned to face a node, some passages land at 13–15. The addon then mirrors a strip of that wall, blocks 8–15 along it, the full height of the level and 3 blocks deep, so the passage lands back at 8–10. Keep decorations that must not be mirrored out of this strip, or set `"rotatable": false`.

A two-level room grows upwards from the node's level, so it can't be grown from the top level. On the top level only 18 blocks of height are available.

Problems are reported in the server log when datapacks load, one line per problem, starting with `Datapack room <id>:`. A room with problems is skipped.

### Exporting a room

`/tardis_rooms export <id>` saves the room you are standing in, whether built-in, from a datapack or rebuilt by hand, as a datapack room. It needs operator rights.

- The result is a complete datapack in `<world>/tardis_rooms_export` with the templates, the JSON and `pack.mcmeta`. Move it to the world's `datapacks` folder and run `/reload`.
- The room is saved as it stands, without turning it. The entrance is the side it was attached to the node with.
- Growth nodes, seals and their lamps become air, and the Room Control Block becomes the floor next to it, with its position written to `control_block`. TARDIS Interfaces become air too, so that their IDs don't repeat.
- Only rectangular rooms on one level or on two neighbouring levels can be exported.

If you remove a datapack, the rooms already built from it stay in place. They show up as "Unknown room" and can still be replaced or removed.

## Configuration

`config/tardis_rooms/common.toml`:

| Option | Default | What it does |
|---|---|---|
| `disableProceduralRooms` | `true` | Stop Tardis Refined from generating procedural ARS rooms and corridors |
| `simplifyHub` | `true` | Strip the TR hub down to its essential parts (see above) |
| `debug` | `false` | Log detailed information about what the addon builds, replaces or removes |
| `compat.mekanism` / `compat.ae2` / `compat.jade` | `true` | Turn individual integrations on or off |

The client config sets the Factory console's door lever angle.

## Building

You need JDK 21. Mekanism, AE2, Jade and Team Reborn Energy come from Maven automatically. Tardis Refined from PR #618 has to be built locally:

1. Build Tardis Refined from the PR:
   ```sh
   git clone https://github.com/Whocraft/TardisRefined.git
   cd TardisRefined
   git fetch origin pull/618/head
   git checkout FETCH_HEAD
   ./gradlew :forge:downloadAssets   # first time only, together with the next line
   ./gradlew :forge:runData          # generates models, recipes and translations; close it once it reports the files are written
   ./gradlew :forge:build
   ./gradlew :fabric:build
   ```
2. Copy both jars into this project's `libs/` folder:
   - `forge/build/libs/forge-tardis_refined-mc1.21.1-v2.1.6.jar` as `tardis_refined-neoforge-1.21.1-2.1.6-pr618.jar`;
   - `fabric/build/libs/fabric-tardis_refined-mc1.21.1-v2.1.6.jar` as `tardis_refined-fabric-1.21.1-2.1.6-pr618.jar`.

   To keep the original names, change `tr_neoforge_jar` and `tr_fabric_jar` in `gradle.properties` instead.
3. Build the addon:
   ```sh
   ./gradlew build
   ```
   The jars end up in `neoforge/build/libs/` and `fabric/build/libs/`. On Windows, `build.bat` does the same: it finds a JDK 21 and writes the output to `build.log`.

The code shared by both loaders lives in `common/`; `neoforge/` and `fabric/` hold only what differs between them.

> [!NOTE]
> Java treats `!` in a path as a separator inside jar paths, so if the project folder has `!` in its path, the game (data generation, test runs) fails to start. `build.bat` works around it by mapping the folder to a temporary drive letter; when running Gradle by hand, use a path without `!`.

English translations are generated from `TRoomsText` with `./gradlew :neoforge:runData`. Other languages are plain files in `common/src/main/resources/assets/tardis_rooms/lang/`.

## Contributing

Bug reports, ideas and pull requests are welcome.

## AI assistance

This addon is developed with the help of AI.

## License

[PolyForm Noncommercial 1.0.0](LICENSE.md): you may use, change and share the code, and send pull requests, but not for commercial purposes.

Tardis Refined itself is a separate project by Whocraft under its own license.
