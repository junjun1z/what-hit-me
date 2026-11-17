# 設定ファイル

ファイルはすべて `config/whathitme/` にあります。2 つの `.cfg` ファイルは、どのローダーでも（Fabric でも）Forge の設定ファイル形式で、`/whm reload` で読み直されます。

| ファイル | 対象 | 内容 |
|---|---|---|
| `whathitme-common.cfg` | Modpack 制作者、サーバー管理者 | 表示スイッチ、ヒントのスイッチ、フィルター、記録。mod 入りのサーバーは、ログイン時・読み直し後・設定変更後にこれを mod 入りの全クライアントへ送り、クライアント自身の common ファイルの代わりに使わせます。 |
| `whathitme-client.cfg` | 各プレイヤー | 個人用の表示スイッチとフィルター、`showHints`、パネルの配置 |
| `whathitme-client-templates.json` <Badge type="tip" text="1.3.0+" /> | 各プレイヤー | 個人用の[テンプレート](/ja/modpack/templates)。コメントアウトされた例と一緒に作られます。 |
| `pack/` | Modpack 制作者 | [pack フォルダー](/ja/modpack/pack-folder)：名前、ヒント、非表示 ID、設定のロック、死亡トリガー、テンプレート、死亡メッセージ、ルール |

::: tip 2 つのファイルのルール
表示項目は**両方**のファイルで `true` のときだけ表示されます。個人の名前とヒントは pack の設定の上に重ねて適用されます。優先順位：`.cfg` の項目 > pack の JSON > 組み込みの自動ヒント。
:::

## 表示スイッチ

カテゴリー `display`。**両方**のファイルにあり、初期値はすべて `true` です。

| キー | 表示するもの |
|---|---|
| `enableDeathScreenPanel` | 死亡画面のまとめパネル |
| `enableRespawnChatSummary` | リスポーン後のチャット要約（`/whm togglechat`） |
| `enableTotemChatAlert` | 不死のトーテム（や他の mod）で助かったときの、詳細リンク付きの通知 |
| `enableHeavyHitSnapshot` | 1 回で最大体力の `heavyHitThreshold` 以上を受けたときのスナップショットと通知 |
| `showTimestamp` | ダメージを受けてからの時間 |
| `showDamageAmount` | 最終的なダメージ量 |
| `showMitigationDetails` | 防具で軽減する前のダメージ、軽減率、衝撃吸収、被弾後の体力 |
| `showSmartMergeHitCount` | まとめた連続ダメージの回数（`x12 hits`） |
| `showSourceModName` | ダメージまたは攻撃者を追加している mod の名前 |
| `showDamageProperties` | 炎・魔法・防具無視などの属性 |
| `showInternalDamageId` | 詳細表示の内部ダメージ ID |
| `showAttackerName` | 攻撃者の名前 |
| `showAttackerWeapon` | 攻撃者の武器とエンチャント |
| `showAttackerDistance` | 攻撃者との距離 |
| `showAttackerInvisibility` | 攻撃者が透明だったときのバッジ |
| `showEntityOwner` | タレット・ペット・召喚物・飛び道具の持ち主 |
| `showActionButtons` | クリックできる `[Copy ID]`・`[Mute]`・`[Copy Recap]` ボタン |
| `showToggleChatNotice` | 要約の最後の `/whm togglechat` の案内 |
| `showMaxHpPercentage` | 最大体力に対する割合（例：`(30% Max HP)`） |
| `showEnvironmentContext` | 攻撃者のいないダメージで、足元と体の位置のブロック / 液体と手のアイテム |
| `showReflectedBadge` | `[Reflected / Thorns]` バッジ |
| `showArmorBreakEvents` | 防具が壊れたときのタイムラインの行 |
| `includeEquipmentInCopy` | `[Copy Recap]` にディメンション・最大体力・防具・効果を追加 |
| `includeCoordsInCopy` | `[Copy Recap]` に死亡地点の座標を追加（`includeEquipmentInCopy` が必要） |

### ヒントのスイッチ

| キー | ファイル | 初期値 | 意味 |
|---|---|---|---|
| `enableAutoHints` | common | `true` | ダメージの種類と属性から作る自動ヒント |
| `enableCustomHints` | common | `true` | `hintMap`（設定ファイルと pack フォルダー）のカスタムヒント |
| `showHints` | client | `true` | ヒント（自動と pack のもの）をそもそも表示するか |

1.0 / 1.1 の `showHintsAndTips` の後継です。古い `showHintsAndTips=false` があると 3 つともオフになり、古いキーは削除されます。

## フィルター

カテゴリー `filters`。**両方**のファイルにあります。

| キー | 初期値 | 意味 |
|---|---|---|
| `minDamageThreshold` | `0.05` | これ未満のダメージ（体力 + 衝撃吸収）は記録しない。範囲 0 - 1000 |
| `heavyHitThreshold` | `0.8` | 1 回で最大体力のこの割合以上を受けたら大ダメージ（0.8 = 80 %）。範囲 0.05 - 100 |
| `ignoredDamageIds` | 空 | 非表示にする[マッチキー](/ja/modpack/match-keys)（`*` ワイルドカード、複合キー） |
| `renameMap` | 空 | `キー = 名前` または `キー = lang:翻訳キー` |
| `hintMap` | 空 | `キー = ヒント` または `キー = lang:翻訳キー` |

`whathitme-client.cfg` のしきい値が効くのはクライアントだけのモードのときだけです。それ以外はサーバーがダメージを記録します。

ファイル内のリストはこのように書きます。

```
S:renameMap <
    minecraft:outOfWorld = &5The Abyss
    mob:minecraft:zombie @item:minecraft:iron_sword = Armed Zombie
 >
```

## 記録

カテゴリー `tracking`。**common ファイルだけ**にあります。

| キー | 初期値 | 範囲 | 意味 |
|---|---|---|---|
| `historySize` | `50` | 5 - 500 | プレイヤーごとに保持する記録の数（リングバッファ） |
| `mergeWindowSeconds` | `3.0` | 0 - 60 | 連続ダメージをまとめる時間（0 = まとめない） |
| `summarySeconds` | `10` | 1 - 300 | 「Last Xs Total」の時間 |
| `recapSeconds` | `30` | 1 - 600 | 死亡のまとめに含める時間 |
| `maxSnapshots` | `5` | 1 - 50 | プレイヤーごとのトーテム / 大ダメージのスナップショット数 |
| `requireClientMod` <Badge type="tip" text="1.3.0+" /> | `false` | | サーバー：What Hit Me? を入れていないクライアントを拒否する（バージョンは問わない）。[requireClientMod](/ja/modpack/require-client-mod) を参照 |

## 配置

カテゴリー `layout`。**client ファイルだけ**にあります。

| キー | 初期値 | 意味 |
|---|---|---|
| `anchorPoint` | `CENTER_LEFT` | `TOP_LEFT`、`TOP_RIGHT`、`BOTTOM_LEFT`、`BOTTOM_RIGHT`、`CENTER_LEFT`、`CENTER_RIGHT` |
| `offsetX` / `offsetY` | `4` / `0` | 基準位置からのずれ（GUI ピクセル）。パネルをドラッグすると保存されます |
| `scale` | `0.85` | パネルの大きさ（0.25 - 3） |
| `backgroundOpacity` | `0.75` | 背景の不透明度 0.0 - 1.0 |
| `maxVisibleLines` | `8` | スクロールせずに見える行数（1 - 40） |
| `minimized` | `false` | `[-]` ボタンで保存される |
| `chatHistoryLines` | `15` | `/whm history` で表示する行数（1 - 100） |

## キャッシュフォルダー

`config/whathitme/.cache/` には mod が自分で書くファイルが入ります。

| ファイル | 内容 |
|---|---|
| `catalog_cache.json` | ダメージの種類・mob・効果・スキャンしたダメージ ID と、`modid:version` の一覧の SHA-256 |
| `stats_<uuid>.json` | `/whm stats` のデータと、実行済みの `oncePerPlayer` トリガー |
| `whathitme_dump.csv` | `/whm dump` が書き出す |

::: warning
`stats_<uuid>.json` を消すと、そのプレイヤーの統計がリセットされ、`oncePerPlayer` の死亡トリガーがもう一度動くようになります。カタログのキャッシュは自動で作り直されます。
:::

## 設定の変え方

- ゲーム内：[設定画面](/ja/player/settings)、または `/whm config <key> <value>`
- ファイル：編集してから `/whm reload`（OP）
