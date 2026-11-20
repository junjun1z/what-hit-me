# What is What Hit Me?

What Hit Me? records the damage a player takes and explains a death in plain words:

- the **killing blow** and the **death message**,
- the **attacker**, its **weapon** and the **mod** it comes from,
- how much **armor** and **absorption** helped,
- whether the damage **ignored armor**, was **fire**, **magic**, an **explosion** or a **projectile**,
- every hit of the **last 30 seconds**, with repeated damage merged (`x12 hits`),
- and a **tip** for next time.

It adds no blocks or items and creates no world data. It runs on the client, on the server, or on both.

![Death screen panel](/images/death-screen.webp)

## Feature overview

| Feature | Where | Page |
|---|---|---|
| Recap panel next to the vanilla death screen | Client | [Death screen panel](/player/death-screen) |
| Hover details for every hit | Client / chat | [Death screen panel](/player/death-screen#hover-details) |
| Short summary in your chat after respawning | Chat | [Chat summary & alerts](/player/chat) |
| Totem, revive and heavy-hit alerts with snapshots | Chat | [Chat summary & alerts](/player/chat#alerts-and-snapshots) |
| `[Copy Recap]` as Markdown for bug reports | Client | [Chat summary & alerts](/player/chat#copy-recap) |
| Damage history, stats, effects, search | Commands | [History, stats & search](/player/tools) |
| Settings screen on every version and loader | Client | [Settings screen](/player/settings) |
| Pack folder: names, tips, hidden ids, locked settings, death triggers | Server / pack | [Modpack makers](/modpack/) |
| Templates for every output, in-game editor <Badge type="tip" text="1.3.0+" /> | Pack / player | [Templates](/modpack/templates) |
| Custom death messages <Badge type="tip" text="1.3.0+" /> | Server / pack | [Custom death messages](/modpack/death-messages) |
| Progression rules (quests, advancements, scores) <Badge type="tip" text="1.3.0+" /> | Server / pack | [Progression rules](/modpack/rules) |

## Built to stay out of the way

- **Only players are tracked.** Every damage and death handler returns on its first line for any other entity. Fake players are ignored.
- **No server tick handlers.** Time windows, merging, reflected damage and cooldowns compare timestamps. Checks that must wait for the end of a hit are queued once on the server task queue.
- **Network traffic only when needed:** on death, on a totem / heavy hit, and when a player runs a command. Never per hit.
- **Smart catalog cache.** Damage types, mobs and effects are scanned once and saved with a hash of the mod list. Same mods on the next start: loaded from the cache.
- **Every hook is guarded.** Event handlers, packet handlers, scans and screens catch their own errors and fall back to "unknown", so broken data from another mod does not become a crash.
- **Small memory use.** The history is a fixed-size ring buffer of immutable records per player (no entity references), dropped on logout.
- **Forge / NeoForge:** standard events only, no Mixins, no coremods. **Fabric:** Fabric API events plus a few tiny Mixins where no event exists.

## Facts

| | |
|---|---|
| Mod id | `whathitme` |
| Loaders | Forge, Fabric (needs Fabric API), NeoForge |
| Minecraft | 1.7.10 - 26.3 (63 files), see [supported versions](/guide/getting-started#supported-versions) |
| Languages | English, 日本語, 简体中文, 繁體中文, 한국어, Русский, Deutsch, Français, Español, Português (Brasil) |
| License | MIT. You may include it in any modpack without asking. |
| Author | junjun1z |
| Source | [GitHub](https://github.com/junjun1z/what-hit-me) |

::: info Why no emoji?
Older Minecraft fonts cannot draw emoji, so every version uses symbols the game font has (☠ ⚠ ❤ ⚡ Σ) and text labels.
:::
