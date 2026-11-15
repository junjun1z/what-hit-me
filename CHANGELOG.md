# Changelog

The release workflow publishes the section whose heading matches the pushed tag (for example `## [1.0.0]` for tag `1.0.0`) as the release notes.

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
