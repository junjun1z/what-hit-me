# Settings screen

## Opening it

| Version / loader | How |
|---|---|
| Every version and loader | `/whm config` (no arguments) |
| Forge, NeoForge | Mods list > What Hit Me? > Config |
| Fabric | Mod Menu > What Hit Me? > settings icon (only when [Mod Menu](https://modrinth.com/mod/modmenu) is installed; it is optional) |

<!-- TODO media: retake: settings screen showing the Templates... and Pack editor... buttons (1.3.0) -->

![Settings screen](/images/config-gui.png)

## What is in it

Two groups, one per file:

| Group | File | Who may change it |
|---|---|---|
| **Personal** | `whathitme-client.cfg` | You |
| **Modpack / server** | `whathitme-common.cfg` | Operators, or single player with cheats |

Each group has categories: display switches, filters / names / tips, and the death screen panel layout (personal) or recording (modpack / server). Every key is explained in the [configuration reference](/reference/configuration).

- Changes are saved and applied when you press **Done**. In single player the server side is updated at once.
- Keys the modpack locked with [`lockedFeatures`](/modpack/locked-features) are drawn disabled.
- The **Templates...** button opens the [template editor](/modpack/template-editor) <Badge type="tip" text="1.3.0+" />.
- The **Pack editor...** button <Badge type="tip" text="1.3.0+" /> opens the [pack editor](/modpack/pack-editor). Without operator permission (or outside a world) it opens in **personal mode**: your own `renameMap`, `hintMap` and `ignoredDamageIds` as searchable lists with the match key builder and a preview, instead of one-line comma lists. The same is `/whm pack personal`.

::: tip A display element needs both files
An element is shown only when it is `true` in **both** files. If something does not appear although your personal switch is on, the pack or server has turned it off.
:::

## Changing settings with commands

```
/whm config                       open the screen (players without the mod: list of settings)
/whm config showTimestamp         show the current value
/whm config showTimestamp false   change it
/whm config showTimestamp toggle  flip it (true / false / on / off / toggle)
```

Personal keys can be changed by everyone. Common (modpack / server) keys can be viewed by everyone and changed by operators. From a modded client, `/whm server config <key> <value>` changes the server's file. See [Commands](/reference/commands).

## Key bindings

What Hit Me? adds **no key bindings**. Everything is on the death screen (mouse), in chat (click / hover) and in `/whm` commands, so it never conflicts with the controls of other mods.

## Language

The mod follows your Minecraft language. Included: English, 日本語, 简体中文, 繁體中文, 한국어, Русский, Deutsch, Français, Español, Português (Brasil). Players without the mod see English text sent by the server.
