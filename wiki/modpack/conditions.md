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
| `ftbquests:<id>` | The quest or chapter is completed by the player's team (FTB Quests 1.15.2 / 1604: by the player). The hex id FTB Quests shows; `#` is optional. | `ftbquests:1A2B3C4D5E6F7081` |
| `bq:<id>` | The Better Questing quest is complete. A number (the quest id the quest book and `/bq_admin` show); GTNH fork (1.7.10): the quest UUID, dashed or in the Base64 form its commands show. | `bq:42` |
| `heracles:<id>` | The Heracles / Odyssey Quests quest is complete | `heracles:kill_the_lich` |
| `script:<name>` | A condition registered by a KubeJS / CraftTweaker script ([script integration](/modpack/scripts)); unknown names are false with a `/whm reload` warning | `script:veteran` |
| `!<condition>` | The condition is false | `!tag:veteran` |

How the lists combine: all of `if` **and** at least one of `any` (when given) **and** none of `unless`.

::: tip Script mods on every version
KubeJS, CraftTweaker and command blocks can drive rules on every version: let them set a **scoreboard tag** or **score**, and use `tag:` / `score:` conditions. Where a binding exists, scripts register `script:` conditions directly ([script integration](/modpack/scripts)).
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
| `stage:` | Game Stages | Forge 1.11.2 - 1.20.3, NeoForge 1.20.1 (Game Stages exists only for Forge) |
| `ftbquests:` | FTB Quests | 1.12.2, 1.15.2, 1.16.4 - 1.21.1, 1.21.11, 26.1 - 26.1.2 as published (see the matrix) |
| `bq:` | Better Questing | Forge 1.7.10 - 1.12.2 (also the GTNH fork and Better Questing Unofficial), Forge / NeoForge 1.20.1 (Better Questing 4) |
| `heracles:` | Heracles (now "Odyssey Quests", mod id `heracles`) | 1.20 / 1.20.1 (Fabric, Forge, NeoForge with the Forge jar), 1.21 / 1.21.1 (Fabric), 1.21.1 (NeoForge) |

### Quest mod matrix {#quest-mod-matrix}

Every Minecraft version and loader What Hit Me? supports, checked against Modrinth, CurseForge and the mods' own Maven
repositories (November 2026). ✔ = tested in game with an automated test (rule checked before and after the quest progress, and
the pack editor's condition list); the Minecraft version in brackets is where it ran when the jar covers several versions.
– = the quest mod has no build for it. ✘ = a build exists but does not run there.

| What Hit Me? jar (Minecraft) | FTB Quests | Better Questing | Heracles / Odyssey Quests | Game Stages |
|---|---|---|---|---|
| Forge 1.7.10 | – | ✔ 3.0.328, ✔ 3.9.2-GTNH (GTNH fork) | – | – |
| Forge 1.8.9 | – | ✔ 1.1.159 | – | – |
| Forge 1.9.4 (1.9) | – | ✔ 1.2.160, ✔ 1.2.114 (1.9) | – | – |
| Forge 1.10.2 (1.10) | – | ✔ 3.3.298, ✔ 1.3.140 (1.10) | – | – |
| Forge 1.11.2 (1.11) | – | ✔ 2.4.235, ✔ 2.4.187 (1.11) | – | ✔ 1.0.11 |
| Forge 1.12.2 (1.12, 1.12.1) | ✔ 1202.9.0.15 (1.12.2) | ✔ 3.5.329, ✔ Better Questing Unofficial 4.3.2, ✔ 2.5.236 (1.12.1) | – | ✔ 2.0.123, ✔ 1.0.62 (1.12.1) |
| Forge 1.14.4 | – | – | – | ✔ 4.2.15 |
| Forge 1.15.2 | ✔ 1502.0.2.252 | – | – | ✔ 5.0.2 |
| Fabric 1.16.5 (1.16.2 - 1.16.4) | ✔ 1605.3.7 (1.16.5) | – | – | – |
| Forge 1.16.5 (1.16.3, 1.16.4) | ✔ 1605.3.7, ✔ 1604.1.1.21 (1.16.4) | – | – | ✔ 7.3.13, ✔ 6.0.4 (1.16.4), ✔ 5.0.1 (1.16.3) |
| Fabric 1.18.2 | ✔ 1802.3.16 | – | – | – |
| Forge 1.18.2 | ✔ 1802.3.16 | – | – | ✔ 8.1.3 |
| Fabric 1.19.2 (1.19, 1.19.1) | ✔ 1902.5.10 (1.19.2) | – | – | – |
| Forge 1.19.2 (1.19, 1.19.1) | ✔ 1902.5.10 (1.19.2) | – | – | ✔ 11.1.4, ✔ 9.1.2 (1.19), ✔ 10.0.1 (1.19.1) |
| Forge 1.19.4 | – | – | – | ✔ 13.0.1 |
| Fabric 1.20.1 (1.20) | ✔ 2001.4.22 (1.20.1) | – | ✔ 1.1.13, ✔ 0.0.7 (1.20) | – |
| Forge 1.20.1 (1.20) | ✔ 2001.4.22 (1.20.1) | ✔ 4.0.71 | ✔ 1.1.13, ✔ 0.0.7 (1.20) | ✔ 15.0.2, ✔ 14.0.10 (1.20) |
| NeoForge 1.20.1 (Forge jar) | ✘ | ✔ 4.0.71 | ✔ 1.1.13 | ✔ 15.0.2 |
| Fabric 1.20.2 | ✔ 2002.2.0 snapshot | – | – | – |
| Forge 1.20.2 | – | – | – | ✔ 16.0.1 |
| Fabric 1.20.4 (1.20.3) | ✔ 2004.2.3 (1.20.4) | – | – | – |
| Forge 1.20.4 (1.20.3) | ✔ 2004.2.3 (1.20.4) | – | – | ✔ 17.0.1 (1.20.3) |
| NeoForge 1.20.4 | ✔ 2004.2.3 | – | – | – |
| Fabric 1.20.6 (1.20.5) | ✘ | – | – | – |
| NeoForge 1.20.6 | ✔ 2006.1.0 snapshot | – | – | – |
| Fabric 1.21.1 (1.21) | ✔ 2101.1.39, ✔ 2100.1.4 (1.21) | – | ✔ 2.0.3 (1.21.1 and 1.21) | – |
| NeoForge 1.21.1 (1.21) | ✔ 2101.1.39, ✔ 2100.1.4 (1.21) | – | ✔ 2.0.3 (1.21.1) | – |
| Fabric, NeoForge 1.21.11 | ✔ 2111.1.5 | – | – | – |
| Fabric 26.1.2 (26.1, 26.1.1) | ✔ 26.1.2.10 (26.1.2 and 26.1) | – | – | – |
| NeoForge 26.1.2 (26.1, 26.1.1) | ✔ 26.1.2.10 (26.1.2) | – | – | – |

No quest mod exists for: Fabric 1.14.4, 1.15.2, 1.16.1, 1.17.1; Forge 1.17.1; 1.19.3; Fabric 1.19.4; Forge 1.20.6; every
Forge jar from 1.21.1 on; 1.21.3 - 1.21.10; 26.2 and 26.3.

Why some versions are missing:

- **FTB Quests** needs exactly the Minecraft version it was built for: 1.19.2 (not 1.19 / 1.19.1), 1.20.1 (not 1.20), 1.20.4
  (not 1.20.3 on Forge), 26.1.2 on NeoForge (not 26.1 / 26.1.1). Fabric 1.16.2 - 1.16.4: FTB Teams for Fabric needs 1.16.5.
  NeoForge 1.20.1: FTB Library 2001 needs Forge 47.3, which NeoForge 1.20.1 does not report. There are no FTB Quests builds
  for 1.14, 1.17, 1.19.3, 1.19.4, Forge after 1.20.4, 1.21.2 - 1.21.10, 26.2 and 26.3. 1.20.2 and 1.20.6 have only unreleased
  snapshot builds on FTB's Maven; the Fabric 1.20.6 snapshot crashes at start by itself. On Fabric 26.1, FTB Library needs Fabric
  API 0.149.1 or newer (0.155.3 works).
- **Better Questing** was never made for Fabric or 1.13 - 1.19; the 1.20.1 version (4.x) is an alpha for Forge (it also runs
  on NeoForge 1.20.1).
- **Heracles / Odyssey Quests** was released only for 1.20 / 1.20.1 and 1.21.1 (its 1.19.4 branch was never released). The
  NeoForge 1.21.1 jar needs 1.21.1; the Fabric jar also runs on 1.21.
- **Game Stages** exists only for Forge, up to 1.20.3 (the 1.20.1 version also lists NeoForge). Similar stage mods for newer
  versions (for example AStages, ProgressiveStages, SDM GameStages) use their own API and are not supported by `stage:`; use a
  scoreboard tag or score set by them instead.

## Finding quest ids

| Mod | Where to find the id |
|---|---|
| FTB Quests | The hex id of a quest or chapter (FTB Quests can copy it in edit mode). Write it with or without `#`. |
| Better Questing (1.7.10 - 1.12.2, 1.20.1) | The numeric quest id (quest editor, `/bq_admin` commands). On 1.20.1 (Better Questing 4) use the short id the quest book shows. |
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
