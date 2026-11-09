# Example datapack room

*[Русская версия](README.ru.md)*

`tardis_rooms_example` adds one room, the Library: bookshelves along the walls and a lectern in the middle. Copy the folder into a world's `datapacks` folder and run `/reload`. The Library appears among the rooms in the catalogue of a Room Growth Node.

```
tardis_rooms_example/
├── pack.mcmeta
└── data/example/
    ├── structure/rooms/library.nbt          the template: one module, 24×24 blocks
    └── tardis_rooms/rooms/library.json      the room description
```

## library.json, field by field

```json
{
  "name": "Library",
  "description": "Bookshelves along the walls and a lectern in the middle",
  "group": "room",
  "structure": "example:rooms/library",
  "entrance": {"x": 0, "z": 0, "side": "south"},
  "exits": [{"x": 0, "z": 0, "side": "north"}],
  "control_block": [11, 1, 15],
  "removable": [[11, 2, 11], "minecraft:player_head{profile:{name:\"MHF_Question\"}}"]
}
```

- `name`, `description`: the name and the tooltip in the catalogue.
- `group`: the catalogue group, here the same as the built-in rooms.
- `structure`: the template, `data/example/structure/rooms/library.nbt`.
- `entrance`: the room grows from a node through the south wall of its only module (`x` 0, `z` 0). The room is turned so that this side faces the node.
- `exits`: one more passage in the north wall. It gets a growth node, joins a neighbour's passage or is sealed.
- `control_block`: the Room Control Block stands at x 11, y 1, z 15 of the template, in the floor four blocks south of the lectern.
- `removable`: blocks that go with the room whatever is in them, so they never keep it from being removed:
  - `[11, 2, 11]` is the lectern in the middle. Without it, a book left on the lectern would keep the room from being removed. A lectern placed by a player elsewhere still has to be emptied first.
  - `"minecraft:player_head{profile:{name:\"MHF_Question\"}}"` shows the other kind of entry: any question-mark head in this room, wherever it stands. The Library has none; the entry is only here as an example.

All fields, the template requirements and the entry format for `removable` are described in the main [README](../../README.md#custom-rooms-from-datapacks).
