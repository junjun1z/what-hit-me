# FTB Quests <Badge type="tip" text="1.3.0+" />

`ftbquests:<id>` は、そのクエストかチャプターをプレイヤーのチームが達成しているときに真になります。FTB Quests 1.15.2（1502）と 1.16.4（1604）では、進行度はチームではなくプレイヤー個人のものです。クエスト条件全体のしくみは[クエスト mod](/ja/modpack/quest-mods)を見てください。

## 条件の書き方

```json
"unless": ["ftbquests:4F2A9C1D7B3E6A05"]
```

| 形 | 意味 |
|---|---|
| `ftbquests:<16 進数の ID>` | その ID のクエストかチャプターを達成している。大文字でも小文字でも構いません。 |
| `ftbquests:#<16 進数の ID>` | 同じ。FTB Quests が ID の前に表示する `#` は省略できます。 |
| `!ftbquests:<16 進数の ID>` | 達成していない |

チャプターは、FTB Quests が達成済みと判定したとき（中のクエストをすべて終えたとき）に達成になります。

## 対応バージョン

| What Hit Me? の jar（Minecraft） | FTB Quests |
|---|---|
| Forge 1.12.2（1.12、1.12.1） | ✔ 1202.9.0.15（1.12.2） |
| Forge 1.15.2 | ✔ 1502.0.2.252 |
| Fabric 1.16.5（1.16.2 〜 1.16.4） | ✔ 1605.3.7（1.16.5） |
| Forge 1.16.5（1.16.3、1.16.4） | ✔ 1605.3.7、✔ 1604.1.1.21（1.16.4） |
| Fabric 1.18.2 | ✔ 1802.3.16 |
| Forge 1.18.2 | ✔ 1802.3.16 |
| Fabric 1.19.2（1.19、1.19.1） | ✔ 1902.5.10（1.19.2） |
| Forge 1.19.2（1.19、1.19.1） | ✔ 1902.5.10（1.19.2） |
| Fabric 1.20.1（1.20） | ✔ 2001.4.22（1.20.1） |
| Forge 1.20.1（1.20） | ✔ 2001.4.22（1.20.1） |
| NeoForge 1.20.1（Forge 版の jar） | ✘ |
| Fabric 1.20.2 | ✔ 2002.2.0 スナップショット |
| Fabric 1.20.4（1.20.3） | ✔ 2004.2.3（1.20.4） |
| Forge 1.20.4（1.20.3） | ✔ 2004.2.3（1.20.4） |
| NeoForge 1.20.4 | ✔ 2004.2.3 |
| Fabric 1.20.6（1.20.5） | ✘ |
| NeoForge 1.20.6 | ✔ 2006.1.0 スナップショット |
| Fabric 1.21.1（1.21） | ✔ 2101.1.39、✔ 2100.1.4（1.21） |
| NeoForge 1.21.1（1.21） | ✔ 2101.1.39、✔ 2100.1.4（1.21） |
| Fabric、NeoForge 1.21.11 | ✔ 2111.1.5 |
| Fabric 26.1.2（26.1、26.1.1） | ✔ 26.1.2.10（26.1.2 と 26.1） |
| NeoForge 26.1.2（26.1、26.1.1） | ✔ 26.1.2.10（26.1.2） |

✔ = ゲーム内でテスト済み（クエストの前と後でルールを確認し、パックエディターのクエスト一覧も確認）。かっこ内は実際にテストした Minecraft のバージョンです。✘ = ビルドはあるが、そこでは動かない。ほかのバージョンには FTB Quests のビルドがありません。すべてのクエスト mod の表は[クエスト mod の対応表](/ja/modpack/quest-mods#quest-mod-matrix)にあります。

## ID の調べ方

- **クエストブックで：** 編集モードにして、クエストかチャプターを右クリックし、ID をコピーします。`4F2A9C1D7B3E6A05` のような 16 桁の 16 進数です。
- **ファイルで：** `config/ftbquests/quests/chapters/*.snbt` の各クエストに `id:` があります（1.16 以降）。
- **パックエディターで：** [条件の選択](/ja/modpack/pack-editor#condition-picker)が、サーバーのチャプターとクエストをタイトル付きで一覧にします。クリックすると ID が入ります。

![パックエディターの条件の選択。サーバーの FTB Quests のチャプターとクエストが並ぶ](/images/ja/pack-editor-condition-ftbquests.webp)

## ルールの例

チームがボスのクエストを終えるまで、ボスは `???` でヒントもなし：

```json
{
  "rules": [
    {
      "match": "*@twilightforest:naga",
      "unless": ["ftbquests:4F2A9C1D7B3E6A05"],
      "name": "???",
      "hint": "",
      "hide": ["attacker", "mod", "weapon"]
    }
  ]
}
```

「ネザー」チャプターの後だけ出るヒントと、クエストの前にボスに倒されたときの 1 回だけの報酬：

```json
{
  "rules": [
    { "match": "minecraft:lava", "if": ["ftbquests:1B6D0E42C9A7F830"], "hint": "ネザライトの装備は溶岩に浮く。表面を探そう。" },
    { "match": "*@twilightforest:naga", "unless": ["ftbquests:4F2A9C1D7B3E6A05"], "commands": "give {player} minecraft:golden_apple 2", "oncePerPlayer": true }
  ]
}
```

同じ死亡の、クエストの前と後：

![ボスのクエストの前：攻撃者は ??? で隠れ、ヒントもない](/images/ja/rules-before-ftb.webp)
![ボスのクエストの後：攻撃者、その mod、ヒントまで表示される](/images/ja/rules-after-ftb.webp)

## バージョンごとの注意

- **チームか個人か。** 1.16.5（1605）からは進行度が FTB Teams のチームのものになり、チームの 1 人が終えたクエストはチーム全員の達成になります。1.12.2 では FTB Library がプレイヤーごとにチームを作ります。1.15.2（1502）と 1.16.4（1604）は個人の進行度です。
- **Minecraft のバージョンが完全に一致する必要があります。** FTB Quests は作られたバージョンでしか動きません。1.19.2（1.19 / 1.19.1 は不可）、1.20.1（1.20 は不可）、1.20.4（Forge の 1.20.3 は不可）、NeoForge の 26.1.2（26.1 / 26.1.1 は不可）です。
- **Fabric 1.16.2 〜 1.16.4：** Fabric 版の FTB Teams が 1.16.5 を必要とします。
- **NeoForge 1.20.1：** FTB Library 2001 が Forge 47.3 を必要とし、NeoForge 1.20.1 はそのバージョンを名乗らないため、FTB Quests が起動しません。
- **ビルドがない：** 1.14、1.17、1.19.3、1.19.4、1.20.4 より後の Forge、1.21.2 〜 1.21.10、26.2、26.3。
- **スナップショット：** 1.20.2 と 1.20.6 は FTB の Maven に未公開のスナップショットがあるだけです。Fabric 1.20.6 のスナップショットはそれ自体が起動時にクラッシュします。
- **Fabric 26.1：** FTB Library が Fabric API 0.149.1 以降を必要とします（0.155.3 で動きます）。

## /whm test で確認する

1. `/whm reload`：`ftbquests:` についての警告がなければ、ID の形は正しく、FTB Quests も見つかっています。
2. セーブデータに触れずに、両方の状態を試します。

```
/whm test minecraft:generic twilightforest:naga --as Steve --assume !ftbquests:4F2A9C1D7B3E6A05
/whm test minecraft:generic twilightforest:naga --as Steve --assume ftbquests:4F2A9C1D7B3E6A05
```

3. 実際に試すには、FTB Quests のコマンドでクエストを達成・リセットします（1.16 以降）。

```
/ftbquests change_progress Steve complete 4F2A9C1D7B3E6A05
/ftbquests change_progress Steve reset 4F2A9C1D7B3E6A05
```

1.12.2 ではチーム ID を指定します：`/ftbquests change_progress complete <チーム> <id>`。
