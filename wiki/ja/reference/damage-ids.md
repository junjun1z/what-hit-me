# ダメージ ID とバッジ

1 回 1 回のダメージには**ダメージ ID** が付きます。名前、ヒント、非表示、死亡トリガー、死亡メッセージ、ルールのすべてでこの ID を使います。詳細表示の `ID: ...`、`[Copy ID]`、[`/whm search`](/ja/player/tools#search-whm-search)、[`/whm dump`](/ja/modpack/scanner#whm-dump) で確認できます。

## ID の作られ方

| ダメージ | ID | 例 |
|---|---|---|
| エンティティの攻撃（近接） | `mob:<エンティティ ID>` | `mob:minecraft:zombie`、`mob:minecraft:player`、`mob:lycanitesmobs:ventoraptor` |
| それ以外 | `<modid>:<ダメージの種類>` | `minecraft:fall`、`minecraft:lava`、`minecraft:arrow`、`minecraft:outOfWorld`、`minecraft:explosion.player` |
| mod が不明 | `unknown:<ダメージの種類>` | |
| ルールで隠したもの <Badge type="tip" text="1.3.0+" /> | `whathitme:hidden` | |

ダメージの種類がどの mod のものかは、バニラの種類の一覧、ダメージソースのクラスがある jar、種類名の `modid.` の接頭辞、または（新しい種類ごとに 1 回だけ）ダメージを与えた mod のクラスから判断します。

飛び道具の場合、攻撃者は撃った側で、飛び道具は「via Arrow」のように表示します。攻撃者のエンティティ ID は別に保存しているので、`minecraft:arrow @minecraft:skeleton` のような[マッチキー](/ja/modpack/match-keys)でスケルトンの矢とプレイヤーの矢を区別できます。

::: tip ダメージの種類名はバージョンで違う
古いバージョンでは `outOfWorld` や `inFire`、1.19.4 以降では `out_of_world` のようなレジストリ名を使います。自分の pack のバージョンで `/whm search` や `/whm dump` を使って確認するか、ワイルドカード（`minecraft:out*world`）を使ってください。
:::

## 属性

| 属性 | 意味 |
|---|---|
| Bypasses Armor | 防具で軽減されない |
| Bypasses Magic | 防具・エンチャント・効果のどれでも軽減されない（絶対ダメージ） |
| Magic | 魔法ダメージ |
| Fire | 炎ダメージ |
| Explosion | 爆発 |
| Projectile | 飛び道具 |
| Through Shield | 盾で防御中に受けた |
| Ignores I-Frames | 前のダメージの無敵時間中に新しく受けた |
| Bypasses Creative | クリエイティブモードでも受ける |

## バッジ

| バッジ | 意味 |
|---|---|
| `☠` | とどめの一撃 |
| `[Invisible]` | 攻撃者が透明だった |
| `[Totem]` | 不死のトーテムが発動した |
| `[Heavy Hit]` | 最大体力の `heavyHitThreshold` 以上を受けた |
| `[⚡ Reflected / Thorns]` | 棘の鎧、または 50 ms 以内に攻撃した相手からのダメージ |
| `[⚠ Broken: <防具>]` | 防具が壊れた |
| `[Localized / Vital Hit (...)]` | 合計体力が 0 より多いまま死んだ（部位別体力の mod） |
| `[Client Est.]` | クライアントの推定（サーバーに mod がない） |
| `x12 hits` | まとめた連続ダメージ |

## 自動ヒント

カスタムヒントがない場合は、ダメージの種類と属性からヒントを選びます：落下、炎、溶岩、溺れ、奈落、爆発、飛び道具、防具無視、絶対ダメージ、魔法、ウィザー、透明な攻撃者、近接、プレイヤー、反射ダメージなど。ダメージ ID ごとに [`hintMap`](/ja/modpack/names-and-tips) で置き換えるか、`enableAutoHints=false` でオフにできます。
