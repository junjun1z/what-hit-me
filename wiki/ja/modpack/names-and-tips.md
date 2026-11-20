# 名前・ヒント・非表示 ID

## カスタム名：`renameMap`

ダメージの表示名を置き換えます（パネル、詳細表示、チャット、コピー文）。

```json
{
  "renameMap": {
    "minecraft:outOfWorld": "&5奈落",
    "mob:minecraft:zombie @item:minecraft:iron_sword": "武装ゾンビ",
    "*@twilightforest:lich": "&dリッチ",
    "draconicevolution:chaos": "lang:mypack.damage.chaos"
  }
}
```

## カスタムヒント：`hintMap`

自動の対策ヒントを置き換えます。

```json
{
  "hintMap": {
    "mob:minecraft:creeper": "クリーパーはシューッと鳴ってから爆発する。走って離れよう！",
    "minecraft:lava @dim:minecraft:the_nether": "ネザーでは耐火のポーションを持ち歩こう。",
    "mob:lycanitesmobs:*": "lang:mypack.hint.lycanites"
  }
}
```

## 値の書き方

| 書いたもの | 表示 |
|---|---|
| `普通の文章` | その文章 |
| `&5奈落` | 色付きの文章。`&0`〜`&f` が色、`&l` が太字、`&n` が下線、`&r` がリセット。セクション記号も使えます。 |
| `lang:mypack.hint.lava` | プレイヤーの言語でのそのキーの翻訳（リソースパックから） |
| `mypack.hint.lava` | 値が既知の翻訳キーなら、これも翻訳されます |
| `""`（空） | 前の pack ファイルの項目を削除 |

キーは[マッチキー](/ja/modpack/match-keys)です：ダメージ ID、`@エンティティ`、`@item:`、`@dim:`、ワイルドカード。

### 名前とヒントを翻訳する

1. pack ファイルで `lang:` の値を使います。

   ```json
   { "hintMap": { "minecraft:lava": "lang:mypack.hint.lava" } }
   ```

2. キーをリソースパックに入れます。たとえば `assets/mypack/lang/en_us.json` と `ja_jp.json`：

   ```json
   { "mypack.hint.lava": "Carry a water bucket." }
   ```

   ```json
   { "mypack.hint.lava": "水バケツを持ち歩こう。" }
   ```

翻訳は各プレイヤーのクライアントで行われるので、それぞれが自分の言語で読めます。

::: warning mod なしのプレイヤー
サーバーはリソースパックを読めないため、mod なしのプレイヤー（サーバーからの英語チャット）には翻訳キーがそのまま見えることがあります。mod なしで参加する人が多いなら、普通の文章にするか [requireClientMod](/ja/modpack/require-client-mod) を使ってください。
:::

## ダメージを隠す {#hiding-damage}

`defaultIgnoredDamageIds` は、全員に対してダメージを隠します。パネル、履歴、要約に表示されません。

```json
{
  "defaultIgnoredDamageIds": [
    "minecraft:cactus",
    "minecraft:sweetBerryBush",
    "somemod:*_tick"
  ]
}
```

- プレイヤーは `/whm ignore <id>` や `[Mute]` ボタンで、自分用にさらに隠せます。
- `whathitme-common.cfg` の `ignoredDamageIds` も pack のキーと同じ働きです。
- ごく小さなダメージをそもそも記録しないようにするには `minDamageThreshold`（初期値 0.05）を使います。

## 名前とヒントを設定するほかの方法

| 場所 | 範囲 | 複合キー |
|---|---|---|
| pack の JSON（`pack/*.json`） | 全員 | 可 |
| `whathitme-common.cfg` の `renameMap` / `hintMap` | 全員。pack の JSON より優先 | 可 |
| `/whm server rename` / `sethint`（OP） | `whathitme-common.cfg` に書き込む | 可（1 コマンドで 1 キー） |
| `whathitme-client.cfg`、クライアントでの `/whm rename` | そのプレイヤーだけ。何よりも優先 | 可 |
| [進行度ルール](/ja/modpack/rules)の `name` / `hint` <Badge type="tip" text="1.3.0+" /> | プレイヤーの進行度ごと。`renameMap` / `hintMap` より優先 | 可 |

::: tip 全部の一覧から始める
`/whm dump` は、既知のダメージ ID とすべての mob を値を空にして並べた `config/whathitme/hint_template.cfg` を書き出します。そのブロックを `whathitme-common.cfg` に貼り付けて、必要なものだけ埋めてください。空の値は無視されます。[スキャナー・dump・/whm test](/ja/modpack/scanner) を参照。
:::

## 大きな pack {#large-packs}

What Hit Me? 1.3 のサーバーは、リストがどれだけ長くても 1.3 のクライアントにすべての項目を送ります。**What Hit Me? 1.2 のクライアント**が受け取るのは各リスト（設定ファイルの `renameMap` と `hintMap`、pack フォルダーの `renameMap` と `hintMap`、非表示 ID、ロックした機能）の最初の 1000 項目だけです。1.7.10 のサーバーでは文章がとても長いとさらに少なくなることがあります。それより後の項目は、そのクライアントでは元の名前のまま、ヒントなしで表示されます。mod を入れていないプレイヤーと、サーバーが決める部分（ルール、死亡メッセージ、サーバーからのチャット）には影響しません。リストが 1000 項目を超えると `/whm reload` とサーバーログに警告が出ます。

::: tip ワイルドカードでキーをまとめる
[ワイルドカードのキー](/ja/modpack/match-keys#ワイルドカード) 1 つで何百もの ID をまとめられ、pack の手入れも楽になります：

```json
{
  "hintMap": {
    "*@twilightforest:lich": "柱の陰に隠れて、シールドの弾を打ち返す。",
    "@twilightforest:*": "Twilight Forest の mob：Twilight Forest のガイドを確認。",
    "*acid*": "酸：防具をフルで着るか、耐火のポーションを飲む。"
  }
}
```

いちばん具体的なキーが優先されるので、一般的なキーと並べて特別な項目をいくつか残せます。
:::
