# What Hit Me?

A Minecraft mod for Forge, Fabric and NeoForge (1.7.10 - 26.3) that shows exactly what killed you. **Works in any modpack, with any combination of mods — no required dependencies.**

[日本語版 README](README.ja.md)

![Death screen panel](docs/screenshots/death-screen.png)

- Minecraft 1.7.10 - 26.3: Forge, Fabric (needs Fabric API) and NeoForge. The full table is in [docs/description.md](docs/description.md#supported-versions).
- Mod id `whathitme`
- Client, server, or both. No blocks or items. Forge / NeoForge: standard events only, no Mixins or coremods. Fabric: Fabric API events plus a few tiny Mixins where no event exists.
- License: MIT

## Features

- **Death screen panel**: killing blow, death message, damage properties, every hit from the last 30 seconds, "Last 10s Total" and a tip. Hover for details. Drag the header to move it, `[-]` to minimize, mouse wheel to scroll, `[Copy]` to copy the recap.
- **Respawn chat summary** in your own chat: killing blow, properties, total of the last 10 seconds, tip, link to the full recap. `/whm togglechat` turns it off.
- **Damage history** (`/whm`): recent hits, alive or dead. Repeated damage from the same source within 3 seconds is merged into one line (`x12 hits`).
- **Hover details**: damage before / after armor, mitigation %, absorption used, health after the hit, attacker weapon and enchantments, distance, `[Invisible]` attacker, owner of turrets / pets / arrows, source mod, internal damage id. `[Copy ID]` and `[Mute]` buttons.
- **Totem and heavy-hit snapshots**: when a Totem of Undying saves you, another mod prevents your death, or one hit takes 80% of your max health, the moment is saved and a chat alert with `[Details]` appears (`/whm snapshot`).
- **Effects** (`/whm effects`): active effects with mod, level, time left and `[Milk ✔]` / `[Milk ✖]`.
- **Search** (`/whm search`): damage types, mobs and effects by `#bypasses_armor`, `#absolute`, `#magic`, `#fire`, `#explosion`, `#projectile`, `#damage`, `#mob`, `#effect`, `#debuff`, `@modid` or text. Terms can be combined (`@minecraft zombie`).
- **Automatic tips** by damage type and properties, replaceable per damage id.
- 10 languages.

Older Minecraft fonts cannot render emoji, so the mod uses symbols the game font has (☠ ⚠ ❤ Σ) and text labels.

## Install modes

| Installed on | What players get |
|---|---|
| Client and server | Accurate data from the server (raw damage, mitigation %, absorption, weapon, owner, shield and i-frame flags), text in the player's language, clipboard buttons. The server's `whathitme-common.cfg` applies to everyone. |
| Client only | Client fallback: the client watches its own health and guesses the cause (fall, lava, fire, drowning, suffocation, void, nearby mob, projectile, explosion, poison, wither, starvation). Entries are marked `[Client Est.]`. `/whm inspect` is not available. |
| Server only | Players without the mod (even plain Forge clients) get English chat: respawn summary, totem / heavy-hit alerts and every `/whm` command. `[Copy ID]` puts the id into the chat box. `togglechat` and `ignore` are stored per player. |

Single player counts as "client and server". On mixed servers each player gets the best mode their client supports.

## For modpack makers

### Why include it

- Fewer "what killed me?" questions: players see the attacker, its mod and the damage type themselves.
- Guide players through your pack's dangers with your own tips (`hintMap`) and names (`renameMap`). Hide noise with `ignoredDamageIds`.
- No dependencies, no world data, nothing in chat on join. Safe to add or remove at any time.

### Config files

`config/whathitme/`, Forge configuration format (the same files and keys on every loader), re-read by `/whm reload`.

| File | For | Contents |
|---|---|---|
| `whathitme-common.cfg` | Pack makers, server owners | Display switches, filters (`ignoredDamageIds`, `renameMap`, `hintMap`, `minDamageThreshold`, `heavyHitThreshold`) and tracking (`historySize`, `mergeWindowSeconds`, `summarySeconds`, `recapSeconds`, `maxSnapshots`). A server with the mod sends its file to every modded client on login and after a reload or config change; it replaces the client's own common file. |
| `whathitme-client.cfg` | Each player | The same display switches and filters for personal use, plus the panel layout (`anchorPoint`, `offsetX`, `offsetY`, `scale`, `backgroundOpacity`, `maxVisibleLines`, `minimized`, `chatHistoryLines`). |

An element is shown only when it is `true` in **both** files. Personal renames and tips are applied on top of the pack's.

<details>
<summary>All settings</summary>

**Display switches** (both files, category `display`, all default `true`):
`enableDeathScreenPanel`, `enableRespawnChatSummary`, `enableTotemChatAlert`, `enableHeavyHitSnapshot`, `showTimestamp`, `showDamageAmount`, `showMitigationDetails`, `showSmartMergeHitCount`, `showSourceModName`, `showDamageProperties`, `showInternalDamageId`, `showAttackerName`, `showAttackerWeapon`, `showAttackerDistance`, `showAttackerInvisibility`, `showEntityOwner`, `showHintsAndTips`, `showActionButtons`, `showToggleChatNotice`.

**Filters** (both files, category `filters`):

| Key | Default | Meaning |
|---|---|---|
| `minDamageThreshold` | 0.05 | Damage below this is not recorded |
| `heavyHitThreshold` | 0.8 | A hit taking this part of max health is a heavy hit |
| `ignoredDamageIds` | empty | Ids to hide, `*` wildcard |
| `renameMap` | empty | `id = Name` or `id = lang:key` |
| `hintMap` | empty | `id = Tip` or `id = lang:key` |

The thresholds in `whathitme-client.cfg` only matter in client-only mode; otherwise the server records the damage.

**Tracking** (common file only, category `tracking`):

| Key | Default | Meaning |
|---|---|---|
| `historySize` | 50 | Records kept per player (5-500) |
| `mergeWindowSeconds` | 3.0 | Merge window for repeated damage (0 = off) |
| `summarySeconds` | 10 | Window of "Last Xs Total" |
| `recapSeconds` | 30 | How far back a death recap goes |
| `maxSnapshots` | 5 | Totem / heavy-hit snapshots per player |

**Layout** (client file only, category `layout`):

| Key | Default | Meaning |
|---|---|---|
| `anchorPoint` | `CENTER_LEFT` | `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`, `CENTER_LEFT`, `CENTER_RIGHT` |
| `offsetX` / `offsetY` | 4 / 0 | Offset from the anchor, saved when you drag the panel |
| `scale` | 0.85 | Panel scale |
| `backgroundOpacity` | 0.75 | 0.0 - 1.0 |
| `maxVisibleLines` | 8 | Lines before scrolling |
| `minimized` | false | Saved by the `[-]` button |
| `chatHistoryLines` | 15 | Lines shown by `/whm history` |

</details>

### Custom tips and names

```
S:hintMap <
    mob:minecraft:creeper = Creepers hiss before exploding - sprint away!
    mob:lycanitesmobs:* = Check the Beastiary for this mob's weaknesses.
 >

S:renameMap <
    minecraft:outOfWorld = &5The Abyss
    draconicevolution:chaos = lang:mypack.damage.chaos
 >
```

- Ids: `mob:<entity id>` for attacks by an entity, `<modid>:<damage type>` for everything else (`minecraft:fall`, `minecraft:outOfWorld`, `minecraft:explosion.player`). `*` wildcards work.
- `&` color codes work.
- `lang:<translation.key>` is translated on each player's client, so a resource pack can provide the text in every language.
- Ids without a `hintMap` entry get an automatic tip.
- In game: `/whm rename <id> <name>`, `/whm sethint <id> <tip>`. From a modded client these edit your personal file; use `/whm server rename ...` (op) to edit the server's common file.

### `/whm dump` workflow

1. Run `/whm dump` in a world with your pack (op on a server).
2. You get `whathitme_dump.csv` (game or server folder: damage types seen, mobs, effects, their mods and properties, current settings) and `config/whathitme/hint_template.cfg`.
3. The template has `S:hintMap <...>` and `S:renameMap <...>` blocks with every vanilla damage type, every damage id seen in the world and `mob:<id>` for every registered mob, each with an empty value.
4. Paste them into `whathitme-common.cfg` and fill in what you need. Empty values are ignored.

Damage types from mods are listed once they have hit someone in that world.

### Performance

- Only players are tracked; damage handlers return on their first line for other entities. Fake players are ignored.
- No per-tick server work beyond an empty-list check.
- Fixed-size ring buffer per player of immutable records (primitives and strings, no entity references), dropped on logout.
- Packets only on death, totem / heavy hit and commands. Never per hit.

### Can I include it in my modpack?

Yes. MIT license, no need to ask.

## Commands

`/whm` (alias `/whathitme`), everything Tab-completes.

| Command | What it does | Who |
|---|---|---|
| `/whm` or `/whm history` | Recent damage taken | Everyone |
| `/whm last` | Full recap of your last death | Everyone |
| `/whm snapshot [n]` | Saved totem / heavy-hit snapshots (1 = newest) | Everyone |
| `/whm effects` | Active effects, mod, milk curability | Everyone |
| `/whm search <text\|#tag\|@modid>` | Search damage types, mobs and effects | Everyone |
| `/whm togglechat` | Respawn chat summary on / off | Everyone |
| `/whm ignore <id>` | Hide / show a damage id | Everyone (personal); op for the server list |
| `/whm rename <id> <name...>` | Custom name (empty removes it) | Client: personal; server: op |
| `/whm sethint <id> <tip...>` | Custom tip (empty removes it) | Client: personal; server: op |
| `/whm config [key] [value]` | List, show or change settings (`true`/`false`/`on`/`off`/`toggle`) | Client: personal; server: op |
| `/whm inspect <player>` | Last death and history of an online player | Op (level 2) |
| `/whm dump` | Write `whathitme_dump.csv` and `hint_template.cfg` | Client: everyone; server: op |
| `/whm reload` | Reload config files | Client: everyone; server: op |
| `/whm server <subcommand>` | From a modded client: run it on the server | Needs the mod on the server |
| `/whm help` | Command list | Everyone |

`/whm copy` and `/whm copyrecap` are used internally by the `[Copy ID]` and `[Copy Recap]` buttons.

## Languages

English, 日本語, 简体中文, 繁體中文, 한국어, Русский, Deutsch, Français, Español, Português (Brasil).

`python tools/check_lang.py` lists keys missing or extra in each language compared to `en_us.json`.

## Building

Requires JDK 25 to run Gradle; the Java 8 / 17 / 21 / 25 toolchains of the targets are downloaded automatically.

```
./gradlew collectJars -Pmod_version=1.0.0
```

Every Minecraft version / loader is one target; all jars are copied to `build/dist/`. Layout, architecture, how to add a version and the in-game self-test: [BUILD.md](BUILD.md).

Releases: pushing a tag such as `1.0.0` runs `.github/workflows/release.yml`, which builds every target and publishes the jars listed in `publish.json` to GitHub Releases, Modrinth and CurseForge with the matching section of [CHANGELOG.md](CHANGELOG.md).

## License

[MIT](LICENSE) © junjun1z
