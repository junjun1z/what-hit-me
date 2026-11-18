# pack フォルダー

`config/whathitme/pack/` には pack の定義を JSON ファイルで置きます。modpack でカスタマイズするものは、すべてここに置くのがおすすめです。

## 初回起動時に作られるもの

フォルダーがなければ、テーマごとに `_example.json` を入れて作ります。中の項目はすべてコメントアウトされているので、**編集するまで何も起きません**。

```
config/whathitme/pack/
  hints/_example.json          hintMap
  renames/_example.json        renameMap
  ignored/_example.json        defaultIgnoredDamageIds, lockedFeatures
  triggers/_example.json       deathTriggers
  templates/_example.json      templates（1.3.0+）
  deathmessages/_example.json  deathMessages（1.3.0+）
  rules/_example.json          rules（1.3.0+）
  custom/_example.json         メモ：どのファイルにどのキーを書いてもよい
```

1.2 から 1.3 に更新したときは、`templates/`・`deathmessages/`・`rules/` のサンプルが、そのフォルダーがまだない場合だけ追加されます。自分で作ったファイルには触れません。

## ファイルの読み込み方 {#how-files-are-read}

1. `pack/` 以下の `.json` ファイルを、**サブフォルダーも含めて**すべて集めます。
2. パス（小文字）のアルファベット順に並べます。たとえば `hints/_example.json` → `hints/nether.json` → `rules/bosses.json` の順です。<Badge type="tip" text="1.3.0+" /> 最上位の `"order": <数値>` があればそれで先に並べ（ないファイルは 0）、`"enabled": false` のファイルは消さずに読み込みから外します。[パックエディター](/ja/modpack/pack-editor)はこの 2 つと `"memo"`（メモ）を書きます。
3. その順番で合成します。同じキーがあれば**後のファイルが前の項目を上書き**します。
4. 解析できないファイルは読み飛ばされます。`/whm reload` がファイル名を表示し、サーバーのログに理由が出ます。ほかのファイルは通常どおり読み込まれます。<Badge type="tip" text="1.3.0+" /> 型が違うセクション（たとえば `"hintMap": 5` や、値がオブジェクトになっているリネーム）は警告付きで読み飛ばします。ルールやデストリガーをリストにせず 1 つのオブジェクトで書いた場合は、警告付きで 1 件として読み込みます。9 階層以上深いサブフォルダーのファイルはエラーとして表示します。
5. ファイルは UTF-8（BOM の有無は問いません）で保存してください。ほかの文字コードのファイルはシステムの文字コードで読み込み、ログに警告を出します。

フォルダー名は整理のためだけのものです。**どのファイルにどのキーを書いても構いません。** テーマ別（`pack/nether.json`）、種類別（`pack/hints/...`）、mod 別（`pack/twilightforest.json`）など、好きなように整理してください。

::: tip 名前で順番を決める
あるファイルを別のファイルより優先したいときは、名前に番号を付けます：`pack/10-base.json`、`pack/20-twilight.json`、`pack/90-overrides.json`
:::

## JSON の書き方

コメントと末尾のカンマが使えます。

```json
{
  // 行コメント
  /* ブロックコメント */
  "renameMap": {
    "minecraft:outOfWorld": "&5奈落",   // 下の末尾カンマも OK
  },
}
```

## すべてのキー

| キー | 形式 | ページ |
|---|---|---|
| `renameMap` | オブジェクト `"キー": "名前"`、または `"キー = 名前"` のリスト | [名前・ヒント・非表示 ID](/ja/modpack/names-and-tips) |
| `hintMap` | オブジェクト `"キー": "ヒント"`、または `"キー = ヒント"` のリスト | [名前・ヒント・非表示 ID](/ja/modpack/names-and-tips) |
| `defaultIgnoredDamageIds` | マッチキーのリスト | [名前・ヒント・非表示 ID](/ja/modpack/names-and-tips#hiding-damage) |
| `lockedFeatures` | 設定キーのリスト | [設定のロックと権限](/ja/modpack/locked-features) |
| `deathTriggers` | オブジェクトのリスト | [死亡トリガー](/ja/modpack/death-triggers) |
| `templates` <Badge type="tip" text="1.3.0+" /> | オブジェクト：出力名 → 行のリスト | [テンプレート](/ja/modpack/templates) |
| `deathMessages` <Badge type="tip" text="1.3.0+" /> | オブジェクト `"キー": "メッセージのテンプレート"` | [カスタム死亡メッセージ](/ja/modpack/death-messages) |
| `rules` <Badge type="tip" text="1.3.0+" /> | オブジェクトのリスト | [進行度ルール](/ja/modpack/rules) |
| `order` <Badge type="tip" text="1.3.0+" /> | 数値（初期値 0） | ファイルの読み込み順。小さいほど先（[パックエディター](/ja/modpack/pack-editor#what-the-editor-stores-in-the-files)） |
| `enabled` <Badge type="tip" text="1.3.0+" /> | 真偽値（初期値 `true`） | `false` ならそのファイルを読み込まない |
| `memo` <Badge type="tip" text="1.3.0+" /> | 文字列 | ファイルについてのメモ（ルールや死亡トリガーのオブジェクトの中にも書けます） |
| `entryMemos` <Badge type="tip" text="1.3.0+" /> | オブジェクト：セクション → キー → メモ | リネーム、ヒント、死亡メッセージ、テンプレート、非表示 ID、ロックするキーのメモ |

### キーごとの合成ルール

| キー | 2 つのファイルにあるとき |
|---|---|
| `renameMap`、`hintMap`、`deathMessages` | キーごとに合成。後のファイルが同じキーの値を置き換えます。**空の値（`""`）は前のファイルの項目を削除**します。 |
| `defaultIgnoredDamageIds`、`lockedFeatures` | リストを足し合わせます。 |
| `deathTriggers`、`rules` | ファイルの順にリストの後ろへ追加します。ルールではこの順番が重要です（最初に当てはまったルールが使われる）。 |
| `templates` | 後のファイルが、その出力のテンプレートを**まるごと**置き換えます。空のリストで削除します。 |

### オブジェクトとリスト

次の 2 つは同じ意味です。

```json
{
  "hintMap": {
    "minecraft:lava": "水バケツを持ち歩こう。"
  }
}
```

```json
{
  "hintMap": [
    "minecraft:lava = 水バケツを持ち歩こう。"
  ]
}
```

## 変更を反映する

- `/whm reload`（OP）を実行します。読み込んだファイル数とエラーのあるファイル数（ファイル名付き）、不明なテンプレート変数・不明な hide 項目・セクションの型の間違い・条件の問題などの警告が表示されます。開いている[パックエディター](/ja/modpack/pack-editor)にもすぐ反映されます。
- 専用サーバーでは、サーバーが読み直して、新しい定義をすぐに mod 入りの全クライアントへ送ります。
- <Badge type="tip" text="1.3.0+" /> シングルプレイでワールドを開いたとき（サーバーの起動時も）、pack フォルダーと `whathitme-common.cfg` を読み直します。ゲーム起動中に編集した内容は、`/whm reload` をしなくても次に開いたワールドに反映されます。

::: warning クライアントだけに入れた場合
サーバーに mod がない場合、各クライアントは**自分の** `config/whathitme/pack/` を使います。進行度ルールと死亡トリガーはサーバーの mod が必要なので、この場合は動きません。
:::
