# Game Stages <Badge type="tip" text="1.3.0+" />

`stage:<name>` is true when the player has the Game Stages stage. Stages are often given by quest rewards (FTB Quests, Better Questing), by advancements or by scripts, so a stage is a handy switch for packs that gate content in steps. See [Quest mods](/modpack/quest-mods) for how quest conditions work in general.

## Condition syntax

```json
"if": ["stage:nether"]
```

| Form | Meaning |
|---|---|
| `stage:<name>` | The player has the stage |
| `!stage:<name>` | The player does not have it |

The name is the one used by `/gamestage add`. Write it exactly as Game Stages uses it.

## Supported versions

| What Hit Me? jar (Minecraft) | Game Stages |
|---|---|
| Forge 1.11.2 (1.11) | ✔ 1.0.11 |
| Forge 1.12.2 (1.12, 1.12.1) | ✔ 2.0.123, ✔ 1.0.62 (1.12.1) |
| Forge 1.14.4 | ✔ 4.2.15 |
| Forge 1.15.2 | ✔ 5.0.2 |
| Forge 1.16.5 (1.16.3, 1.16.4) | ✔ 7.3.13, ✔ 6.0.4 (1.16.4), ✔ 5.0.1 (1.16.3) |
| Forge 1.18.2 | ✔ 8.1.3 |
| Forge 1.19.2 (1.19, 1.19.1) | ✔ 11.1.4, ✔ 9.1.2 (1.19), ✔ 10.0.1 (1.19.1) |
| Forge 1.19.4 | ✔ 13.0.1 |
| Forge 1.20.1 (1.20) | ✔ 15.0.2, ✔ 14.0.10 (1.20) |
| NeoForge 1.20.1 (Forge jar) | ✔ 15.0.2 |
| Forge 1.20.2 | ✔ 16.0.1 |
| Forge 1.20.4 (1.20.3) | ✔ 17.0.1 (1.20.3) |

✔ = tested in game (rule before and after the stage, pack editor stage list); the Minecraft version in brackets is where it ran. All other versions have no Game Stages build. The full table of all quest mods is the [quest mod matrix](/modpack/quest-mods#quest-mod-matrix).

## Finding the names

- **Your pack:** the stage names are whatever your quests, scripts or commands give (`/gamestage add <player> <stage>`).
- **Commands:** `/gamestage info` shows your own stages, `/gamestage check <player> <stage>` checks one.
- **Pack editor:** the [condition picker](/modpack/pack-editor#condition-picker) lists the stages Game Stages knows (its known stages list; Game Stages 1.0.x for 1.11.2 - 1.12.1 has none). Any name can be typed.

## Example rules

Nether mobs are `???` until the player has the `nether` stage, and lava gets a beginner tip until the `iron_age` stage:

```json
{
  "rules": [
    { "match": "*@minecraft:blaze", "unless": ["stage:nether"], "name": "???", "hint": "" },
    { "match": "minecraft:lava", "unless": ["stage:iron_age"], "hint": "A water bucket puts out the fire. Craft one with three iron ingots." }
  ]
}
```

Combined with a quest mod: the stage must be there **and** the FTB Quests chapter must not be done yet:

```json
{
  "rules": [
    { "match": "*@cataclysm:ignis", "if": ["stage:nether"], "unless": ["ftbquests:2C7E0B91A4D35F18"], "name": "???" }
  ]
}
```

## Version notes

- **Forge only**, up to 1.20.3. The 1.20.1 version also lists NeoForge and runs on NeoForge 1.20.1 with the same jar.
- **Two APIs:** Game Stages 1.0.x (1.11.2 - 1.12.1) and 2.x and newer (1.12.2 on). What Hit Me? uses whichever is installed.
- **Other stage mods** for newer versions (for example AStages, ProgressiveStages, SDM GameStages) have their own API and are not read by `stage:`. Let them set a scoreboard tag or score instead and use `tag:` / `score:` ([Conditions](/modpack/conditions)).

## Testing with /whm test

1. `/whm reload`: a missing Game Stages is listed there.
2. Preview both states:

```
/whm test minecraft:generic minecraft:blaze --as Steve --assume !stage:nether
/whm test minecraft:generic minecraft:blaze --as Steve --assume stage:nether
```

3. To try it for real:

```
/gamestage add Steve nether
/gamestage remove Steve nether
```
