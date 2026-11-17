# マッチキー

ダメージを選ぶ場所 —— `renameMap`、`hintMap`、`ignoredDamageIds`、`defaultIgnoredDamageIds`、`deathTriggers`、`deathMessages`、`rules` —— では、すべて同じ**マッチキー**の書き方を使います。

## いちばん簡単なキー：ダメージ ID

```json
"minecraft:lava": "..."
"mob:minecraft:zombie": "..."
```

ID の作られ方は[ダメージ ID](/ja/reference/damage-ids)を参照してください。

## 複合キー

条件を足して絞り込みます。順番は自由で、スペースか `&` で区切ります。

| 条件 | 当てはまるもの | 例 |
|---|---|---|
| `<ダメージ ID>` | ダメージ ID | `minecraft:arrow`、`mob:minecraft:zombie` |
| `@<エンティティ ID>` または `@mob:<エンティティ ID>` | 攻撃者のエンティティ ID（`@entity:` も可） | `@minecraft:skeleton` |
| `@item:<アイテム ID>` | 攻撃者がメインハンドに持っているアイテム | `@item:minecraft:iron_sword` |
| `@dim:<ディメンション ID>` | ダメージを受けたディメンション | `@dim:minecraft:the_nether` |

**すべての条件**に当てはまる必要があります。

```json
{
  "renameMap": {
    "mob:minecraft:zombie @item:minecraft:iron_sword": "武装ゾンビ",
    "minecraft:arrow @minecraft:skeleton": "スケルトンの狙撃",
    "minecraft:lava @dim:minecraft:the_nether": "ネザーの溶岩"
  }
}
```

### スペースなしで書く

`@` で始まる条件は、前の条件にそのまま続けて書けます。

```
*@twilightforest:lich                         リッチからのあらゆるダメージ
minecraft:arrow@twilightforest:skeleton_druid その mob の矢
```

### ディメンション

`@dim:the_nether`（名前空間なし）は `minecraft:the_nether` にも当てはまります。

## ワイルドカード

どの条件でも `*`（任意の文字列）と `?`（任意の 1 文字）が使えます。大文字と小文字は区別しません。

| キー | 当てはまるもの |
|---|---|
| `*@twilightforest:lich` または `@twilightforest:lich` | ダメージ ID が何であれ、リッチからのすべての攻撃 |
| `@twilightforest:*` | Twilight Forest のすべての攻撃者 |
| `mob:lycanitesmobs:*` | Lycanites Mobs の全生物の近接攻撃 |
| `*acid*` | 「acid」を含むすべてのダメージ ID（実行時に ID を作る mod に便利） |
| `minecraft:arrow @twilightforest:skeleton_druid` | その mob の矢だけ |
| `*` | すべて（優先度が最も低い受け皿） |

## どのキーが使われる？

`renameMap`、`hintMap`、`deathMessages` で同じダメージに複数のキーが当てはまるときは、**最も具体的な**キーが使われます。

1. **条件の多い**キーが勝ちます。`*` だけの条件は数に入りません。
2. 同じ数なら、**ワイルドカードを使った条件の少ない**キーが勝ちます。

例（すべて同じ `hintMap` にあるとき）：

| キー | 条件の数 | リッチの弾で使われる？ |
|---|---|---|
| `*` | 0 | 他に何も当てはまらないときだけ |
| `@twilightforest:lich` | 1 | |
| `twilightforest:lich_bolt @twilightforest:lich` | 2 | **これ** |

つまり、mob 全体向けの一般的なヒント、その攻撃の 1 つ向けの特別なヒント、すべての受け皿としての `*` を同時に書けます。

::: warning ルールは別の決め方
[進行度ルール](/ja/modpack/rules)は具体性で選び**ません**。上から順に調べて、**最初に**当てはまったルールが使われます。特別なルールを一般的なルールより前に書いてください。
:::

その他の場所：

- `ignoredDamageIds` / `defaultIgnoredDamageIds`：**どれか 1 つでも**当てはまれば非表示
- `deathTriggers`：当てはまったトリガーが**すべて**、ファイルの順に実行される

## 正しい ID を探す

| 知りたいこと | 方法 |
|---|---|
| ある一撃のダメージ ID | 詳細表示の `ID: ...`、または `[Copy ID]` |
| ある mod のエンティティ ID・ダメージ ID | `/whm search @modid` |
| まとめて全部 | `/whm dump` で CSV と貼り付け用のテンプレートを書き出す |
| キーを試す | `/whm test <damage_id> [entity] [item] [dim]` |
