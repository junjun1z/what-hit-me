# Quest mods <Badge type="tip" text="1.3.0+" />

[Progression rules](/modpack/rules) and [death triggers](/modpack/death-triggers) can check quest progress: a boss shows up as `???` until the boss quest is done, a tip appears only after a chapter, a reward command runs only for players who have not finished a quest yet.

## How it works

- Each quest mod has its own condition prefix. You write the quest id after it, in the `if`, `any` or `unless` list of a rule.
- The conditions use reflection. A quest mod is **never a dependency**: its provider starts only when the mod is installed.
- The **server** reads the progress, whenever it builds an output for a player (death recap, respawn summary, alerts, `/whm history`, ...). It works in multiplayer and players cannot fake it. Nothing is cached, so completing a quest reveals old entries at once.
- A missing mod or an API the provider does not know makes the condition **false**. The reason is logged once and `/whm reload` names it. The game never crashes because of it.

| Prefix | Mod | Argument | Minecraft versions | Page |
|---|---|---|---|---|
| `ftbquests:` | FTB Quests | hex quest or chapter id | 1.12.2, 1.15.2, 1.16.4 - 1.21.1, 1.21.11, 26.1 - 26.1.2 as published | [FTB Quests](/modpack/ftb-quests) |
| `bq:` | Better Questing (also the GTNH fork, Better Questing Unofficial, 4.x) | quest id (number; GTNH: UUID) | Forge 1.7.10 - 1.12.2, Forge / NeoForge 1.20.1 | [Better Questing](/modpack/better-questing) |
| `heracles:` | Heracles (now "Odyssey Quests", mod id `heracles`) | quest id | 1.20 / 1.20.1, 1.21 / 1.21.1 | [Heracles / Odyssey Quests](/modpack/heracles) |
| `stage:` | Game Stages | stage name | Forge 1.11.2 - 1.20.3, NeoForge 1.20.1 | [Game Stages](/modpack/game-stages) |

Vanilla advancements, scoreboard tags and scores are on [Conditions](/modpack/conditions); `script:` conditions on [Script integration](/modpack/scripts).

```json
{
  "rules": [
    { "match": "*@twilightforest:lich", "unless": ["ftbquests:4F2A9C1D7B3E6A05"], "name": "???", "hint": "", "hide": ["attacker", "mod"] }
  ]
}
```

## Quest mod matrix {#quest-mod-matrix}

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
Forge jar from 1.21.1 on; 1.21.3 - 1.21.10; 26.2 and 26.3. The reasons for each gap are on the page of each mod. There you can
still use [scoreboard tags or scores](/modpack/conditions) set by the quest mod's rewards or by commands.

## Combining several quest mods

All prefixes work in the same lists, so a pack with two quest mods (for example FTB Quests and Game Stages) can mix them in one rule:

```json
{
  "rules": [
    {
      "match": "*@cataclysm:ignis",
      "if": ["stage:nether_unlocked"],
      "unless": ["ftbquests:2C7E0B91A4D35F18"],
      "name": "???",
      "hint": "Finish the quest \"Into the Fire\" first."
    },
    {
      "match": "*@cataclysm:ignis",
      "any": ["ftbquests:2C7E0B91A4D35F18", "heracles:ignis_slain"],
      "hint": "Bring Fire Resistance and Golden Apples."
    }
  ]
}
```

- `if`: all must be true, `any`: at least one, `unless`: none. See [How rules are chosen](/modpack/rules#how-rules-are-chosen).
- Each provider is independent. A condition of a mod that is not installed is **false**, so `!` in front of it is **true**. Keep that in mind when a pack is shipped in versions with and without a quest mod: in `unless`, a missing mod never blocks the rule.
- A pack folder can serve several versions of a pack. Conditions of a mod that is not installed on that version only add a line to `/whm reload`.

## Checking

- `/whm reload` lists missing quest mods, API mismatches and malformed ids, per file.
- `/whm test <damage_id> [entity] --as <player>` evaluates the rules with that player's real progress. `--assume <condition>` treats a condition as fulfilled (`--assume !<condition>` as not fulfilled), without touching the save.
- The [pack editor](/modpack/pack-editor#condition-picker) lists the quests the server knows, with their titles, and checks a typed id.

```
/whm test minecraft:generic cataclysm:ignis --as Steve --assume stage:nether_unlocked --assume !ftbquests:2C7E0B91A4D35F18
```
