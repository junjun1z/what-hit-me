# Heracles / Odyssey Quests <Badge type="tip" text="1.3.0+" />

`heracles:<id>` is true when the player has completed the Heracles quest. Heracles (by Team Resourceful) is now published as **Odyssey Quests**; the mod id is still `heracles`, so the same condition works for both. See [Quest mods](/modpack/quest-mods) for how quest conditions work in general.

## Condition syntax

```json
"unless": ["heracles:kill_the_lich"]
```

| Form | Meaning |
|---|---|
| `heracles:<quest id>` | The quest is completed by the player |
| `!heracles:<quest id>` | Not completed |

The quest id is the name of the quest file (without `.json`), not the title shown in the quest book.

## Supported versions

| What Hit Me? jar (Minecraft) | Heracles / Odyssey Quests |
|---|---|
| Fabric 1.20.1 (1.20) | ✔ 1.1.13, ✔ 0.0.7 (1.20) |
| Forge 1.20.1 (1.20) | ✔ 1.1.13, ✔ 0.0.7 (1.20) |
| NeoForge 1.20.1 (Forge jar) | ✔ 1.1.13 |
| Fabric 1.21.1 (1.21) | ✔ 2.0.3 (1.21.1 and 1.21) |
| NeoForge 1.21.1 (1.21) | ✔ 2.0.3 (1.21.1) |

✔ = tested in game (rule before and after the quest, pack editor quest list); the Minecraft version in brackets is where it ran. All other versions have no Heracles build. The full table of all quest mods is the [quest mod matrix](/modpack/quest-mods#quest-mod-matrix).

## Finding the ids

- **Files:** every quest is a file in `config/heracles/quests/` (in sub folders per group). The file name without `.json` is the id: `config/heracles/quests/bosses/kill_the_lich.json` → `heracles:kill_the_lich`.
- **Commands:** `/heracles complete` and `/heracles reset` suggest the quest ids while you type.
- **Pack editor:** the [condition picker](/modpack/pack-editor#condition-picker) lists the quests of the server with their titles. Click one to insert the id.

## Example rules

```json
{
  "rules": [
    { "match": "*@twilightforest:lich", "unless": ["heracles:kill_the_lich"], "name": "???", "hint": "", "hide": ["attacker", "mod"] },
    { "match": "*@twilightforest:lich", "hint": "The quest book's Lich chapter has the strategy." }
  ]
}
```

The first rule wins until the quest is done; after that the second one adds a tip. A death trigger can give a hint item once, only before the quest:

```json
{
  "deathTriggers": [
    { "match": "*@twilightforest:lich", "unless": ["heracles:kill_the_lich"], "command": "give {player} minecraft:golden_apple 1", "oncePerPlayer": true }
  ]
}
```

## Version notes

- **Released for 1.20 / 1.20.1 and 1.21 / 1.21.1 only.** Its 1.19.4 branch was never released.
- **NeoForge 1.21.1** needs 1.21.1 (not 1.21). The Fabric jar also runs on 1.21.
- **NeoForge 1.20.1** uses the Forge jar of Heracles, which runs there.
- **Renamed to Odyssey Quests.** Newer files carry the new name; the mod id `heracles` and the quest format are the same.

## Testing with /whm test

1. `/whm reload`: a missing Heracles or an API mismatch is listed there.
2. Preview both states:

```
/whm test minecraft:generic twilightforest:lich --as Steve --assume !heracles:kill_the_lich
/whm test minecraft:generic twilightforest:lich --as Steve --assume heracles:kill_the_lich
```

3. To try it for real, complete and reset the quest:

```
/heracles complete kill_the_lich Steve
/heracles reset kill_the_lich Steve
```
