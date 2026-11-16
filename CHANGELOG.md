# Changelog

The release workflow publishes the section whose heading matches the pushed tag (for example `## [1.0.0]` for tag `1.0.0`) as the release notes.

## [1.2.0]

New features for players and modpack makers on every supported version: **Forge 1.7.10 - 26.3, Fabric 1.14 - 26.3, NeoForge 1.20.1 - 26.3** (63 files, each started and tested in game). Existing config files keep working.

**Permission change:** `/whm rename`, `/whm sethint`, `/whm dump` and `/whm reload` now need op (permission level 2), or single player with cheats enabled, also when run on the client. `/whm inspect` in single player now also needs cheats. Personal settings stay open to everyone through `/whm config` and the settings screen.

### Added

- Pack folder `config/whathitme/pack/`: every `.json` in it (any subfolder, alphabetical path order) is merged. Keys: `renameMap`, `hintMap`, `defaultIgnoredDamageIds`, `lockedFeatures`, `deathTriggers`. Created on first start with commented-out examples in `hints/`, `renames/`, `ignored/`, `triggers/` and `custom/`. Priority: `.cfg` files > pack JSON > automatic tips.
- `lockedFeatures`: config keys that cannot be changed from the settings screen or `/whm config`, even in single player; shown greyed out in the settings screen.
- Composite match keys for `renameMap`, `hintMap`, `ignoredDamageIds` and `deathTriggers`: damage id, `@entity_id` / `@mob:entity_id`, `@item:modid:item_id` (attacker main hand) and `@dim:dimension_id` in any order, with `*` wildcards. The key with the most conditions wins.
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
