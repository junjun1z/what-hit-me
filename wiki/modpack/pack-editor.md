# In-game pack editor <Badge type="warning" text="planned" />

::: warning Coming in 1.3.0 — not implemented yet
This page describes **planned** behaviour. `/whm pack` does not exist in the current build. Today, edit the [pack folder](/modpack/pack-folder) files and run `/whm reload`; for templates use the [template editor](/modpack/template-editor).
:::

## Planned: `/whm pack`

An editor in the style of a quest book for **everything** that lives in the pack JSON and the server config today: renames, tips, rules, death messages, templates (the template editor as a tab), death triggers, ignored ids, and server settings such as `requireClientMod`, `lockedFeatures` and the display switches.

| Planned behaviour | Details |
|---|---|
| Who | Single player with cheats, or operators |
| Opened by | `/whm pack` and a button in the settings screen |
| Server-authoritative | The client sends edits; the server writes the JSON and reloads at once, so every player gets the change |
| Saving | When an entry is confirmed (Done / moving to another entry), not per keystroke; a backup of the previous file and undo |
| Categories | One category = one JSON file. Create, rename, delete, reorder; move entries between categories. Hand-written files appear as categories; unknown keys are kept. Category order is stored, so rule priority across files is explicit. |
| Folders | Subfolders appear as groups, so you can organize by topic (`pack/<category>.json`), by type (`pack/rules/<category>.json`) or by mod |
| Memos | Memo fields per category and per entry, stored in the JSON (hand-written comments may be lost on save) |
| Helpers | Match key builder with searchable damage id / mob lists and wildcard buttons; condition picker with lists from the server (advancements, FTB quests, stages, scoreboard objectives and tags); live preview "as player / assuming conditions" (the same as `/whm test --as / --assume`); inline errors; a notice when two people edit at once |

## Planned: personal mode

The same editor, opened from the client settings screen (no permission needed), edits **your own** `renameMap`, `hintMap` and `ignoredDamageIds` with the same list, search, match key builder and preview, instead of one-line comma lists. Server-only tabs and conditions are not shown there.

The exact command names may still change before release.
