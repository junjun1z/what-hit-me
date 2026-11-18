# コマンド一覧

すべてのコマンドは `/whm`（別名 `/whathitme`）で始まり、Tab で補完できます。

**OP** はサーバーの権限レベル 2、またはチートを有効にしたシングルプレイのことです。チートなしのシングルプレイは OP では**ありません**。

## 一覧

| コマンド | 内容 | 使える人 |
|---|---|---|
| `/whm` または `/whm history` | 最近受けたダメージ | 全員 |
| `/whm last` | 最後の死亡のまとめ | 全員 |
| `/whm snapshot [n]` | 保存したトーテム / 大ダメージのスナップショット（1 = 最新） | 全員 |
| `/whm effects` | かかっている効果、mod、牛乳で消せるか | 全員 |
| `/whm stats` | 死因ごとの回数、合計死亡数、受けたダメージ | 全員 |
| `/whm search <text\|#tag\|@modid>` | ダメージの種類・mob・効果を検索 | 全員 |
| `/whm togglechat` | リスポーン後のチャット要約のオン / オフ | 全員 |
| `/whm ignore <id>` | ダメージ ID の非表示 / 表示 | 全員（個人）、サーバーのリストは OP |
| `/whm config` | 設定画面を開く（mod なしのプレイヤーには一覧） | 全員 |
| `/whm config <key> [value]` | 設定の表示・変更 | 個人のキー：全員。common のキー：表示は全員、変更は OP |
| `/whm rename <id> <name...>` | カスタム名（空にすると削除） | OP |
| `/whm sethint <id> <tip...>` | カスタムヒント（空にすると削除） | OP |
| `/whm inspect <player>` | オンラインのプレイヤーの最後の死亡と履歴 | OP |
| `/whm test <damage_id> [entity] [item] [dim] [--as <player>] [--assume <condition>]...` | 名前・ヒント・ルール・死亡トリガー・死亡メッセージ・チャットのプレビュー | OP |
| `/whm dump` | `whathitme_dump.csv` と `hint_template.cfg` を書き出す | OP |
| `/whm reload` | 設定ファイルと pack フォルダーを読み直し、カタログを再スキャン | OP |
| `/whm pack` <Badge type="tip" text="1.3.0+" /> | [パックエディター](/ja/modpack/pack-editor)を開く | OP（mod 入りクライアント、mod 入りサーバー） |
| `/whm pack personal` <Badge type="tip" text="1.3.0+" /> | 個人モードのパックエディター（自分のリスト） | 全員（mod 入りクライアント） |
| `/whm template` <Badge type="tip" text="1.3.0+" /> | テンプレートエディターを開く | 全員（mod 入りクライアント） |
| `/whm template preview <output>` <Badge type="tip" text="1.3.0+" /> | 自分の最新の死亡でテンプレートを表示 | 全員 |
| `/whm template reset [output]` <Badge type="tip" text="1.3.0+" /> | 個人用テンプレートを削除 | 全員（mod 入りクライアント） |
| `/whm server <subcommand>` | mod 入りクライアントから、サーバー側でコマンドを実行 | サーバーに mod が必要 |
| `/whm help` | コマンドの一覧 | 全員 |

mod 入りのクライアントには `/whm copy <text>` と `/whm copyrecap` もあります。`[Copy ID]` と `[Copy Recap]` ボタンが内部で使っています。

## 詳細と例

### `/whm ignore <id>`

ダメージ ID の表示 / 非表示を切り替えます。もう一度実行すると元に戻ります。

```
/whm ignore minecraft:cactus
/whm ignore mob:minecraft:*
```

- mod 入りクライアント：`whathitme-client.cfg` の自分の `ignoredDamageIds`
- サーバー側（mod なしのプレイヤー、コンソール）：OP は `whathitme-common.cfg` のサーバーのリスト、OP 以外はサーバーに保存される自分のリストを変更します。
- `[Mute]` ボタンも同じ動作です。

### `/whm config <key> [value]`

```
/whm config showInternalDamageId false
/whm config anchorPoint TOP_RIGHT
/whm config ignoredDamageIds minecraft:cactus, minecraft:sweetBerryBush
```

真偽値には `true`・`false`・`on`・`off`・`yes`・`no`・`toggle` が使えます。リストはカンマ区切りで、リスト全体を置き換えます。[`lockedFeatures`](/ja/modpack/locked-features) にあるキーは誰も変更できません。

### `/whm rename` と `/whm sethint`

```
/whm rename minecraft:outOfWorld &5The Abyss
/whm sethint mob:minecraft:creeper Creepers hiss before exploding - sprint away!
/whm rename minecraft:outOfWorld
```

最後の行のように文章を付けないと、その項目を削除します。`&` のカラーコードと `lang:<翻訳キー>` が使えます。

- mod 入りクライアントから実行すると、**自分の** `whathitme-client.cfg` を編集します。
- `/whm server rename ...`（またはコンソール）はサーバーの `whathitme-common.cfg` を編集し、全員に送ります。
- modpack では、複合キーが使える [pack ファイル](/ja/modpack/names-and-tips)がおすすめです。

### `/whm inspect <player>`

オンラインのプレイヤーの最後の死亡のまとめと履歴を、`[Copy ID]` / `[Mute]` ボタンなしで表示します。進行度ルールは**調べられる側のプレイヤー**の進行度で判定します。サーバーに mod が必要です。

### `/whm test`

```
/whm test minecraft:lava
/whm test mob:minecraft:zombie - minecraft:iron_sword
/whm test minecraft:arrow twilightforest:skeleton_druid - twilightforest:twilight_forest
/whm test minecraft:generic --as Steve --assume ftbquests:1A2B3C4D5E6F7081
```

仮のとどめの一撃を作り、その結果の名前、ヒント、該当する[死亡トリガー](/ja/modpack/death-triggers)（値を埋めて表示するだけで**実行はしません**）、[カスタム死亡メッセージ](/ja/modpack/death-messages)、チャット要約のプレビューを表示します。`-` で引数を飛ばせます。ディメンションの初期値は自分のいる場所です。ルールがある場合は、どの[ルール](/ja/modpack/rules)に当てはまったか、何を隠すか、強制されるかも表示します。詳しくは[スキャナー・dump・/whm test](/ja/modpack/scanner#whm-test)へ。

### `/whm dump`

`config/whathitme/.cache/whathitme_dump.csv` と `config/whathitme/hint_template.cfg` を書き出します。[スキャナー・dump・/whm test](/ja/modpack/scanner#whm-dump) を参照してください。

### `/whm reload`

2 つの `.cfg` ファイルと pack フォルダーを読み直し、カタログを再スキャンして、次の内容を報告します。

- 読み込んだ pack ファイルの数と、解析できなかったファイル（そのファイルは読み飛ばされます）
- 不明なテンプレート変数と出力名 <Badge type="tip" text="1.3.0+" />
- ルールの問題：不明な条件の接頭辞、入っていないクエスト mod、このバージョンで使えない条件、引数の誤り <Badge type="tip" text="1.3.0+" />
- Forge / NeoForge の[死亡メッセージの警告](/ja/modpack/require-client-mod#the-death-message-warning) <Badge type="tip" text="1.3.0+" />

mod 入りのクライアントではクライアント側のファイルも読み直し、OP ならサーバーにも読み直しを依頼します。

### `/whm template` <Badge type="tip" text="1.3.0+" />

```
/whm template                    エディターを開く
/whm template preview summary    hover | line | summary | alert | copy
/whm template reset              個人用テンプレートをすべて削除
/whm template reset hover        個人用テンプレートを 1 つ削除
```

[テンプレート](/ja/modpack/templates)と[テンプレートエディター](/ja/modpack/template-editor)を参照してください。

### `/whm server <subcommand>`

mod 入りのクライアントから、コマンドのサーバー版を実行します。サーバーの common ファイルを変更するときに使います。

```
/whm server config showTimestamp false
/whm server rename minecraft:outOfWorld &5The Abyss
/whm server ignore minecraft:cactus
```

## 1.2.0 での権限の変更

1.2.0 から、`rename`・`sethint`・`test`・`dump`・`reload` はクライアントで実行する場合も OP が必要になり、シングルプレイの `/whm inspect` はチートが必要になりました。個人の設定は、これまでどおり `/whm config` と設定画面で誰でも変更できます。
