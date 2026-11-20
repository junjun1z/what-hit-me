# Game Stages <Badge type="tip" text="1.3.0+" />

`stage:<名前>` は、プレイヤーがその Game Stages のステージを持っているときに真になります。ステージはクエストの報酬（FTB Quests、Better Questing）、進捗、スクリプトで与えることが多いので、段階的に要素を解放するパックでは便利なスイッチになります。クエスト条件全体のしくみは[クエスト mod](/ja/modpack/quest-mods)を見てください。

## 条件の書き方

```json
"if": ["stage:nether"]
```

| 形 | 意味 |
|---|---|
| `stage:<名前>` | プレイヤーがそのステージを持っている |
| `!stage:<名前>` | 持っていない |

名前は `/gamestage add` で使うものです。Game Stages での書き方のとおりに書いてください。

## 対応バージョン

| What Hit Me? の jar（Minecraft） | Game Stages |
|---|---|
| Forge 1.11.2（1.11） | ✔ 1.0.11 |
| Forge 1.12.2（1.12、1.12.1） | ✔ 2.0.123、✔ 1.0.62（1.12.1） |
| Forge 1.14.4 | ✔ 4.2.15 |
| Forge 1.15.2 | ✔ 5.0.2 |
| Forge 1.16.5（1.16.3、1.16.4） | ✔ 7.3.13、✔ 6.0.4（1.16.4）、✔ 5.0.1（1.16.3） |
| Forge 1.18.2 | ✔ 8.1.3 |
| Forge 1.19.2（1.19、1.19.1） | ✔ 11.1.4、✔ 9.1.2（1.19）、✔ 10.0.1（1.19.1） |
| Forge 1.19.4 | ✔ 13.0.1 |
| Forge 1.20.1（1.20） | ✔ 15.0.2、✔ 14.0.10（1.20） |
| NeoForge 1.20.1（Forge 版の jar） | ✔ 15.0.2 |
| Forge 1.20.2 | ✔ 16.0.1 |
| Forge 1.20.4（1.20.3） | ✔ 17.0.1（1.20.3） |

✔ = ゲーム内でテスト済み（ステージの前と後でルールを確認し、パックエディターのステージ一覧も確認）。かっこ内は実際にテストした Minecraft のバージョンです。ほかのバージョンには Game Stages のビルドがありません。すべてのクエスト mod の表は[クエスト mod の対応表](/ja/modpack/quest-mods#quest-mod-matrix)にあります。

## 名前の調べ方

- **パックの中：** ステージ名は、クエスト・スクリプト・コマンド（`/gamestage add <player> <ステージ>`）で与えている名前です。
- **コマンド：** `/gamestage info` で自分のステージ、`/gamestage check <player> <ステージ>` で 1 つのステージを確認できます。
- **パックエディター：** [条件の選択](/ja/modpack/pack-editor#condition-picker)が、Game Stages が知っているステージ（既知のステージの一覧。1.11.2 〜 1.12.1 用の Game Stages 1.0.x にはありません）を一覧にします。どんな名前でも入力できます。

## ルールの例

`nether` ステージを持つまでネザーの mob は `???`、`iron_age` ステージまでは溶岩に初心者向けのヒント：

```json
{
  "rules": [
    { "match": "*@minecraft:blaze", "unless": ["stage:nether"], "name": "???", "hint": "" },
    { "match": "minecraft:lava", "unless": ["stage:iron_age"], "hint": "水入りバケツで火を消せる。バケツは鉄インゴット 3 個で作れる。" }
  ]
}
```

クエスト mod との組み合わせ：ステージを持っていて、**かつ** FTB Quests のチャプターをまだ終えていないとき：

```json
{
  "rules": [
    { "match": "*@cataclysm:ignis", "if": ["stage:nether"], "unless": ["ftbquests:2C7E0B91A4D35F18"], "name": "???" }
  ]
}
```

## バージョンごとの注意

- **Forge 用だけ**で、1.20.3 までです。1.20.1 版は NeoForge にも対応と書かれており、同じ jar が NeoForge 1.20.1 で動きます。
- **2 つの API：** Game Stages 1.0.x（1.11.2 〜 1.12.1）と 2.x 以降（1.12.2 から）。What Hit Me? は入っているほうを使います。
- **ほかのステージ mod**（新しいバージョン向けの AStages、ProgressiveStages、SDM GameStages など）は独自の API なので `stage:` では読めません。それらの mod でスコアボードのタグやスコアを設定し、`tag:` / `score:` を使ってください（[条件](/ja/modpack/conditions)）。

## /whm test で確認する

1. `/whm reload`：Game Stages が入っていないことがここに出ます。
2. 両方の状態を試します。

```
/whm test minecraft:generic minecraft:blaze --as Steve --assume !stage:nether
/whm test minecraft:generic minecraft:blaze --as Steve --assume stage:nether
```

3. 実際に試すには：

```
/gamestage add Steve nether
/gamestage remove Steve nether
```
