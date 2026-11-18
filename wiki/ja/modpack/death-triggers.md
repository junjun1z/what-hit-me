# 死亡トリガー

`deathTriggers` は、死亡がキーに当てはまったときにサーバーのコマンドを実行します。残念賞のアイテム、お知らせ、スコアボードの集計、スクリプトの呼び出しなどに使えます。

```json
{
  "deathTriggers": [
    { "match": "minecraft:lava", "command": "say {player} fell into lava at {x} {y} {z}", "cooldownSeconds": 60 },
    { "match": "mob:minecraft:creeper", "command": "give {player} minecraft:bread 4", "oncePerPlayer": true }
  ]
}
```

## 項目

| 項目 | 必須 | 初期値 | 意味 |
|---|---|---|---|
| `match` | ○ | | とどめの一撃の[マッチキー](/ja/modpack/match-keys) |
| `command` | ○ | | サーバーのコマンド。先頭の `/` はあってもなくても構いません。 |
| `oncePerPlayer` | | `false` | 各プレイヤーの最初の該当する死亡でだけ実行（`.cache/stats_<uuid>.json` に記録） |
| `cooldownSeconds` | | `0` | 同じプレイヤーで次に実行できるまでの最短時間 |
| `if` / `any` / `unless` <Badge type="tip" text="1.3.0+" /> | | | 死んだプレイヤーの進行度に対する[条件](/ja/modpack/conditions) |
| `memo` <Badge type="tip" text="1.3.0+" /> | | | 自分用のメモ。プレイヤーには表示されません（[パックエディター](/ja/modpack/pack-editor)が書き込みます） |

## プレースホルダー {#placeholders}

| プレースホルダー | 値 |
|---|---|
| `{player}` | 死んだプレイヤーの名前 |
| `{x}` `{y}` `{z}` | 死亡地点のブロック座標（切り捨て） |
| `{damage}` | とどめの一撃の体力ポイント（小数 1 桁） |
| すべての[テンプレート変数](/ja/modpack/templates#variables) <Badge type="tip" text="1.3.0+" /> | たとえば `{attacker_name}`、`{damage_id}`、`{dimension}`、`{weapon}`。表示スイッチに関係なく英語で埋められます。不明な `{...}` はそのまま残ります。 |

::: info コマンドの中の `{damage}`
コマンドの中の `{damage}` は 1.2 と同じく、とどめの一撃の**体力ポイント**です。テンプレートの変数 `{damage}` はハート単位です。
:::

## 実行のされ方

- サーバーだけで（シングルプレイの内蔵サーバーでも）、**コンソール権限**で実行されます。
- 当てはまったトリガーは**すべて**、ファイルの順に実行されます（名前のような「最も具体的なものだけ」ではありません）。
- 当てはまった[進行度ルール](/ja/modpack/rules)に `commands` があれば、トリガーの後に実行されます。

## 死なずに確かめる

```
/whm test minecraft:lava
```

で、実行されるはずのトリガーをプレースホルダーを埋めた状態で表示します。実際には**実行しません**。

## 例

溶岩での死亡回数をスコアボードで数える：

```json
{ "match": "minecraft:lava", "command": "scoreboard players add {player} lava_deaths 1" }
```

新しいプレイヤーにだけ、一度だけガイドブックを案内する：

```json
{ "match": "*", "command": "tellraw {player} {\"text\":\"クエストブックに生き残るコツがあります！\"}", "oncePerPlayer": true, "unless": ["tag:veteran"] }
```

ボスに倒されたことを、攻撃者の名前付きで知らせる：

```json
{ "match": "@twilightforest:*", "command": "say {player} was defeated by {attacker_name} in {dimension}", "cooldownSeconds": 30 }
```
