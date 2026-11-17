# History, stats & search

All of these work for every player. The full list of commands is in the [command reference](/reference/commands).

## Damage history: `/whm`

`/whm` (or `/whm history`) lists the damage you took recently, alive or dead. Hover a line for details; `[Copy ID]` and `[Mute]` are at the end of each line.

![/whm history with hover details](/images/history.png)

- Repeated damage from the same source within 3 seconds is merged into one line (`x4 hits`).
- The last line shows the `Last 10s Total`.
- The number of lines is `chatHistoryLines` (default 15). The server keeps up to `historySize` records per player (default 50); they are dropped when you log out.

## Last death: `/whm last`

The full recap of your last death, like the death screen panel, in chat: death message, killing blow, properties, every hit of the last 30 seconds, `Last 10s Total`, tip and `[Copy Recap]`.

## Snapshots: `/whm snapshot [n]`

Saved totem, prevented-death and heavy-hit moments. `1` is the newest. See [alerts and snapshots](/player/chat#alerts-and-snapshots).

## Statistics: `/whm stats`

Your total deaths, total damage taken (health actually lost) and your top 5 causes of death. Up to 100 causes per player are counted. They are saved to `config/whathitme/.cache/stats_<uuid>.json` on death, logout and world save.

## Effects: `/whm effects`

Your active potion effects with level, time left, mod and whether milk cures them: `[Milk ✔]` or `[Milk ✖]`. Hover an effect for its id and its curative items.

![/whm effects](/images/effects.png)

## Search: `/whm search`

Finds damage types, mobs and effects in your pack. Combine terms with spaces.

![/whm search fire](/images/search.png)

| Query | Finds |
|---|---|
| `zombie` | Anything whose id or name contains "zombie" |
| `@twilightforest` | Everything from that mod |
| `@minecraft zombie` | Vanilla entries containing "zombie" |
| `#bypasses_armor` | Damage that ignores armor |
| `#absolute` | Damage that ignores armor, enchantments and effects (bypasses magic) |
| `#magic`, `#fire`, `#explosion`, `#projectile` | Damage with that property |
| `#damage` | Damage types only |
| `#scanned` | Damage ids found by the [scanner](/modpack/scanner) before anyone took that damage |
| `#mob` | Mobs |
| `#effect`, `#debuff` | Effects, harmful effects only |

Up to 30 results are shown.
