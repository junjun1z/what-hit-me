# Troubleshooting & FAQ

## General

**Does it need anything else?**
On Forge and NeoForge, no. On Fabric, only Fabric API; Mod Menu is optional.

**Do players have to install it?**
No. On a server with the mod, players without it still get the chat summary and the commands in English. Installing it on the client adds the death screen panel, translations, the settings screen, clipboard buttons and templates. A server can require it with [`requireClientMod`](/modpack/require-client-mod).

**Can I use it on a server that does not have it?**
Yes. The client estimates the damage from its own health and nearby entities. Those entries are marked `[Client Est.]` and are less detailed. Rules and death triggers do not work then.

**Will it conflict with other mods?**
On Forge and NeoForge it uses only standard events: no Mixins, no coremods. On Fabric it uses Fabric API events and a few tiny Mixins only where no event exists. No screen is replaced: the panel is drawn on top of the vanilla death screen.

**Does it slow down my server?**
Only players are tracked, there are no server tick handlers, and packets are sent only on death, on a totem / heavy hit and on commands. The history is a small ring buffer per player.

**Can I include it in my modpack?**
Yes. It is MIT licensed. No need to ask.

## Something is not shown

**The panel / summary / some detail does not appear.**
An element is shown only when it is `true` in **both** `whathitme-common.cfg` and `whathitme-client.cfg`. On a server, the server's common file counts. Ask the server owner, or check with `/whm config <key>`.

**I cannot change a setting: "locked".**
The pack lists the key in [`lockedFeatures`](/modpack/locked-features). Nobody can change it in game; edit the files instead.

**The chat summary is too much. How do I turn it off?**
`/whm togglechat`, or `enableRespawnChatSummary=false`. Pack makers can turn off any element for everyone in `whathitme-common.cfg`, and lock it.

**A damage type does not show up at all.**
It may be ignored (`/whm ignore <id>` toggles it back, or check `ignoredDamageIds` / `defaultIgnoredDamageIds`), or the damage was below `minDamageThreshold` (default 0.05).

**Why does it say "Unknown" or `unknown:` for some damage?**
Some mods create damage without a type the game can trace back to them. Give it a name with [`renameMap`](/modpack/names-and-tips).

**Why are there no emoji?**
Older Minecraft fonts cannot render them, so every version uses symbols the game font has (☠ ⚠ ❤ ⚡) and text labels.

## Commands

**I could use `/whm rename` or `/whm reload` in 1.1, but not anymore.**
Since 1.2, `rename`, `sethint`, `test`, `dump` and `reload` need op, or single player with cheats enabled. Personal settings stay open to everyone through `/whm config` and the settings screen.

**`/whm inspect` says it needs the server.**
Inspect needs the mod on the server, because only the server knows other players' damage.

## Pack files

**My pack file does nothing.**
1. Run `/whm reload` and read the output: `files read` and `files with errors`. A file with a JSON error is skipped; the server log has the reason.
2. Make sure the file ends in `.json` and is inside `config/whathitme/pack/` (any subfolder).
3. Check that the entries are not still commented out (`//`).
4. Check the key with `/whm test <damage_id> [entity] [item] [dim]`.
5. On a dedicated server, edit the **server's** files; the client's pack folder is not used while connected to a server with the mod.

**Two files set the same key. Which one wins?**
The later file in alphabetical path order. See [How files are read](/modpack/pack-folder#how-files-are-read).

**My general tip wins over my special one.**
For `renameMap` / `hintMap` the most specific key wins (more conditions, then fewer wildcards). Check that the special key really matches with `/whm test`. For rules the **first** matching rule wins, so put special rules first.

## Templates <Badge type="tip" text="1.3.0+" />

**A template line disappears.**
A line whose placeholders are all empty is dropped on purpose. A variable is empty when there is no value or when its display switch is off (or the field is hidden by a rule). Use `{variable|fallback}` to show something instead.

**`/whm reload` says "unknown variable".**
Check the spelling against the [variable list](/modpack/templates#variables). The editor shows unknown variables in red.

**My personal template is ignored.**
The pack locked that output (`templates` or `templates.<output>`), or a rule with `enforce` / `hide` applies to that record.

## Death messages and rules <Badge type="tip" text="1.3.0+" />

**Players still see the vanilla death message.**
On Forge / NeoForge, players **without** the mod always see the vanilla message. Use [`requireClientMod`](/modpack/require-client-mod). On Fabric the message is replaced for everyone.

**A rule never matches.**
- Check `/whm reload` for condition warnings: unknown prefix, missing quest mod, not supported on this version (for example `advancement:` before 1.12, `tag:` before 1.9).
- Test with `/whm test <damage_id> [entity] --as <player>` and `--assume <condition>`.
- An earlier rule may already match the record (first match wins).
- Rules need the mod on the server.

**A quest condition is always false.**
The quest mod is missing, or its version has an API the provider does not know (logged once in the server log). See the [tested versions](/modpack/conditions#quest-mod-matrix). FTB Quests ids are hex ids; Better Questing GTNH ids are UUIDs.

## Pack editor and scripts <Badge type="tip" text="1.3.0+" />

**`/whm pack` says "Only operators ..." or "needs What Hit Me? on the server".**
The pack editor edits the server's files, so it needs the mod on the server and operator permission (or single player with cheats). Without them, `/whm pack personal` edits your own renames, tips and ignored ids.

**The comments in my pack file are gone.**
Saving an entry in the [pack editor](/modpack/pack-editor) rewrites that file, and hand-written comments in it are lost. Use the memo fields instead. The previous version of the file is in `config/whathitme/.backup/`.

**I saved something wrong in the pack editor.**
Press **Undo** (30 steps per player until the server stops), or copy the file back from `config/whathitme/.backup/<time>/`.

**My `script:` condition is always false.**
The name is not registered (check the `Scripts:` line of `/whm reload`), the script mod is missing, or the script function threw an error (logged once). Register it inside the register event so it survives reloads. See [Script integration](/modpack/scripts#reload-and-errors).

## Reporting a bug

Use `[Copy Recap]` after the death and include it in your report on [GitHub](https://github.com/junjun1z/what-hit-me/issues), together with the Minecraft version, the loader and the What Hit Me? version.
