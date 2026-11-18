# In-game pack editor <Badge type="tip" text="1.3.0+" />

`/whm pack` opens an editor in the style of a quest book for **everything** that lives in the [pack folder](/modpack/pack-folder) and in `whathitme-common.cfg`: renames, tips, [rules](/modpack/rules), [death messages](/modpack/death-messages), [templates](/modpack/templates), [death triggers](/modpack/death-triggers), ignored ids, [locked features](/modpack/locked-features) and the server settings (display switches, limits, [`requireClientMod`](/modpack/require-client-mod)).

| | |
|---|---|
| Who | Single player with cheats, or operators (permission level 2) on a server with What Hit Me? |
| Opened by | `/whm pack`, or the **Pack editor...** button in the settings screen |
| Server-authoritative | The client only sends edits. The server checks them, writes the JSON file, reloads the pack at once and sends the new state to everyone, so every player gets the change without `/whm reload`. |
| Saving | When an entry is confirmed: **Done**, Enter in a field, or selecting another entry / tab / category. Nothing is saved per keystroke. |
| Backup and undo | Before a file changes, its previous version is copied to `config/whathitme/.backup/<time>/`. **Undo** restores the files of your last change (30 steps per editor). |
| Versions | The screen exists on Forge 1.7.10 - 1.12.2, Fabric / Forge 1.14.4 - 1.17.1 and Fabric / Forge / NeoForge 1.21.1 for now; other versions print "not available on this version" and keep working with the files. Clients of 1.7.10 / 1.8.9 are not told their operator level, so `/whm pack` asks the server first (a non-operator gets the "only operators" message, the settings button opens the own lists). |


## Layout

- **Tabs** at the top: Renames, Hints, Rules, Death msgs, Templates, Triggers, Ignored, Locked, Server settings. A tab shows only the entries of that type.
- **Categories** (left): every JSON file of the pack folder is a category, sub-folders are groups. A category without entries of the current tab is still listed (grey), so you can add entries to it. `#n` is the position in the load order; the checkbox switches a category on or off; `✎` marks a memo; red `!` marks a file with a syntax error (shown, not editable). Entries registered by scripts appear under **Scripts (read-only)**.
- **Entries** (middle): the entries of the selected category with a search field, **+ Add**, and ↑ / ↓ to change the order (for rules the order is the priority).
- **Details** (right): the form of the selected entry with inline errors and warnings, **Done**, **Revert**, **Delete** and **Move to...** (another category).
- In windows narrower than 400 GUI pixels (large GUI scale, old versions) the three parts are shown one at a time with a **<** button to go back.

## Categories and folders

The buttons under the category tree:

| Button | Does |
|---|---|
| `+` | New category: a JSON file in the selected folder (default `pack/<name>.json`) |
| `+□` | New folder (a group in the tree) |
| `✎` | Rename the category or folder |
| `⇨` | Move it into another folder |
| `↑` `↓` | Load earlier / later. Later files override earlier ones, and rules are checked in this order, so the priority across files is explicit. |
| `M` | Memo of the category |
| `✕` | Delete (with a backup; Undo restores it). Folders must be empty. |

Pack makers can work either way: one file per topic holding every section (the default for new categories), one folder per type (`pack/rules/<name>.json`), or one folder per mod.

### What the editor stores in the files

| Key | Where | Meaning |
|---|---|---|
| `"order": 10` | top level | Load order (lower first). Files without `order` count as 0 and load in alphabetical path order, exactly as before 1.3.0. Moving a category writes `order` into the files (10, 20, 30, ...). |
| `"enabled": false` | top level | The category is switched off: the file stays, but it is not loaded. |
| `"memo": "..."` | top level | Memo of the category |
| `"memo": "..."` | inside a rule / death trigger object | Memo of that entry |
| `"entryMemos": {"renameMap": {"<key>": "..."}}` | top level | Memos of entries of map / string-list sections (renames, hints, death messages, templates, ignored ids, locked features) |

Keys the editor does not know are kept. `order`, `enabled` and the category memo are written without touching the rest of the text, so comments survive; saving an entry rewrites its file, and **hand-written comments in that file are lost** - use the memo fields instead.

## Entries

- **Renames / Hints / Death msgs**: match key and text. The **Pick...** button opens the match key builder.
- **Rules**: match key, `if` / `any` / `unless` conditions, name, tip (or "no tip at all"), hidden fields, enforce (automatic = on when something is hidden), commands, once per player, cooldown.
- **Templates**: the output and its lines; **Edit in the template editor...** opens the [template editor](/modpack/template-editor) for these lines, **Done** there saves the entry.
- **Triggers**: match key, command, conditions, once per player, cooldown.
- **Ignored / Locked**: one id or config key per entry (**Pick...** lists the lockable keys).
- Every entry has a **memo** (not shown to players).

### Match key builder

Searchable lists of the damage ids and mobs the server knows (registries, the damage id scanner and recorded deaths) with their mod names. Clicking a row fills the damage id or the attacker; `*`, `mod:*` and `mob:mod:*` insert wildcards; item (`@item:`) and dimension (`@dim:`) are separate fields. See [match keys](/modpack/match-keys).


### Condition picker

The condition prefixes with the values the server can list: advancements, scoreboard objectives, tags of online players, script conditions, and for quest mods what their API offers (FTB Quests chapters and quests with their titles, Game Stages known stages, Better Questing quests with their names, also the GTNH version). Advancements exist from 1.12, tags from 1.9. Greyed prefixes are not available on this server (mod missing / version). **not (!)** negates; for `score:` choose the comparison and the number. Any value can also be typed. See [conditions](/modpack/conditions).


### Inline checks

Under the fields: unknown condition prefixes and wrong `score:` syntax (refused), conditions of a missing mod or an unregistered script condition (warning, counts as false), a match key that matches no known damage id / mob, unknown template variables, and the server's `/whm reload` warnings for that entry.

### Preview

**Preview** (bottom bar) shows a test pane under the form: **As** chooses an online player whose progress is used, the field next to it lists conditions to assume, **Test** runs exactly what `/whm test <damage_id> ... --as <player> --assume <condition>` prints, for the saved state of the entry's match key. It runs again after every save.


## Server settings

Every key of `whathitme-common.cfg`, grouped like the settings screen: switches toggle at once, numbers and lists are changed with **Apply** (or Enter) and checked against their range. Operators may change keys the pack locked for players.

## Two editors at once

The title shows who else has the editor open. When someone saves, every other editor gets the new state and a notice "*name* changed *file*". If you are editing an entry of that file, a red line warns you: pressing Done saves your version (last save wins).

## Security

Everything is checked on the server: permission (operator / single player owner) for every message, file and folder names (letters, digits, space, `- _ . ( )`; paths stay inside `config/whathitme/pack/`, no `..`), known sections and value types only, size limits (8000 characters per text, 512 KB per file, 5000 entries per section). A refused edit leaves the files unchanged and shows the reason in the status line.

## Personal mode

The same editor without permission: `/whm pack personal`, or **Pack editor...** in the settings screen when you are not an operator. It edits **your own** `renameMap`, `hintMap` and `ignoredDamageIds` in `whathitme-client.cfg` with the same lists, search, match key builder and preview, instead of one-line comma lists. Server-only tabs, conditions and memos are not shown there; Undo works the same way. Operators switch between **Pack** and **Personal** with the tabs at the top left.

