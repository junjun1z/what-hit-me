# 他の mod との連携

What Hit Me? は、どの mod ともそのまま一緒に動きます。どの mod のダメージも、設定なしで記録し、名前を付けて説明します。そのうえで、下の mod には専用の対応があります。**どれも必須ではありません。** mod が入っていなければ、その機能が使えないだけです。

ここに載せた組み合わせは、すべてゲーム内で起動して試験しています。What Hit Me? のダウンロードは [Modrinth](https://modrinth.com/mod/what-hit-me) または [CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-hit-me) から。

## クエスト・進行度の mod <Badge type="tip" text="1.3.0+" />

[進行度ルール](/ja/modpack/rules)と[死亡トリガー](/ja/modpack/death-triggers)の条件として使います。進行度はサーバーが読むので、マルチプレイでも使え、ごまかせません。

| mod | 条件 | Minecraft / ローダー | 試験した mod のバージョン |
|---|---|---|---|
| **FTB Quests** | `ftbquests:<クエストまたはチャプターの ID>`（チームの進行度） | Forge 1.12.2、1.15.2、1.16.4 / 1.16.5、1.18.2、1.19.2、1.20.1、1.20.4。Fabric 1.16.5、1.18.2、1.19.2、1.20.1、1.20.2、1.20.4、1.21 / 1.21.1、1.21.11、26.1 〜 26.1.2。NeoForge 1.20.4、1.20.6、1.21 / 1.21.1、1.21.11、26.1.2 | 1202.9 〜 26.1.2.10 |
| **Better Questing**（GTNH 版と Better Questing Unofficial も） | `bq:<クエスト ID>` | Forge 1.7.10 〜 1.12.2（各マイナーバージョン）、Forge / NeoForge 1.20.1 | 1.1 〜 3.5、GTNH 3.9.2、Unofficial 4.3.2、4.0.71 |
| **Heracles / Odyssey Quests** | `heracles:<クエスト ID>` | Fabric / Forge 1.20、1.20.1（NeoForge 1.20.1 も）、Fabric 1.21 / 1.21.1、NeoForge 1.21.1 | 0.0.7、1.1.13、2.0.3 |
| **Game Stages** | `stage:<名前>` | Forge 1.11.2 〜 1.20.3、NeoForge 1.20.1 | 1.0.11 〜 17.0.1 |
| バニラの進捗・スコアボードのタグ・スコア | `advancement:`、`tag:`、`score:` | 全バージョン（進捗は 1.12 以降、タグは 1.9 以降） | - |

Minecraft のバージョン・ローダーごとの正確な表と、対応できない組み合わせの理由は[条件とクエスト mod](/ja/modpack/conditions#quest-mod-matrix)にあります。

## スクリプト mod <Badge type="tip" text="1.3.0+" />

スクリプトで `script:` の条件、`{script:}` のテンプレート変数、イベント（`damageRecorded`、`death`、`respawn`、`alert`）、読み取り、実行時の設定を追加できます。[スクリプト連携](/ja/modpack/scripts)を見てください。

| mod | 書き方 | Minecraft / ローダー | スクリプト mod のバージョン |
|---|---|---|---|
| **KubeJS** | グローバル `WhatHitMe`、イベント `WhatHitMeEvents.*`（古い版は `onEvent` / `events.listen`） | NeoForge 26.1.2、1.21.1、1.20.4。Forge / Fabric 1.20.1、1.19.2、1.18.2、1.16.5。Fabric 1.17.1、1.16.1。Forge 1.15.2、1.14.4、1.12.2 | 1.1（1.12.2）〜 8（26.1.2） |
| **CraftTweaker** | ZenScript の `mods.whathitme.WhatHitMe` | Fabric / NeoForge 1.21.1、1.20.6。Forge / Fabric / NeoForge 1.20.4。Forge / Fabric 1.18.2 〜 1.20.2。Forge 1.17.1、1.16.5、1.15.2、1.14.4、1.12.2 | 4.1（1.12.2）、5 〜 21 |
| **MineTweaker 3** / CraftTweaker 3.x（GTNH 版も） | ZenScript の `mods.whathitme.WhatHitMe`（`Api` / `Player` オブジェクト経由） | Forge 1.7.10、1.8.9、1.9.4、1.10.2、1.11.2 | MineTweaker 3.0.10B と CraftTweaker 3.0.9 は一部のみ。CraftTweaker 3.1.0、GTNH 3.5.0 はすべて |

ほかのバージョン（1.21.3 〜 1.21.11、26.2、26.3、Fabric / Forge 1.21 以降の KubeJS）にはスクリプト mod がありません。

::: tip どのバージョンでもスクリプトと組み合わせられます
専用の連携がないバージョンでも、スクリプトでスコアボードのタグやスコアを設定し、ルールの `tag:` / `score:` で判定できます。[死亡トリガー](/ja/modpack/death-triggers)でスクリプト側のコマンドを実行することもできます。
:::

## その他の mod

| mod | What Hit Me? との関係 | Minecraft / ローダー |
|---|---|---|
| **Fabric API** | Fabric では必須（1.21 / 1.21.1 では 0.116 以降） | Fabric の全バージョン |
| **Mod Menu** | mod 一覧から What Hit Me? の設定画面を開ける（任意） | Fabric の全バージョン |
| **体力系の mod**（First Aid のようなもの） | 合計体力が残ったままの死亡に `[部位 / 急所ダメージ]` を付けます。最大体力に対する割合は被弾した時点の最大体力で計算します | 全バージョン |
| **mob に名前を付ける mod**（レベルや亜種。Lycanites Mobs など） | 登録名ではなく、その mod が作った名前を表示します | 全バージョン |
| **死亡を取り消す・巻き戻す mod**（蘇生系） | その瞬間をスナップショットとして保存し、「致命的なダメージが防がれた」通知を出します | 全バージョン |
| **ダメージを与えるすべての mod** | どの mod のダメージ ID や攻撃者も記録。スキャナーなら誰も被弾していないうちに一覧を出せます | 全バージョン |

[死亡画面のパネル](/ja/player/death-screen)、[スキャナー](/ja/modpack/scanner)、[ダメージ ID とバッジ](/ja/reference/damage-ids)も見てください。

## 連携のしくみ

- **必須の依存はありません。** 連携部分は、その mod が入っているときだけ読み込まれます。What Hit Me? を動かすためにクエスト mod やスクリプト mod を入れる必要はありません。
- **バージョンごとの違いに対応しています。** クエスト mod やスクリプト mod はバージョンによって中身の作りが違いますが、古い作りにも新しい作りにも対応しています。
- **合わなくても落ちません。** 入っている mod の版を読み取れなかったときは、その条件を「満たしていない」として扱い、ログと `/whm reload` に警告を 1 回出します。ゲームが落ちることはありません。
- **判定はサーバー側です。** クエストの進み具合とスクリプトはサーバーで確認するので、マルチプレイでも使え、プレイヤー側でごまかすこともできません。
