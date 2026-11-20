# 進行度ルール <Badge type="tip" text="1.3.0+" />

進行度ルールは、**プレイヤーの進み具合に応じて**死亡時の表示を変えます。代表的な例は、ボスのクエストを終えるまで、そのボスを `???` と表示し、ヒントも詳細も出さない使い方です。

判定するのはサーバーです。サーバーは出力を作るたびにプレイヤーごとにルールを評価し、完成した結果だけを送ります。隠した情報がクライアントに届くことはありません。tick ごとの処理もありません。

## 最初のルール

![進捗の前：攻撃者は ??? で隠れ、ヒントもない](/images/ja/rules-before.webp)
![進捗の後：同じ死亡で、攻撃者・パックの名前・ヒントが表示される](/images/ja/rules-after.webp)

`config/whathitme/pack/rules/bosses.json`：

```json
{
  "rules": [
    {
      "match": "*@twilightforest:lich",
      "unless": ["advancement:twilightforest:progress_lich"],
      "name": "&k???",
      "hint": "",
      "hide": ["damage_id", "attacker", "mod", "weapon", "death_message"]
    }
  ]
}
```

プレイヤーが進捗 `twilightforest:progress_lich` を達成するまでは：

- リッチの攻撃は `???`（読めない文字）と表示され、
- ヒントは出ず、
- ダメージ ID・攻撃者・mod・武器・死亡メッセージは、送る前に取り除かれます。

進捗を達成すると、**過去の記録も含めて** `/whm` などで普通に表示されます。ルールは出力を作るたびに評価されるからです。

確認：`/whm reload` の後に

```
/whm test minecraft:generic twilightforest:lich --as Steve
/whm test minecraft:generic twilightforest:lich --as Steve --assume advancement:twilightforest:progress_lich
```

## 項目

| 項目 | 形式 | 初期値 | 意味 |
|---|---|---|---|
| `match` | 文字列 | `*` | 記録の[マッチキー](/ja/modpack/match-keys) |
| `if` | 条件 1 つかリスト | | **すべて**満たす必要がある |
| `any` | 条件 1 つかリスト | | **少なくとも 1 つ**満たす必要がある |
| `unless` | 条件 1 つかリスト | | **どれも**満たしていてはいけない |
| `name` | 文字列 | | 名前を置き換える。テンプレート変数、`&` の色、`lang:` キーが使える |
| `hint` | 文字列 | | ヒントを置き換える。`""` = **ヒントなし** |
| `hide` | 文字列かリスト | | 取り除く項目（下の表） |
| `commands` | 文字列かリスト | | 該当する**死亡**のときに実行するサーバーコマンド（`command` でも可） |
| `enforce` | 真偽値 | `hide` があれば `true`、なければ `false` | 個人の名前・ヒント・テンプレートを使わせない |
| `oncePerPlayer` | 真偽値 | `false` | `commands` をプレイヤーごとに 1 回だけ実行 |
| `cooldownSeconds` | 数値 | `0` | `commands` のプレイヤーごとのクールダウン |
| `memo` | 文字列 | | 自分用のメモ。プレイヤーには表示されません（[パックエディター](/ja/modpack/pack-editor)が書き込みます） |

条件の詳細は[条件](/ja/modpack/conditions)（バニラ）と[クエスト mod](/ja/modpack/quest-mods)にあります。

## ルールの選ばれ方 {#how-rules-are-chosen}

1. ルールは [pack ファイルの順](/ja/modpack/pack-folder#how-files-are-read)に、ファイルの中では上から順に読まれます。
2. 各記録について、`match` が合い、**かつ**条件を満たす**最初の**ルールが使われます。それより後のルールは見ません。
3. どのルールにも当てはまらなければ、ルールがない場合と同じです（renameMap、hintMap、テンプレートはいつもどおり）。

::: warning 「最も具体的」ではなく「最初に当てはまったもの」
`renameMap` と違い、ルールは具体性で選びません。特別なルールを一般的なルールより**前に**書いてください。

```json
"rules": [
  { "match": "twilightforest:lich_bolt @twilightforest:lich", "unless": ["advancement:twilightforest:progress_lich"], "name": "奇妙な弾" },
  { "match": "*@twilightforest:lich", "unless": ["advancement:twilightforest:progress_lich"], "name": "???" }
]
```
:::

## `name` と `hint`

- 当てはまったルールの `name` / `hint` は、その記録について `renameMap` / `hintMap` より優先されます。
- `enforce` がなければ、プレイヤーの**個人用**の名前 / ヒントがさらに優先されます。
- `"hint": ""` はヒントを完全に消します（自動ヒントも出ません）。
- どちらもテンプレート変数が使えます：`"name": "正体不明の{damage_name}"`

## `hide`

| 項目 | 取り除くもの |
|---|---|
| `damage_id` | ダメージ ID（`whathitme:hidden` として送られる）。`[Copy ID]` / `[Mute]` も ID の行も出ない |
| `attacker` | 攻撃者の名前・ID・飛び道具・持ち主・距離・透明。近接の ID（`mob:...`）も隠れる |
| `mod` | mod の名前 |
| `weapon` | 武器、その ID とエンチャント |
| `death_message` | 死亡メッセージ（置き換わる。[隠した死亡メッセージ](/ja/modpack/death-messages#hidden-death-messages-from-rules)を参照） |
| `tip`（別名 `hint`） | ヒント |
| `properties` | ダメージの属性 |
| `environment` | ブロック / 液体 / 手のアイテムの状況 |
| `owner` | ペット・タレット・飛び道具の持ち主 |
| `distance` | 攻撃者との距離 |
| `position`（別名 `coords`、`dimension`） | ディメンションと座標（コピー文からも） |

隠した項目は、**パケットを作る前にサーバーで**取り除かれます。`/whm search`、`/whm dump`、`/whm inspect`、コピー文でも同じです。不明な hide 項目は `/whm reload` が警告します。

`hide` を書くと `"enforce": true` になるので、プレイヤーが個人用テンプレートや名前で情報を戻すことはできません。許可したいなら `"enforce": false` と書きます。

## `commands`

プレイヤーが**死んだ**とき、このルールが当てはまったものならコンソール権限で実行します。プレースホルダーは[死亡トリガー](/ja/modpack/death-triggers#placeholders)と同じです。

```json
{ "match": "*@twilightforest:lich", "unless": ["tag:lich_warned"],
  "commands": ["tellraw {player} {\"text\":\"リッチの弱点は……\"}", "tag {player} add lich_warned"] }
```

## ルールが評価されるとき

サーバーがプレイヤー向けの出力を作るたびです。

- 死亡のまとめ、リスポーン後の要約、通知
- `/whm history`、`last`、`snapshot`、`inspect`（**調べられる側**の進行度）
- `/whm search` と `dump` の項目
- テンプレートのプレビュー
- カスタム死亡メッセージ

出力をまたいだキャッシュはないので、クエストを達成すると過去の記録もすぐに表示されます。

::: info 必要なもの
ルールにはサーバー（またはシングルプレイ）の mod が必要です。クライアントだけに入れた場合、ルールは適用されません。1.3 のサーバーにつないだ 1.2 のクライアントには、隠す項目を取り除いた記録が届きますが、ルールの名前とヒントは届きません。
:::

## mod なしのプレイヤー

サーバーは、mod なしのプレイヤーに送る英語のチャットにもルールを適用します。ただし **Forge / NeoForge** では、その人たちに見えるバニラの死亡メッセージは変えられないため、隠した攻撃者がそこに出てしまうことがあります。問題になるなら [`requireClientMod`](/ja/modpack/require-client-mod) を使ってください。Fabric のサーバーは全員の死亡メッセージを置き換えます。

## 条件付きの死亡トリガー

`deathTriggers` でも `if`・`any`・`unless` が使えます（死んだプレイヤーの進行度で判定）。

```json
{ "match": "minecraft:lava", "command": "give {player} minecraft:water_bucket", "unless": ["tag:veteran"], "oncePerPlayer": true }
```

## ほかの例

すぐに使えるルールは[レシピ集](/ja/cookbook#progression-rules)にあります：解放されるまでディメンションを丸ごと隠す、何度か死ぬとヒントが変わる、FTB Quests や Game Stages の例など。
