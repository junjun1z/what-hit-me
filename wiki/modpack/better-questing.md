# Better Questing <Badge type="tip" text="1.3.0+" />

`bq:<id>` is true when the player has completed the Better Questing quest. It covers every Better Questing generation: 1.x - 3.x for Forge 1.7.10 - 1.12.2, the **GTNH fork** (1.7.10), **Better Questing Unofficial** (1.12.2) and **Better Questing 4** (1.20.1). See [Quest mods](/modpack/quest-mods) for how quest conditions work in general.

## Condition syntax

```json
"unless": ["bq:42"]
```

| Mod | Form | Example |
|---|---|---|
| Better Questing 1.x - 3.x, Better Questing Unofficial | the numeric quest id | `bq:42` |
| Better Questing 4 (1.20.1) | the short quest id the quest book shows | `bq:42` |
| GTNH fork (1.7.10) | the quest UUID, with dashes or in the Base64 form its commands show | `bq:0f6c3c4e-2a1b-4d7e-9c55-8b1e2d3a4f60` |
| any | `!bq:<id>` = not completed | `!bq:42` |

## Supported versions

| What Hit Me? jar (Minecraft) | Better Questing |
|---|---|
| Forge 1.7.10 | ✔ 3.0.328, ✔ 3.9.2-GTNH (GTNH fork) |
| Forge 1.8.9 | ✔ 1.1.159 |
| Forge 1.9.4 (1.9) | ✔ 1.2.160, ✔ 1.2.114 (1.9) |
| Forge 1.10.2 (1.10) | ✔ 3.3.298, ✔ 1.3.140 (1.10) |
| Forge 1.11.2 (1.11) | ✔ 2.4.235, ✔ 2.4.187 (1.11) |
| Forge 1.12.2 (1.12, 1.12.1) | ✔ 3.5.329, ✔ Better Questing Unofficial 4.3.2, ✔ 2.5.236 (1.12.1) |
| Forge 1.20.1 (1.20) | ✔ 4.0.71 |
| NeoForge 1.20.1 (Forge jar) | ✔ 4.0.71 |

✔ = tested in game (rule before and after the quest, pack editor quest list); the Minecraft version in brackets is where it ran. All other versions have no Better Questing build. The full table of all quest mods is the [quest mod matrix](/modpack/quest-mods#quest-mod-matrix).

## Finding the ids

- **Quest editor / quest book:** in edit mode the quest editor shows the quest id. Better Questing 4 shows a short id in the quest book.
- **Commands:** `/bq_admin` commands list and take quest ids. The GTNH fork prints the UUID in its Base64 form; What Hit Me? accepts that form and the dashed UUID.
- **Files:** the quest database (`config/betterquesting/DefaultQuests.json`, or the `DefaultQuests/` folder of the GTNH fork) has the ids.
- **Pack editor:** the [condition picker](/modpack/pack-editor#condition-picker) lists the quests of the server with their names (also the GTNH fork). Click one to insert the id.

## Example rules

A boss stays `???` until the player finished quest 128:

```json
{
  "rules": [
    { "match": "*@twilightforest:lich", "unless": ["bq:128"], "name": "???", "hint": "", "hide": ["attacker", "mod"] }
  ]
}
```

GTNH fork, a tip for lava deaths that points to a quest, shown until the player has done that quest:

```json
{
  "rules": [
    {
      "match": "minecraft:lava",
      "unless": ["bq:9d2c11e0-6b3f-4a58-b2c4-0e7f1a6d8c35"],
      "hint": "Do the quest \"Fireproof\": its reward is a stack of Fire Resistance potions."
    }
  ]
}
```

On 1.7.10 / 1.8.9 vanilla has no scoreboard tags. Combine quests with scores instead: `"if": ["bq:42", "score:deaths>=3"]`.

## Version notes

- **Every API generation:** 1.x (1.8.9, 1.9.4: a static quest database), 2.x / 3.x (`QuestingAPI`, 1.10.2 - 1.12.2 and 1.7.10), the GTNH fork (UUID ids) and 4.x (1.20.1, a new code base with its own progress manager). What Hit Me? detects the generation at run time.
- **Better Questing Unofficial** (1.12.2) works like 3.x.
- **No Fabric version** and no version for 1.13 - 1.19 exist.
- **Better Questing 4** for 1.20.1 is an alpha for Forge. It also runs on NeoForge 1.20.1 with the same jar.
- **1.7.10 / 1.8.9:** combine with `score:` instead of `tag:` (no entity tags before 1.9), see [Conditions](/modpack/conditions#built-in-conditions-per-minecraft-version).

## Testing with /whm test

1. `/whm reload`: a malformed id (not a number / UUID) or a missing Better Questing is listed there.
2. Preview both states:

```
/whm test minecraft:generic twilightforest:lich --as Steve --assume !bq:128
/whm test minecraft:generic twilightforest:lich --as Steve --assume bq:128
```

3. To try it for real, complete and reset the quest:

```
/bq_admin complete 128 Steve
/bq_admin reset 128 Steve
```

Better Questing 4 (1.20.1) resets with `/bq_admin reset Steve 128`. The GTNH fork takes the Base64 id in these commands.
