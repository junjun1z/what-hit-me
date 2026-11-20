# 条件 <Badge type="tip" text="1.3.0+" />

条件は[進行度ルール](/ja/modpack/rules)（`if`、`any`、`unless`）と[死亡トリガー](/ja/modpack/death-triggers)で使います。1 つの条件は `接頭辞:引数` の形で、先頭に `!` を付けると否定になります。

```json
"if":     ["advancement:minecraft:story/enter_the_nether", "score:deaths>=3"],
"any":    ["tag:nether_guide", "tag:veteran"],
"unless": ["!advancement:minecraft:story/mine_diamond"]
```

このページでは Minecraft に組み込まれている条件を説明します。クエスト mod とスクリプトはさらに接頭辞を追加します。[クエスト mod](/ja/modpack/quest-mods) と[スクリプト連携](/ja/modpack/scripts)を見てください。

## 組み込みの条件

| 条件 | 真になるとき | 例 |
|---|---|---|
| `advancement:<id>` | プレイヤーがその進捗を達成している | `advancement:minecraft:end/kill_dragon` |
| `tag:<タグ>` | プレイヤーがそのスコアボードタグを持っている（`/tag` コマンド） | `tag:veteran` |
| `score:<目的><演算子><数>` | プレイヤーのスコアの比較が真。演算子：`>=` `>` `<=` `<` `=` `!=`。スコアがなければ偽 | `score:deaths>=3` |
| `!<条件>` | その条件が偽 | `!tag:veteran` |

リストの組み合わせ方：`if` をすべて満たし、**かつ** `any` を（書いてあれば）少なくとも 1 つ満たし、**かつ** `unless` をどれも満たさないこと。

## ほかの接頭辞

| 接頭辞 | 提供元 | ページ |
|---|---|---|
| `ftbquests:<id>` | FTB Quests | [FTB Quests](/ja/modpack/ftb-quests) |
| `bq:<id>` | Better Questing（GTNH 版、Better Questing Unofficial、4.x も） | [Better Questing](/ja/modpack/better-questing) |
| `heracles:<id>` | Heracles / Odyssey Quests | [Heracles / Odyssey Quests](/ja/modpack/heracles) |
| `stage:<名前>` | Game Stages | [Game Stages](/ja/modpack/game-stages) |
| `script:<name>` | KubeJS / CraftTweaker / MineTweaker のスクリプト、または Java API を使うほかの mod | [スクリプト連携](/ja/modpack/scripts) |

## クエスト mod {#quest-mods}

クエストの進行度も同じリストで使え、1 つのルールに複数のクエスト mod を混ぜられます。どのクエスト mod がどの Minecraft バージョンで動くか、組み合わせ方は[クエスト mod](/ja/modpack/quest-mods)（[対応表](/ja/modpack/quest-mods#quest-mod-matrix)）にあります。

::: tip スクリプト mod をどのバージョンでも使うには
KubeJS、CraftTweaker、コマンドブロックからでもルールを動かせます。**スコアボードタグ**や**スコア**を設定させて、`tag:` / `score:` の条件で読み取ってください。連携があるバージョンでは、スクリプトが `script:` 条件を直接登録できます（[スクリプト連携](/ja/modpack/scripts)）。
:::

## Minecraft のバージョンごとの組み込み条件 {#built-in-conditions-per-minecraft-version}

| 条件 | 1.7.10、1.8.9 | 1.9 - 1.11.2 | 1.12+ |
|---|---|---|---|
| `advancement:` | 非対応 | 非対応 | ✔ |
| `tag:` | 非対応 | ✔ | ✔ |
| `score:` | ✔ | ✔ | ✔ |

Forge・Fabric・NeoForge で同じです。進捗は 1.12 より前に、エンティティのタグは 1.9 より前に存在しません。非対応の条件は偽になり、`/whm reload` が「not supported on Forge 1.x」と表示します。1.7.10 / 1.8.9 では、タグの代わりにスコアを目印に使ってください：`score:veteran>=1`。

## ID の調べ方

| 条件 | 値の場所 |
|---|---|
| `advancement:` | 進捗の ID。`/advancement grant @s only ` と入力すると、ID の候補がすべて出ます。mod の進捗はその mod の名前空間です（`twilightforest:progress_lich`）。 |
| `tag:` | `/tag <player> add <タグ>`（1.13 以降）や `/scoreboard players tag <player> add <タグ>`（1.9 〜 1.12.2）で付けた好きな名前 |
| `score:` | `/scoreboard objectives add <名前> <基準>` の目的の名前。`deathCount` や `minecraft.killed:minecraft.wither` などの基準を使えば自動で数えられます。 |

[パックエディター](/ja/modpack/pack-editor#condition-picker)は、サーバーが知っている進捗・目的・タグを一覧にするので、入力せずにクリックで選べます。

## 条件の確認

- `/whm reload` が、不明な接頭辞、接頭辞のない条件、入っていない mod、このバージョンで使えない条件、引数の誤り（形の崩れた `score:` など）を一覧にします。
- `/whm test <damage_id> ... --as <player>` で、そのプレイヤーの実際の進行度でルールを評価します。
- `--assume <条件>` で条件を満たしたものとして、`--assume !<条件>` で満たしていないものとして扱います。複数の条件なら何度でも書けます。

```
/whm test minecraft:generic twilightforest:lich --as Steve --assume advancement:twilightforest:progress_lich --assume !tag:veteran
```
