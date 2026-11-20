# 死亡画面のパネル

死ぬと、バニラの死亡画面の横にパネルが表示されます。**クライアント**に mod が必要です（サーバー側はあってもなくても構いません）。

![死亡画面のパネル](/images/ja/death-screen.webp)

![攻撃を受けてから死亡画面のパネル、詳細表示、リスポーン後のチャット要約まで](/images/ja/death-recap.gif)

## パネルの内容

上から順に：

1. **ヘッダー** `❤ What Hit Me?` と `[Copy]`・`[-]` ボタン
2. **とどめの一撃**：何に倒されたかと、その一撃のダメージ（例：`☠ Creeper (Explosion) [26.4 ❤]`）
3. とどめの一撃の**属性**：`Explosion`、`Fire`、`Magic`、`Bypasses Armor` など
4. **タイムライン**：直前 30 秒（`recapSeconds`）のすべての被ダメージ。新しい順に、死亡までの時間（`-1.3s`）、名前、ダメージ（ハート）、まとめた回数（`x2`）
5. **Last 10s Total**：直前 10 秒（`summarySeconds`）の合計ダメージと回数
6. **ヒント**：とどめの一撃への対策

ダメージは**ハート**（`❤`）で表示します。ハート 1 個 = 体力 2 ポイントです。

## パネルの操作

| 操作 | 方法 |
|---|---|
| パネルを動かす | ヘッダーをマウスでドラッグ。ボタンを離した位置が保存されます。 |
| 最小化 / 元に戻す | `[-]` / `[+]` をクリック。状態は保存されます。 |
| タイムラインをスクロール | パネルの上でマウスホイール（`maxVisibleLines` より行が多いとき） |
| 1 回分の詳細を見る | 行にマウスを乗せる |
| まとめをコピー | `[Copy]` をクリック。[Copy Recap](/ja/player/chat#copy-recap) を参照 |

GUI スケールが大きいときや小さいウィンドウでは、パネルが自動で縮み、「リスポーン」「タイトル画面へ」のボタンに重なりません。

::: tip 大きさと位置を変える
`scale`、`backgroundOpacity`、`maxVisibleLines`、`anchorPoint` は `whathitme-client.cfg` と[設定画面](/ja/player/settings)にあります。コマンドでも変えられます。たとえば右上に置いて大きくするなら：

```
/whm config anchorPoint TOP_RIGHT
/whm config scale 1.0
/whm config maxVisibleLines 12
```
:::

## マウスオーバーの詳細 {#hover-details}

パネル、`/whm`、チャット要約のどの行でも、マウスを乗せるとその一撃についてわかっていることがすべて表示されます。

![/whm 履歴の詳細表示](/images/ja/death-hover.webp)

| 行 | 意味 | スイッチ |
|---|---|---|
| 名前（太字） | カスタム名、攻撃者、またはダメージの種類 | |
| `ID: mob:minecraft:zombie` | 内部の[ダメージ ID](/ja/reference/damage-ids) | `showInternalDamageId` |
| `Damage: 3.8 ❤ (7.6 HP)` | 実際に受けたダメージ | `showDamageAmount` |
| `(30% Max HP)` | そのときの最大体力に対する割合 | `showMaxHpPercentage` |
| `Before armor: 7 ❤` / `Mitigated: 46%` | 防具で軽減する前のダメージと、防具・エンチャント・効果で防いだ割合 | `showMitigationDetails` |
| `Absorbed` | 使われた衝撃吸収（黄色いハート） | `showMitigationDetails` |
| `Health after: 3.8 / 10 ❤` | 被弾後の体力 | `showMitigationDetails` |
| `Merged hits: 2` | この行にまとめた連続ダメージの回数 | `showSmartMergeHitCount` |
| `Attacker`、`via Arrow` | 攻撃者。飛び道具が別ならその名前も | `showAttackerName` |
| `Weapon: Iron Sword [Sharpness III, Fire Aspect I]` | 攻撃者がメインハンドに持っていたアイテムとエンチャント | `showAttackerWeapon` |
| `Distance: 9.2m` | 攻撃者との距離 | `showAttackerDistance` |
| `Owner` | ペット・タレット・召喚物・飛び道具の持ち主 | `showEntityOwner` |
| `[Invisible]` | 攻撃者が透明だった | `showAttackerInvisibility` |
| `Properties` | 炎、魔法、爆発、飛び道具、防具無視など | `showDamageProperties` |
| `Mod: Minecraft` | ダメージまたは攻撃者を追加している mod | `showSourceModName` |
| 周囲の状況 | 攻撃者のいないダメージで、足元と体の位置のブロック / 液体、両手のアイテム（それぞれの mod 付き） | `showEnvironmentContext` |
| `Tip` | 対策のヒント | `showHints` |

modpack では、mod 名を見ればその生き物がどこから来たのかがすぐにわかります。

![modpack での詳細表示](/images/modpack-death-screen.png)

## 特別な行とバッジ

| 表示 | 意味 |
|---|---|
| `x12 hits` | 3 秒以内（`mergeWindowSeconds`）の同じ原因の連続ダメージを 1 行にまとめたもの。間に別のダメージが挟まっても（mob の攻撃の合間の毒など）まとめます。 |
| `[⚡ Reflected / Thorns]` | 棘の鎧、または 50 ms 以内に自分が攻撃した相手からのダメージ。専用のヒントが出ます。 |
| `[⚠ Broken: Diamond Chestplate]` | タイムラインのこの時点で防具が壊れた（`showArmorBreakEvents`） |
| `[Localized / Vital Hit (Died with 4.5 ❤ total HP left)]` | 合計体力がまだ 0 より多いのに死んだ（First Aid のような部位別体力 mod） |
| `[Client Est.]` | サーバーに mod がないため、クライアントが推定した記録 |
| `[Totem]`、`[Heavy Hit]` | トーテムが発動した / [大ダメージのスナップショット](/ja/player/chat#alerts-and-snapshots)が保存された一撃 |
| `☠` | とどめの一撃 |

::: info ダメージイベントのない死亡
mod によっては、体力を直接 0 にするなどしてプレイヤーを倒すことがあります。その場合も死亡イベントからとどめの一撃を作るので、まとめが空になることはありません。
:::

## パネルをオフにする

- 自分だけ：`whathitme-client.cfg` で `enableDeathScreenPanel=false`、または設定画面
- 全員（pack / サーバー）：`whathitme-common.cfg` の同じキー

1.3.0 からは、パネルの行と詳細表示を[テンプレート](/ja/modpack/templates)で書き換えられます <Badge type="tip" text="1.3.0+" />。
