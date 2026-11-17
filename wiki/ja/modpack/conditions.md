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
| `ftbquests:<id>` | そのクエストかチャプターをプレイヤーのチームが達成している（FTB Quests 1.15.2 はプレイヤー個人）。FTB Quests が表示する 16 進数の ID。`#` は省略可 | `ftbquests:1A2B3C4D5E6F7081` |
| `bq:<id>` | Better Questing のクエストを達成している。1.12.2：数字。GTNH 版（1.7.10）：クエストの UUID（ハイフン付き、またはコマンドが表示する Base64 形式） | `bq:42` |
| `heracles:<id>` | Heracles / Odyssey Quests のクエストを達成している | `heracles:kill_the_lich` |
| `!<条件>` | その条件が偽 | `!tag:veteran` |

リストの組み合わせ方：`if` をすべて満たし、**かつ** `any` を（書いてあれば）少なくとも 1 つ満たし、**かつ** `unless` をどれも満たさないこと。

::: tip スクリプト mod を今すぐ使うには
KubeJS、CraftTweaker、コマンドブロックからでもルールを動かせます。**スコアボードタグ**や**スコア**を設定させて、`tag:` / `score:` の条件で読み取ってください。スクリプトとの直接の連携は[予定](/ja/modpack/scripts)しています。
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
| `stage:` | Game Stages | Game Stages があるすべてのバージョン（1.12.2 - 1.20.x） |
| `ftbquests:` | FTB Quests | 1.12.2 と 1.15.2 以降（公開されている Fabric / Forge / NeoForge） |
| `bq:` | Better Questing | 1.12.2、1.7.10 の GTNH 版 |
| `heracles:` | Heracles（現在の名前は「Odyssey Quests」、mod ID は `heracles`） | 1.20 - 1.21.1 |

### ゲーム内でテスト済み {#tested-in-game}

次の組み合わせは、自動のゲーム内テスト（クエストの進行前と後でルールを確認）で起動・確認しています。

| Minecraft | ローダー | mod とバージョン |
|---|---|---|
| 1.7.10 | Forge | Better Questing 3.9.2-GTNH（GTNH 版） |
| 1.12.2 | Forge | Game Stages 2.0.123、Better Questing 3.5.329、FTB Quests 1202.9.0.15 |
| 1.14.4 | Forge | Game Stages 4.2.15 |
| 1.15.2 | Forge | Game Stages 5.0.2、FTB Quests 1502.0.2.252 |
| 1.16.5 | Forge | Game Stages 7.3.13、FTB Quests 1605.3.7-build.165 |
| 1.16.5 | Fabric | FTB Quests 1605.3.7-build.165 |
| 1.18.2 | Forge | Game Stages 8.1.3、FTB Quests 1802.3.16-build.479 |
| 1.18.2 | Fabric | FTB Quests 1802.3.16-build.479 |
| 1.19.2 | Forge | Game Stages 11.1.4、FTB Quests 1902.5.10-build.497 |
| 1.19.2 | Fabric | FTB Quests 1902.5.10-build.497 |
| 1.19.4 | Forge | Game Stages 13.0.1 |
| 1.20.1 | Forge | Game Stages 15.0.2、FTB Quests 2001.4.22、Heracles 1.1.13 |
| 1.20.1 | Fabric | FTB Quests 2001.4.22、Heracles 1.1.13 |
| 1.20.1 | NeoForge | Heracles 1.1.13 |
| 1.20.2 | Forge | Game Stages 16.0.1 |
| 1.20.3 | Forge | Game Stages 17.0.1 |
| 1.20.4 | NeoForge | FTB Quests 2004.2.3 |
| 1.21.1 | Fabric | FTB Quests 2101.1.38 |

まだゲーム内でテストしていないもの（コードは対応済みで、mod の API との照合のみ）：NeoForge 1.21.1 の FTB Quests 2101.x、FTB Quests 2111.x（1.21.11）と 26.1.2.x（26.1 - 26.1.2）、Odyssey Quests 2.0.3（1.21.1）。Game Stages と Better Questing は 1.21 以降には存在せず、Heracles は 1.20.6 にはありません。

## クエスト ID の調べ方

| mod | ID の場所 |
|---|---|
| FTB Quests | クエストかチャプターの 16 進数の ID（FTB Quests の編集モードでコピーできます）。`#` は付けても付けなくても構いません。 |
| Better Questing 1.12.2 | 数字のクエスト ID（クエストエディター、`/bq_admin` コマンド） |
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
