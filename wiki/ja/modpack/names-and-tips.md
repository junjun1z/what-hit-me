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
