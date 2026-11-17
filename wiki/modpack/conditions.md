# Conditions & quest mods <Badge type="tip" text="1.3.0+" />

Conditions are used by [progression rules](/modpack/rules) (`if`, `any`, `unless`) and by [death triggers](/modpack/death-triggers). Each condition is `prefix:argument`. A `!` in front negates it.

```json
"if":     ["advancement:minecraft:story/enter_the_nether", "score:deaths>=3"],
"any":    ["stage:nether_unlocked", "tag:nether_guide"],
"unless": ["ftbquests:1A2B3C4D5E6F7081"]
```

## All conditions

| Condition | True when | Example |
|---|---|---|
| `advancement:<id>` | The player has completed the advancement | `advancement:minecraft:end/kill_dragon` |
| `tag:<tag>` | The player has the scoreboard tag (`/tag` command) | `tag:veteran` |
| `score:<objective><op><n>` | The player's score compares true. Ops: `>=` `>` `<=` `<` `=` `!=`. No score = false. | `score:deaths>=3` |
| `stage:<name>` | The player has the Game Stages stage | `stage:nether` |
| `ftbquests:<id>` | The quest or chapter is completed by the player's team (FTB Quests 1.15.2: by the player). The hex id FTB Quests shows; `#` is optional. | `ftbquests:1A2B3C4D5E6F7081` |
| `bq:<id>` | The Better Questing quest is complete. 1.12.2: a number. GTNH fork (1.7.10): the quest UUID, dashed or in the Base64 form its commands show. | `bq:42` |
| `heracles:<id>` | The Heracles / Odyssey Quests quest is complete | `heracles:kill_the_lich` |
| `!<condition>` | The condition is false | `!tag:veteran` |

How the lists combine: all of `if` **and** at least one of `any` (when given) **and** none of `unless`.

::: tip Script mods today
KubeJS, CraftTweaker and command blocks can already drive rules: let them set a **scoreboard tag** or **score**, and use `tag:` / `score:` conditions. Direct script integration is [planned](/modpack/scripts).
:::

## Built-in conditions per Minecraft version

| Condition | 1.7.10, 1.8.9 | 1.9 - 1.11.2 | 1.12+ |
|---|---|---|---|
| `advancement:` | not supported | not supported | ✔ |
| `tag:` | not supported | ✔ | ✔ |
| `score:` | ✔ | ✔ | ✔ |

Same on Forge, Fabric and NeoForge. Advancements do not exist before 1.12 and entity tags not before 1.9. An unsupported condition counts as false and `/whm reload` says "not supported on Forge 1.x".

## Quest mods

The quest mod conditions use reflection. They are **never a dependency**: a provider starts only when its mod is installed. A missing mod or an API mismatch makes the condition false and is logged once; `/whm reload` names it.

| Prefix | Mod | Minecraft versions |
|---|---|---|
| `stage:` | Game Stages | Every version Game Stages exists for (1.12.2 - 1.20.x) |
| `ftbquests:` | FTB Quests | 1.12.2 and 1.15.2+ (Fabric, Forge, NeoForge as published) |
| `bq:` | Better Questing | 1.12.2, and the GTNH fork on 1.7.10 |
| `heracles:` | Heracles (now "Odyssey Quests", mod id `heracles`) | 1.20 - 1.21.1 |

### Tested in game

These combinations were started and checked with an automated in-game test (rule before / after the quest progress):

| Minecraft | Loader | Mods and versions |
|---|---|---|
| 1.7.10 | Forge | Better Questing 3.9.2-GTNH (GTNH fork) |
| 1.12.2 | Forge | Game Stages 2.0.123, Better Questing 3.5.329, FTB Quests 1202.9.0.15 |
| 1.14.4 | Forge | Game Stages 4.2.15 |
| 1.15.2 | Forge | Game Stages 5.0.2, FTB Quests 1502.0.2.252 |
| 1.16.5 | Forge | Game Stages 7.3.13, FTB Quests 1605.3.7-build.165 |
| 1.16.5 | Fabric | FTB Quests 1605.3.7-build.165 |
| 1.18.2 | Forge | Game Stages 8.1.3, FTB Quests 1802.3.16-build.479 |
| 1.18.2 | Fabric | FTB Quests 1802.3.16-build.479 |
| 1.19.2 | Forge | Game Stages 11.1.4, FTB Quests 1902.5.10-build.497 |
| 1.19.2 | Fabric | FTB Quests 1902.5.10-build.497 |
| 1.19.4 | Forge | Game Stages 13.0.1 |
| 1.20.1 | Forge | Game Stages 15.0.2, FTB Quests 2001.4.22, Heracles 1.1.13 |
| 1.20.1 | Fabric | FTB Quests 2001.4.22, Heracles 1.1.13 |
| 1.20.1 | NeoForge | Heracles 1.1.13 |
| 1.20.2 | Forge | Game Stages 16.0.1 |
| 1.20.3 | Forge | Game Stages 17.0.1 |
| 1.20.4 | NeoForge | FTB Quests 2004.2.3 |
| 1.21.1 | Fabric | FTB Quests 2101.1.38 |

Not yet tested in game (supported in the code, checked against the mod's API only): FTB Quests 2101.x on NeoForge 1.21.1, FTB Quests 2111.x (1.21.11) and 26.1.2.x (26.1 - 26.1.2), Odyssey Quests 2.0.3 (1.21.1). Game Stages and Better Questing do not exist for 1.21+, Heracles not for 1.20.6.

## Finding quest ids

| Mod | Where to find the id |
|---|---|
| FTB Quests | The hex id of a quest or chapter (FTB Quests can copy it in edit mode). Write it with or without `#`. |
| Better Questing 1.12.2 | The numeric quest id (quest editor, `/bq_admin` commands) |
| Better Questing GTNH | The quest UUID; the Base64 form the `/bq_admin` commands show works too |
| Heracles / Odyssey Quests | The quest id, i.e. the name of the quest file in `config/heracles/quests/` |
| Game Stages | The stage name used by `/gamestage add` |

## Checking conditions

- `/whm reload` lists unknown prefixes, conditions without prefix, missing mods, conditions unsupported on this version and bad arguments (for example a malformed `score:`).
- `/whm test <damage_id> ... --as <player>` evaluates the rules with that player's real progress.
- `--assume <condition>` treats a condition as fulfilled, `--assume !<condition>` as not fulfilled. Repeat it for several conditions.

```
/whm test minecraft:generic twilightforest:lich --as Steve --assume stage:twilight --assume !tag:veteran
```
