# Templates <Badge type="tip" text="1.3.0+" />

Templates let you rewrite what What Hit Me? shows, line by line, with `{variables}`. Pack makers set them in the pack folder; players can have personal ones.

::: info Nothing changes until you write one
Without a template every output looks exactly like 1.2.0, in every language. You only replace the outputs you want.
:::

## The outputs

| Output | What it changes | Also used for |
|---|---|---|
| `hover` | The hover details of a record (death screen tooltip) | Hover text of `/whm` history lines and of the chat summary |
| `line` | One timeline line of the death screen panel | Lines of `/whm history` |
| `summary` | The respawn chat summary | |
| `alert` | The totem / prevented death / heavy-hit chat alert | |
| `copy` | The text copied by `[Copy]` / `[Copy Recap]` | |

Custom death messages use the same syntax, see [Custom death messages](/modpack/death-messages).

## Where templates come from

| Priority | Source | File |
|---|---|---|
| 1 (highest) | Personal | `config/whathitme/whathitme-client-templates.json` (client) |
| 2 | Pack | `"templates"` in any `config/whathitme/pack/**/*.json` |
| 3 | Built-in | The normal 1.2.0 output |

- Lock an output with `lockedFeatures` (`templates` or `templates.<output>`) and personal templates are ignored for it. See [Locked features](/modpack/locked-features#locking-templates).
- A [progression rule](/modpack/rules) with `enforce` (or `hide`) skips personal templates for its records, so players cannot get around hidden fields.
- On a server, pack templates are also used for the English chat sent to players without the mod.

## Writing a template

In a pack file, each output is a list of lines (or one string with `\n`):

```json
{
  "templates": {
    "summary": [
      "&6&l===== What Hit Me? =====",
      "&cKilling blow: &f{killing_blow} &c({damage} ❤)",
      "&3Properties: {properties}",
      "&6Last {total_seconds}s: {total} ❤ in {total_hits} hits",
      "&aTip: {tip}",
      "&b{recap_link}",
      "{toggle_notice}"
    ]
  }
}
```

The personal file has the same shape:

```json
{
  "templates": {
    "line": ["&4{killing} &8{time_ago} &f{name} &c-{damage} ❤ &e{badges}"]
  }
}
```

The easiest way to write them is the in-game [template editor](/modpack/template-editor) with its live preview.

## Syntax

| Syntax | Meaning | Example |
|---|---|---|
| `{variable}` | Value of the variable. Names are case-insensitive. | `{attacker_name}` |
| `{variable\|fallback}` | Fallback text when the variable is empty | `{attacker_owner\|nobody}` |
| `{variable:2}` | Number of decimals (0 - 9) for numbers; default is 1 for most numbers | `{damage:2}` -> `3.75` |
| `&` + code | Color and format codes: `&0`-`&f` colors, `&l` bold, `&n` underline, `&r` reset | `&cDanger` |
| A line `lang:<key>` | The whole line is a translation key, translated for each reader | `lang:mypack.summary.header` |
| A line that is a known translation key | Translated too | `whathitme.summary.header` |
| `{mob_...}` | Alias of `{attacker_...}` | `{mob_name}` = `{attacker_name}` |

**Automatic line dropping:** a line whose placeholders are **all empty** is left out. So `"Weapon: {weapon}"` disappears when the attacker had no weapon. A line without any placeholder is always shown.

Unknown variables are empty. `/whm reload` lists them with the file name, and the editor shows them in red.

Long lines wrap automatically in the tooltip and the panel.

## Variables

::: tip Variables respect the display switches
A variable whose display switch is off (in either file, or masked by the server) is **empty**, so its line disappears. Templates can never reveal information the server disabled. Fields hidden by a [rule](/modpack/rules) are empty too.
:::

### Damage

| Variable | Value | Switch |
|---|---|---|
| `{damage}` | Damage taken in hearts | `showDamageAmount` |
| `{damage_hp}` | Damage taken in health points | `showDamageAmount` |
| `{raw_damage}` | Damage before armor, in hearts | `showMitigationDetails` |
| `{mitigation}` | Percentage blocked by armor and effects (0 decimals by default) | `showMitigationDetails` |
| `{absorbed}` | Absorption (yellow hearts) used | `showMitigationDetails` |
| `{health_after}` | Health left after the hit, in hearts | `showMitigationDetails` |
| `{max_health}` | Max health in hearts | `showMitigationDetails` |
| `{max_hp_percent}` | Damage as a percentage of max health (0 decimals) | `showMaxHpPercentage` |
| `{hits}` | Merged hit count (empty for a single hit) | `showSmartMergeHitCount` |

### Damage type

| Variable | Value | Switch |
|---|---|---|
| `{name}` | Main name: rule name, custom name, attacker or damage type | |
| `{damage_name}` | Damage type name (with renames), without the attacker | |
| `{damage_id}` | Internal damage id | `showInternalDamageId` |
| `{damage_mod}` | Mod of the damage | `showSourceModName` |
| `{properties}` | Damage properties (Fire, Magic, ...) | `showDamageProperties` |
| `{death_message}` | The vanilla death message (summary, alert, copy, death messages) | |

### Attacker

| Variable | Value | Switch |
|---|---|---|
| `{attacker_name}` | Attacker name | `showAttackerName` |
| `{attacker_id}` | Attacker entity id | `showAttackerName` |
| `{attacker_mod}` | Mod of the attacker | `showAttackerName` + `showSourceModName` |
| `{attacker_distance}` | Distance to the attacker in blocks | `showAttackerDistance` |
| `{attacker_owner}` | Owner of the pet, turret or projectile | `showEntityOwner` |
| `{direct}` | Projectile / direct entity (when not the attacker) | `showAttackerName` |
| `{weapon}` | Attacker weapon | `showAttackerWeapon` |
| `{weapon_id}` | Attacker weapon item id | `showAttackerWeapon` |
| `{weapon_enchants}` | Enchantments of the weapon | `showAttackerWeapon` |

### Context

| Variable | Value | Switch |
|---|---|---|
| `{environment}` | Block / fluid at feet and body, items in hand | `showEnvironmentContext` |
| `{dimension}` | Dimension id | `includeEquipmentInCopy` |
| `{x}` `{y}` `{z}` | Death position (whole blocks; `{x:1}` for decimals) | `includeEquipmentInCopy` + `includeCoordsInCopy` |
| `{time_ago}` | How long ago the hit happened | `showTimestamp` |
| `{time}` | Clock time of the hit (HH:mm:ss) | `showTimestamp` |

### Badges and other

| Variable | Value | Switch |
|---|---|---|
| `{badges}` | Badges: estimate, totem, heavy hit, reflected, invisible, vital hit | per badge |
| `{killing}` | `☠` when this is the killing blow | |
| `{tip}` | Counter-measure tip | `showHints` |
| `{player}` | Player name | |

### Summary, alert and copy

These need a whole death or snapshot, so they are filled in `summary`, `alert` and `copy` (and death messages), and empty in `hover` / `line`.

| Variable | Value |
|---|---|
| `{killing_blow}` | Killing blow name (hover shows its details) |
| `{total}` | Damage of the last `summarySeconds` in hearts |
| `{total_hits}` | Hits of the last `summarySeconds` |
| `{total_seconds}` | Length of that window (`summarySeconds`) |
| `{recap_link}` | Clickable link to the full recap (`/whm last`) |
| `{toggle_notice}` | The `/whm togglechat` notice (`showToggleChatNotice`) |
| `{alert_kind}` | Totem of Undying, Heavy Hit or Death Prevented |
| `{details_link}` | Clickable `[Details]` link of an alert |
| `{timeline}` | Every damage line of the recap (copy) |
| `{equipment}` | Equipment block: dimension, armor, effects (`includeEquipmentInCopy`) |
| `{effects}` | Active effects at death (`includeEquipmentInCopy`) |
| `{recap}` | The complete built-in recap text |

## Examples per output

### `hover`

```json
"hover": [
  "&l{name}",
  "{badges}",
  "&8ID: {damage_id}",
  "&cDamage: {damage} ❤ ({damage_hp} HP)",
  "&7Before armor: {raw_damage} ❤, {mitigation}% mitigated",
  "&eAbsorbed: {absorbed} ❤",
  "&7Health: {health_after} / {max_health} ❤",
  "&eMerged hits: x{hits}",
  "&fAttacker: {attacker_name}",
  "&7Via: {direct}",
  "&bWeapon: {weapon} &d{weapon_enchants}",
  "&7Distance: {attacker_distance}m",
  "&7Owner: {attacker_owner}",
  "&3Properties: {properties}",
  "&9Mod: {damage_mod}",
  "&7{environment}",
  "&aTip: {tip}"
]
```

### `line`

```json
"line": ["&4{killing} &8{time_ago} &f{name} &c-{damage} ❤ &e{badges}"]
```

A compact line with the share of max health:

```json
"line": ["&f{name} &c{damage:0}❤ &7({max_hp_percent|?}%)"]
```

### `summary`

A short two-line summary:

```json
"summary": [
  "&c☠ {killing_blow} &7({damage} ❤, {total} ❤ in {total_seconds}s)",
  "&a✎ {tip} &b{recap_link}"
]
```

### `alert`

```json
"alert": ["&6{alert_kind}: &f{name} &c({damage} ❤) {details_link}"]
```

### `copy`

Markdown for Discord:

```json
"copy": [
  "**Death recap** ({time})",
  "Killing blow: {killing_blow} ({damage} hearts)",
  "Death message: {death_message}",
  "{timeline}",
  "Last {total_seconds}s: {total} hearts in {total_hits} hits",
  "{equipment}"
]
```

### Translated lines

```json
"summary": [
  "lang:mypack.whm.header",
  "&c{killing_blow} &7- {tip}"
]
```

## Checking a template

| Way | What it shows |
|---|---|
| [Template editor](/modpack/template-editor) | Live preview on every keystroke, with your latest death or a sample death |
| `/whm template preview <output>` | The template rendered with your latest death (or a sample death) in chat |
| `/whm reload` | Unknown variables and outputs, with the file name |

## Personal templates

Players can write their own templates (unless the pack locks them):

- In game: `/whm template`, or **Templates...** in the settings screen, then **Save**.
- In the file `config/whathitme/whathitme-client-templates.json` (created with commented examples), then `/whm reload`.
- Back to the pack / built-in look: **Reset** in the editor, `/whm template reset <output>`, or `/whm template reset` for all.
