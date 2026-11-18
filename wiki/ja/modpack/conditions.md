# 条件とクエスト mod <Badge type="tip" text="1.3.0+" />

条件は[進行度ルール](/ja/modpack/rules)（`if`、`any`、`unless`）と[死亡トリガー](/ja/modpack/death-triggers)で使います。1 つの条件は `接頭辞:引数` の形で、先頭に `!` を付けると否定になります。

```json
"if":     ["advancement:minecraft:story/enter_the_nether", "score:deaths>=3"],
"any":    ["stage:nether_unlocked", "tag:nether_guide"],
"unless": ["ftbquests:1A2B3C4D5E6F7081"]
```

## すべての条件

| 条件 | 真になるとき | 例 |
|---|---|---|
| `advancement:<id>` | プレイヤーがその進捗を達成している | `advancement:minecraft:end/kill_dragon` |
| `tag:<タグ>` | プレイヤーがそのスコアボードタグを持っている（`/tag` コマンド） | `tag:veteran` |
| `score:<目的><演算子><数>` | プレイヤーのスコアの比較が真。演算子：`>=` `>` `<=` `<` `=` `!=`。スコアがなければ偽 | `score:deaths>=3` |
| `stage:<名前>` | プレイヤーが Game Stages のそのステージを持っている | `stage:nether` |
| `ftbquests:<id>` | そのクエストかチャプターをプレイヤーのチームが達成している（FTB Quests 1.15.2 / 1604 はプレイヤー個人）。FTB Quests が表示する 16 進数の ID。`#` は省略可 | `ftbquests:1A2B3C4D5E6F7081` |
| `bq:<id>` | Better Questing のクエストを達成している。数字（クエストブックや `/bq_admin` が表示するクエスト ID）。GTNH 版（1.7.10）：クエストの UUID（ハイフン付き、またはコマンドが表示する Base64 形式） | `bq:42` |
| `heracles:<id>` | Heracles / Odyssey Quests のクエストを達成している | `heracles:kill_the_lich` |
| `script:<name>` | KubeJS / CraftTweaker のスクリプトが登録した条件（[スクリプト連携](/ja/modpack/scripts)）。未登録の名前は偽で、`/whm reload` で警告 | `script:veteran` |
| `!<条件>` | その条件が偽 | `!tag:veteran` |

リストの組み合わせ方：`if` をすべて満たし、**かつ** `any` を（書いてあれば）少なくとも 1 つ満たし、**かつ** `unless` をどれも満たさないこと。

::: tip スクリプト mod を今すぐ使うには
KubeJS、CraftTweaker、コマンドブロックからでもルールを動かせます。**スコアボードタグ**や**スコア**を設定させて、`tag:` / `score:` の条件で読み取ってください。連携があるバージョンでは、スクリプトが `script:` 条件を直接登録できます（[スクリプト連携](/ja/modpack/scripts)）。
:::

## Minecraft のバージョンごとの組み込み条件

| 条件 | 1.7.10、1.8.9 | 1.9 - 1.11.2 | 1.12+ |
|---|---|---|---|
| `advancement:` | 非対応 | 非対応 | ✔ |
| `tag:` | 非対応 | ✔ | ✔ |
| `score:` | ✔ | ✔ | ✔ |

Forge・Fabric・NeoForge で同じです。進捗は 1.12 より前に、エンティティのタグは 1.9 より前に存在しません。非対応の条件は偽になり、`/whm reload` が「not supported on Forge 1.x」と表示します。

## クエスト mod {#quest-mods}

クエスト mod の条件はリフレクションで動きます。**前提 mod になることはありません。** その mod が入っているときだけ動き始め、mod がない場合や API が合わない場合は条件が偽になり、ログに 1 回だけ記録されます。`/whm reload` でも表示されます。

| 接頭辞 | mod | Minecraft のバージョン |
|---|---|---|
| `stage:` | Game Stages | Forge 1.11.2 〜 1.20.3、NeoForge 1.20.1（Game Stages は Forge 用しかありません） |
| `ftbquests:` | FTB Quests | 1.12.2、1.15.2、1.16.4 〜 1.21.1、1.21.11、26.1 〜 26.1.2（公開されているもの。表を参照） |
| `bq:` | Better Questing | Forge 1.7.10 〜 1.12.2（GTNH 版と Better Questing Unofficial も）、Forge / NeoForge 1.20.1（Better Questing 4） |
| `heracles:` | Heracles（現在の名前は「Odyssey Quests」、mod ID は `heracles`） | 1.20 / 1.20.1（Fabric、Forge、Forge 版を使う NeoForge）、1.21 / 1.21.1（Fabric）、1.21.1（NeoForge） |

### クエスト mod の対応表 {#quest-mod-matrix}

What Hit Me? が対応するすべての Minecraft バージョンとローダーについて、Modrinth、CurseForge、各 mod の Maven リポジトリで確認しました（2026 年 11 月）。
✔ = 自動のゲーム内テスト（クエストの進行前と後でルールを確認し、パックエディターの条件リストも確認）で確認済み。jar が複数のバージョンに対応する場合、
かっこ内は実際にテストした Minecraft のバージョンです。– = そのクエスト mod のビルドがない。✘ = ビルドはあるが、そこでは動かない。

| What Hit Me? の jar（Minecraft） | FTB Quests | Better Questing | Heracles / Odyssey Quests | Game Stages |
|---|---|---|---|---|
| Forge 1.7.10 | – | ✔ 3.0.328、✔ 3.9.2-GTNH（GTNH 版） | – | – |
| Forge 1.8.9 | – | ✔ 1.1.159 | – | – |
| Forge 1.9.4（1.9） | – | ✔ 1.2.160、✔ 1.2.114（1.9） | – | – |
| Forge 1.10.2（1.10） | – | ✔ 3.3.298、✔ 1.3.140（1.10） | – | – |
| Forge 1.11.2（1.11） | – | ✔ 2.4.235、✔ 2.4.187（1.11） | – | ✔ 1.0.11 |
| Forge 1.12.2（1.12、1.12.1） | ✔ 1202.9.0.15（1.12.2） | ✔ 3.5.329、✔ Better Questing Unofficial 4.3.2、✔ 2.5.236（1.12.1） | – | ✔ 2.0.123、✔ 1.0.62（1.12.1） |
| Forge 1.14.4 | – | – | – | ✔ 4.2.15 |
| Forge 1.15.2 | ✔ 1502.0.2.252 | – | – | ✔ 5.0.2 |
| Fabric 1.16.5（1.16.2 〜 1.16.4） | ✔ 1605.3.7（1.16.5） | – | – | – |
| Forge 1.16.5（1.16.3、1.16.4） | ✔ 1605.3.7、✔ 1604.1.1.21（1.16.4） | – | – | ✔ 7.3.13、✔ 6.0.4（1.16.4）、✔ 5.0.1（1.16.3） |
| Fabric 1.18.2 | ✔ 1802.3.16 | – | – | – |
| Forge 1.18.2 | ✔ 1802.3.16 | – | – | ✔ 8.1.3 |
| Fabric 1.19.2（1.19、1.19.1） | ✔ 1902.5.10（1.19.2） | – | – | – |
| Forge 1.19.2（1.19、1.19.1） | ✔ 1902.5.10（1.19.2） | – | – | ✔ 11.1.4、✔ 9.1.2（1.19）、✔ 10.0.1（1.19.1） |
| Forge 1.19.4 | – | – | – | ✔ 13.0.1 |
| Fabric 1.20.1（1.20） | ✔ 2001.4.22（1.20.1） | – | ✔ 1.1.13、✔ 0.0.7（1.20） | – |
| Forge 1.20.1（1.20） | ✔ 2001.4.22（1.20.1） | ✔ 4.0.71 | ✔ 1.1.13、✔ 0.0.7（1.20） | ✔ 15.0.2、✔ 14.0.10（1.20） |
| NeoForge 1.20.1（Forge 版の jar） | ✘ | ✔ 4.0.71 | ✔ 1.1.13 | ✔ 15.0.2 |
| Fabric 1.20.2 | ✔ 2002.2.0 スナップショット | – | – | – |
| Forge 1.20.2 | – | – | – | ✔ 16.0.1 |
| Fabric 1.20.4（1.20.3） | ✔ 2004.2.3（1.20.4） | – | – | – |
| Forge 1.20.4（1.20.3） | ✔ 2004.2.3（1.20.4） | – | – | ✔ 17.0.1（1.20.3） |
| NeoForge 1.20.4 | ✔ 2004.2.3 | – | – | – |
| Fabric 1.20.6（1.20.5） | ✘ | – | – | – |
| NeoForge 1.20.6 | ✔ 2006.1.0 スナップショット | – | – | – |
| Fabric 1.21.1（1.21） | ✔ 2101.1.39、✔ 2100.1.4（1.21） | – | ✔ 2.0.3（1.21.1 と 1.21） | – |
| NeoForge 1.21.1（1.21） | ✔ 2101.1.39、✔ 2100.1.4（1.21） | – | ✔ 2.0.3（1.21.1） | – |
| Fabric、NeoForge 1.21.11 | ✔ 2111.1.5 | – | – | – |
| Fabric 26.1.2（26.1、26.1.1） | ✔ 26.1.2.10（26.1.2 と 26.1） | – | – | – |
| NeoForge 26.1.2（26.1、26.1.1） | ✔ 26.1.2.10（26.1.2） | – | – | – |

クエスト mod がひとつもないもの：Fabric 1.14.4、1.15.2、1.16.1、1.17.1、Forge 1.17.1、1.19.3、Fabric 1.19.4、Forge 1.20.6、
1.21.1 以降のすべての Forge、1.21.3 〜 1.21.10、26.2、26.3。

対応していないバージョンがある理由：

- **FTB Quests** は、作られた Minecraft バージョンでしか動きません。1.19.2（1.19 / 1.19.1 は不可）、1.20.1（1.20 は不可）、1.20.4（Forge の 1.20.3 は不可）、
  NeoForge の 26.1.2（26.1 / 26.1.1 は不可）です。Fabric 1.16.2 〜 1.16.4 は、Fabric 版の FTB Teams が 1.16.5 を必要とします。NeoForge 1.20.1 は、
  FTB Library 2001 が Forge 47.3 を必要とし、NeoForge 1.20.1 はそのバージョンを名乗らないため動きません。1.14、1.17、1.19.3、1.19.4、1.20.4 より後の Forge、
  1.21.2 〜 1.21.10、26.2、26.3 用の FTB Quests はありません。1.20.2 と 1.20.6 は FTB の Maven に未公開のスナップショットがあるだけで、Fabric 1.20.6 の
  スナップショットはそれ自体が起動時にクラッシュします。Fabric 26.1 では、FTB Library が Fabric API 0.149.1 以降を必要とします（0.155.3 で動きます）。
- **Better Questing** は Fabric 版と 1.13 〜 1.19 版がありません。1.20.1 版（4.x）は Forge 用のアルファ版です（NeoForge 1.20.1 でも動きます）。
- **Heracles / Odyssey Quests** は 1.20 / 1.20.1 と 1.21.1 用だけが公開されています（1.19.4 のブランチは公開されませんでした）。NeoForge 1.21.1 版は
  1.21.1 が必要で、Fabric 版は 1.21 でも動きます。
- **Game Stages** は Forge 用だけで、1.20.3 までです（1.20.1 版は NeoForge にも対応と書かれています）。新しいバージョン向けの似たステージ mod
  （AStages、ProgressiveStages、SDM GameStages など）は独自の API なので `stage:` では使えません。それらの mod でスコアボードのタグやスコアを設定し、
  `tag:` / `score:` で確認してください。

## クエスト ID の調べ方

| mod | ID の場所 |
|---|---|
| FTB Quests | クエストかチャプターの 16 進数の ID（FTB Quests の編集モードでコピーできます）。`#` は付けても付けなくても構いません。 |
| Better Questing（1.7.10 〜 1.12.2、1.20.1） | 数字のクエスト ID（クエストエディター、`/bq_admin` コマンド）。1.20.1（Better Questing 4）ではクエストブックに表示される短い ID を使います。 |
| Better Questing GTNH 版 | クエストの UUID。`/bq_admin` コマンドが表示する Base64 形式も使えます |
| Heracles / Odyssey Quests | クエスト ID（`config/heracles/quests/` にあるクエストのファイル名） |
| Game Stages | `/gamestage add` で使うステージ名 |

## 条件の確認

- `/whm reload` が、不明な接頭辞、接頭辞のない条件、入っていない mod、このバージョンで使えない条件、引数の誤り（形の崩れた `score:` など）を一覧にします。
- `/whm test <damage_id> ... --as <player>` で、そのプレイヤーの実際の進行度でルールを評価します。
- `--assume <条件>` で条件を満たしたものとして、`--assume !<条件>` で満たしていないものとして扱います。複数の条件なら何度でも書けます。

```
/whm test minecraft:generic twilightforest:lich --as Steve --assume stage:twilight --assume !tag:veteran
```
