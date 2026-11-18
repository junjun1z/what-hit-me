# Pack folder

`config/whathitme/pack/` holds your pack definitions as JSON files. It is the recommended place for everything a modpack customizes.

## Created on first start

If the folder is missing, the mod creates it with one `_example.json` per topic. Every entry in them is commented out, so **nothing happens until you edit them**.

```
config/whathitme/pack/
  hints/_example.json          hintMap
  renames/_example.json        renameMap
  ignored/_example.json        defaultIgnoredDamageIds, lockedFeatures
  triggers/_example.json       deathTriggers
  templates/_example.json      templates (1.3.0+)
  deathmessages/_example.json  deathMessages (1.3.0+)
  rules/_example.json          rules (1.3.0+)
  custom/_example.json         notes: any file may contain any key
```

When you update from 1.2 to 1.3, the new `templates/`, `deathmessages/` and `rules/` samples are added only if those folders do not exist yet. Your own files are never touched.

## How files are read

1. Every `.json` file below `pack/` is collected, **in any subfolder**.
2. The files are sorted by their path (lower case, alphabetical), for example `hints/_example.json` before `hints/nether.json` before `rules/bosses.json`. <Badge type="tip" text="1.3.0+" /> A top-level `"order": <number>` sorts first (files without it count as 0), and `"enabled": false` skips a file without deleting it; the [pack editor](/modpack/pack-editor) writes both, plus `"memo"` notes.
3. They are merged in that order. **Later files override earlier entries** with the same key.
4. A file that cannot be parsed is skipped; `/whm reload` names it and the server log shows the reason.

The folder names are only for your own order. **Any file may contain any of the keys.** Organize by topic (`pack/nether.json`), by type (`pack/hints/...`) or by mod (`pack/twilightforest.json`), whatever you like.

::: tip Control the order with names
Need one file to win over another? Prefix the names: `pack/10-base.json`, `pack/20-twilight.json`, `pack/90-overrides.json`.
:::

## JSON format

Comments and trailing commas are allowed:

```json
{
  // line comment
  /* block comment */
  "renameMap": {
    "minecraft:outOfWorld": "&5The Abyss",   // trailing comma below is fine
  },
}
```

## All keys

| Key | Type | Page |
|---|---|---|
| `renameMap` | object `"key": "Name"` or list of `"key = Name"` | [Names, tips & ignored ids](/modpack/names-and-tips) |
| `hintMap` | object `"key": "Tip"` or list of `"key = Tip"` | [Names, tips & ignored ids](/modpack/names-and-tips) |
| `defaultIgnoredDamageIds` | list of match keys | [Names, tips & ignored ids](/modpack/names-and-tips#hiding-damage) |
| `lockedFeatures` | list of config keys | [Locked features & permissions](/modpack/locked-features) |
| `deathTriggers` | list of objects | [Death triggers](/modpack/death-triggers) |
| `templates` <Badge type="tip" text="1.3.0+" /> | object: output -> list of lines | [Templates](/modpack/templates) |
| `deathMessages` <Badge type="tip" text="1.3.0+" /> | object `"key": "message template"` | [Custom death messages](/modpack/death-messages) |
| `rules` <Badge type="tip" text="1.3.0+" /> | list of objects | [Progression rules](/modpack/rules) |

### Merging rules per key

| Key | When two files have it |
|---|---|
| `renameMap`, `hintMap`, `deathMessages` | Merged by key. A later file replaces the value of the same key. **An empty value (`""`) removes** the entry of an earlier file. |
| `defaultIgnoredDamageIds`, `lockedFeatures` | Lists are added together. |
| `deathTriggers`, `rules` | Lists are appended in file order. For rules this order matters: the first matching rule wins. |
| `templates` | A later file replaces the **whole** template of an output. An empty list removes it. |

### Object or list?

Both forms are the same:

```json
{
  "hintMap": {
    "minecraft:lava": "Carry a water bucket."
  }
}
```

```json
{
  "hintMap": [
    "minecraft:lava = Carry a water bucket."
  ]
}
```

## Applying changes

- Run `/whm reload` (op). It reports `files read` and `files with errors`, plus warnings for unknown template variables, unknown rule hide fields and condition problems.
- On a dedicated server the server reloads and sends the new definitions to every modded client at once.

::: warning Client-only installs
When the server does not have the mod, each client uses its **own** `config/whathitme/pack/`. Progression rules and death triggers need the mod on the server and do nothing there.
:::
