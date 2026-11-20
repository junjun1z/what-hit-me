# Heracles / Odyssey Quests <Badge type="tip" text="1.3.0+" />

`heracles:<id>` は、プレイヤーがその Heracles のクエストを達成しているときに真になります。Heracles（Team Resourceful 製）は現在 **Odyssey Quests** という名前で公開されていますが、mod ID は `heracles` のままなので、同じ条件がどちらでも使えます。クエスト条件全体のしくみは[クエスト mod](/ja/modpack/quest-mods)を見てください。

## 条件の書き方

```json
"unless": ["heracles:kill_the_lich"]
```

| 形 | 意味 |
|---|---|
| `heracles:<クエスト ID>` | プレイヤーがそのクエストを達成している |
| `!heracles:<クエスト ID>` | 達成していない |

クエスト ID はクエストのファイル名（`.json` を除く）で、クエストブックに表示されるタイトルではありません。

## 対応バージョン

| What Hit Me? の jar（Minecraft） | Heracles / Odyssey Quests |
|---|---|
| Fabric 1.20.1（1.20） | ✔ 1.1.13、✔ 0.0.7（1.20） |
| Forge 1.20.1（1.20） | ✔ 1.1.13、✔ 0.0.7（1.20） |
| NeoForge 1.20.1（Forge 版の jar） | ✔ 1.1.13 |
| Fabric 1.21.1（1.21） | ✔ 2.0.3（1.21.1 と 1.21） |
| NeoForge 1.21.1（1.21） | ✔ 2.0.3（1.21.1） |

✔ = ゲーム内でテスト済み（クエストの前と後でルールを確認し、パックエディターのクエスト一覧も確認）。かっこ内は実際にテストした Minecraft のバージョンです。ほかのバージョンには Heracles のビルドがありません。すべてのクエスト mod の表は[クエスト mod の対応表](/ja/modpack/quest-mods#quest-mod-matrix)にあります。

## ID の調べ方

- **ファイル：** クエストは `config/heracles/quests/`（グループごとのサブフォルダー）にある 1 つずつのファイルです。`.json` を除いたファイル名が ID です：`config/heracles/quests/bosses/kill_the_lich.json` → `heracles:kill_the_lich`。
- **コマンド：** `/heracles complete` と `/heracles reset` は、入力中にクエスト ID の候補を出します。
- **パックエディター：** [条件の選択](/ja/modpack/pack-editor#condition-picker)が、サーバーのクエストをタイトル付きで一覧にします。クリックすると ID が入ります。

## ルールの例

```json
{
  "rules": [
    { "match": "*@twilightforest:lich", "unless": ["heracles:kill_the_lich"], "name": "???", "hint": "", "hide": ["attacker", "mod"] },
    { "match": "*@twilightforest:lich", "hint": "攻略法はクエストブックのリッチのチャプターにある。" }
  ]
}
```

クエストを終えるまでは 1 つ目のルールが選ばれ、その後は 2 つ目がヒントを足します。死亡トリガーで、クエストの前に 1 回だけアイテムを渡すこともできます：

```json
{
  "deathTriggers": [
    { "match": "*@twilightforest:lich", "unless": ["heracles:kill_the_lich"], "command": "give {player} minecraft:golden_apple 1", "oncePerPlayer": true }
  ]
}
```

## バージョンごとの注意

- **公開されているのは 1.20 / 1.20.1 と 1.21 / 1.21.1 だけです。** 1.19.4 のブランチは公開されませんでした。
- **NeoForge 1.21.1** 版は 1.21.1 が必要です（1.21 は不可）。Fabric 版は 1.21 でも動きます。
- **NeoForge 1.20.1** では Heracles の Forge 版の jar が動きます。
- **Odyssey Quests に改名。** 新しいファイルは新しい名前ですが、mod ID `heracles` とクエストの形式は同じです。

## /whm test で確認する

1. `/whm reload`：Heracles が入っていないことや API の不一致がここに出ます。
2. 両方の状態を試します。

```
/whm test minecraft:generic twilightforest:lich --as Steve --assume !heracles:kill_the_lich
/whm test minecraft:generic twilightforest:lich --as Steve --assume heracles:kill_the_lich
```

3. 実際に試すには、クエストを達成・リセットします。

```
/heracles complete kill_the_lich Steve
/heracles reset kill_the_lich Steve
```
