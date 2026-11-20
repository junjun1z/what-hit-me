# スキャナー・dump・/whm test

名前やヒントを書くにはダメージ ID が必要です。ここで紹介する機能を使えば、**誰かがそのダメージを受ける前に** ID を一覧にでき、死ななくても設定を確認できます。

## ダメージ ID スキャナー

| Minecraft | ID の見つけ方 |
|---|---|
| 1.7.10 - 1.19.3 | キャッシュが使えないとき（初回起動、mod リストの変更、`/whm reload`）、バックグラウンドのスレッドが mod の jar のクラスファイルを（クラスを読み込まずに）読み、ダメージソースのコンストラクターに渡される文字列 ID を集めます。サブクラスや、コンストラクター直後のセッター呼び出しも対象です。jar の中の jar も 1 階層まで読みます。 |
| 1.19.4+ | ゲームのダメージタイプのレジストリのキーをすべて、タグから得た属性と一緒に並べます。スキャンは不要です。 |

結果は `config/whathitme/.cache/catalog_cache.json` の `scan` に保存され、次の場所に出てきます。

- `/whm search #scanned`
- `/whm test` の Tab 補完
- `/whm dump`（`source` 列）

### カタログのキャッシュ

ダメージの種類・mob・効果は 1 回だけ調べ、`modid:version` の一覧（並べ替え済み）の SHA-256 と一緒に保存します。次回の起動で mod が同じならキャッシュから読み込みます。mod リストが変わったとき、または `/whm reload` のときに調べ直します。

## `/whm dump` {#whm-dump}

OP 専用です。2 つのファイルを書き出します。

### 1. `config/whathitme/.cache/whathitme_dump.csv`

ダメージの種類・mob・効果を 1 行ずつ並べたものです。表計算ソフトで開けます。

| 列 | 内容 |
|---|---|
| `category` | damage、mob、effect |
| `id` | pack ファイルで使う ID |
| `name`、`mod_id`、`mod_name` | 表示名と mod |
| `properties` | Bypasses Armor、Fire など |
| `times_seen` | このワールドで起きた回数 |
| `custom_name`、`custom_tip`、`ignored` | 今の設定での扱い |
| `source` | `seen`（ゲーム内で発生）、`scanned`（スキャナー）、`registry`（1.19.4+ のレジストリ） |

### 2. `config/whathitme/hint_template.cfg`

既知のダメージ ID すべてと、登録済みのすべての mob の `mob:<id>` を、**値を空にして**並べた、そのまま貼れる `S:hintMap <...>` と `S:renameMap <...>` のブロックです。

```
S:hintMap <
    minecraft:lava =
    mob:minecraft:creeper =
    mob:twilightforest:lich =
 >
```

ブロックを `whathitme-common.cfg` に貼り付けて、必要なところから少しずつ埋めてください。空の値は無視されるので、埋めていない行は何もしません。

## `/whm test` {#whm-test}

OP 専用です。仮のとどめの一撃を作り、プレイヤーに何が見えるかを表示します。

```
/whm test <damage_id> [entity_id] [item_id] [dim_id] [--as <player>] [--assume <condition>]...
```

| 引数 | 意味 |
|---|---|
| `damage_id` | ダメージ ID（例：`minecraft:lava`、`mob:minecraft:zombie`） |
| `entity_id` | 攻撃者（`-` = なし。`mob:` の ID なら ID からエンティティを取ります） |
| `item_id` | 攻撃者のメインハンドのアイテム（`-` = なし） |
| `dim_id` | ディメンション（初期値：自分のいる場所） |
| `--as <player>` <Badge type="tip" text="1.3.0+" /> | オンラインのそのプレイヤーの進行度で[ルール](/ja/modpack/rules)を判定 |
| `--assume <condition>` <Badge type="tip" text="1.3.0+" /> | 条件を満たしたものとして扱う（`!条件` なら満たしていないものとして）。何度でも書けます。 |

表示される内容：

1. 当てはまったルール、隠される項目、強制されるかどうか <Badge type="tip" text="1.3.0+" />
2. 結果の**名前**と**ヒント**
3. 実行されるはずの**死亡トリガー**とルールのコマンド（プレースホルダーを埋めた状態。**実行はしません**）
4. 使われる**死亡メッセージ** <Badge type="tip" text="1.3.0+" />
5. **リスポーン後のチャット要約**のプレビュー

例：

```
/whm test minecraft:lava
/whm test mob:minecraft:zombie - minecraft:iron_sword
/whm test minecraft:arrow minecraft:skeleton
/whm test twilightforest:lich_bolt twilightforest:lich - twilightforest:twilight_forest
/whm test minecraft:generic --as Steve
/whm test minecraft:generic --as Steve --assume advancement:minecraft:story/mine_diamond --assume !tag:veteran
```

::: tip 配布前に確認を
pack ファイルを変えるたびに、`/whm reload` で警告を読み、変えたキーを `/whm test` で確かめましょう。ルールがあるなら、`--assume` で「達成前」と「達成後」の両方を確認してください。
:::

![/whm test でウィザースケルトンを --as Steve で試したところ：一致したルール、隠す項目、チャットのプレビュー](/images/ja/whm-test.webp)
