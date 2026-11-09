# Rooms from datapacks

**English** · [Русский](datapack-rooms.ru.md) · [Contents](README.md)

A datapack can add its own rooms to the catalogue. Each room is one or two structure templates (one per level) and a JSON file. A ready-made example is in [`examples/datapack/tardis_rooms_example`](../examples/datapack/tardis_rooms_example): copy the folder into a world's `datapacks` folder and the Library appears among the rooms. Its [README](../examples/datapack/README.md) goes through the example field by field.

## Files

- `data/<namespace>/structures/<path>.nbt` holds the templates, saved with a structure block or with the export command, see [Exporting a room](#exporting-a-room).
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
| `removable` | no | Blocks of this room that never keep it from being removed, see below |
| `rotatable` | no | `true` by default. `false` means the room is never turned: it can only be grown from nodes it already faces |

A passage is `{"x": ..., "z": ..., "side": ..., "floor": ...}`: the module inside the template (counted from 0), the wall it is in (`north`, `south`, `west` or `east`) and the level (`0` is the bottom one, the default).

## Removable blocks

`removable` lists blocks that go with the room whatever is in them, so they never keep it from being removed or replaced. An entry is either a place or a description:

- `[x, y, z]` is a block in template coordinates on the bottom level, `[x, y, z, floor]` on another level. Only that exact block counts: the same block placed by a player elsewhere in the room still has to be emptied first. Use places for ordinary blocks such as a lectern or a chest.
- A string matches every such block in this room, wherever it stands, including blocks placed by players, written as described in [Block entries](block-entries.md). Use it for blocks a player is unlikely to place, such as a head with a particular profile: `"minecraft:player_head{SkullOwner:{Name:\"MHF_Question\"}}"`.

```json
"removable": [[11, 2, 11], [3, 2, 5, 1], "minecraft:player_head{SkullOwner:{Name:\"MHF_Question\"}}"]
```

Anything in these blocks is lost together with the room, so mark only blocks where players are not expected to keep things.

Places follow the room when it is turned to face a node, like `control_block`, including blocks moved by the mirrored strip next to a passage (see below). Places and descriptions are remembered with the room when it grows, so changing the datapack later doesn't change the rules for rooms already built.

## Template requirements

- Width and depth are multiples of 24 blocks (one module). A level is at most 27 blocks high. Both levels of a two-level room have the same footprint.
- Each passage is a hole in the outer wall at blocks 8–10 along the wall, counted from the module's north or west corner, and 2–4 blocks above the bottom of the template. These blocks must be air. The row above, at 5, may hold an arch.
- Save the template with air included. Empty spots in a template leave whatever was there before.
- The template is placed with its bottom at the level's floor: layers 0–1 are the floor, as in the built-in rooms.

The passages at 8–10 are not centred in the wall, so when a room is turned to face a node, some passages land at 13–15. The addon then mirrors a strip of that wall, blocks 8–15 along it, the full height of the level and 3 blocks deep, so the passage lands back at 8–10. Keep decorations that must not be mirrored out of this strip, or set `"rotatable": false`.

A two-level room grows upwards from the node's level, so it can't be grown from the top level. On the top level only 18 blocks of height are available.

Problems are reported in the server log when datapacks load, one line per problem, starting with `Datapack room <id>:`. A room with problems is skipped.

## Exporting a room

`/tardis_rooms export <id>` saves the room you are standing in, whether built-in, from a datapack or rebuilt by hand, as a datapack room. It needs operator rights.

- The result is a complete datapack in `<world>/tardis_rooms_export` with the templates, the JSON and `pack.mcmeta`. Move it to the world's `datapacks` folder and run `/reload`.
- The room is saved as it stands, without turning it. The entrance is the side it was attached to the node with.
- Growth nodes, seals and their lamps become air, and the Room Control Block becomes the floor next to it, with its position written to `control_block`. TARDIS Interfaces become air too, so that their IDs don't repeat.
- Only rectangular rooms on one level or on two neighbouring levels can be exported.

If you remove a datapack, the rooms already built from it stay in place. They show up as "Unknown room" and can still be replaced or removed.
