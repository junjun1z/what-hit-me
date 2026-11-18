# Installation

## 1. Pick the right file

Downloads: **[Modrinth](https://modrinth.com/mod/what-hit-me)**, **[CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-hit-me)** or [GitHub Releases](https://github.com/junjun1z/what-hit-me/releases). The same files are on all three.

Download the file that matches your **Minecraft version** and **loader**. Every file name contains both, for example `whathitme-forge-1.20.1-1.3.0.jar` or `whathitme-fabric-1.21.1-1.3.0.jar`.

| Loader | Also needed |
|---|---|
| Forge | nothing |
| NeoForge | nothing |
| Fabric | [Fabric API](https://modrinth.com/mod/fabric-api) (on 1.21 / 1.21.1: Fabric API 0.116 or newer). [Mod Menu](https://modrinth.com/mod/modmenu) is optional. |

## 2. Put it in the `mods` folder

1. Close the game.
2. Copy the `.jar` into the `mods` folder of your instance (or of your server).
3. Start the game. On the first start the mod creates `config/whathitme/` with its config files and the [pack folder](/modpack/pack-folder).

That is all. Nothing appears in chat on join; you will see the mod the first time you take damage and die.

::: tip Client, server or both?
You can install it on the client only, on the server only, or on both. Both gives the most accurate data. See [Client, server or both](/guide/multiplayer).
:::

## Supported versions

| Minecraft | Forge | Fabric | NeoForge |
|---|---|---|---|
| 1.7.10 | ✔ | | |
| 1.8.9 | ✔ | | |
| 1.9 / 1.9.4 | ✔ | | |
| 1.10 / 1.10.2 | ✔ | | |
| 1.11 / 1.11.2 | ✔ | | |
| 1.12 / 1.12.1 / 1.12.2 | ✔ | | |
| 1.14 - 1.14.4 | 1.14.4 | ✔ | |
| 1.15 - 1.15.2 | 1.15.1 / 1.15.2 | ✔ | |
| 1.16 - 1.16.5 | 1.16.3 - 1.16.5 | ✔ | |
| 1.17 / 1.17.1 | 1.17.1 | ✔ | |
| 1.18.2 | ✔ | ✔ | |
| 1.19 - 1.19.4 | ✔ | ✔ | |
| 1.20 / 1.20.1 | ✔ | ✔ | 1.20.1 (Forge file) |
| 1.20.2 - 1.20.4 | ✔ | ✔ | 1.20.4 |
| 1.20.5 / 1.20.6 | 1.20.6 | ✔ | 1.20.6 |
| 1.21 / 1.21.1 | 1.21.1 | ✔ | ✔ |
| 1.21.2 - 1.21.5 | 1.21.3 - 1.21.5 | ✔ | 1.21.3 - 1.21.5 |
| 1.21.6 - 1.21.8 | 1.21.8 | ✔ | 1.21.7 / 1.21.8 |
| 1.21.9 - 1.21.11 | ✔ | ✔ | ✔ |
| 26.1 - 26.1.2 | 26.1.2 | ✔ | ✔ |
| 26.2 / 26.3 | ✔ | ✔ | ✔ |

Not available: Minecraft 1.13.x, 1.18 / 1.18.1, and the loader / version pairs left empty above. On NeoForge 1.20.1 use the Forge 1.20.1 file (it is published for both).

Java: the game version decides it, as usual (Java 8 up to 1.16.5, 16 for 1.17, 17 for 1.18 - 1.20.4, 21 for 1.20.5 - 1.21.11, 25 for 26.x).

## Files the mod creates

```
config/whathitme/
  whathitme-common.cfg            pack and server settings
  whathitme-client.cfg            personal settings and panel layout (client only)
  whathitme-client-templates.json personal templates (1.3.0+, client only)
  hint_template.cfg               written by /whm dump
  pack/                           pack definitions (any *.json, any subfolder)
  .cache/                         catalog_cache.json, stats_<uuid>.json, whathitme_dump.csv
  .backup/                        files before a pack editor change (1.3.0+, server)
```

Details: [Configuration files](/reference/configuration) and [Pack folder](/modpack/pack-folder).

## Removing the mod

Delete the `.jar`. The mod stores nothing in the world except, for players without the mod on a server, their `togglechat` / personal ignore choices (Forge: the player's persistent data; Fabric: `<world>/whathitme/players/<uuid>.properties`). You can delete `config/whathitme/` too.
