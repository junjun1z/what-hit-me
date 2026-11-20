# FTB Quests <Badge type="tip" text="1.3.0+" />

`ftbquests:<id>` is true when the quest or chapter is completed by the player's team. On FTB Quests 1.15.2 (1502) and 1.16.4 (1604) progress belongs to the player instead of a team. See [Quest mods](/modpack/quest-mods) for how quest conditions work in general.

## Condition syntax

```json
"unless": ["ftbquests:4F2A9C1D7B3E6A05"]
```

| Form | Meaning |
|---|---|
| `ftbquests:<hex id>` | The quest or chapter with that id is completed. Upper or lower case. |
| `ftbquests:#<hex id>` | The same; the `#` FTB Quests shows in front of ids is optional. |
| `!ftbquests:<hex id>` | Not completed |

A chapter counts as completed when FTB Quests reports it completed (all its quests done).

## Supported versions

| What Hit Me? jar (Minecraft) | FTB Quests |
|---|---|
| Forge 1.12.2 (1.12, 1.12.1) | ✔ 1202.9.0.15 (1.12.2) |
| Forge 1.15.2 | ✔ 1502.0.2.252 |
| Fabric 1.16.5 (1.16.2 - 1.16.4) | ✔ 1605.3.7 (1.16.5) |
| Forge 1.16.5 (1.16.3, 1.16.4) | ✔ 1605.3.7, ✔ 1604.1.1.21 (1.16.4) |
| Fabric 1.18.2 | ✔ 1802.3.16 |
| Forge 1.18.2 | ✔ 1802.3.16 |
| Fabric 1.19.2 (1.19, 1.19.1) | ✔ 1902.5.10 (1.19.2) |
| Forge 1.19.2 (1.19, 1.19.1) | ✔ 1902.5.10 (1.19.2) |
| Fabric 1.20.1 (1.20) | ✔ 2001.4.22 (1.20.1) |
| Forge 1.20.1 (1.20) | ✔ 2001.4.22 (1.20.1) |
| NeoForge 1.20.1 (Forge jar) | ✘ |
| Fabric 1.20.2 | ✔ 2002.2.0 snapshot |
| Fabric 1.20.4 (1.20.3) | ✔ 2004.2.3 (1.20.4) |
| Forge 1.20.4 (1.20.3) | ✔ 2004.2.3 (1.20.4) |
| NeoForge 1.20.4 | ✔ 2004.2.3 |
| Fabric 1.20.6 (1.20.5) | ✘ |
| NeoForge 1.20.6 | ✔ 2006.1.0 snapshot |
| Fabric 1.21.1 (1.21) | ✔ 2101.1.39, ✔ 2100.1.4 (1.21) |
| NeoForge 1.21.1 (1.21) | ✔ 2101.1.39, ✔ 2100.1.4 (1.21) |
| Fabric, NeoForge 1.21.11 | ✔ 2111.1.5 |
| Fabric 26.1.2 (26.1, 26.1.1) | ✔ 26.1.2.10 (26.1.2 and 26.1) |
| NeoForge 26.1.2 (26.1, 26.1.1) | ✔ 26.1.2.10 (26.1.2) |

✔ = tested in game (rule before and after the quest, pack editor quest list). ✘ = a build exists but does not run there. All other versions have no FTB Quests build. The full table of all quest mods is the [quest mod matrix](/modpack/quest-mods#quest-mod-matrix).

## Finding the ids

- **In the quest book:** turn on edit mode, right-click a quest or chapter and copy its id. It is a 16 digit hex number like `4F2A9C1D7B3E6A05`.
- **In the files:** every quest has an `id:` in `config/ftbquests/quests/chapters/*.snbt` (1.16 and newer).
- **In the pack editor:** the [condition picker](/modpack/pack-editor#condition-picker) lists the chapters and quests of the server with their titles. Click one to insert the id.

![The pack editor's condition picker with the FTB Quests chapters and quests of the server](/images/pack-editor-condition-ftbquests.webp)

## Example rules

The boss is `???` with no tip until the team has finished the boss quest:

```json
{
  "rules": [
    {
      "match": "*@twilightforest:naga",
      "unless": ["ftbquests:4F2A9C1D7B3E6A05"],
      "name": "???",
      "hint": "",
      "hide": ["attacker", "mod", "weapon"]
    }
  ]
}
```

A tip that only shows after the "Nether" chapter, and a one-time reward for dying to the boss before the quest:

```json
{
  "rules": [
    { "match": "minecraft:lava", "if": ["ftbquests:1B6D0E42C9A7F830"], "hint": "Your Netherite gear floats in lava. Look for it at the surface." },
    { "match": "*@twilightforest:naga", "unless": ["ftbquests:4F2A9C1D7B3E6A05"], "commands": "give {player} minecraft:golden_apple 2", "oncePerPlayer": true }
  ]
}
```

Before and after the quest, with the same death:

![Before the boss quest: the attacker is hidden as ???, no tip](/images/rules-before-ftb.webp)
![After the boss quest: the full record with the attacker, its mod and the tip](/images/rules-after-ftb.webp)

## Version notes

- **Team or player.** From 1.16.5 (1605) progress belongs to the FTB Teams team, so a quest one team member completes counts for the whole team. On 1.12.2 FTB Library makes a team per player. 1.15.2 (1502) and 1.16.4 (1604) keep progress per player.
- **Exact Minecraft version.** FTB Quests needs the Minecraft version it was built for: 1.19.2 (not 1.19 / 1.19.1), 1.20.1 (not 1.20), 1.20.4 (not 1.20.3 on Forge), 26.1.2 on NeoForge (not 26.1 / 26.1.1).
- **Fabric 1.16.2 - 1.16.4:** FTB Teams for Fabric needs 1.16.5.
- **NeoForge 1.20.1:** FTB Library 2001 needs Forge 47.3, which NeoForge 1.20.1 does not report, so FTB Quests does not start.
- **No builds** for 1.14, 1.17, 1.19.3, 1.19.4, Forge after 1.20.4, 1.21.2 - 1.21.10, 26.2 and 26.3.
- **Snapshots:** 1.20.2 and 1.20.6 only have unreleased snapshot builds on FTB's Maven. The Fabric 1.20.6 snapshot crashes at start by itself.
- **Fabric 26.1:** FTB Library needs Fabric API 0.149.1 or newer (0.155.3 works).

## Testing with /whm test

1. `/whm reload`: no warning about `ftbquests:` means the ids are well formed and FTB Quests was found.
2. Preview both states without touching the save:

```
/whm test minecraft:generic twilightforest:naga --as Steve --assume !ftbquests:4F2A9C1D7B3E6A05
/whm test minecraft:generic twilightforest:naga --as Steve --assume ftbquests:4F2A9C1D7B3E6A05
```

3. To try it for real, complete and reset the quest with FTB Quests' own command (1.16 and newer):

```
/ftbquests change_progress Steve complete 4F2A9C1D7B3E6A05
/ftbquests change_progress Steve reset 4F2A9C1D7B3E6A05
```

On 1.12.2 the command takes the team id: `/ftbquests change_progress complete <team> <id>`.
