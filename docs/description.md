# What Hit Me?

See exactly what killed you. **Works in any modpack, with any combination of mods — no required dependencies.**

![What Hit Me? in action: death screen recap, hover details, respawn chat summary](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/hero.gif)

What Hit Me? records the damage a player takes and explains a death in plain words: the killing blow, the attacker and its mod, how much armor helped, whether the damage ignored armor, and what to do about it next time. It runs on the client, on the server, or both, and adds no blocks or items. Minecraft 1.7.10 - 26.3 on Forge, Fabric and NeoForge: 63 builds, each one started and tested in game.

**New in 1.3.0 for modpack makers:** templates for every text the mod shows, custom death messages, progression rules that keep bosses secret until a quest is done (FTB Quests, Better Questing, Heracles, Game Stages, advancements, scores), KubeJS / CraftTweaker / MineTweaker scripts, and an in-game editor for the whole pack setup. Full documentation with examples: [wiki](https://github.com/junjun1z/what-hit-me/tree/main/wiki).

![Death screen panel](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/screenshots/death-screen.png)

## Built to stay out of the way

- **Only players are tracked.** Every damage and death handler returns on its first line for any other entity, so mob fights are not touched.
- **No server tick handlers.** Time windows, merging, reflected damage and cooldowns compare timestamps; the few checks that must wait for the end of a hit are queued once on the server's task queue.
- **Network traffic only when there is something to show:** on death, on a totem / heavy hit, and when a player runs a command. Never per hit.
- **Smart mod-list cache.** Damage types, mobs and effects are scanned once and saved together with a hash of the mod list. As long as the mods do not change, the next start loads the list from the cache.
- **Every hook is guarded.** Event handlers, packet handlers, scans and screens catch their own errors and fall back to "unknown", so a broken damage source, entity or item from another mod is not passed on as a crash.

## For players

- **Death screen panel.** A recap next to the vanilla death screen: killing blow, death message, damage properties, every hit from the last 30 seconds, the "Last 10s Total" and a tip. Hover a line for details. Drag the header to move it, `[-]` to minimize, mouse wheel to scroll, `[Copy]` to copy the recap.
- **Respawn chat summary.** After respawning, a short summary in your own chat only: killing blow, properties, total damage in the last 10 seconds, a tip and a link to the full recap. Turn it off with `/whm togglechat`.
- **Damage history.** `/whm` lists recent hits, alive or dead. Repeated damage from the same source (fire, poison, a cactus) is merged into one line with a hit counter (`x12 hits`).
- **Details on hover.** Damage before and after armor, mitigation %, absorption used, health after the hit, attacker weapon and enchantments, distance, invisible attacker, owner of turrets, pets and arrows, the mod that added it, and the internal damage id.
- **Damage as a share of max health.** `[-12.0 ❤ (30% Max HP)]` keeps big numbers readable in packs that raise max health.
- **Where it happened.** For damage without an attacker (fire, lava, cactus, modded acid or shock blocks), the hover shows the block or fluid at your feet and body and the items in your hands, each with its mod.
- **Reflected damage.** Thorns, or damage from an entity you hit a moment earlier, gets a `[⚡ Reflected / Thorns]` badge and its own tip.
- **Armor break.** When an armor piece breaks, the timeline shows `[⚠ Broken: Diamond Chestplate]` at that point.
- **Totem and revive snapshots.** When a Totem of Undying saves you, another mod cancels your death or revives you, or one hit takes 80% of your max health, the moment is saved and a chat alert with a `[Details]` link appears. `/whm snapshot` shows them later.
- **Copy for a bug report.** `[Copy Recap]` copies the recap as Markdown, including dimension, coordinates (optional), max health, the four armor pieces with enchantments and break state, and active effects.
- **Your death statistics.** `/whm stats` shows your total deaths, total damage taken and your top 5 causes of death.
- **Effects.** `/whm effects` lists active potion effects with their mod, level, time left and whether milk cures them (`[Milk ✔]` / `[Milk ✖]`).
- **Search.** `/whm search` finds damage types, mobs and effects in your pack: `#bypasses_armor`, `#magic`, `#fire`, `#explosion`, `#projectile`, `#absolute`, `#scanned`, `@modid`, or plain text.
- **Settings screen everywhere.** `/whm config` opens the settings screen on every version and loader. It is also in the mods list on Forge and NeoForge, and in Mod Menu on Fabric when Mod Menu is installed (optional).
- **Make it yours.** Rewrite the hover details, panel lines, chat summary, alerts and copy text with your own templates in the in-game template editor, with a live preview (`/whm template`).

| Respawn chat | History | Effects |
|---|---|---|
| ![Respawn chat](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/screenshots/respawn-chat.png) | ![History](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/screenshots/history.png) | ![Effects](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/screenshots/effects.png) |

| Search | Totem alert |
|---|---|
| ![Search](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/screenshots/search.png) | ![Totem](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/screenshots/totem.png) |

| In a 21-mod pack | In-game settings |
|---|---|
| ![Modpack death screen](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/screenshots/modpack-death-screen.png) | ![Config GUI](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/screenshots/config-gui.png) |

## For modpack makers

### Why include it

- Fewer "what killed me?" questions in your Discord and issue tracker. Players see the attacker, the mod and the damage type themselves.
- Guide players through your pack's dangers with your own names and tips, matched by damage type, attacker, weapon and dimension.
- Safe to add late: no dependencies, no world data, no blocks or items, nothing in chat on join. Remove it any time.
- Keep secrets until players get there: a boss shows as `???` with no tip until its quest, advancement or stage is done.
- Make every text match your pack: templates, custom death messages, and an in-game editor for all of it.

### Health-overhaul mods

- The `(30% Max HP)` share is taken from the player's max health at the moment of the hit, so it stays meaningful with mods that change max health.
- If a player dies while their total health is still above zero (body-part health such as First Aid), the recap adds `[Localized / Vital Hit (Died with 4.5 ❤ total HP left)]`.
- A death that arrives without any damage event before it (a mod calling `setHealth(0)`, for example) still gets a killing blow, taken from the death event, so the recap is never empty.

### The pack folder

On first start the mod creates `config/whathitme/pack/` with the subfolders `hints/`, `renames/`, `ignored/`, `triggers/` and `custom/`, each holding an `_example.json` whose entries are commented out. Every `.json` file in the folder and its subfolders is read in alphabetical path order and merged; later files override earlier entries. The folder names are only for your own order: any file may contain any of these keys.

```json
{
  // comments and trailing commas are allowed
  "renameMap": {
    "minecraft:outOfWorld": "&5The Abyss",
    "mob:minecraft:zombie @item:minecraft:iron_sword": "Armed Zombie"
  },
  "hintMap": {
    "minecraft:lava @dim:minecraft:the_nether": "Carry Fire Resistance potions in the Nether.",
    "mob:lycanitesmobs:*": "lang:mypack.hint.lycanites"
  },
  "defaultIgnoredDamageIds": ["minecraft:cactus"],
  "lockedFeatures": ["showInternalDamageId"],
  "deathTriggers": [
    { "match": "minecraft:lava", "command": "say {player} fell into lava at {x} {y} {z}", "cooldownSeconds": 60 },
    { "match": "mob:minecraft:creeper", "command": "give {player} minecraft:bread 4", "oncePerPlayer": true }
  ]
}
```

- `renameMap` / `hintMap`: display names and tips. `&` color codes work. A value of `lang:<key>` (or a plain translation key) is translated on each player's client, so a resource pack can provide the text in every language.
- `defaultIgnoredDamageIds`: damage ids hidden for everyone by default.
- `lockedFeatures`: config keys players cannot change from the settings screen or `/whm config`, even in single player. Locked entries are shown greyed out.
- `templates`, `deathMessages`, `rules`: see below. New samples in `templates/`, `deathmessages/` and `rules/`.
- `order`, `enabled`, `memo`, `entryMemos`: load order (`"order": 10`), on / off switch (`"enabled": false` skips the file) and notes. The in-game pack editor writes them; by hand they are optional.
- Priority: entries in the `.cfg` files > pack JSON files > built-in automatic tips. `/whm reload` re-reads everything without a restart and reports how many pack files were read, which ones had errors, unknown template variables and rule problems.

### Composite match keys

`renameMap`, `hintMap`, `ignoredDamageIds`, `defaultIgnoredDamageIds` and `deathTriggers` all use the same keys. Combine any of these conditions, in any order, separated by spaces (or `&`):

| Condition | Matches |
|---|---|
| `minecraft:lava`, `mob:minecraft:zombie` | Damage id: `mob:<entity id>` for attacks by an entity, `<modid>:<damage type>` for everything else |
| `@minecraft:zombie` or `@mob:minecraft:zombie` | The attacker |
| `@item:minecraft:iron_sword` | The item in the attacker's main hand |
| `@dim:minecraft:the_nether` | The dimension |

Every condition may use `*` and `?` wildcards. Conditions can also be written without a separator in front of an `@`, so `*@twilightforest:lich` (any damage from the Lich, whatever its id) and `minecraft:arrow@twilightforest:skeleton_druid` work too. Useful patterns:

| Key | Matches |
|---|---|
| `*@twilightforest:lich` or `@twilightforest:lich` | Every hit by the Lich, whatever damage id its attacks use |
| `@twilightforest:*` | Any attacker from Twilight Forest |
| `*acid*` | Every damage id that contains "acid" (for ids a mod builds at run time) |
| `minecraft:arrow @twilightforest:skeleton_druid` | Only that mob's arrows |

All conditions of a key must match. When several keys match, the most specific one wins: the key with more conditions (a bare `*` does not count), and on a tie the one with fewer wildcards. So a tip for `twilightforest:lich_bolt @twilightforest:lich` beats a general tip for `@twilightforest:lich`, and `*` alone works as a catch-all with the lowest priority.

### Death triggers

`deathTriggers` run a server command when a player's death matches `match`:

- Placeholders: `{player}`, `{x}`, `{y}`, `{z}` (block coordinates of the death) and `{damage}` (health points of the killing blow), plus every template variable.
- Conditions: `if` / `any` / `unless` with the same conditions as progression rules (below), checked against the dead player's progress.
- `oncePerPlayer`: only the first matching death of each player (remembered across restarts). `cooldownSeconds`: per player.
- Commands run on the server only, with console permission. Every matching trigger runs, in file order.

### Testing: `/whm test`

`/whm test <damage_id> [entity_id] [item_id] [dim_id]` builds a dummy death and shows the resulting name, the tip, the matching progression rule and what it hides, the death triggers that would run (they are not run), the death message and a preview of the chat summary. Use `-` to skip an argument. Arguments Tab-complete, including damage ids found by the scanner. Add `--as <player>` to use a player's real progress and `--assume <condition>` (repeatable) to pretend a condition is met.

### Templates

Rewrite what the mod shows, line by line, with `{variables}`. Nothing changes until you write one: the built-in output is exactly the 1.2.0 text in every language.

| Output | What it changes |
|---|---|
| `hover` | Hover details of a record (death screen, `/whm` history, chat summary) |
| `line` | One line of the death screen panel and of `/whm history` |
| `summary` | The respawn chat summary |
| `alert` | Totem / prevented death / heavy-hit chat alerts |
| `copy` | The `[Copy]` / `[Copy Recap]` text, including the equipment block |

```json
{
  "templates": {
    "line": ["&4{killing} &8{time_ago} &f{name} &c-{damage} ❤ &7({max_hp_percent}% max) &e{badges}"],
    "summary": [
      "&6&l===== {player} died =====",
      "&cKilled by &f{killing_blow} &c({damage} ❤)",
      "&7Weapon: {weapon} &d{weapon_enchants}",
      "&aTip: {tip}",
      "&b{recap_link}"
    ]
  }
}
```

- Syntax: `{variable}`, `{variable|fallback}`, `{damage:2}` for decimals, `&` color codes, a line `lang:<key>` for translated text. A line whose placeholders are all empty is left out, so "Weapon:" disappears when there is no weapon. Long lines wrap in the tooltip and the panel.
- Variables follow the display switches: a variable that is turned off on the server or by the player is empty. Templates never reveal what the server disabled.
- Where they come from: a player's personal `whathitme-client-templates.json` > the pack's `templates` > built-in. Lock outputs with `lockedFeatures` (`templates` or `templates.<output>`). Players without the mod get the pack templates in the server's English chat.

| Group | Variables |
|---|---|
| Damage | `{damage}` (hearts), `{damage_hp}`, `{raw_damage}` (before armor), `{mitigation}` (%), `{absorbed}`, `{health_after}`, `{max_health}`, `{max_hp_percent}`, `{hits}` |
| Damage type | `{name}`, `{damage_name}`, `{damage_id}`, `{damage_mod}`, `{properties}`, `{death_message}` |
| Attacker | `{attacker_name}`, `{attacker_id}`, `{attacker_mod}`, `{attacker_distance}`, `{attacker_owner}`, `{direct}`, `{weapon}`, `{weapon_id}`, `{weapon_enchants}` (`{mob_...}` = `{attacker_...}`) |
| Context | `{environment}`, `{dimension}`, `{x}` `{y}` `{z}`, `{time_ago}`, `{time}` |
| Other | `{badges}`, `{killing}`, `{tip}`, `{player}`, `{script:<name>}` |
| Summary / alert / copy | `{killing_blow}`, `{total}`, `{total_hits}`, `{total_seconds}`, `{recap_link}`, `{toggle_notice}`, `{alert_kind}`, `{details_link}`, `{timeline}`, `{equipment}`, `{effects}`, `{recap}` |

**Template editor:** `/whm template` or **Templates...** in the settings screen, on every version and loader. Each line is a text field; click a variable to insert it; unknown variables turn red. The live preview is drawn by the same code as the real output (tooltip, panel line, chat lines or copy text), with your latest death or a sample death. **Save** keeps it for you; **Save to pack** (single player with cheats) writes `pack/templates/<output>.json` for your pack. Outputs locked by the pack are shown read-only.

### Custom death messages

`deathMessages` replaces the vanilla death message ("Steve was slain by Zombie") for the deaths you choose. Off by default: with no entries nothing changes. Keys are composite match keys, values are one-line templates with every variable, `&` colors and `lang:` keys.

```json
{
  "deathMessages": {
    "mekanism:acid": "{player} dissolved in acid",
    "*@twilightforest:lich": "{player} was outsmarted by the Lich",
    "mob:minecraft:zombie @item:*sword*": "{player} was cut down by {attacker_name} wielding {weapon|a blade}"
  }
}
```

| Server | Players with the mod | Players without the mod | Server log |
|---|---|---|---|
| Fabric (every version) | Custom message | Custom message | Custom message |
| Forge / NeoForge (every version) | Custom message (chat line and death screen are replaced) | Vanilla message | `[What Hit Me?] Death message: ...` |

Forge and NeoForge have no event to change the death message, so players without the mod keep the vanilla text there. If that matters, turn on `requireClientMod`.

### Progression rules and quest mods

`rules` change what a death shows depending on the player's progress. The server checks them every time it builds an output and removes hidden data before anything is sent, so finishing a quest reveals old entries too and nothing leaks to the client.

```json
{
  "rules": [
    {
      "match": "*@twilightforest:lich",
      "unless": ["ftbquests:1A2B3C4D5E6F7081"],
      "name": "&k???",
      "hint": "",
      "hide": ["damage_id", "attacker", "mod", "weapon", "death_message"]
    }
  ]
}
```

- Fields: `match`, `if` (all must be true), `any` (one of), `unless` (none), `name`, `hint` (`""` = no tip), `hide`, `enforce` (personal renames, tips and templates do not apply; on when `hide` is set), `commands`, `oncePerPlayer`, `cooldownSeconds`. The first matching rule wins.
- `hide`: `damage_id`, `attacker`, `mod`, `weapon`, `death_message`, `tip`, `properties`, `environment`, `owner`, `distance`, `position`. A hidden death message becomes "Steve was killed by ???" (or "Steve died"), translated for players with the mod.

| Condition (`!` negates) | True when |
|---|---|
| `advancement:<id>` | The player has the advancement (Minecraft 1.12+) |
| `tag:<tag>` | The player has the scoreboard tag (1.9+) |
| `score:<objective><op><n>` | The score compares true (`>=`, `>`, `<=`, `<`, `=`, `!=`) |
| `stage:<name>` | Game Stages: Forge 1.11.2 - 1.20.3, NeoForge 1.20.1 |
| `ftbquests:<id>` | FTB Quests: the quest or chapter is done by the player's team (1.12.2 - 26.1.2 as published) |
| `bq:<id>` | Better Questing: Forge 1.7.10 - 1.12.2 (also the GTNH fork and Better Questing Unofficial), Forge / NeoForge 1.20.1 |
| `heracles:<id>` | Heracles / Odyssey Quests: 1.20 / 1.20.1, 1.21 / 1.21.1 |
| `script:<name>` | A condition registered by a script |

Quest mods are never required; their conditions load only when the mod is installed. Each quest mod was tested in game on every Minecraft version and loader it exists for. `/whm reload` reports unknown conditions and missing mods; `/whm test ... --as <player> --assume <condition>` shows the result for a player.

### requireClientMod

`requireClientMod=true` in `whathitme-common.cfg` refuses players without What Hit Me? with a readable message (only the presence is checked, not the version). Use it on Forge / NeoForge when rules hide the killer from the death message: the server warns on start and on `/whm reload` while it is off.

### Script integration: KubeJS, CraftTweaker, MineTweaker

Scripts can register `script:` conditions and `{script:}` template variables, listen to `damageRecorded`, `death`, `respawn` and `alert`, read the last death, history, stats and `/whm test` results, and add renames, tips, rules, death messages and templates at run time. Supported on every version and loader where the script mod exists: KubeJS (1.12.2 - 26.1.2), CraftTweaker (1.12.2 - 1.21.1), MineTweaker 3 / CraftTweaker 3.x (1.7.10 - 1.11.2, also the GTNH fork).

```js
// kubejs/server_scripts/whathitme.js
WhatHitMeEvents.register(event => {
  event.condition('veteran', 'Has killed the Wither', player => player.stages.has('wither_down'))
  event.addRule({ match: '*@minecraft:wither', unless: ['script:veteran'], name: '???', hide: ['attacker', 'mod'] })
})
```

Everything is rebuilt on `/whm reload` and the script mod's reload. A failing script function is logged once and never breaks a recap. Other mods can use the same Java API (`io.github.junjun1z.whathitme.api.WhatHitMeApi`).

### In-game pack editor

`/whm pack` (or **Pack editor...** in the settings screen) edits everything above without touching a file: renames, tips, rules, death messages, templates, death triggers, ignored ids, locked features and the server settings. For single player with cheats and operators, on every version and loader.

- The server checks every edit, writes the JSON file and reloads at once; every player gets the change. Saved when you confirm an entry.
- Each JSON file of the pack folder is a category (sub-folders are groups) with load order, on / off switch and a memo; entries have memos too. Comments written by hand in a file the editor rewrites are lost, the memo fields replace them.
- Match key builder with searchable damage id and mob lists, condition picker with the advancements, scores, tags, quests, stages and script conditions the server knows, a preview "as player / assuming conditions", inline errors, and a notice when another operator saves.
- Backups in `config/whathitme/.backup/` and 30 undo steps.
- Players without permission get the personal mode (`/whm pack personal`): their own renames, tips and ignored ids as searchable lists.

### Every damage id, before anyone takes it

Mod damage ids are listed before anyone has taken that damage:

- Minecraft 1.7.10 - 1.19.3: the mod reads the class files of the installed mod jars (without loading any classes) and collects the ids passed to damage source constructors. This runs on a background thread on first start, after the mod list changes and on `/whm reload`, and the result is saved in the cache.
- Minecraft 1.19.4 and newer: the game's damage type registry already lists every damage type, so it is read directly.

The ids appear in `/whm search` (`#scanned`), in `/whm test` completion, and in `/whm dump`, which writes `config/whathitme/.cache/whathitme_dump.csv` (every damage type, mob and effect with its mod, properties and source: seen, scanned or registry) and `config/whathitme/hint_template.cfg` with ready-to-paste `S:hintMap <...>` and `S:renameMap <...>` blocks. Lines with an empty value are ignored, so you can paste the whole block and fill it in gradually.

### Install modes

| Installed on | Works | What players get |
|---|---|---|
| Client and server | Yes | Full, accurate data from the server: damage before armor, mitigation %, absorption, weapon, owner, shield and i-frame flags, armor and environment details. Text in the player's language. Clipboard buttons. The server's settings and pack folder apply to everyone. |
| Client only | Yes | Client fallback on servers without the mod: the client compares its own health and, only when it drops, guesses the cause (fall, lava, fire, drowning, void, nearby mob, projectile, explosion, poison, wither, starvation). Entries are marked `[Client Est.]`. `/whm inspect` is not available. |
| Server only | Yes | Players without the mod (even plain Forge clients) get readable English chat: respawn summary, totem / heavy-hit alerts and all `/whm` commands. `[Copy ID]` puts the id into the chat box instead of the clipboard. |

Single player counts as "client and server". Mixed servers work too: each player gets the best mode their client supports. Features turned off on the server (in its common config or through `lockedFeatures`) are removed before the data is sent to clients.

### Files

```
config/whathitme/
  whathitme-common.cfg    pack and server settings (sent to modded clients)
  whathitme-client.cfg    personal settings and panel layout
  whathitme-client-templates.json   personal templates
  hint_template.cfg       written by /whm dump
  pack/                   pack definitions (*.json, any subfolder)
  .cache/                 catalog_cache.json, stats_<uuid>.json, whathitme_dump.csv
  .backup/                previous versions of files changed by the pack editor
```

An element is shown only when it is `true` in **both** `.cfg` files, so a pack or server can turn something off for everyone, and each player can turn off more for themselves. Players can add their own renames and tips on top of yours. Tips have three switches: `enableAutoHints` and `enableCustomHints` (common file) and `showHints` (client file); an old `showHintsAndTips=false` is carried over to all three.

New switches in 1.2 (all default `true`): `showMaxHpPercentage`, `showEnvironmentContext`, `showReflectedBadge`, `showArmorBreakEvents`, `includeEquipmentInCopy`, `includeCoordsInCopy`.

Death statistics are kept in memory (at most 100 causes per player) and written to `.cache/stats_<uuid>.json` only on death, logout and world save.

### Can I include it in my modpack?

Yes. It is MIT licensed. No need to ask.

## Commands

All commands are `/whm` (alias `/whathitme`) and Tab-complete. "Op" means permission level 2 on a server, or single player with cheats enabled.

| Command | What it does | Who |
|---|---|---|
| `/whm` or `/whm history` | Recent damage taken | Everyone |
| `/whm last` | Full recap of your last death | Everyone |
| `/whm snapshot [n]` | Saved totem / heavy-hit snapshots (1 = newest) | Everyone |
| `/whm effects` | Active effects, their mod, and whether milk cures them | Everyone |
| `/whm stats` | Your deaths per cause, total deaths and damage taken | Everyone |
| `/whm search <text\|#tag\|@modid>` | Search damage types, mobs and effects | Everyone |
| `/whm togglechat` | Respawn chat summary on / off | Everyone |
| `/whm ignore <id>` | Hide / show a damage id | Everyone (personal); op for the server list |
| `/whm config` | Open the settings screen (players without the mod get the list of settings) | Everyone |
| `/whm config <key> [value]` | Show or change a setting | Personal keys: everyone; pack / server keys: op |
| `/whm rename <id> <name...>` | Custom display name (empty removes it) | Op |
| `/whm sethint <id> <tip...>` | Custom tip (empty removes it) | Op |
| `/whm inspect <player>` | Last death and history of an online player | Op |
| `/whm test <damage_id> [entity] [item] [dim] [--as <player>] [--assume <cond>]` | Preview the name, tip, rule, death triggers, death message and chat for a dummy death | Op |
| `/whm dump` | Write `whathitme_dump.csv` and `hint_template.cfg` | Op |
| `/whm reload` | Reload the config files and the pack folder, rescan the catalog | Op |
| `/whm template` | Open the template editor | Everyone (mod on the client) |
| `/whm template preview <output>` / `reset [output]` | Show a template in chat / delete your personal templates | Everyone |
| `/whm pack` | Open the pack editor | Op (mod on the server) |
| `/whm pack personal` | Edit your own renames, tips and ignored ids as lists | Everyone (mod on the client) |
| `/whm server <subcommand>` | From a modded client: run the subcommand on the server (server-wide settings) | Needs the mod on the server |
| `/whm help` | Command list | Everyone |

Keys listed in `lockedFeatures` cannot be changed by anyone through the settings screen or `/whm config`.

## Supported versions

| Minecraft | Forge | Fabric | NeoForge |
|---|---|---|---|
| 1.7.10 | ✔ | | |
| 1.8.9 | ✔ | | |
| 1.9 / 1.9.4 | ✔ | | |
| 1.10 / 1.10.2 | ✔ | | |
| 1.11 / 1.11.2 | ✔ | | |
| 1.12 / 1.12.1 / 1.12.2 | ✔ | | |
| 1.14 - 1.14.4 | 1.14.4 | ✔ | |
| 1.15 - 1.15.2 | 1.15.1 / 1.15.2 | ✔ | |
| 1.16 - 1.16.5 | 1.16.3 - 1.16.5 | ✔ | |
| 1.17 / 1.17.1 | 1.17.1 | ✔ | |
| 1.18.2 | ✔ | ✔ | |
| 1.19 - 1.19.4 | ✔ | ✔ | |
| 1.20 / 1.20.1 | ✔ | ✔ | 1.20.1 |
| 1.20.2 - 1.20.4 | ✔ | ✔ | 1.20.4 |
| 1.20.5 / 1.20.6 | 1.20.6 | ✔ | 1.20.6 |
| 1.21 / 1.21.1 | 1.21.1 | ✔ | ✔ |
| 1.21.2 - 1.21.5 | 1.21.3 - 1.21.5 | ✔ | 1.21.3 - 1.21.5 |
| 1.21.6 - 1.21.8 | 1.21.8 | ✔ | 1.21.7 / 1.21.8 |
| 1.21.9 - 1.21.11 | ✔ | ✔ | ✔ |
| 26.1 - 26.1.2 | 26.1.2 | ✔ | ✔ |
| 26.2 / 26.3 | ✔ | ✔ | ✔ |

Fabric needs [Fabric API](https://modrinth.com/mod/fabric-api) (on 1.21 / 1.21.1: Fabric API 0.116 or newer). [Mod Menu](https://modrinth.com/mod/modmenu) is optional. Forge and NeoForge need nothing else. Every listed version was started and tested in game.

## Supported languages

The mod follows your Minecraft language setting. 10 languages are included:

| Language | In game |
|---|---|
| English (default) | English |
| Japanese | 日本語 |
| Simplified Chinese | 简体中文 |
| Traditional Chinese | 繁體中文 |
| Korean | 한국어 |
| Russian | Русский |
| German | Deutsch |
| French | Français |
| Spanish | Español |
| Brazilian Portuguese | Português (Brasil) |

Players without the mod see English text sent by the server.

## FAQ

**Does it need anything else?**
On Forge and NeoForge, no. On Fabric, only Fabric API; Mod Menu is optional. See *Supported versions* above.

**Do players have to install it?**
No. On a server with the mod, players without it still get the chat summary and commands in English. Installing it on the client adds the death screen panel, translations, the settings screen and clipboard buttons. A server can require it with `requireClientMod=true`.

**Does it need a quest or script mod?**
No. FTB Quests, Better Questing, Heracles, Game Stages, KubeJS, CraftTweaker and MineTweaker are optional; their support loads only when they are installed. Without them, rules can still use advancements, scoreboard tags and scores.

**Can I use it on a server that does not have it?**
Yes. The client estimates the damage from its own health and nearby entities. Those entries are marked `[Client Est.]` and are less detailed.

**Will it conflict with other mods?**
On Forge and NeoForge it uses only standard events: no Mixins, no coremods. On Fabric it uses Fabric API events and a few tiny Mixins only where no event exists. No screen is replaced: the panel is drawn on top of the vanilla death screen.

**I could use `/whm rename` or `/whm reload` in 1.1, but not anymore.**
Since 1.2, `rename`, `sethint`, `test`, `dump` and `reload` need op, or single player with cheats enabled. Personal settings stay open to everyone through `/whm config` and the settings screen, unless the pack locks them.

**The chat summary is too much. How do I turn it off?**
`/whm togglechat`, or set `enableRespawnChatSummary=false`. Pack makers can turn off any element for everyone in `whathitme-common.cfg`, and lock it with `lockedFeatures`.

**Why does it say "Unknown" or `unknown:` for some damage?**
Some mods create damage without a type the game can trace back to them. Use `renameMap` to give it a name.

**Why are there no emoji?**
Older Minecraft fonts cannot render them, so every version uses the same symbols the game font has (☠ ⚠ ❤ ⚡) and text labels.
