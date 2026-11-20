# スクリプト API リファレンス <Badge type="tip" text="1.3.0+" />

すべての連携（[KubeJS](/ja/modpack/kubejs)、[CraftTweaker](/ja/modpack/crafttweaker)、[MineTweaker 3](/ja/modpack/minetweaker)）にあるメソッドとオブジェクト、ほかの mod が呼べる Java API です。連携によって違う部分（MineTweaker 3 の `Api` / `Player` オブジェクトなど）は、それぞれのページに書いてあります。

## 登録

| メソッド | 効果 |
|---|---|
| `condition(name, [description,] fn)` | ルール条件 `script:<name>`。`fn(player)` が true / false を返す |
| `variable(name, [description,] fn)` | テンプレート変数 `{script:<name>}`。`fn(player, damage)` が文字列を返す（`&` の色コード、`lang:` キー可） |
| `addRename(key, name)` / `addHint(key, tip)` | `renameMap` / `hintMap` と同じ（[マッチキー](/ja/modpack/match-keys)） |
| `addDeathMessage(key, message)` | [`deathMessages`](/ja/modpack/death-messages) と同じ |
| `addTemplate(output, lines)` | `hover`、`line`、`summary`、`alert`、`copy` の[テンプレート](/ja/modpack/templates) |
| `addRule(rule)` | [`rules`](/ja/modpack/rules) の 1 項目（KubeJS はオブジェクト、ZenScript は JSON の文字列） |

名前に使えるのは英数字と `_` `.` `-` で、大文字と小文字は区別しません。同じ名前で登録し直すと置き換わります。実行時の設定はパックのファイルの**後に**合成される追加の設定元で、パックのファイル自体は変更されません。`/whm reload` では `script:<mod>` として表示されます。

## イベント

| イベント | プロパティ | メソッド |
|---|---|---|
| `damageRecorded` | `player`、`damage` | `setName(text)`、`setHint(text)`（`""` = ヒントなし）、`addBadge(text)`、`hide(field)`、`cancel()` |
| `death` | `player`、`death`、`killingBlow` | `setName`、`setHint`、`addBadge`、`hide`（すべてとどめの一撃に対して）、`addSummaryLine(text)` |
| `respawn` | `player`、`death` | - |
| `alert` | `player`、`kind`（`totem`、`heavy`、`prevented`）、`damage`、`snapshot` | - |

`hide` にはルールの `hide` リストと同じ項目名を使います。一致するルールがあれば、名前とヒントはルールが優先されます。ルールとスクリプトの非表示項目は合わせて適用されます。まとめの行とバッジには `&` の色コード、`lang:` キー、`{player}` などのテンプレート変数を使えます。

## Damage

すべてのスクリプト mod: `damageId`、`damageType`、`name`（英語。名前の変更やルールを反映した表示名）、`attackerName`、`attackerId`、`modId`、`weaponName`、`weaponId`、`amount`（体力のポイント）、`hearts`、`healthAfter`、`maxHealth`、`distance`（-1 = 不明）、`hits`、`dimension`、`killingBlow`、`projectile`、`fire`、`explosion`、`magic`、`variable(name)`。
KubeJS のみ: `directName`、`modName`、`ownerName`、`rawAmount`、`absorbed`、`time`、`totem`、`heavy`、`reflected`、`bypassArmor`、`properties`、`hidden`、`badges`。

## Death

`kind`、`time`、`records`（古い順）、`killingBlow`、`deathMessage`（英語の文字列）、`totalAmount`。

## 読み取り

| メソッド | 結果 |
|---|---|
| `lastDeath(player)` | 最後の死亡のまとめ（なければ空） |
| `history(player, n)` | 最新の `n` 件の記録（古い順） |
| `stats(player)` | `deaths`、`damageTaken`、`causes`（KubeJS） |
| `test(player, damageId[, entityId])` | そのプレイヤーで `/whm test` が表示する内容: `name`、`hint`、`rule`、`hidden`、`deathMessage`、`commands` |
| `check(player, condition)` | 任意のルール条件。例: `check(player, 'advancement:minecraft:story/mine_diamond')` |
| `registrations()`（KubeJS） | スクリプトが登録したもの: `kind`、`owner`、`name`、`value`、`fromHook`、`failed` |
| `apiVersion`（KubeJS） | What Hit Me? の API のバージョン。現在は `1` で、メソッドが増えると上がります |

## mod 開発者向けの Java API {#java-api}

ほかの mod は、スクリプト mod なしで同じ API を Java から使えます: `io.github.junjun1z.whathitme.api.WhatHitMeApi`（Java 8、Minecraft のクラスなし）。owner には自分の mod ID を渡します。プレイヤーは Minecraft のプレイヤーオブジェクト、UUID、名前のどれでも渡せます。

```java
WhatHitMeApi.registerCondition("mymod", "boss_ready", "ボスの祭壇が起動済み", p -> MyData.ready(p.getUuid()));
WhatHitMeApi.listen("mymod", DeathEvent.class, e -> e.addSummaryLine("&7My Mod からのヒント"));
```

| メソッド | 内容 |
|---|---|
| `API_VERSION` | 1.3.0 では `1` |
| `registerCondition(owner, name, description, condition)` / `registerVariable(owner, name, description, variable)` | `script:<name>` の条件（`WhmPlayer -> boolean`）/ `{script:<name>}` の変数（`(WhmPlayer, WhmDamage) -> 文字列`） |
| `listen(owner, イベントのクラス, listener)` / `listen(owner, "death", listener)` | `DamageRecordedEvent`、`DeathEvent`、`RespawnEvent`、`AlertEvent`（名前は `damageRecorded`、`death`、`respawn`、`alert`） |
| `onRegister(owner, hook)` | 作り直すたび（サーバーの起動、`/whm reload`、スクリプト mod の再読み込み。サーバーが動いていればすぐ）にフックを実行します。フックが登録したものは作り直しの前に消えます。フックは owner ごとに 1 つで、新しいものが古いものを置き換えます |
| `addRename`、`addHint`、`addDeathMessage`、`addTemplate`、`addRule(owner, ...)` | 実行時のパック設定（出どころ `script:<owner>`）。パックのファイルの後に合成されます |
| `lastDeath(player)`、`history(player, n)`、`stats(player)`、`test(player, damageId, entityId[, assume])`、`player(player)` | 読み取り。`player(...)` は `getUuid()`、`getName()`、`getNativePlayer()`、`check(condition)` を持つ `WhmPlayer` を返します |
| `registrations()` | 登録されたすべてのもの（owner と失敗の有無付き） |
| `clear(owner)` / `removeHooks(owner)` | その owner のものをすべて消す / 登録フックだけ消す |
| `rebuild()` / `rebuildLater()` | すぐに作り直す / 次のサーバー tick で作り直す（「スクリプトの後」のフックがないスクリプト mod 用） |

What Hit Me? が入っているときだけ連携用のクラスを読み込むようにしてください（mod が読み込まれているかを確認してから呼ぶなど）。そうすれば、あなたの mod にとっても What Hit Me? は任意のままです。
