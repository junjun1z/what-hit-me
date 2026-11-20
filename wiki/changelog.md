# Changelog

The full changelog is in [CHANGELOG.md on GitHub](https://github.com/junjun1z/what-hit-me/blob/main/CHANGELOG.md). Downloads: [Modrinth](https://modrinth.com/mod/what-hit-me), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-hit-me) and [GitHub Releases](https://github.com/junjun1z/what-hit-me/releases).

## 1.3.0 <Badge type="tip" text="upcoming" />

- [Templates](/modpack/templates) for the hover details, panel / history line, respawn summary, alerts and copy text. Personal (`whathitme-client-templates.json`) > pack (`templates`) > built-in. Lockable with `lockedFeatures`.
- In-game [template editor](/modpack/template-editor) with live preview (`/whm template`, **Templates...** in the settings screen).
- `/whm template preview <output>` and `/whm template reset [output]`.
- [Custom death messages](/modpack/death-messages) (`deathMessages`): Fabric servers replace the message for everyone, Forge / NeoForge servers for players with the mod.
- [Progression rules](/modpack/rules) (`rules`): names, tips, hidden fields and commands per player progress, with [conditions](/modpack/conditions) `advancement:`, `tag:`, `score:`, `stage:`, `ftbquests:`, `bq:`, `heracles:`.
- `deathTriggers` accept `if` / `any` / `unless` and every template variable as placeholder.
- `/whm test ... --as <player> --assume <condition>`.
- [`requireClientMod`](/modpack/require-client-mod) (common config).
- New sample folders `pack/templates/`, `pack/deathmessages/`, `pack/rules/`.
- Network protocol 3; 1.0 - 1.2 clients and servers keep working.

- [Script integration](/modpack/scripts): KubeJS, CraftTweaker and MineTweaker scripts add `script:` conditions, `{script:}` template variables, event listeners and runtime entries (public Java API `io.github.junjun1z.whathitme.api`), on every version and loader where one of these script mods exists: KubeJS 1.12.2 - 26.1.2, CraftTweaker 1.12.2 - 1.21.1, MineTweaker 3 / CraftTweaker 3.x 1.7.10 - 1.11.2 (also the GTNH fork).

- In-game [pack editor](/modpack/pack-editor) (`/whm pack`, **Pack editor...** in the settings screen) for every pack section and the server settings, server-authoritative with backups and undo; pack files may carry `order`, `enabled` and memos. Personal mode (`/whm pack personal`) for your own renames, tips and ignored ids. Screen on every supported version and loader (Forge 1.7.10 - 26.3, Fabric 1.14 - 26.3, NeoForge 1.20.1 - 26.3).

- Quest mod conditions on every version and loader where the quest mod exists ([matrix](/modpack/quest-mods#quest-mod-matrix)): Better Questing 1.7.10 - 1.12.2 (every API generation, also Better Questing Unofficial) and 1.20.1 (Forge, NeoForge), Game Stages 1.11.2 - 1.20.3 and NeoForge 1.20.1, Heracles 1.20 and Fabric 1.21, FTB Quests 1.16.4 (1604), 1.20.4 Forge / Fabric, 1.21 and 26.1; each tested in game, quest conditions in server rules tested on dedicated servers.

- `/whm reload` also lists what scripts registered and script functions that failed.

- Fix (Forge 1.7.10 - 1.12.1): a hit healed in the same tick (food regeneration, Regeneration) was recorded with too little damage, and a 1 ❤ hit such as a burning tick could be lost (merged lines showed x2 instead of x3).

- Fixes from the pre-release test pass: one deeply nested pack file no longer stops the whole pack folder from loading, and wrong section types / too deep folders are reported; `.cfg` files saved in another encoding than UTF-8 are no longer reset to the defaults; decimal settings refuse `NaN`; `/whm reload` on a modded client lists the pack files and the files with errors; `/whm pack` against an older server says so instead of loading forever; 1.2 clients on a 1.3 server get English text instead of raw keys; `/kill` shows `∞% Max HP` instead of `2147483647%`; First Aid hits on Forge / NeoForge 1.13+ are heavy hits, not prevented deaths; single player re-reads the pack folder when a world opens; open pack editors refresh after `/whm reload`; undoing a case-only category rename no longer deletes the file on Windows; the death screen panel wraps the total line.

- No limit on the names, tips, templates and other lists a 1.3 server sends to 1.3 clients (before, each list was cut at 1000 entries for players with the mod). 1.2 clients still get the first 1000 entries of each list; `/whm reload` and the server log warn about longer lists. See [Large packs](/modpack/names-and-tips#large-packs).

- A very large reply (a long history or death recap with long tips, a huge `/whm dump`) could disconnect the player (packet limit 1 MiB, 2 MiB on Forge 1.7.10). 1.3 clients get large replies in parts on every version, and `/whm dump` lists every damage id (it stopped at 4000); 1.2 clients get what fits in one packet, with a note on how many records or entries were left out.

## 1.2.0

- Pack folder `config/whathitme/pack/` with `renameMap`, `hintMap`, `defaultIgnoredDamageIds`, `lockedFeatures`, `deathTriggers`.
- Composite match keys (`@entity`, `@item:`, `@dim:`, wildcards, most specific wins).
- `/whm test`, `/whm stats`, damage id scanner, smart catalog cache.
- Settings screen on every version and loader (Mod Menu on Fabric, optional).
- Max HP share, vital hits, environment context, reflected damage badge, armor break lines, equipment in `[Copy Recap]`.
- Tip switches `enableAutoHints`, `enableCustomHints`, `showHints`.
- Permission change: `rename`, `sethint`, `dump`, `reload` need op also on the client.

## 1.1.0

Every major Minecraft version and loader: Forge 1.7.10 - 26.3, Fabric 1.14 - 26.3, NeoForge 1.20.1 - 26.3 (63 files).

## 1.0.1

Bug fixes from a full in-game test pass, in-game settings screen on Forge 1.7.10 - 1.12.2.

## 1.0.0

First release.
