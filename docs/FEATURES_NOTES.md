# What Hit Me? - feature notes

Machine-readable summary of what the code does (version 1.2.0). Source of truth: the code and `en_us.json`.
Mod id `whathitme`, Minecraft 1.7.10 - 26.3, Forge / Fabric / NeoForge, 63 builds. No required dependencies (Fabric: Fabric API; Mod Menu optional). Forge / NeoForge: no Mixins, no coremods.

## Install modes

| Installed on | Works? | What you get |
|---|---|---|
| Client + server | yes | Accurate data from the server (raw damage before armor, mitigation %, absorption, attacker weapon/enchantments, owner, i-frame / shield flags). Translated text (10 languages), clipboard buttons. The server's `whathitme-common.cfg` limits are synced to clients on login and override the client's local common cfg. |
| Client only (server without the mod, or vanilla server) | yes | Client fallback: the client compares its own health and, only when it drops, guesses the cause (fall, lava, fire, drowning, suffocation, void, nearby mob / projectile / creeper / TNT, poison, wither, starvation). Entries are labelled `[Client Est.]`. Death message is taken from the death screen. `inspect` is not available. Single player always counts as "both" (the integrated server has the mod). |
| Server only (players without the mod, even plain Forge clients) | yes | The server sends readable English chat (no translation keys): respawn summary, totem / heavy-hit alerts, all `/whm` commands. `[Copy ID]` puts the id into the chat box (SUGGEST_COMMAND) because the client cannot copy to the clipboard. Per-player `togglechat` and `ignore` are stored in the player's persistent NBT. |
| Mixed (some players have it) | yes | Each player gets the best mode for their client. The server knows who has the mod (a hello packet once per login). |

Nothing is sent to the chat on join. Packets are only sent on death, totem / heavy hit and when a command asks for data. Never per hit.

## Commands (`/whm`, alias `/whathitme`, everything Tab-completes)

op = permission level 2 (`PlayerHandle.isAdmin()`), or single player with cheats enabled. Changed in 1.2: `rename`, `sethint`, `dump`, `reload` need op also on the client; single player without cheats is not op.

| Command | What it does | Permission |
|---|---|---|
| `/whm` or `/whm history` | Recent damage taken (ring buffer, includes totem / heavy hit markers). Hover a line for details; `[Copy ID]` `[Mute]` buttons. | everyone |
| `/whm last` | Full recap of the last death: death message, killing blow, properties, every hit in the last 30 s, "Last 10s Total", tip, `[Copy Recap]`. | everyone |
| `/whm stats` | Total deaths, total damage taken (health actually lost), top 5 causes. | everyone |
| `/whm effects` | Active potion effects: name, level, time left, mod, `[Milk ✔]`/`[Milk ✖]` (from `PotionEffect#getCurativeItems()`), hover shows curative items. | everyone |
| `/whm togglechat` | Respawn chat summary on/off (client: saved to whathitme-client.cfg; server-only: per player). | everyone |
| `/whm ignore <damage_id>` | Hide / show a damage id (toggle). Client: client cfg. Server command: op = common cfg, non-op = personal list. | everyone |
| `/whm rename <damage_id> <name...>` | Custom display name (empty = remove). `&` color codes, or `lang:<key>`. Client: own cfg; server command: common cfg. | op |
| `/whm sethint <damage_id> <tip...>` | Custom tip (empty = remove). `lang:<key>` allowed. | op |
| `/whm config` / `/whm config <key>` / `/whm config <key> <value>` | No argument: opens the settings screen on a modded client (Fabric: the Mod Menu screen factory when Mod Menu is installed, the own screen otherwise); the server command lists the settings. Show one / change one (booleans accept true/false/on/off/toggle). `lockedFeatures` keys are refused. | client keys: everyone; common keys: viewing everyone, changing op |
| `/whm search <query>` | Search damage types (seen), mobs and effects. `#bypasses_armor #absolute #magic #fire #explosion #projectile` (damage properties), `#damage #mob #effect #debuff` (type), `@modid`, or text (id / name). Terms can be combined: `@minecraft zombie`. | everyone |
| `/whm inspect <player>` | Last death recap + history of an online player. | op |
| `/whm test <damage_id> [entity_id] [item_id] [dim_id]` | Dummy killing blow: resulting name, tip, matching deathTriggers (filled in, not run), chat preview. `-` skips an argument. | op |
| `/whm dump` | Writes `config/whathitme/.cache/whathitme_dump.csv` AND `config/whathitme/hint_template.cfg`. | op |
| `/whm reload` | Reloads both cfg files and the pack folder, forces a catalog rescan (client also asks the server to reload if the player is op). Reports pack files read / with errors. | op |
| `/whm snapshot [n]` | Saved totem / heavy-hit snapshots (1 = newest). | everyone |
| `/whm copy <text>` | (client) copy text to the clipboard - used by `[Copy ID]`. | client only |
| `/whm copyrecap` | (client) copy the last death recap as plain text. | client only |
| `/whm server <subcommand...>` | (client) run the server-side command, e.g. `/whm server config showTimestamp false` to change the server's common cfg as op. | needs the mod on the server |
| `/whm help` | Command list. | everyone |

## Damage ids

- Melee by an entity: `mob:<entity registry id>`, e.g. `mob:minecraft:zombie`, `mob:minecraft:player`, `mob:lycanitesmobs:...`.
- Everything else: `<modid>:<DamageSource type>`, e.g. `minecraft:fall`, `minecraft:arrow`, `minecraft:outOfWorld`, `minecraft:explosion.player`, `draconicevolution:...`. The mod id is found from: vanilla type list, the DamageSource subclass's jar, a `modid.` prefix in the type, or (once per new type) the mod class that called `attackEntityFrom`. Unknown = `unknown:`.
- The attacker of a projectile is kept separately (attacker = shooter, "via Arrow").

Properties shown: Bypasses Armor (`isUnblockable`), Bypasses Magic (`isDamageAbsolute`), Magic, Fire, Explosion, Projectile, Through Shield (hit while blocking), Ignores I-Frames (fresh hit within 10 ticks of the previous one), Bypasses Creative. Badges: `[Invisible]` attacker, `[Totem]`, `[Heavy Hit]`, `[Client Est.]`, ☠ killing blow.

## Screens / chat

- Death screen panel (`GuiGameOver`): killing blow, death message, properties, scrollable hit list (mouse wheel), hover tooltip per line, "Last Xs Total", tip. Drag the header to move (position saved to offsetX/offsetY on release), `[-]`/`[+]` minimize (saved), `[Copy]` copies the recap.
- Respawn chat summary (format from the spec): header, ☠ Killing Blow (hover details, [Copy ID] [Mute]), ⚠ Properties, Σ Last 10s Total (hearts, hits), ✎ Tip, ➜ [Click here for full death recap] (/whm last), ✖ To disable this message, run /whm togglechat, footer.
- Totem alert: "⚠ [What Hit Me?] A Totem of Undying saved you! Last hit: ... [Details]" (details = /whm snapshot 1). Also "Lethal damage was prevented!" when another mod saved the player, and "Heavy hit: X took Y at once." for hits >= heavyHitThreshold of max health.
- Damage is shown in hearts (HP / 2) with ❤. Smart merge: same id + same source within mergeWindowSeconds -> one line "x12 hits".
- Automatic tips by type / property (fall, fire, drowning, void, explosion, projectile, bypasses armor, absolute, magic, wither, invisible attacker, melee, player, ...), overridden by hintMap.

## Config files (`config/whathitme/`, Forge Configuration, re-read by `/whm reload`)

In-game editor (1.0.1 on Forge 1.7.10 - 1.12.2, every version and loader since 1.2.0; also `/whm config`): Mods list > What Hit Me? > Config (Fabric: Mod Menu, optional). Two groups: "Personal (whathitme-client.cfg)" and "Modpack / server (whathitme-common.cfg)", each with its categories (Display switches, Filters / names / tips, Death screen panel or Recording). Saved and applied on Done.

An element is shown only if it is true in BOTH `whathitme-common.cfg` and `whathitme-client.cfg`. When the server has the mod, the server's common file replaces the client's common file (display switches + ignore/rename/hint lists + summarySeconds).

### Both files - category `display` (all default `true`)
enableDeathScreenPanel, enableRespawnChatSummary, enableTotemChatAlert, enableHeavyHitSnapshot, showTimestamp, showDamageAmount, showMitigationDetails, showSmartMergeHitCount, showSourceModName, showDamageProperties, showInternalDamageId, showAttackerName, showAttackerWeapon, showAttackerDistance, showAttackerInvisibility, showEntityOwner, showActionButtons, showToggleChatNotice, showMaxHpPercentage, showEnvironmentContext, showReflectedBadge, showArmorBreakEvents, includeEquipmentInCopy, includeCoordsInCopy.
Tips: enableAutoHints, enableCustomHints (common only), showHints (client only). They replace showHintsAndTips (1.0 / 1.1); an old showHintsAndTips=false sets all three false, then the old key is removed.

### Both files - category `filters`
| key | default | meaning |
|---|---|---|
| minDamageThreshold | 0.05 | damage (health + absorption) below this is not recorded |
| heavyHitThreshold | 0.8 | one hit >= this part of max health = heavy hit (snapshot + alert) |
| ignoredDamageIds | [] | ids to hide, `*` wildcard (`mob:minecraft:*`) |
| renameMap | [] | `id = Custom Name` or `id = lang:translation.key`; `&` colors; `*` in id allowed |
| hintMap | [] | `id = Tip text` or `id = lang:translation.key` |

### Common only - category `tracking`
| key | default | meaning |
|---|---|---|
| historySize | 50 | records per player (fixed ring buffer, 5-500) |
| mergeWindowSeconds | 3.0 | smart merge window (0 = off) |
| summarySeconds | 10 | window of "Last Xs Total" |
| recapSeconds | 30 | how far back a death recap / snapshot goes |
| maxSnapshots | 5 | totem / heavy-hit snapshots kept per player |

### Client only - category `layout`
| key | default | meaning |
|---|---|---|
| anchorPoint | CENTER_LEFT | TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_LEFT, CENTER_RIGHT |
| offsetX / offsetY | 4 / 0 | offset from the anchor (saved by dragging) |
| scale | 0.85 | panel scale |
| backgroundOpacity | 0.75 | 0.0 - 1.0 |
| maxVisibleLines | 8 | lines before scrolling |
| minimized | false | saved by the [-] button |
| chatHistoryLines | 15 | lines shown by /whm history |

## Modpack-maker features

- Server authority: server common cfg is synced on login (and after `/whm reload` / config changes) and overrides clients.
- `lang:<key>` values in renameMap / hintMap -> translate pack tips with a resource pack.
- `/whm dump` -> `whathitme_dump.csv` (category,id,name,mod_id,mod_name,properties,times_seen,custom_name,custom_tip,ignored + current settings) and `config/whathitme/hint_template.cfg`: ready-to-paste `S:hintMap <...>` and `S:renameMap <...>` blocks listing every vanilla damage type, every damage id seen in the world and `mob:<id>` for every registered living entity, each with an empty value (empty values are ignored, so the whole block can be pasted and filled in gradually).
- Example entries:
  - `S:hintMap < mob:minecraft:creeper = Creepers hiss before exploding - sprint away! >`
  - `S:renameMap < minecraft:outOfWorld = &5The Abyss >`, `draconicevolution:chaos = lang:mypack.damage.chaos`
- No chat on first join, every element toggleable, no client install required.
- Performance: every damage / death handler returns on its first line for non-players; fake players are ignored; history = fixed-size ring buffer of immutable records holding only primitives and Strings (no Entity references); a player's buffer is dropped on logout; no server tick handlers (timestamps for windows / merge / reflect / cooldowns, `ServerPort.schedule` for checks after a hit); the client fallback scans entities only when health drops; packets only on death / totem / heavy hit / commands; every hook, packet handler, scan and UI draw is guarded (`Safe`).
- License MIT: free to include in modpacks.

## 1.2.0 features

- Files: `config/whathitme/pack/` (every `.json`, recursive, alphabetical path order, merged; `//` and block comments and trailing commas allowed; created with inert `_example.json` files in hints/ renames/ ignored/ triggers/ custom/), `config/whathitme/.cache/` (`catalog_cache.json`, `stats_<uuid>.json`, `whathitme_dump.csv`).
- Pack JSON keys: `renameMap`, `hintMap` (object or list of `"key = value"`; an empty value removes an earlier entry), `defaultIgnoredDamageIds`, `lockedFeatures` (config keys, case-insensitive), `deathTriggers` (`match`, `command`, `oncePerPlayer`, `cooldownSeconds`). Priority: .cfg > pack JSON > automatic hints. A value that is a translation key (or `lang:<key>`) is translated on the client.
- Composite keys (`MatchKey`): damage id, `@entity_id` / `@mob:entity_id` / `@entity:entity_id`, `@item:modid:item_id` (attacker main hand), `@dim:dimension_id`; separated by spaces or `&`; `*` / `?` wildcards; all conditions must match; the key with the most conditions wins. Used by renameMap, hintMap, ignoredDamageIds, defaultIgnoredDamageIds, deathTriggers.
- deathTriggers: server only, console permission (`ServerPort.runCommand`); placeholders `{player}` `{x}` `{y}` `{z}` (floored) `{damage}` (killing blow health, one decimal); every matching trigger runs, in file order; `oncePerPlayer` is stored in the stats file; `cooldownSeconds` per player.
- Catalog cache: SHA-256 of the sorted `modid:version` list; hit = load, miss = rescan + rewrite, `/whm reload` = forced rescan.
- Damage id scanner (`DamageIdScanner`, 1.7.10 - 1.19.3): reads mod jar class files (no class loading), string ids given to damage source constructors (also subclasses, anonymous classes, setters right after the constructor), nested jars one level deep; daemon thread after a cache miss; stored under `scan` in `catalog_cache.json`; `#scanned` search tag, dump column `source` (seen / scanned / registry), `/whm test` completion. 1.19.4+: every key of the DamageType registry, flags from its tags.
- `showMaxHpPercentage` `[-12.0 ❤ (30% Max HP)]` from max health at hit time; vital hit `[Localized / Vital Hit (Died with X.X ❤ total HP left)]` when total health > 0 at death; a death without a preceding damage event gets its killing blow from the death event.
- `showEnvironmentContext`: block / fluid at the feet and body, main / off hand item, with mod ids; merged hits reuse the first hit's values.
- `showReflectedBadge` `[⚡ Reflected / Thorns]`: thorns, or damage from an entity the player attacked within 50 ms; automatic hint.
- `showArmorBreakEvents` `[⚠ Broken: <armor>]`: PlayerDestroyItemEvent (Forge / NeoForge) plus comparing the 4 armor slots at hurt / damage time.
- Totem / revive: totem snapshot; death cancelled by another mod (Forge / NeoForge LivingDeathEvent LOWEST, receiveCanceled) or health restored after death -> snapshot.
- `/whm stats`: at most 100 causes per player in memory; saved on death, logout and world save.
- `[Copy Recap]` Markdown with `includeEquipmentInCopy` (dimension, max health, 4 armor pieces with enchantments and broken state, effects) and `includeCoordsInCopy`.
- Config screen: Forge 1.7.10 - 1.12.2 `GuiConfig`, newer Forge / NeoForge an own screen in the mods list, Fabric through an optional Mod Menu entrypoint (`suggests`); locked keys disabled.
- Protocol 2: ServerInfo carries lockedFeatures and pack renames / hints; features disabled on the server are masked out of packets; 1.0 / 1.1 peers get the old format.

## 1.3.0 progression rules

- Pack JSON key `rules` (sample `pack/rules/_example.json`): list of objects, evaluated top to bottom, the FIRST match wins. `match` (composite key, default `*`), `if` (all), `any` (one of), `unless` (none), `name`, `hint` (`""` = no tip), `hide`, `commands`, `enforce`, `oncePerPlayer`, `cooldownSeconds`. A matching rule's `name` / `hint` override renameMap / hintMap for that record; without `enforce` a player's personal rename / tip still wins. `hide` implies `"enforce": true` unless the rule says `"enforce": false`; enforced records skip personal renames, tips and templates.
- Conditions (`"!"` negates): `advancement:<id>` (1.12+), `tag:<scoreboard tag>`, `score:<objective><op><n>` (`>= > <= < = !=`), `stage:<name>` (Game Stages), `ftbquests:<hex quest or chapter id>` (team progress; 1.15.2: player progress), `bq:<quest id>` (Better Questing 1.12.2: number, GTNH fork: UUID), `heracles:<quest id>`. Mod providers use reflection, are created only when the mod is loaded; a missing mod / API mismatch = false, logged once. `/whm reload` (and server start) lists unknown prefixes, missing mods, unsupported built-ins, bad arguments.
- `hide` fields: `damage_id` (sent as `whathitme:hidden`, no [Copy ID] / [Mute], no ID line), `attacker` (name, id, projectile, owner, distance, invisibility; melee ids hidden too), `mod`, `weapon`, `death_message`, `tip` (alias `hint`), `properties`, `environment`, `owner`, `distance`, `position` (dimension, coordinates). Hidden fields are removed before packets are built; protocol 3 clients also get the rule result (name, hint, flags), 1.2 clients only the stripped record.
- Evaluated by the server whenever it builds an output for a player (death recap, respawn summary, alerts, /whm history / last / snapshot / inspect, search / dump, template preview, custom death message) with that player's progress (inspect: the inspected player); nothing cached across outputs, so completing a quest reveals old entries too. Client-only installs never apply rules.
- Hidden death message (a rule hides `death_message`, no deathMessages entry): `{player} was killed by {name}` (with an attacker, `{name}` = the rule's name) or `{player} died`; translated for modded clients, English for vanilla clients (Fabric 1.19.4+: translation with English fallback). The recap quotes the same text.
- `deathTriggers` accept `if` / `any` / `unless` (progress of the dead player).
- `/whm test <damage_id> ... --as <player> --assume <condition>` (repeatable): rule, hidden fields, enforce, name, tip, rule commands, death message for that player's progress.
- `requireClientMod` (common, default false): clients without the mod are refused with a readable message ("This server requires the What Hit Me? mod on your client ..."): Fabric and Forge 1.7.10 - 1.17.1 disconnect a client that sent no hello within 5 seconds (one timer task per join), newer Forge / NeoForge use the channel handshake where it can carry that message. Without it, Forge / NeoForge servers whose rules hide death messages (or with deathMessages) log a warning on start and `/whm reload` naming loader and version ("On Forge 1.20.1, players without What Hit Me? still see the vanilla death message; set requireClientMod=true to hide it from everyone."); never on Fabric.
- The template editor preview shows the rule result of the real death ("progression rule: enforce, attacker, ...").

## Build / test (for README "Building" section)

- `./gradlew build` (JDK 25 runs Gradle; the Java 8 toolchain is downloaded automatically). Version: `-Pmod_version=X.Y.Z` or env `VERSION`, default 1.0.0. Output: `build/libs/whathitme-<version>.jar` (reobfuscated, the only jar there).
- Development self-test: `./gradlew build -Pselftest` adds `devtools/` (never in release jars). Put the jar in a 1.12.2 instance and create `config/whathitme/selftest.flag` containing `sp` (single player, 71 checks), `mp-mod` / `mp-nomod` (join a server with `--server`), `pack` (modded mobs) or `showcase` (documentation screenshots). Result: `whathitme_selftest_result.txt`. `build/devlibs/whmdriver-dev.jar` drives a client without the mod (`config/whmdriver.flag`).
- Smart merge also joins interleaved damage (e.g. poison ticks between mob hits) with one of the last 5 lines.

## Screenshots (docs/screenshots/)

death-screen.png, respawn-chat.png, history.png (hover tooltip), effects.png, search.png, totem.png (third person, player skin visible), config-gui.png (in-game settings), modpack-death-screen.png (21-mod pack, Lycanites Mobs creature, tooltip with "Mod: Lycanites Mobs").
