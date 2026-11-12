# Configuration and commands

**English** · [Русский](configuration.ru.md) · [Contents](README.md)

## Server and world: `config/tardis_rooms/common.toml`

| Option | Default | What it does |
|---|---|---|
| `disableProceduralRooms` | `true` | Stop Tardis Refined from generating procedural ARS rooms and corridors around the interior, including the random corridors above the hub. The console room, the Eye of Harmony and the central corridor hub stay. Affects only chunks that haven't been generated yet |
| `simplifyHub` | `true` | Strip the Tardis Refined hub down to its essential parts, once per TARDIS, when a player first enters it. See [Rooms](rooms.md#the-hub) |
| `debug` | `false` | Also write the addon's details to the main log (`latest.log`), see [Logs](#logs) |
| `compat.mekanism` | `true` | Mekanism chemicals through the TARDIS Interface |
| `compat.ae2` | `true` | AE2 ME networks through the TARDIS Interface and the Memory Card for IDs |
| `compat.jade` | `true` | Interface status in the Jade tooltip |
| `compat.acuaTardisGadgets` | `true` | Permissions to grow rooms, manage rooms and set up interfaces in the AcuaTARDISGadgets Security Manager, see [Permissions](permissions.md). Off: the permissions aren't added and everyone may build. Read once at startup, so restart the game after changing it |

A `compat` option only matters if that mod is installed.

## Blocks that go with a room: `config/tardis_rooms/removable_with_room.toml`

Blocks that never keep a room from being removed or replaced and go with it, whatever is in them. It is a separate file so that a modpack can add its mods without touching the main config. It is created with the default list and comments describing the entries. Changes are usually picked up by themselves, no restart needed; if one isn't (on Windows this happens on 1.20.1), run `/reload`. A line of your own can be commented out with `#`.

```toml
blocks = ["#minecraft:all_signs", "#minecraft:banners",
    "minecraft:skeleton_skull", "minecraft:skeleton_wall_skull", "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_wall_skull",
    "minecraft:zombie_head", "minecraft:zombie_wall_head", "minecraft:creeper_head", "minecraft:creeper_wall_head",
    "minecraft:piglin_head", "minecraft:piglin_wall_head", "minecraft:dragon_head", "minecraft:dragon_wall_head",
    "minecraft:player_head", "minecraft:player_wall_head"]
```

Entries are written as described in [Block entries](block-entries.md). Anything in these blocks is lost together with the room: don't add blocks where players keep things, and be careful with whole mods, since in tech mods almost every block holds something. TARDIS Interfaces always count. Datapack rooms can mark such blocks for themselves, see [Rooms from datapacks](datapack-rooms.md#removable-blocks).

## Logs

- **`latest.log`, always:** a summary when the server starts: interface kinds, main options, and each compatibility that is on, either working or with its mod not installed (compatibilities switched off in the config aren't mentioned); rooms grown, replaced and removed (who, what, where); a room removed together with what was in the way, with the list of lost blocks; the hub simplified or left as it is and why; datapack rooms loaded; warnings and errors.
- **Details, at DEBUG level, always.** Forge keeps them in `logs/debug.log`; Fabric by default doesn't keep DEBUG lines at all, so there they are only seen with `debug = true`. The details are: every player action with rooms, interfaces, the screwdriver, wrenches and the Memory Card; why a request was refused, including silent refusals (too far, no permission); interface status changes and new IDs; what keeps a room from being removed; what was remembered when a room grew; skipped entries.
- With `debug = true`, the details also go to `latest.log`, marked `[debug]`, together with the connectivity graph when a room is removed or replaced. That's the log people usually send, and on Fabric the only one.

## Client: `config/tardis_rooms/client.toml`

| Option | Default | What it does |
|---|---|---|
| `doorLeverClosedAngle` | `60.0` | Factory console: how many degrees the door lever turns when the door is closed. If the lever turns the wrong way, change the sign. `0` turns the animation off |

## Commands

| Command | Who | What it does |
|---|---|---|
| `/tardis_rooms export <id>` | operators | Save the room you are standing in as a datapack room. See [Exporting a room](datapack-rooms.md#exporting-a-room) |
| `/tardis_rooms show_blockers <x y z>` | everyone | Outline again what keeps the room with the Room Control Block at `x y z` from being removed, up to 64 blocks away. Clicking the "In the way" chat message runs it |
