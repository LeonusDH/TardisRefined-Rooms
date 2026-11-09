# Configuration and commands

**English** · [Русский](configuration.ru.md) · [Contents](README.md)

## Server and world: `config/tardis_rooms/common.toml`

| Option | Default | What it does |
|---|---|---|
| `disableProceduralRooms` | `true` | Stop Tardis Refined from generating procedural ARS rooms and corridors around the interior, including the random corridors above the hub. The console room, the Eye of Harmony and the central corridor hub stay. Affects only chunks that haven't been generated yet |
| `simplifyHub` | `true` | Strip the Tardis Refined hub down to its essential parts, once per TARDIS, when a player first enters it. See [Rooms](rooms.md#the-hub) |
| `debug` | `false` | Write details to the log: who grew, replaced or removed what, why an action was refused, the connectivity graph when removing or replacing, where nodes were placed in the hub |
| `compat.mekanism` | `true` | Mekanism chemicals through the TARDIS Interface |
| `compat.ae2` | `true` | AE2 ME networks through the TARDIS Interface and the Memory Card for IDs |
| `compat.jade` | `true` | Interface status in the Jade tooltip |

A `compat` option only matters if that mod is installed.

## Blocks that go with a room: `config/tardis_rooms/removable_with_room.json`

Blocks that never keep a room from being removed or replaced and go with it, whatever is in them. It is a separate file so that a modpack can add its mods without touching the main config. It is created with the default list if it doesn't exist, read when the server starts and read again on `/reload`.

```json
{
  "_comment": "...",
  "blocks": [
    "#minecraft:all_signs", "#minecraft:banners",
    "minecraft:skeleton_skull", "minecraft:skeleton_wall_skull", "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_wall_skull",
    "minecraft:zombie_head", "minecraft:zombie_wall_head", "minecraft:creeper_head", "minecraft:creeper_wall_head",
    "minecraft:piglin_head", "minecraft:piglin_wall_head", "minecraft:dragon_head", "minecraft:dragon_wall_head",
    "minecraft:player_head", "minecraft:player_wall_head"
  ]
}
```

Entries are written as described in [Block entries](block-entries.md). Anything in these blocks is lost together with the room: don't add blocks where players keep things, and be careful with whole mods, since in tech mods almost every block holds something. TARDIS Interfaces always count. Datapack rooms can mark such blocks for themselves, see [Rooms from datapacks](datapack-rooms.md#removable-blocks).

## Client: `config/tardis_rooms/client.toml`

| Option | Default | What it does |
|---|---|---|
| `doorLeverClosedAngle` | `60.0` | Factory console: how many degrees the door lever turns when the door is closed. If the lever turns the wrong way, change the sign. `0` turns the animation off |

## Commands

| Command | Who | What it does |
|---|---|---|
| `/tardis_rooms export <id>` | operators | Save the room you are standing in as a datapack room. See [Exporting a room](datapack-rooms.md#exporting-a-room) |
| `/tardis_rooms show_blockers <x y z>` | everyone | Outline again what keeps the room with the Room Control Block at `x y z` from being removed, up to 64 blocks away. Clicking the "In the way" chat message runs it |
