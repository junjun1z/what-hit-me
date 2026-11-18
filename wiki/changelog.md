# Changelog

The full changelog is in [CHANGELOG.md on GitHub](https://github.com/junjun1z/what-hit-me/blob/main/CHANGELOG.md). Downloads: [GitHub Releases](https://github.com/junjun1z/what-hit-me/releases), Modrinth and CurseForge.

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
