# Changelog

The release workflow publishes the section whose heading matches the pushed tag (for example `## [1.0.0]` for tag `1.0.0`) as the release notes.

## [1.3.0]

A release for modpack makers: rewrite every text the mod shows, replace death messages, keep bosses secret until the player has progressed, drive everything from quests and scripts, and edit the whole pack setup in game. Same versions as 1.2.0: **Forge 1.7.10 - 26.3, Fabric 1.14 - 26.3, NeoForge 1.20.1 - 26.3** (63 files, each started and tested in game, also on dedicated servers with and without the mod on the client). Nothing changes until you use the new features: without templates, rules or death messages every output looks exactly like 1.2.0, and existing config and pack files keep working.

Documentation with examples for everything below: the [wiki](https://github.com/junjun1z/what-hit-me/tree/main/wiki) (English and Japanese).

### Added

- **Templates** for the six places the mod writes text: death screen hover details, death screen panel line, `/whm` history line and its hover (they share the first two), respawn chat summary, totem / heavy-hit alerts and the `[Copy]` recap (with the equipment block). `{variable}` placeholders (40+ variables for damage, damage type, attacker, weapon, position, time, badges, tip, totals, links, equipment; `mob_*` aliases), `{var|fallback}`, `{damage:2}` decimals, `&` colors and `lang:` lines. A line whose placeholders are all empty is dropped. Variables follow the display switches, so a template never shows what the server turned off. Sources: personal `whathitme-client-templates.json` > pack `"templates"` > built-in; lock them with `lockedFeatures` (`templates` or `templates.<output>`).
- **Template editor** with a live preview drawn by the real output code: `/whm template`, or **Templates...** in the settings screen. Clickable variable list with descriptions, unknown variables in red, preview with your latest death or a sample death. **Save** (personal), **Save to pack** (single player with cheats), **Reset**. `/whm template preview <output>` and `/whm template reset [output]`.
- **Custom death messages** (`deathMessages` in pack files, off by default): match key -> message template, for example `"*@twilightforest:lich": "{player} was outsmarted by the Lich"`. Fabric servers replace the message for everyone; Forge / NeoForge servers for players with the mod (the client swaps the chat line and the death screen text), the server log shows the custom text. `lang:` keys are translated per player.
- **Progression rules** (`rules` in pack files): change the name and tip, hide fields and run commands depending on the player's progress, for example `???` and no tip until the boss quest is done. Fields `match`, `if` (all) / `any` (one of) / `unless` (none), `name`, `hint`, `hide` (`damage_id`, `attacker`, `mod`, `weapon`, `death_message`, `tip`, `properties`, `environment`, `owner`, `distance`, `position`), `enforce` (overrides personal renames, tips and templates; on by default when something is hidden), `commands` with `oncePerPlayer` / `cooldownSeconds`. The first matching rule wins. The server decides and removes hidden data before anything is sent; completing a quest reveals old entries too. A hidden death message becomes "{player} was killed by ???" / "{player} died".
- **Conditions** for rules and death triggers (`!` negates): `advancement:` (1.12+), `tag:` (1.9+), `score:<objective><op><n>`, `stage:` (Game Stages), `ftbquests:` (FTB Quests), `bq:` (Better Questing, also the GTNH fork and Better Questing Unofficial), `heracles:` (Heracles / Odyssey Quests), `script:` (scripts). Quest mods are optional and read by reflection; each one is tested on every version and loader it exists for (matrix in the wiki).
- `deathTriggers` accept `if` / `any` / `unless` and every template variable as a placeholder.
- `/whm test ... --as <player> --assume <condition>`: preview rules, hidden fields and the death message with a player's real progress, or pretend a condition is met.
- **`requireClientMod`** (common config, default `false`): the server refuses clients without What Hit Me? with a readable message. Forge / NeoForge servers whose rules hide death messages warn on start and on `/whm reload` while it is off, because players without the mod still see the vanilla message there.
- **Script integration** for KubeJS, CraftTweaker and MineTweaker 3, on every version and loader where these script mods exist: register `script:` conditions and `{script:}` template variables, listen to `damageRecorded` (rename, tip, badge, hide, cancel), `death` (tip, hide, extra summary lines), `respawn` and `alert`, read the last death, history, stats and `/whm test` results, and add renames, tips, rules, death messages and templates at run time. Rebuilt cleanly on `/whm reload` and the script mod's reload. Other mods can use the same Java API (`io.github.junjun1z.whathitme.api`).
- **In-game pack editor** `/whm pack` (and **Pack editor...** in the settings screen) for everything in the pack folder and `whathitme-common.cfg`: renames, tips, rules, death messages, templates, death triggers, ignored ids, locked features and the server settings. For single player with cheats and operators. The server checks every edit, writes the JSON, reloads at once and updates every player. Each JSON file is a category (sub-folders are groups) with order, on / off switch and memo; entries have memos too. Match key builder with searchable damage id and mob lists, condition picker with the advancements, scores, tags, quests, stages and script conditions the server knows, live preview "as player / assuming conditions", inline errors, a notice when someone else edits. Backups in `config/whathitme/.backup/` and 30 undo steps.
- **Personal mode** of the pack editor (`/whm pack personal`, or the settings button without op): your own renames, tips and ignored ids as searchable lists instead of one-line comma lists.
- Pack files may carry `order` (load order), `enabled` (`false` skips the file), `memo` and `entryMemos`; written by the pack editor, optional by hand.
- New commented samples `pack/templates/`, `pack/deathmessages/`, `pack/rules/` (only added when those folders do not exist).
- `/whm reload` also reports unknown template variables, rule and condition problems, missing quest / script mods and what scripts registered.

### Changed

- Network protocol 3. 1.0 - 1.2 clients and servers keep working with 1.3 (they get the 1.2 data, with hidden fields already removed).

### Fixed

- Forge 1.7.10 - 1.12.1: a heal in the same tick as a hit (food regeneration, Regeneration, any heal) was subtracted from that hit, so hits were recorded too low and a 1 ❤ hit such as a burning tick could be lost (merged lines showed x2 instead of x3).
- Custom death messages with `lang:` keys on Fabric 1.19.4+: sent as a translation with the server's English text as fallback, so resource packs translate them for every client and a missing translation never shows the raw key (found in pre-release testing).
- Pack editor dialogs no longer let the text below them show through on 1.20 - 1.21.5 (found in pre-release testing).
- Forge 1.19 / 1.19.1 / 1.20.6 / 1.21.10+: custom death messages also replace the chat line where Forge posts no chat event for system messages, or the message arrives after the chat line (found in pre-release testing).
- Pack folder: one file with extremely deep nesting no longer stops the whole pack folder from loading (that file is skipped with an error). Sections of the wrong type (for example `"hintMap": 5`) and files more than 8 sub-folders deep are reported instead of being ignored silently (found in pre-release testing).
- Config files saved in another encoding than UTF-8 (for example by Notepad as ANSI with a non-English rename) were reset to the defaults and overwritten (1.13+). They are now read with the system encoding, and a file that cannot be read is never overwritten.
- Decimal settings no longer accept `NaN`.
- `/whm reload` on a client with the mod now shows how many pack files were read and which files have errors, as documented (it showed only the warnings).
- `/whm pack` against a server with an older What Hit Me? says that the server needs 1.3 instead of loading forever; the settings button opens your own lists there (found in pre-release testing).
- Players with What Hit Me? 1.2 on a 1.3 server saw raw translation keys in `/whm test` and `/whm reload` output; they get English text now (found in pre-release testing).
- `/kill` and other infinite hits showed `(2147483647% Max HP)`; now `(∞% Max HP)`.
- Forge / NeoForge 1.13+ with First Aid: a big hit that First Aid spread over the body parts was announced as "Lethal damage was prevented"; it is a heavy hit now. A cancelled death and a used totem are still reported.
- Single player: pack files and `whathitme-common.cfg` are read again when a world is opened, so edits made while the game runs apply to the next world (found in pre-release testing).
- An open pack editor shows hand edits at once after `/whm reload` (found in pre-release testing).
- Pack editor: undoing a rename that only changed the letter case (`Boss.json` -> `boss.json`) deleted the category on Windows (found in pre-release testing).
- Death screen panel: the "Last Xs total" line wraps instead of running past the panel in long translations (German).

## [1.2.0]

New features for players and modpack makers on every supported version: **Forge 1.7.10 - 26.3, Fabric 1.14 - 26.3, NeoForge 1.20.1 - 26.3** (63 files, each started and tested in game). Existing config files keep working.

**Permission change:** `/whm rename`, `/whm sethint`, `/whm dump` and `/whm reload` now need op (permission level 2), or single player with cheats enabled, also when run on the client. `/whm inspect` in single player now also needs cheats. Personal settings stay open to everyone through `/whm config` and the settings screen.

### Added

- Pack folder `config/whathitme/pack/`: every `.json` in it (any subfolder, alphabetical path order) is merged. Keys: `renameMap`, `hintMap`, `defaultIgnoredDamageIds`, `lockedFeatures`, `deathTriggers`. Created on first start with commented-out examples in `hints/`, `renames/`, `ignored/`, `triggers/` and `custom/`. Priority: `.cfg` files > pack JSON > automatic tips.
- `lockedFeatures`: config keys that cannot be changed from the settings screen or `/whm config`, even in single player; shown greyed out in the settings screen.
- Composite match keys for `renameMap`, `hintMap`, `ignoredDamageIds` and `deathTriggers`: damage id, `@entity_id` / `@mob:entity_id`, `@item:modid:item_id` (attacker main hand) and `@dim:dimension_id` in any order (separated by spaces or `&`, or written directly before an `@` such as `*@twilightforest:lich`), with `*` / `?` wildcards. The most specific key wins: more conditions (a bare `*` does not count), then fewer wildcards.
- `deathTriggers`: server commands run with console permission when a death matches. Placeholders `{player}`, `{x}`, `{y}`, `{z}`, `{damage}`; options `oncePerPlayer` and `cooldownSeconds`.
- `/whm test <damage_id> [entity_id] [item_id] [dim_id]`: shows the name, tip, matching death triggers (not run) and the chat preview of a dummy death.
- `/whm stats`: total deaths, total damage taken and the top 5 causes. Saved to `.cache/stats_<uuid>.json` on death, logout and world save.
- Damage id scanner: mod damage ids are listed before anyone takes that damage. On 1.7.10 - 1.19.3 the mod files are read on a background thread (first start, mod list change, `/whm reload`) and the result is cached; on 1.19.4+ the game's damage type registry is read. Shown in `/whm search` (`#scanned`), `/whm test` completion and `/whm dump` (new `source` column).
- Smart catalog cache (`.cache/catalog_cache.json`): damage types, mobs and effects are scanned once per mod list (SHA-256 of the sorted `modid:version` list) and loaded from the cache while the mods stay the same.
- Settings screen on every version and loader: Mods list on Forge and NeoForge, Mod Menu on Fabric when installed (optional, never required). `/whm config` without arguments opens it.
- Max HP share: `[-12.0 ❤ (30% Max HP)]` (`showMaxHpPercentage`).
- Health-overhaul mods: a death with total health still above zero (First Aid style) is marked `[Localized / Vital Hit (Died with X.X ❤ total HP left)]`.
- Environment context for damage without an attacker: block / fluid at the feet and body, items in both hands, each with its mod (`showEnvironmentContext`).
- `[⚡ Reflected / Thorns]` badge and tip for thorns and for damage from an entity the player hit within the last 50 ms (`showReflectedBadge`).
- Armor break lines `[⚠ Broken: <armor>]` in the timeline (`showArmorBreakEvents`).
- `[Copy Recap]` adds dimension, coordinates, max health, armor with enchantments and break state, and effects as Markdown (`includeEquipmentInCopy`, `includeCoordsInCopy`).
- `renameMap` / `hintMap` values that are plain translation keys are translated like `lang:` values.
- Tip switches `enableAutoHints`, `enableCustomHints` (common) and `showHints` (client).

### Changed

- No server tick handlers: time windows, merging, reflection and cooldowns use timestamps; checks after a hit are queued once on the server task queue.
- Every event hook, packet handler, scan and screen is guarded with try/catch and null checks; broken data from other mods falls back to "unknown".
- `showHintsAndTips` is replaced by the three tip switches. An old `showHintsAndTips=false` turns all three off.
- `/whm dump` writes `whathitme_dump.csv` to `config/whathitme/.cache/` (was the game or server folder).
- `/whm reload` also re-reads the pack folder, rescans the catalog and reports pack files with errors.
- `/whm config <key>` on a server can be read by everyone; changing a common key needs op.
- Features turned off on the server (common config or `lockedFeatures`) are removed from the packets the server builds. Network protocol 2; 1.0 / 1.1 clients and servers keep working with 1.2.
- The client fallback compares the local player's health and records `[Client Est.]` entries only when it drops.

### Fixed

- A death without any damage event before it (for example a mod calling `setHealth(0)`) no longer leaves the recap empty: the killing blow is taken from the death event.

## [1.1.0]

Now available for every major Minecraft version and loader: **Forge 1.7.10 - 26.3, Fabric 1.14 - 26.3, NeoForge 1.20.1 - 26.3** (63 files). Same features, commands, config files and 10 languages everywhere. Every listed version was started and tested in game (single player; dedicated server with the mod, without the mod, and with a client without the mod for each loader family).

- Fabric needs Fabric API (on 1.21 / 1.21.1: 0.116 or newer). Forge and NeoForge need nothing else.
- Not included: Minecraft 1.13.x, 1.18 / 1.18.1, Forge 1.20.5 / 1.21.6 / 1.21.7 / 26.1 / 26.1.1, NeoForge 1.20.5 / 1.21.2 / 1.21.6.
- The in-game settings screen is available on Forge 1.7.10 - 1.12.2; on other versions edit the files in `config/whathitme/` and run `/whm reload`.

Fixes:

- 1.12 / 1.12.1: the 1.12.2 file now also starts on these versions.

## [1.0.1]

Bug fixes found by a full in-game test pass (single player, dedicated server with and without the mod, a client without the mod, two players, a 21-mod pack).

- Death screen panel: dragging the header could stop at once on some setups; the drag now follows the mouse events and the position is saved when the button is released.
- Death screen panel: at large GUI scales (3 / Auto) or in small windows it no longer covers the Respawn / Title screen buttons; it shrinks to fit (never below one screen pixel per font pixel).
- Death recap, respawn summary and "Last Xs Total" only count damage taken since the previous death. Before, hits and killing blows from an earlier life within the last 30 seconds were included after quick repeated deaths.
- The first hit after respawning was wrongly marked "Ignores I-Frames" when the previous death was less than half a second earlier.
- `/whm inspect` (client and server side) no longer shows `[Copy ID]` / `[Mute]` buttons in the inspected player's history.
- Client fallback: a death that follows another one within 1.5 seconds no longer reuses the previous killing blow.
- Attackers whose mod builds the name itself (for example Lycanites Mobs "Ventoraptor Lvl 2", subspecies names) now show that name instead of the plain registry name.

Added:

- In-game settings screen: Mods list > What Hit Me? > Config, with both files (Personal / Modpack - server). Changes are saved and applied on Done; in single player the server side is updated too.

## [1.0.0]

First release.

- Damage tracking for players: raw damage before armor, final damage, mitigation %, absorption used, health after the hit.
- Normalized damage ids (`mob:<entity>` for attacks, `<modid>:<damage type>` for the rest) with properties: Bypasses Armor, Bypasses Magic, Magic, Fire, Explosion, Projectile, Through Shield, Ignores I-Frames, Bypasses Creative.
- Attacker details: name, mod, distance, weapon and enchantments, invisibility, owner of turrets, pets and projectiles.
- Smart merge of repeated damage from the same source (`x12 hits`).
- Death screen panel: draggable, minimizable, scrollable, hover details, copy button.
- Respawn chat summary with hover details, `[Copy ID]`, `[Mute]` and a link to the full recap.
- Totem of Undying, prevented-death and heavy-hit snapshots with chat alerts.
- Commands: `history`, `last`, `snapshot`, `effects`, `search`, `togglechat`, `ignore`, `rename`, `sethint`, `config`, `inspect`, `dump`, `reload`, `server`, `help`, all with Tab completion.
- `/whm effects` with milk curability; `/whm search` with `#tag`, `@modid` and text queries.
- Two config files: `whathitme-common.cfg` (pack / server limits, synced from the server) and `whathitme-client.cfg` (personal). Every display element can be turned off.
- `renameMap` and `hintMap` with `&` colors, `*` wildcards and `lang:` translation keys.
- `/whm dump` writes `whathitme_dump.csv` and a ready-to-paste `hint_template.cfg`.
- Works client-only (estimated `[Client Est.]` entries), server-only (English chat for players without the mod) or on both.
- Automatic counter-measure tips by damage type.
- Languages: English, Japanese, Simplified Chinese, Traditional Chinese, Korean, Russian, German, French, Spanish, Brazilian Portuguese.
