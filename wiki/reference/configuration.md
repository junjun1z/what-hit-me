# Configuration files

All files are in `config/whathitme/`. Both `.cfg` files use the Forge configuration format on every loader (also on Fabric) and are re-read by `/whm reload`.

| File | For | Contents |
|---|---|---|
| `whathitme-common.cfg` | Pack makers, server owners | Display switches, tip switches, filters and tracking. A server with the mod sends it to every modded client on login and after a reload or config change; it replaces the client's own common file. |
| `whathitme-client.cfg` | Each player | The same display switches and filters for personal use, `showHints`, and the panel layout. |
| `whathitme-client-templates.json` <Badge type="tip" text="1.3.0+" /> | Each player | Personal [templates](/modpack/templates). Created with commented examples. |
| `pack/` | Pack makers | [Pack folder](/modpack/pack-folder): renames, tips, ignored ids, locked features, death triggers, templates, death messages, rules. |

::: tip The two-file rule
An element is shown only when it is `true` in **both** files. Personal renames and tips are applied on top of the pack's. Priority: `.cfg` entries > pack JSON > built-in automatic tips.
:::

## Display switches

Category `display`, in **both** files, all default `true`.

| Key | Shows |
|---|---|
| `enableDeathScreenPanel` | The recap panel on the death screen |
| `enableRespawnChatSummary` | The death summary in chat after respawning (`/whm togglechat`) |
| `enableTotemChatAlert` | A chat alert with a details link when a Totem of Undying (or another mod) saves you |
| `enableHeavyHitSnapshot` | Snapshot and alert when one hit takes `heavyHitThreshold` of your max health |
| `showTimestamp` | How long ago the damage happened |
| `showDamageAmount` | Final damage amount |
| `showMitigationDetails` | Damage before armor, mitigation %, absorption used, health after the hit |
| `showSmartMergeHitCount` | Hit counter of merged repeated damage (`x12 hits`) |
| `showSourceModName` | Name of the mod that added the damage or the attacker |
| `showDamageProperties` | Properties such as Fire, Magic, Bypasses Armor |
| `showInternalDamageId` | Internal damage id in the hover text |
| `showAttackerName` | Attacker name |
| `showAttackerWeapon` | Attacker weapon and its enchantments |
| `showAttackerDistance` | Distance to the attacker |
| `showAttackerInvisibility` | Badge when the attacker was invisible |
| `showEntityOwner` | Owner of turrets, pets, summons and projectiles |
| `showActionButtons` | Clickable `[Copy ID]`, `[Mute]` and `[Copy Recap]` buttons |
| `showToggleChatNotice` | The `/whm togglechat` notice at the end of the summary |
| `showMaxHpPercentage` | Damage as a percentage of max health, e.g. `(30% Max HP)` |
| `showEnvironmentContext` | For damage without an attacker: block / fluid at your feet and body and the items in your hands |
| `showReflectedBadge` | `[Reflected / Thorns]` badge |
| `showArmorBreakEvents` | Timeline line when an armor piece breaks |
| `includeEquipmentInCopy` | `[Copy Recap]` adds dimension, max health, armor and effects |
| `includeCoordsInCopy` | `[Copy Recap]` adds the death coordinates (needs `includeEquipmentInCopy`) |

### Tip switches

| Key | File | Default | Meaning |
|---|---|---|---|
| `enableAutoHints` | common | `true` | Automatic tips from the damage type and properties |
| `enableCustomHints` | common | `true` | Custom tips from `hintMap` (config files and pack folder) |
| `showHints` | client | `true` | Show tips at all (automatic and the pack's) |

They replace `showHintsAndTips` of 1.0 / 1.1. An old `showHintsAndTips=false` turns all three off, then the old key is removed.

## Filters

Category `filters`, in **both** files.

| Key | Default | Meaning |
|---|---|---|
| `minDamageThreshold` | `0.05` | Damage (health + absorption) below this is not recorded. Range 0 - 1000. |
| `heavyHitThreshold` | `0.8` | One hit taking at least this part of max health is a heavy hit (0.8 = 80 %). Range 0.05 - 100. |
| `ignoredDamageIds` | empty | [Match keys](/modpack/match-keys) to hide (`*` wildcard, composite keys) |
| `renameMap` | empty | `key = Name` or `key = lang:translation.key` |
| `hintMap` | empty | `key = Tip` or `key = lang:translation.key` |

The thresholds in `whathitme-client.cfg` only matter in client-only mode; otherwise the server records the damage.

List values in the file look like this:

```
S:renameMap <
    minecraft:outOfWorld = &5The Abyss
    mob:minecraft:zombie @item:minecraft:iron_sword = Armed Zombie
 >
```

## Tracking

Category `tracking`, **common file only**.

| Key | Default | Range | Meaning |
|---|---|---|---|
| `historySize` | `50` | 5 - 500 | Records kept per player (ring buffer) |
| `mergeWindowSeconds` | `3.0` | 0 - 60 | Merge window for repeated damage (0 = off) |
| `summarySeconds` | `10` | 1 - 300 | Window of "Last Xs Total" |
| `recapSeconds` | `30` | 1 - 600 | How far back a death recap goes |
| `maxSnapshots` | `5` | 1 - 50 | Totem / heavy-hit snapshots per player |
| `requireClientMod` <Badge type="tip" text="1.3.0+" /> | `false` | | Server: refuse players whose client does not have What Hit Me? (any version). See [requireClientMod](/modpack/require-client-mod). |

## Layout

Category `layout`, **client file only**.

| Key | Default | Meaning |
|---|---|---|
| `anchorPoint` | `CENTER_LEFT` | `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`, `CENTER_LEFT`, `CENTER_RIGHT` |
| `offsetX` / `offsetY` | `4` / `0` | Offset from the anchor in GUI pixels, saved when you drag the panel |
| `scale` | `0.85` | Panel scale (0.25 - 3) |
| `backgroundOpacity` | `0.75` | 0.0 - 1.0 |
| `maxVisibleLines` | `8` | Lines before scrolling (1 - 40) |
| `minimized` | `false` | Saved by the `[-]` button |
| `chatHistoryLines` | `15` | Lines shown by `/whm history` (1 - 100) |

## Cache folder

`config/whathitme/.cache/` holds files the mod writes itself.

| File | Contents |
|---|---|
| `catalog_cache.json` | Damage types, mobs, effects and scanned damage ids, with a SHA-256 of the sorted `modid:version` list |
| `stats_<uuid>.json` | `/whm stats` data and which `oncePerPlayer` triggers already ran |
| `whathitme_dump.csv` | Written by `/whm dump` |

::: warning
Deleting `stats_<uuid>.json` resets that player's statistics and lets `oncePerPlayer` death triggers run again. The catalog cache is rebuilt automatically.
:::

## Changing settings

- In game: the [settings screen](/player/settings) or `/whm config <key> <value>`.
- In the file: edit it, then `/whm reload` (op).
