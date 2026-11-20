# Better Questing <Badge type="tip" text="1.3.0+" />

`bq:<id>` は、プレイヤーがその Better Questing のクエストを達成しているときに真になります。Forge 1.7.10 〜 1.12.2 の 1.x 〜 3.x、**GTNH 版**（1.7.10）、**Better Questing Unofficial**（1.12.2）、**Better Questing 4**（1.20.1）のすべての世代に対応しています。クエスト条件全体のしくみは[クエスト mod](/ja/modpack/quest-mods)を見てください。

## 条件の書き方

```json
"unless": ["bq:42"]
```

| mod | 形 | 例 |
|---|---|---|
| Better Questing 1.x 〜 3.x、Better Questing Unofficial | 数字のクエスト ID | `bq:42` |
| Better Questing 4（1.20.1） | クエストブックに表示される短いクエスト ID | `bq:42` |
| GTNH 版（1.7.10） | クエストの UUID（ハイフン付き、またはコマンドが表示する Base64 形式） | `bq:0f6c3c4e-2a1b-4d7e-9c55-8b1e2d3a4f60` |
| すべて | `!bq:<id>` = 達成していない | `!bq:42` |

## 対応バージョン

| What Hit Me? の jar（Minecraft） | Better Questing |
|---|---|
| Forge 1.7.10 | ✔ 3.0.328、✔ 3.9.2-GTNH（GTNH 版） |
| Forge 1.8.9 | ✔ 1.1.159 |
| Forge 1.9.4（1.9） | ✔ 1.2.160、✔ 1.2.114（1.9） |
| Forge 1.10.2（1.10） | ✔ 3.3.298、✔ 1.3.140（1.10） |
| Forge 1.11.2（1.11） | ✔ 2.4.235、✔ 2.4.187（1.11） |
| Forge 1.12.2（1.12、1.12.1） | ✔ 3.5.329、✔ Better Questing Unofficial 4.3.2、✔ 2.5.236（1.12.1） |
| Forge 1.20.1（1.20） | ✔ 4.0.71 |
| NeoForge 1.20.1（Forge 版の jar） | ✔ 4.0.71 |

✔ = ゲーム内でテスト済み（クエストの前と後でルールを確認し、パックエディターのクエスト一覧も確認）。かっこ内は実際にテストした Minecraft のバージョンです。ほかのバージョンには Better Questing のビルドがありません。すべてのクエスト mod の表は[クエスト mod の対応表](/ja/modpack/quest-mods#quest-mod-matrix)にあります。

## ID の調べ方

- **クエストエディター / クエストブック：** 編集モードのクエストエディターにクエスト ID が表示されます。Better Questing 4 ではクエストブックに短い ID が表示されます。
- **コマンド：** `/bq_admin` のコマンドがクエスト ID を表示し、受け付けます。GTNH 版は UUID を Base64 形式で表示します。What Hit Me? はその形もハイフン付きの UUID も受け付けます。
- **ファイル：** クエストのデータベース（`config/betterquesting/DefaultQuests.json`、GTNH 版は `DefaultQuests/` フォルダー）に ID があります。
- **パックエディター：** [条件の選択](/ja/modpack/pack-editor#condition-picker)が、サーバーのクエストを名前付きで一覧にします（GTNH 版も）。クリックすると ID が入ります。

## ルールの例

クエスト 128 を終えるまで、ボスは `???`：

```json
{
  "rules": [
    { "match": "*@twilightforest:lich", "unless": ["bq:128"], "name": "???", "hint": "", "hide": ["attacker", "mod"] }
  ]
}
```

GTNH 版で、溶岩で死んだときにクエストを案内するヒント。そのクエストを終えるまで表示します：

```json
{
  "rules": [
    {
      "match": "minecraft:lava",
      "unless": ["bq:9d2c11e0-6b3f-4a58-b2c4-0e7f1a6d8c35"],
      "hint": "クエスト「耐火」をやろう。報酬は耐火のポーション 1 スタック。"
    }
  ]
}
```

1.7.10 / 1.8.9 のバニラにはスコアボードタグがありません。クエストとはスコアを組み合わせてください：`"if": ["bq:42", "score:deaths>=3"]`。

## バージョンごとの注意

- **すべての API 世代：** 1.x（1.8.9、1.9.4：static なクエストデータベース）、2.x / 3.x（`QuestingAPI`、1.10.2 〜 1.12.2 と 1.7.10）、GTNH 版（UUID の ID）、4.x（1.20.1、独自の進行度管理を持つ新しいコード）。What Hit Me? は実行時に世代を判別します。
- **Better Questing Unofficial**（1.12.2）は 3.x と同じように動きます。
- **Fabric 版**と 1.13 〜 1.19 版はありません。
- **Better Questing 4**（1.20.1）は Forge 用のアルファ版です。同じ jar が NeoForge 1.20.1 でも動きます。
- **1.7.10 / 1.8.9：** `tag:` の代わりに `score:` と組み合わせてください（1.9 より前にエンティティのタグはありません）。[条件](/ja/modpack/conditions#built-in-conditions-per-minecraft-version)を参照してください。

## /whm test で確認する

1. `/whm reload`：形の崩れた ID（数字でも UUID でもない）や、Better Questing が入っていないことがここに出ます。
2. 両方の状態を試します。

```
/whm test minecraft:generic twilightforest:lich --as Steve --assume !bq:128
/whm test minecraft:generic twilightforest:lich --as Steve --assume bq:128
```

3. 実際に試すには、クエストを達成・リセットします。

```
/bq_admin complete 128 Steve
/bq_admin reset 128 Steve
```

Better Questing 4（1.20.1）のリセットは `/bq_admin reset Steve 128` です。GTNH 版のコマンドには Base64 形式の ID を使います。
