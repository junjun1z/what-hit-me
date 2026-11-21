# What Hit Me?

「何にやられたのか」をはっきり見せる Minecraft 1.7.10〜26.3 の Forge・Fabric・NeoForge 用の Mod です。**どんな Mod パックでも、どんな Mod の組み合わせでも動きます。必須の前提 Mod はありません。**

![What Hit Me? の動作：死亡画面のまとめ、ホバーで詳細、リスポーン後のチャット要約](https://raw.githubusercontent.com/junjun1z/what-hit-me/main/docs/hero.gif)

[English README](README.md)

ダウンロード: [Modrinth](https://modrinth.com/mod/what-hit-me) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-hit-me) · [GitHub Releases](https://github.com/junjun1z/what-hit-me/releases)。詳しい説明（英語・日本語）: [Wiki](https://what-hit-me-wiki.vercel.app/ja/)

![死亡画面のパネル](docs/screenshots/death-screen.png)

- Minecraft 1.7.10〜26.3：Forge、Fabric（Fabric API が必要、Mod Menu は任意）、NeoForge の 63 ビルド。対応表は [docs/description.md](docs/description.md#supported-versions) にあります。
- Mod ID は `whathitme`
- クライアントだけ、サーバーだけ、両方、どの入れ方でも動作。ブロックやアイテムの追加はなし。Forge / NeoForge は標準イベントのみ（Mixin・コアMod なし）。Fabric は Fabric API のイベントと、イベントが無い所だけ小さな Mixin を使います
- ライセンス: MIT

## 設計

- 記録するのはプレイヤーだけです。ダメージ・死亡の処理は、それ以外のエンティティなら最初の1行で終わります。偽プレイヤーは無視します。
- サーバーの tick ハンドラーはありません。時間の判定、連続ダメージのまとめ、反射ダメージ、クールダウンはすべて時刻の差で計算します。被弾の処理が終わってから確認が必要なものだけ、サーバーのタスクキューに1回だけ登録します。
- 通信は死亡時、トーテム・大ダメージ時、コマンド実行時だけです。被弾ごとには送りません。
- Mod 一覧のキャッシュ: ダメージの種類・モブ・効果は一度だけ調べ、`modid:バージョン` の一覧（ソート済み）の SHA-256 と一緒に保存します。次回の起動で Mod 構成が同じならキャッシュから読み込み、変わっていたとき（または `/whm reload`）だけ調べ直します。
- イベント処理、パケット処理、スキャン、画面の描画は、それぞれ自分でエラーと null を受け止めて「不明」として扱います。他の Mod の壊れたダメージ源・エンティティ・アイテム・テキストがクラッシュとして広がらないようにしています。
- 履歴はプレイヤーごとの固定長リングバッファで、エンティティへの参照を持たない不変レコード（数値と文字列だけ）です。ログアウト時に破棄します。

## 機能

- **死亡画面のパネル**: 致命傷、死亡メッセージ、ダメージの属性、直前30秒のすべての被弾、「直前10秒の合計」、ヒントを表示。行にマウスを乗せると詳細。ヘッダーをドラッグで移動、`[-]` で最小化、ホイールでスクロール、`[コピー]` で内容をコピー。
- **リスポーン時のチャット通知**（自分のチャット欄だけ）: 致命傷、属性、直前10秒の合計、ヒント、詳細へのリンク。`/whm togglechat` で消せます。
- **被ダメージ履歴**（`/whm`）: 生きている間も含めた最近の被弾。同じ発生源からの3秒以内の連続ダメージは1行にまとめてヒット数を表示（`x12回`）。
- **マウスオーバーの詳細**: 防具による軽減前後のダメージ、軽減率、衝撃吸収の消費量、被弾後の体力、攻撃者の武器とエンチャント、距離、`[透明]` の攻撃者、タレット・ペット・矢の所有者、追加した Mod、内部ダメージID。`[IDをコピー]` `[非表示]` ボタン付き。
- **最大体力に対する割合**: `(最大HPの30%)` のように、被弾した時点の最大体力に対する割合を表示します。
- **周囲の状況**: 攻撃者のいないダメージ（炎、溶岩、サボテン、Mod の酸や感電ブロックなど）では、足元と体の位置にあるブロック・液体と、両手に持っているアイテムを、それぞれの Mod 名つきで表示します。連続ダメージでワールドを調べるのは最初の1回だけです。
- **反射ダメージ**: 棘の鎧のダメージや、直前（50ミリ秒以内）に自分が殴った相手からのダメージに `[⚡ 反射 / 棘の鎧]` を付け、専用のヒントを出します。
- **防具の破損**: 防具が壊れると、その時点のタイムラインに `[⚠ 破損: <防具名>]` を表示します。
- **トーテム・蘇生のスナップショット**: 不死のトーテムが発動したとき、他の Mod が死亡を取り消したり死亡後に体力を戻したりしたとき、1撃で最大体力の80%以上を失ったときに、その瞬間を保存して `[詳細]` リンク付きでチャットに通知（`/whm snapshot`）。
- **体力系 Mod への対応**: 合計体力が残ったまま死亡した場合（First Aid のような部位ダメージ）は `[部位 / 急所ダメージ (合計HPがX.X残った状態で死亡)]` と表示します。ダメージイベントなしに死亡した場合（`setHealth(0)` など）も、死亡イベントから致命傷を作るので詳細が空になりません。
- **`[詳細をコピー]`**: Markdown 形式でコピー。ディメンション、座標（任意）、最大体力、防具4部位のエンチャントと破損状態、効果も含めます。
- **統計**（`/whm stats`）: 死亡回数の合計、受けたダメージの合計、死因の上位5件。
- **効果一覧**（`/whm effects`）: 現在の効果、追加した Mod、レベル、残り時間、牛乳で消せるか（`[牛乳 ✔]` / `[牛乳 ✖]`）。
- **検索**（`/whm search`）: ダメージの種類・モブ・効果を `#bypasses_armor`、`#absolute`、`#magic`、`#fire`、`#explosion`、`#projectile`、`#damage`、`#scanned`、`#mob`、`#effect`、`#debuff`、`@modid`、または文字列で検索。組み合わせも可（`@minecraft zombie`）。
- **ダメージIDのスキャン**: Mod のダメージIDを、誰かがそのダメージを受ける前から一覧に載せます（下記参照）。
- **設定画面**: `/whm config` でどのバージョン・ローダーでも開けます。Forge / NeoForge では Mod 一覧から、Fabric では Mod Menu を入れていればそこからも開けます。パックでロックされた項目は操作できない状態で表示されます。
- **自動ヒント**: ダメージの種類と属性に応じた対策を表示。ダメージIDごとに差し替え可能。
- **テンプレート**（1.3.0）: マウスオーバーの詳細、パネル・履歴の行、リスポーン時の要約、通知、コピー文を `{変数}` で書き換えられます。ライブプレビュー付きの**テンプレートエディター**（`/whm template`）で編集できます。
- 10言語対応。

古い Minecraft のフォントは絵文字を表示できないため、ゲームのフォントにある記号（☠ ⚠ ❤ ⚡ Σ）と文字のラベルを使っています。

## 導入パターン

| 入れる場所 | プレイヤーに見えるもの |
|---|---|
| クライアントとサーバー | サーバーからの正確なデータ（軽減前ダメージ、軽減率、衝撃吸収、武器、所有者、盾・無敵時間のフラグ、防具、周囲の状況）。各自の言語で表示。クリップボードへのコピーボタン。サーバーの `whathitme-common.cfg` と pack フォルダが全員に適用されます。 |
| クライアントだけ | クライアント側の推定モード。自分の体力を比べ、減ったときだけ原因を推測します（落下、溶岩、炎、溺れ、窒息、奈落、近くのモブ、飛び道具、爆発、毒、ウィザー、空腹）。記録には `[クライアント推定]` が付きます。`/whm inspect` は使えません。 |
| サーバーだけ | Mod を入れていないプレイヤー（素の Forge クライアントでも）に英語のチャットを送ります。リスポーン時の通知、トーテム・大ダメージの通知、すべての `/whm` コマンド。`[Copy ID]` は ID をチャット欄に入力します。`togglechat` と `ignore` はプレイヤーごとに保存されます。 |

シングルプレイは「クライアントとサーバー」の扱いです。入れている人と入れていない人が混在するサーバーでは、各プレイヤーにそのクライアントで使える最良のモードが選ばれます。サーバー側で OFF にした機能（common 設定や `lockedFeatures`）は、サーバーがパケットを作る段階で取り除かれます。1.0〜1.2 のクライアントと 1.3 のサーバーの組み合わせ（その逆も）でも動作します。`requireClientMod` で、全員のクライアントに Mod を必須にすることもできます。

## Mod パック製作者の方へ

### 入れる理由

- 「何で死んだの？」という質問が減ります。攻撃者、その Mod、ダメージの種類をプレイヤーが自分で確認できます。
- ダメージの種類・攻撃者・武器・ディメンションの組み合わせで、パック独自のヒントや表示名を出せます。不要な表示は隠せます。
- 進むまで秘密にできます（1.3.0）。ボスのクエスト・進捗・ステージを終えるまでは `???` と表示し、ヒントも出しません。FTB Quests、Better Questing、Heracles、Game Stages、KubeJS、CraftTweaker に対応（どれも任意）。
- パックの雰囲気に合わせられます（1.3.0）。すべての表示のテンプレート、カスタム死亡メッセージ、設定全体を編集できるゲーム内エディター。
- 前提 Mod なし、ワールドデータなし、ログイン時のチャット表示なし。いつ追加・削除しても安全です。

### ファイル

```
config/whathitme/
  whathitme-common.cfg    パック・サーバーの設定
  whathitme-client.cfg    個人の設定とパネルの配置
  whathitme-client-templates.json   個人のテンプレート（1.3.0）
  hint_template.cfg       /whm dump が書き出すテンプレート
  pack/                   パック定義: hints/ renames/ ignored/ triggers/ custom/ templates/ deathmessages/ rules/（どのサブフォルダの *.json でも可）
  .cache/                 catalog_cache.json、stats_<uuid>.json、whathitme_dump.csv
  .backup/                パックエディターが変更する前のファイル（1.3.0）
```

| ファイル | 対象 | 内容 |
|---|---|---|
| `whathitme-common.cfg` | パック製作者・サーバー管理者 | 表示の ON/OFF、ヒントの ON/OFF、フィルター（`ignoredDamageIds`、`renameMap`、`hintMap`、`minDamageThreshold`、`heavyHitThreshold`）、記録の設定（`historySize`、`mergeWindowSeconds`、`summarySeconds`、`recapSeconds`、`maxSnapshots`）。Mod 入りのサーバーは、ログイン時と再読み込み・設定変更時にこのファイルを Mod 入りクライアントへ送り、クライアント側の common ファイルの代わりに使わせます。 |
| `whathitme-client.cfg` | 各プレイヤー | 個人用の表示 ON/OFF とフィルター、`showHints`、死亡画面パネルの配置（`anchorPoint`、`offsetX`、`offsetY`、`scale`、`backgroundOpacity`、`maxVisibleLines`、`minimized`、`chatHistoryLines`）。 |

2つの `.cfg` はどのローダーでも Forge の設定形式で、`/whm reload` で再起動なしに再読み込みできます。表示は **両方のファイルで** `true` の項目だけです。個人の `renameMap` / `hintMap` はパックの設定の上に適用されます。優先順位は `.cfg` の設定 > pack の JSON > 内蔵の自動ヒント です。

<details>
<summary>全設定項目</summary>

**表示の ON/OFF**（両方のファイル、カテゴリ `display`、初期値はすべて `true`）:
`enableDeathScreenPanel`、`enableRespawnChatSummary`、`enableTotemChatAlert`、`enableHeavyHitSnapshot`、`showTimestamp`、`showDamageAmount`、`showMitigationDetails`、`showSmartMergeHitCount`、`showSourceModName`、`showDamageProperties`、`showInternalDamageId`、`showAttackerName`、`showAttackerWeapon`、`showAttackerDistance`、`showAttackerInvisibility`、`showEntityOwner`、`showActionButtons`、`showToggleChatNotice`、`showMaxHpPercentage`、`showEnvironmentContext`、`showReflectedBadge`、`showArmorBreakEvents`、`includeEquipmentInCopy`、`includeCoordsInCopy`（`includeEquipmentInCopy` が必要）

**ヒント**（カテゴリ `display`、初期値 `true`）: `enableAutoHints` と `enableCustomHints`（common ファイルのみ）、`showHints`（client ファイルのみ）。1.0 / 1.1 の `showHintsAndTips` の代わりです。古い `showHintsAndTips=false` が残っていると3つとも OFF にしたうえで、古いキーを削除します。

**フィルター**（両方のファイル、カテゴリ `filters`）:

| キー | 初期値 | 意味 |
|---|---|---|
| `minDamageThreshold` | 0.05 | これ未満のダメージは記録しない |
| `heavyHitThreshold` | 0.8 | 最大体力のこの割合以上を1撃で失うと大ダメージ扱い |
| `ignoredDamageIds` | 空 | 非表示にするキー（複合キー、`*` ワイルドカード可） |
| `renameMap` | 空 | `キー = 名前` または `キー = lang:翻訳キー` |
| `hintMap` | 空 | `キー = ヒント` または `キー = lang:翻訳キー` |

`whathitme-client.cfg` 側のしきい値はクライアントだけの推定モードでのみ使われます。それ以外はサーバーが記録します。

**記録**（common ファイルのみ、カテゴリ `tracking`）:

| キー | 初期値 | 意味 |
|---|---|---|
| `historySize` | 50 | プレイヤーごとに保持する記録数（5〜500） |
| `mergeWindowSeconds` | 3.0 | 連続ダメージをまとめる秒数（0 で無効） |
| `summarySeconds` | 10 | 「直前X秒の合計」の秒数 |
| `recapSeconds` | 30 | 死亡の詳細にさかのぼって含める秒数 |
| `maxSnapshots` | 5 | プレイヤーごとのトーテム・大ダメージのスナップショット数 |
| `requireClientMod` | false | 1.3.0: What Hit Me? を入れていないクライアントの参加を断る |

**配置**（client ファイルのみ、カテゴリ `layout`）:

| キー | 初期値 | 意味 |
|---|---|---|
| `anchorPoint` | `CENTER_LEFT` | `TOP_LEFT`、`TOP_RIGHT`、`BOTTOM_LEFT`、`BOTTOM_RIGHT`、`CENTER_LEFT`、`CENTER_RIGHT` |
| `offsetX` / `offsetY` | 4 / 0 | 基準位置からのずれ。パネルをドラッグすると保存 |
| `scale` | 0.85 | パネルの大きさ |
| `backgroundOpacity` | 0.75 | 背景の不透明度（0.0〜1.0） |
| `maxVisibleLines` | 8 | スクロールせずに見える行数 |
| `minimized` | false | `[-]` ボタンで保存 |
| `chatHistoryLines` | 15 | `/whm history` で表示する行数 |

</details>

### pack フォルダ

`config/whathitme/pack/` が無ければ、`hints/`、`renames/`、`ignored/`、`triggers/`、`custom/`、`templates/`、`deathmessages/`、`rules/` を作り、それぞれに `_example.json` を置きます。例の中身はコメントアウトされているので、編集するまで何も起きません。`pack/` 以下の `.json` はすべて、パスのアルファベット順（最上位に `"order"` の数値があればその順）に読み込んでまとめます。`"enabled": false` のファイルは読み込みません。後のファイルが前のファイルの同じキーを上書きします。コメント（`//`、`/* */`）と末尾のカンマも使えます。どのファイルにどのキーを書いても構いません。

```json
{
  "renameMap": {
    "minecraft:outOfWorld": "&5The Abyss",
    "mob:minecraft:zombie @item:minecraft:iron_sword": "Armed Zombie"
  },
  "hintMap": {
    "minecraft:lava @dim:minecraft:the_nether": "Carry Fire Resistance potions in the Nether.",
    "mob:lycanitesmobs:*": "lang:mypack.hint.lycanites"
  },
  "defaultIgnoredDamageIds": ["minecraft:cactus"],
  "lockedFeatures": ["showInternalDamageId"],
  "deathTriggers": [
    { "match": "minecraft:lava", "command": "say {player} fell into lava at {x} {y} {z}", "cooldownSeconds": 60 },
    { "match": "mob:minecraft:creeper", "command": "give {player} minecraft:bread 4", "oncePerPlayer": true }
  ]
}
```

- `renameMap`、`hintMap`: オブジェクト（`"キー": "値"`）または `"キー = 値"` の文字列のリスト。値を空にすると、前のファイルの同じキーを取り消します。`&` のカラーコードが使えます。`lang:<翻訳キー>`、または翻訳キーそのものを値にすると、各プレイヤーのクライアントで翻訳されます。リソースパックで各言語の文を用意できます。
- `defaultIgnoredDamageIds`: 全員に対して最初から非表示にするキー。
- `lockedFeatures`: 設定画面からも `/whm config` からも変更できなくする設定キー。シングルプレイでも変更できません。
- `deathTriggers`: 下記参照。
- `templates`、`deathMessages`、`rules`（1.3.0）: 下記参照。
- `order`、`enabled`、`memo`、`entryMemos`（1.3.0）: 読み込み順、ON/OFF、メモ。パックエディターが書き込みます。
- `/whm reload` でフォルダを読み直し、読み込んだファイル数、読めなかったファイル（そのファイルは飛ばします）、不明なテンプレート変数、ルールの問題を表示します。

### 複合キー

`renameMap`、`hintMap`、`ignoredDamageIds`、`defaultIgnoredDamageIds`、`deathTriggers` で使えます。条件は順不同で、スペース（または `&`）で区切ります。

| 条件 | 一致するもの |
|---|---|
| `minecraft:lava`、`mob:minecraft:zombie` | ダメージID。エンティティからの攻撃は `mob:<エンティティID>`、それ以外は `<modid>:<ダメージの種類>`（`minecraft:fall`、`minecraft:outOfWorld`、`minecraft:explosion.player` など） |
| `@minecraft:zombie`、`@mob:minecraft:zombie` | 攻撃者のエンティティID |
| `@item:minecraft:iron_sword` | 攻撃者がメインハンドに持っているアイテム |
| `@dim:minecraft:the_nether` | ディメンション（名前空間なしの `the_nether` でも一致） |

どの条件にも `*` と `?` のワイルドカードが使えます。`@` の前は区切りを省略して続けて書けるので、`*@twilightforest:lich`（ID に関係なくリッチからの攻撃すべて）や `minecraft:arrow@twilightforest:skeleton_druid` のようにも書けます。よく使う例:

| キー | 一致するもの |
|---|---|
| `*@twilightforest:lich` または `@twilightforest:lich` | リッチからの攻撃すべて（攻撃のダメージ ID が何であっても） |
| `@twilightforest:*` | Twilight Forest のどの mob からの攻撃でも |
| `*acid*` | 「acid」を含むダメージ ID すべて（mod が実行時に ID を組み立てる場合にも使えます） |
| `minecraft:arrow @twilightforest:skeleton_druid` | その mob が撃った矢だけ |

キーの条件はすべて一致する必要があります。複数のキーが一致したときは、より具体的なものが優先されます。条件の数が多いもの（`*` だけの条件は数えません）、同じ数ならワイルドカードの少ないものです。たとえば `twilightforest:lich_bolt @twilightforest:lich` のヒントは `@twilightforest:lich` の一般的なヒントより優先され、`*` だけのキーは最も優先度の低い「その他すべて」として使えます。

### 死亡トリガー（deathTriggers）

各項目は `match`（複合キー）と `command`、任意で `oncePerPlayer`（初期値 `false`。プレイヤーの統計ファイルに記録されるので再起動後も有効）と `cooldownSeconds`（プレイヤーごと、初期値 `0`）を持ちます。置き換え文字: `{player}`、`{x}`、`{y}`、`{z}`（死亡したブロック座標）、`{damage}`（致命傷の体力ポイント、小数1桁）。1.3.0 からはすべてのテンプレート変数も使え、`if` / `any` / `unless` の条件（進行度ルールを参照）も書けます。コマンドはサーバー側だけで、コンソールの権限で実行されます。一致したトリガーはすべて、ファイルの順に実行されます。先頭の `/` はあってもなくても構いません。

### テンプレート（1.3.0）

表示を1行ずつ書き換えられます。出力は `hover`（マウスオーバーの詳細。履歴やチャットの行の詳細にも使用）、`line`（死亡画面パネルの行と `/whm history` の行）、`summary`（リスポーン時のチャット）、`alert`（トーテム・大ダメージの通知）、`copy`（`[コピー]` の文）です。

```json
{
  "templates": {
    "summary": [
      "&6&l===== {player} died =====",
      "&cKilled by &f{killing_blow} &c({damage} ❤)",
      "&7Weapon: {weapon}",
      "&aTip: {tip}",
      "&b{recap_link}"
    ]
  }
}
```

- `{変数}`、`{変数|代わりの文字}`、`{damage:2}`（小数の桁数）、`&` のカラーコード、`lang:<キー>` の行。変数がすべて空の行は表示しません（武器がなければ「Weapon:」の行が消えます）。
- 変数は表示の ON/OFF に従います。OFF の項目（ルールで隠した項目も）は空になるので、サーバーが止めた情報をテンプレートで見せることはできません。
- 優先順位: 個人の `whathitme-client-templates.json` > pack の `templates` > 内蔵（どの言語でも 1.2.0 と同じ表示）。`lockedFeatures` に `templates` または `templates.<出力>` を書くとロックできます。
- **テンプレートエディター**: `/whm template`、または設定画面の **Templates...**。実際の表示と同じコードで描くライブプレビュー、クリックで挿入できる変数一覧、直前の死亡またはサンプルの死亡でプレビュー。**Save to pack**（チートを有効にしたシングルプレイ）で `pack/templates/<出力>.json` に保存します。`/whm template preview <出力>` でチャットに表示できます。

### カスタム死亡メッセージ（1.3.0）

`deathMessages` で、一致した死亡のバニラの死亡メッセージを置き換えます（初期状態では何もしません）。キーは複合キー、値は1行のテンプレートです。

```json
{
  "deathMessages": {
    "mekanism:acid": "{player} dissolved in acid",
    "*@twilightforest:lich": "{player} was outsmarted by the Lich"
  }
}
```

| サーバー | Mod ありのプレイヤー | Mod なしのプレイヤー |
|---|---|---|
| Fabric | カスタムメッセージ | カスタムメッセージ |
| Forge / NeoForge | カスタムメッセージ（チャットと死亡画面） | バニラのメッセージ（`requireClientMod` を使ってください） |

サーバーのログにもカスタムメッセージが出ます。`lang:` キーはプレイヤーごとに翻訳されます。

### 進行度ルール（1.3.0）

`rules` は、プレイヤーの進行度に応じて表示名とヒントを変え、項目を隠し、コマンドを実行します。最初に一致したルールが使われます。

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

- 項目: `match`、`if`（すべて）、`any`（どれか1つ）、`unless`（どれも満たさない）、`name`、`hint`（`""` でヒントなし）、`hide`、`enforce`（個人の表示名・ヒント・テンプレートを使わせない。`hide` があれば自動で ON）、`commands`、`oncePerPlayer`、`cooldownSeconds`。
- `hide`: `damage_id`、`attacker`、`mod`、`weapon`、`death_message`、`tip`、`properties`、`environment`、`owner`、`distance`、`position`。隠した情報は送る前にサーバーで取り除きます。死亡メッセージを隠すと「{player} was killed by ???」または「{player} died」になります。
- 条件（`!` で否定）: `advancement:<ID>`（1.12 以降）、`tag:<タグ>`（1.9 以降）、`score:<目的><比較><数>`、`stage:<名前>`（Game Stages）、`ftbquests:<ID>`（FTB Quests）、`bq:<ID>`（Better Questing、GTNH 版も）、`heracles:<ID>`（Heracles / Odyssey Quests）、`script:<名前>`（スクリプト）。クエスト mod は任意で、それぞれ存在するすべてのバージョン・ローダーで試験しています。
- 表示を作るたびに判定するので、クエストを終えると過去の記録も見えるようになります。サーバーに Mod が必要です。
- `/whm test <ダメージID> ... --as <プレイヤー> --assume <条件>` で、そのプレイヤーの実際の進行度での結果を確認できます。

### `requireClientMod`（1.3.0）

`whathitme-common.cfg` で `requireClientMod=true` にすると、What Hit Me? を入れていないプレイヤーを分かりやすいメッセージ付きで断ります（入っているかだけを確認し、バージョンは見ません）。Forge / NeoForge では、ルールで死亡メッセージを隠していてこの設定が OFF のとき、警告をログに出します。バニラのクライアントにはバニラの死亡メッセージが見えてしまうためです。

### スクリプト連携（1.3.0）

KubeJS、CraftTweaker、MineTweaker 3 のスクリプトに、それらが存在するすべてのバージョン・ローダーで対応しています（KubeJS 1.12.2〜26.1.2、CraftTweaker 1.12.2〜1.21.1、MineTweaker 3 / CraftTweaker 3.x 1.7.10〜1.11.2）。

```js
// kubejs/server_scripts/whathitme.js
WhatHitMeEvents.register(event => {
  event.condition('veteran', 'Has killed the Wither', player => player.stages.has('wither_down'))
  event.addRule({ match: '*@minecraft:wither', unless: ['script:veteran'], name: '???', hide: ['attacker', 'mod'] })
})
```

スクリプトの条件（`script:<名前>`）、テンプレート変数（`{script:<名前>}`）、イベント（`damageRecorded`、`death`、`respawn`、`alert`）、読み取り（直前の死亡、履歴、統計、`/whm test` の結果）、実行時の表示名・ヒント・ルール・死亡メッセージ・テンプレートの追加。`/whm reload` とスクリプト mod の再読み込みで作り直されます。他の Mod 向けの Java API: `io.github.junjun1z.whathitme.api.WhatHitMeApi`。

### ゲーム内パックエディター（1.3.0）

`/whm pack`（または設定画面の **Pack editor...**）で、ここまでの設定をすべてゲーム内で編集できます。表示名、ヒント、ルール、死亡メッセージ、テンプレート、死亡トリガー、非表示 ID、ロックする設定、サーバーの設定。チートを有効にしたシングルプレイと OP が使えます。

- サーバーが編集内容を確認して JSON ファイルに書き込み、すぐに再読み込みして全員に反映します。保存は項目を確定したときで、1文字ごとではありません。
- JSON ファイル1つが1カテゴリー、サブフォルダーはグループです。順番、ON/OFF、メモはファイルに保存されます。エディターが書き直すファイルの手書きのコメントは消えるので、メモ欄を使ってください。
- 検索できるダメージ ID・モブ一覧付きのマッチキー作成、サーバーにある進捗・スコア・タグ・クエスト・ステージ・スクリプト条件から選べる条件の選択、「このプレイヤーとして / この条件を満たしたとして」のプレビュー、入力欄の下のエラー表示、他の OP が保存したときの通知。
- `config/whathitme/.backup/` にバックアップ、30 回まで元に戻せます。
- 個人モード（`/whm pack personal`、権限不要）: 自分の表示名・ヒント・非表示 ID を一覧で編集できます。

### `/whm test`

`/whm test <ダメージID> [エンティティID] [アイテムID] [ディメンションID] [--as <プレイヤー>] [--assume <条件>]`（OP）は、仮の致命傷を作り、表示名、ヒント、一致したルールと隠される項目、実行されるはずの死亡トリガー（置き換え文字を埋めた形で表示するだけで、実行はしません）、死亡メッセージ、チャットのプレビューを表示します。`-` で引数を飛ばせます。ディメンションを省くと実行した人のディメンションになります。

### ダメージIDのスキャンと `/whm dump`

Mod のダメージIDは、誰かがそのダメージを受ける前から一覧に載ります。

- 1.7.10〜1.19.3: キャッシュが使えないとき（初回起動、Mod 構成の変更、`/whm reload`）に、バックグラウンドのスレッドが Mod の jar のクラスファイルを読み（クラスは読み込みません）、ダメージ源のコンストラクタに渡される文字列のIDを集めます。サブクラスや、コンストラクタ直後のセッター呼び出しも対象です。jar の中の jar は1階層まで読みます。結果は `catalog_cache.json` の `scan` に保存します。
- 1.19.4 以降: ゲームのダメージタイプのレジストリにすべてのキーがあるので、それを直接読みます（属性はタグから取得）。スキャンは不要です。

見つかったIDは `/whm search`（`#scanned`）、`/whm test` の補完、`/whm dump` に出ます。`/whm dump`（OP）は次のファイルを書き出します。

1. `config/whathitme/.cache/whathitme_dump.csv`: すべてのダメージの種類・モブ・効果と、その Mod、属性、発生回数、設定済みの表示名・ヒント・非表示、`source`（`seen`、`scanned`、`registry`）。
2. `config/whathitme/hint_template.cfg`: 分かっているすべてのダメージIDと、登録されている全モブの `mob:<ID>` を値なしで並べた `S:hintMap <...>` と `S:renameMap <...>` のブロック。`whathitme-common.cfg` に貼り付けて、必要な行だけ値を書きます。値が空の行は無視されます。

### 統計

`/whm stats` は死因ごとの死亡回数と、実際に減った体力を数えます。メモリに置くのはプレイヤーごとに最大100件の死因で、`.cache/stats_<uuid>.json` に書き込むのは死亡時、ログアウト時、ワールド保存時だけです。

### Mod パックに入れてもいいですか？

はい。MIT ライセンスです。連絡も不要です。

## コマンド

`/whm`（別名 `/whathitme`）。すべて Tab キーで補完できます。「OP」は権限レベル2、またはチートを有効にしたシングルプレイのことです。

| コマンド | 内容 | 使える人 |
|---|---|---|
| `/whm` または `/whm history` | 最近の被ダメージ | 全員 |
| `/whm last` | 直前の死亡の詳細 | 全員 |
| `/whm snapshot [番号]` | 保存されたトーテム・大ダメージのスナップショット（1 が最新） | 全員 |
| `/whm effects` | 現在の効果、Mod、牛乳で消せるか | 全員 |
| `/whm stats` | 死因ごとの死亡回数、死亡回数の合計、受けたダメージ | 全員 |
| `/whm search <文字列\|#タグ\|@modid>` | ダメージの種類・モブ・効果を検索 | 全員 |
| `/whm togglechat` | リスポーン時のチャット通知の ON/OFF | 全員 |
| `/whm ignore <ID>` | ダメージIDの非表示・再表示 | 全員（個人）、サーバーのリストは OP |
| `/whm config` | 設定画面を開く（Mod を入れていないプレイヤーには設定の一覧を表示） | 全員 |
| `/whm config <キー> [値]` | 設定の表示・変更（`true`/`false`/`on`/`off`/`toggle`） | 個人の設定: 全員、common の設定: OP |
| `/whm rename <ID> <名前...>` | 表示名の変更（空で削除） | OP |
| `/whm sethint <ID> <ヒント...>` | ヒントの設定（空で削除） | OP |
| `/whm inspect <プレイヤー>` | オンラインのプレイヤーの直前の死亡と履歴 | OP |
| `/whm test <ダメージID> [エンティティ] [アイテム] [ディメンション] [--as <プレイヤー>] [--assume <条件>]` | 表示名・ヒント・ルール・死亡トリガー・死亡メッセージ・チャットのプレビュー | OP |
| `/whm dump` | `whathitme_dump.csv` と `hint_template.cfg` を書き出す | OP |
| `/whm reload` | 設定ファイルと pack フォルダの再読み込み、カタログの再スキャン | OP |
| `/whm template` | テンプレートエディターを開く（1.3.0） | 全員（Mod 入りクライアント） |
| `/whm template preview <出力>` / `reset [出力]` | テンプレートをチャットに表示 / 個人のテンプレートを削除（1.3.0） | 全員 |
| `/whm pack` | パックエディターを開く（1.3.0） | OP（サーバーに Mod が必要） |
| `/whm pack personal` | 自分の表示名・ヒント・非表示 ID を編集するパックエディター（1.3.0） | 全員（Mod 入りクライアント） |
| `/whm server <サブコマンド>` | Mod 入りクライアントからサーバー側で実行 | サーバーに Mod が必要 |
| `/whm help` | コマンド一覧 | 全員 |

`lockedFeatures` に書かれたキーは誰も変更できません。Mod 入りクライアントからの `rename` / `sethint` は自分の個人ファイルを変更します。サーバーの common ファイルを変えるには `/whm server rename ...` を使います。`/whm copy` と `/whm copyrecap` は `[IDをコピー]` `[詳細をコピー]` ボタンが内部で使うコマンドです。

## 対応言語

English、日本語、简体中文、繁體中文、한국어、Русский、Deutsch、Français、Español、Português (Brasil)。

`python tools/check_lang.py` で、各言語ファイルに `en_us.json` と比べて足りないキーや余分なキーがないか確認できます。

## ビルド

Gradle の実行には JDK 25 が必要です。各ターゲットの Java 8 / 17 / 21 / 25 ツールチェーンは自動でダウンロードされます。

```
./gradlew collectJars -Pmod_version=1.3.0
```

Minecraft バージョン / ローダーごとに 1 ターゲットで、すべての jar が `build/dist/` にコピーされます。構成、アーキテクチャ、バージョンの追加方法、移植のメモ、ゲーム内セルフテストは [BUILD.md](BUILD.md) を参照してください。

リリース: `1.3.0` のようなタグを push すると `.github/workflows/release.yml` が全ターゲットをビルドし、`publish.json` に載っている jar を [CHANGELOG.md](CHANGELOG.md) の該当バージョンの節を添えて GitHub Releases、Modrinth、CurseForge に公開します。

## ライセンス

[MIT](LICENSE) © junjun1z
