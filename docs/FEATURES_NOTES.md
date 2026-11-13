# What Hit Me? - feature notes

Machine-readable summary of what the code does. Source of truth: the code and `en_us.lang`.
Mod id `whathitme`, Minecraft 1.12.2, Forge 14.23.5.2847+, Java 8. No dependencies, no Mixins, no coremods.

## Install modes

| Installed on | Works? | What you get |
|---|---|---|
| Client + server | yes | Accurate data from the server (raw damage before armor, mitigation %, absorption, attacker weapon/enchantments, owner, i-frame / shield flags). Translated text (10 languages), clipboard buttons. The server's `whathitme-common.cfg` limits are synced to clients on login and override the client's local common cfg. |
| Client only (server without the mod, or vanilla server) | yes | Client fallback: the client watches its own health once per tick and, only when it drops, guesses the cause (fall, lava, fire, drowning, suffocation, void, nearby mob / projectile / creeper / TNT, poison, wither, starvation). Entries are labelled `[Client Est.]`. Death message is taken from the death screen. `inspect` is not available. Single player always counts as "both" (the integrated server has the mod). |
| Server only (players without the mod, even plain Forge clients) | yes | The server sends readable English chat (no translation keys): respawn summary, totem / heavy-hit alerts, all `/whm` commands. `[Copy ID]` puts the id into the chat box (SUGGEST_COMMAND) because the client cannot copy to the clipboard. Per-player `togglechat` and `ignore` are stored in the player's persistent NBT. |
| Mixed (some players have it) | yes | Each player gets the best mode for their client. The server knows who has the mod (a hello packet once per login). |

Nothing is sent to the chat on join. Packets are only sent on death, totem / heavy hit and when a command asks for data. Never per hit.

## Commands (`/whm`, alias `/whathitme`, everything Tab-completes)

| Command | What it does | Permission |
|---|---|---|
| `/whm` or `/whm history` | Recent damage taken (ring buffer, includes totem / heavy hit markers). Hover a line for details; `[Copy ID]` `[Mute]` buttons. | everyone |
| `/whm last` | Full recap of the last death: death message, killing blow, properties, every hit in the last 30 s, "Last 10s Total", tip, `[Copy Recap]`. | everyone |
| `/whm effects` | Active potion effects: name, level, time left, mod, `[Milk ✔]`/`[Milk ✖]` (from `PotionEffect#getCurativeItems()`), hover shows curative items. | everyone |
| `/whm togglechat` | Respawn chat summary on/off (client: saved to whathitme-client.cfg; server-only: per player). | everyone |
| `/whm ignore <damage_id>` | Hide / show a damage id (toggle). Client: client cfg. Server command: op = common cfg, non-op = personal list. | everyone |
| `/whm rename <damage_id> <name...>` | Custom display name (empty = remove). `&` color codes, or `lang:<key>`. | client: everyone (own cfg); server command: op |
| `/whm sethint <damage_id> <tip...>` | Custom tip (empty = remove). `lang:<key>` allowed. | same as rename |
| `/whm config` / `/whm config <key>` / `/whm config <key> <value>` | List settings / show one / change one (booleans accept true/false/on/off/toggle). | client: own cfg; server command: op |
| `/whm search <query>` | Search damage types (seen), mobs and effects. `#bypasses_armor #absolute #magic #fire #explosion #projectile` (damage properties), `#damage #mob #effect #debuff` (type), `@modid`, or text (id / name). Terms can be combined: `@minecraft zombie`. | everyone |
| `/whm inspect <player>` | Last death recap + history of an online player. | op (level 2) or single-player owner |
| `/whm dump` | Writes `whathitme_dump.csv` (game / server dir) AND `config/whathitme/hint_template.cfg`. | client: everyone; server command: op |
| `/whm reload` | Reloads both cfg files (client also asks the server to reload if the player is op). | client: everyone; server: op |
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

An element is shown only if it is true in BOTH `whathitme-common.cfg` and `whathitme-client.cfg`. When the server has the mod, the server's common file replaces the client's common file (display switches + ignore/rename/hint lists + summarySeconds).

### Both files - category `display` (all default `true`)
enableDeathScreenPanel, enableRespawnChatSummary, enableTotemChatAlert, enableHeavyHitSnapshot, showTimestamp, showDamageAmount, showMitigationDetails, showSmartMergeHitCount, showSourceModName, showDamageProperties, showInternalDamageId, showAttackerName, showAttackerWeapon, showAttackerDistance, showAttackerInvisibility, showEntityOwner, showHintsAndTips, showActionButtons, showToggleChatNotice.

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
| offsetX / offsetY | 8 / 0 | offset from the anchor (saved by dragging) |
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
- Performance: every damage handler returns on its first line for non-players (mob fights cost nothing); fake players are ignored; history = fixed-size ring buffer of immutable records holding only primitives and Strings (no Entity references); a player's buffer is dropped on logout; no per-tick work on the server except an empty-list check (lethal/heavy hits are checked at the end of that tick only); the client fallback reads a few floats per tick and scans entities only when health drops; packets only on death / totem / heavy hit / commands.
- License MIT: free to include in modpacks.
