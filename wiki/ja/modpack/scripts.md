# スクリプト連携 <Badge type="tip" text="1.3.0+" />

KubeJS や CraftTweaker のスクリプトから、サーバー側で What Hit Me? を拡張できます。

| 機能 | スクリプトがすること | 使う場所 |
|---|---|---|
| **条件** | `名前 -> (player) => true/false` を登録する | [ルール](/ja/modpack/rules)と[死亡トリガー](/ja/modpack/death-triggers)で `script:<名前>`（`!script:<名前>` で否定） |
| **変数** | `名前 -> (player, damage) => 文字列` を登録する | [テンプレート](/ja/modpack/templates)の `{script:<名前>}`、ルールの名前 / ヒント、死亡トリガーのコマンド |
| **イベント** | `damageRecorded`、`death`、`respawn`、`alert` を受け取る | ダメージの名前 / ヒントの変更、バッジの追加、項目の非表示、記録の取り消し、まとめへの行の追加、任意の処理 |
| **読み書き** | 最後の死亡、履歴、統計、`/whm test` の結果を読む。名前の変更・ヒント・ルール・死亡メッセージ・テンプレートを実行中に追加する | なんでも |

[ルール](/ja/modpack/rules)と同じく、すべてサーバーが決めてプレイヤーに送ります。mod を入れているプレイヤーは自分の（翻訳された）表示でスクリプトの値を見られ、mod がないプレイヤーにはサーバーの英語チャットが届きます。スクリプト mod が前提 mod になることはなく、連携部分はスクリプト mod が入っているときだけ読み込まれます。

## 対応バージョン

| スクリプト mod | 書き方 | Minecraft / ローダー | 状態 |
|---|---|---|---|
| KubeJS 8 / 7 | グローバル `WhatHitMe`、イベント `WhatHitMeEvents.*`（server_scripts） | 26.1.2 NeoForge、1.21.1 NeoForge | <Badge type="tip" text="1.3.0+" /> |
| KubeJS 6（NeoForge 1.20.4 の KubeJS 7 ベータも） | 同じ | 1.20.4 NeoForge、1.20.1 Forge / Fabric（Forge 版の jar を使う NeoForge 1.20.1 も）、1.19.2 Forge / Fabric | <Badge type="tip" text="1.3.0+" /> |
| KubeJS 5 / 3 | グローバル `WhatHitMe`、イベント `onEvent('whathitme.*')` | 1.18.2 Forge / Fabric、1.17.1 Fabric、1.16.5 Forge / Fabric | <Badge type="tip" text="1.3.0+" /> |
| 古い KubeJS（1.x / 2.x） | グローバル `WhatHitMe`、イベント `events.listen('whathitme.*')` | 1.16.1 Fabric、1.15.2 Forge、1.14.4 Forge、1.12.2 Forge | <Badge type="tip" text="1.3.0+" /> |
| CraftTweaker（新しい ZenScript） | ZenScript のクラス `mods.whathitme.WhatHitMe` | 1.21.1、1.20.6 Fabric / NeoForge、1.20.4 Forge / Fabric / NeoForge、1.20.2 - 1.18.2 Forge / Fabric、1.17.1 Forge、1.16.5・1.15.2・1.14.4 Forge | <Badge type="tip" text="1.3.0+" /> |
| CraftTweaker 1.12.2 | ZenScript のクラス `mods.whathitme.WhatHitMe` | 1.12.2 Forge | <Badge type="tip" text="1.3.0+" /> |
| MineTweaker 3 / CraftTweaker 3.x（GTNH 版も） | ZenScript のクラス `mods.whathitme.WhatHitMe` | 1.11.2、1.10.2、1.9.4、1.8.9、1.7.10 Forge（MineTweaker 3.0.10B と CraftTweaker 3.0.9 は一部のみ） | <Badge type="tip" text="1.3.0+" /> |

ほかのバージョン（1.21.3 - 1.21.11、26.2、26.3、Fabric / Forge 1.21 以降の KubeJS）にはスクリプト mod がありません。そこでも[連携なしでできること](#連携なしでできること)の方法は使えます。26.1.2 の KubeJS 8 は Java 25 が必要です（Java 27 では起動しません）。

## KubeJS

スクリプトは `kubejs/server_scripts/` に置きます。登録はすべて `WhatHitMeEvents.register` の中で行ってください。再構築（サーバー起動、`/whm reload`、`/reload`）のたびに実行し直されるので、二重に登録されることはありません。

```js
// kubejs/server_scripts/whathitme.js
WhatHitMeEvents.register(event => {
  // ルール条件: script:veteran
  event.condition('veteran', 'ウィザーを倒した', player => player.stages.has('wither_down'))
  // テンプレート変数: {script:deaths_here}
  event.variable('deaths_here', 'このディメンションでの死亡回数', (player, damage) =>
    '' + player.persistentData.getInt('deaths_' + damage.dimension))
  // 実行時の設定（パックのファイルの後に合成されます）
  event.addRename('mob:minecraft:warden', '&4ウォーデン')
  event.addHint('minecraft:lava', '耐火のポーションを持ち歩こう。')
  event.addRule({ match: '*@minecraft:wither', unless: ['script:veteran'], name: '???', hide: ['attacker', 'mod'] })
})

WhatHitMeEvents.damageRecorded(event => {
  // event.player、event.damage（下の「Damage」を参照）
  if (event.damage.attackerId == 'minecraft:creeper' && event.damage.hearts >= 5) event.addBadge('&c[ドカン]')
  if (event.damage.damageType == 'cactus') event.cancel()   // 記録しない
})

WhatHitMeEvents.death(event => {
  if (event.killingBlow && event.killingBlow.attackerId == 'minecraft:warden') {
    event.setHint('ウォーデンは目が見えない。しゃがんで回り込もう。')
    event.addSummaryLine('&7このディメンションでの死亡: {script:deaths_here} 回')
  }
})

WhatHitMeEvents.respawn(event => {
  let last = WhatHitMe.lastDeath(event.player)
  if (last && last.killingBlow.damageType == 'outOfWorld') event.player.give('minecraft:ender_pearl')
})

WhatHitMeEvents.alert(event => {
  // event.kind: totem, heavy, prevented
  if (event.kind == 'totem') event.player.tell('トーテムが ' + event.damage.name + ' から守ってくれた')
})
```

server_scripts のグローバル `WhatHitMe` には、register イベントと同じ登録メソッドに加えて、読み取り用の `lastDeath(player)`、`history(player, n)`、`stats(player)`、`test(player, damageId[, entityId])`、`check(player, condition)`、`registrations()` があります。register イベントの外（リスナーの中など）で `WhatHitMe` から登録したものは、server_scripts が読み直されるまで残ります。

## KubeJS 5 以前

KubeJS 5（1.18.2）と KubeJS 3（1.16.5、1.17.1 Fabric）は `onEvent`、古い KubeJS（1.12.2 - 1.16.1）は `events.listen` を使います。イベント名は `whathitme.register`、`whathitme.damage_recorded`、`whathitme.death`、`whathitme.respawn`、`whathitme.alert` です。イベントのオブジェクトとグローバル `WhatHitMe` は上と同じです。プレイヤーは Minecraft のプレイヤーオブジェクトなので、タグ・スコア・ステージ・クエストは `WhatHitMe.check(player, ...)` で調べてください。

```js
// kubejs/server_scripts/whathitme.js（1.12.2 は kubejs/whathitme.js）
onEvent('whathitme.register', event => {      // 古い KubeJS: events.listen('whathitme.register', function (event) { ... })
  event.condition('veteran', 'veteran タグを持つ', player => WhatHitMe.check(player, 'tag:veteran'))
  event.addRule({ match: '*@minecraft:wither', unless: ['script:veteran'], name: '???' })
})

onEvent('whathitme.damage_recorded', event => {
  if (event.damage.damageType == 'cactus') event.cancel()
})
```

- KubeJS 1.12.2 / 1.14.4 / 1.16.1 は Nashorn で動きます。ES5（`function`、`var`、アロー関数なし）で書き、ルールは JSON 文字列で渡してください。
- スクリプトのフォルダー：1.12.2 は `kubejs/`、1.14.4・1.15.2・1.16.1 は `kubejs/data/modpack/kubejs/`（1.14.4 と 1.16.1 はそこの `scripts.json` にもファイルを書く）、1.16.5 以降は `kubejs/server_scripts/`。
- 1.16.1 Fabric では KubeJS Fabric 1.1.5 を使ってください（1.5.3 は 1.16.1 用と表示されていますが 1.16.2 以降が必要です）。Nashorn で動くため、最近の Fabric Loader では JVM 引数 `-Dfabric.debug.disableClassPathIsolation=true` が必要です。
- スクリプトの再読み込みは 1.12.2 が `/kubejs reload`、1.14.4 - 1.18.2 が `/reload` です。

## MineTweaker 3 / CraftTweaker 3.x（1.7.10 - 1.11.2）

スクリプトは `scripts/` に置きます。スクリプトはサーバーの起動時と `/minetweaker reload` で実行され、前回の実行で登録したものは What Hit Me? が置き換えます。

ZenScript 1 の関数の中からは static メソッドを呼べないので、関数の中ではすべて引数を使います。`onRegister` には登録メソッドを持つ `Api` が渡され、条件と変数には `Player`（`check(条件)`、`name`、`player`（`IPlayer`）、`lastDeath`、`history(n)`、`stats`、`test(damageId)`）が渡されます。どのイベントにも `event.player`（`Player`）と `event.api` があります。スクリプトの一番外側では `WhatHitMe` の static メソッドが CraftTweaker 1.12.2 と同じように使えます。スコアボードのタグは 1.9 からなので、1.7.10 / 1.8.9 ではスコアで調べてください（`score:<目的>>=1`）。

```zenscript
import mods.whathitme.WhatHitMe;
import mods.whathitme.Api;
import mods.whathitme.Player;
import mods.whathitme.DeathEvent;
import mods.whathitme.RespawnEvent;

WhatHitMe.onRegister(function(api as Api) {
    api.condition("veteran", "veteran スコアを持つ", function(player as Player) as bool {
        return player.check("score:veteran>=1");
    });
    api.addRule('{"match": "minecraft:lava", "unless": ["script:veteran"], "name": "???"}');
});

WhatHitMe.onDeath(function(event as DeathEvent) {
    event.addSummaryLine("&7次はがんばって、{player}。");
});

WhatHitMe.onRespawn(function(event as RespawnEvent) {
    event.api.addHint("my:last", event.player.lastDeath.killingBlow.damageType);
});
```

途中で抜ける `return;` は使わず、`if (...) { ... }` で書いてください（ZenScript 1 は単独の `return;` を受け付けません）。[GTNH 版](https://github.com/GTNewHorizons/CraftTweaker)（mod id は同じ `MineTweaker3`）も同じように使えます。

::: warning MineTweaker 3.0.10B（1.7.10）と CraftTweaker 3.0.9（1.9.4）
この 2 つの ZenScript コンパイラーは戻り値のない関数をコンパイルできないため、`onRegister` とイベント（`onDamageRecorded`、`onDeath`、`onRespawn`、`onAlert`）は使えません。一番外側での登録、条件、変数、`addRule` / `addRename` などと読み取りメソッドは使えます。1.7.10 ですべての機能を使うには CraftTweaker 3.1.0 か GTNH 版を使ってください。
:::

## CraftTweaker 1.12.2

スクリプトは `scripts/` に置きます。CraftTweaker 1.12.2 はゲーム起動時に 1 回だけスクリプトを実行します。

```zenscript
import crafttweaker.player.IPlayer;
import mods.whathitme.WhatHitMe;
import mods.whathitme.Damage;
import mods.whathitme.DamageRecordedEvent;
import mods.whathitme.DeathEvent;

WhatHitMe.onRegister(function() {
    WhatHitMe.condition("veteran", "veteran タグがある", function(player as IPlayer) as bool {
        return WhatHitMe.check(player, "tag:veteran");
    });
    WhatHitMe.variable("hp", "攻撃を受けた後の体力", function(player as IPlayer, damage as Damage) as string {
        return "" ~ damage.healthAfter;
    });
    WhatHitMe.addRename("mob:minecraft:wither_skeleton", "&8ウィザースケルトン");
    WhatHitMe.addRule('{"match": "*@minecraft:wither", "unless": ["script:veteran"], "name": "???"}');
});

WhatHitMe.onDamageRecorded(function(event as DamageRecordedEvent) {
    if (event.damage.damageType == "cactus") event.cancel();
});

WhatHitMe.onDeath(function(event as DeathEvent) {
    event.addSummaryLine("&7最後の攻撃の後の体力: {script:hp}");
});
```

ルールは JSON の文字列で渡します。ほかのメソッド: `onRespawn`、`onAlert`、`addHint`、`addDeathMessage`、`addTemplate(output, lines as string[])`、`lastDeath`、`history`、`stats`、`test`、`check`。

## CraftTweaker（1.14.4 以降）

スクリプトは `scripts/` に置きます。`/reload` のたびにスクリプトが実行し直され、前回の実行で登録したものは What Hit Me? が置き換えます。プレイヤーの型は 1.17.1 から `crafttweaker.api.entity.type.player.Player`、1.16.5 は `crafttweaker.api.player.MCPlayerEntity`、1.14.4 / 1.15.2 は `crafttweaker.api.entity.player.MCPlayerEntity` です。Fabric 1.18.2 / 1.19.2 の CraftTweaker には Ingredient Extension API が必要です。Fabric 1.17.1 の CraftTweaker は What Hit Me? が必要とする Fabric Loader では起動しないため、連携はありません。

```zenscript
import crafttweaker.api.entity.type.player.Player;
import mods.whathitme.WhatHitMe;
import mods.whathitme.Damage;
import mods.whathitme.DeathEvent;

WhatHitMe.onRegister(() => {
    WhatHitMe.condition("veteran", "veteran タグがある", (player as Player) as bool => WhatHitMe.check(player, "tag:veteran"));
    WhatHitMe.variable("dim", "攻撃を受けたディメンション", (player as Player, damage as Damage) as string => damage.dimension);
    WhatHitMe.addRule('{"match": "*@minecraft:wither", "unless": ["script:veteran"], "name": "???"}');
});

WhatHitMe.onDeath((event as DeathEvent) => {
    event.addSummaryLine("&7死亡した場所: {script:dim}");
});
```

## リファレンス

### 登録

| メソッド | 効果 |
|---|---|
| `condition(name, [description,] fn)` | ルール条件 `script:<name>`。`fn(player)` が true / false を返す |
| `variable(name, [description,] fn)` | テンプレート変数 `{script:<name>}`。`fn(player, damage)` が文字列を返す（`&` の色コード、`lang:` キー可） |
| `addRename(key, name)` / `addHint(key, tip)` | `renameMap` / `hintMap` と同じ（[マッチキー](/ja/modpack/match-keys)） |
| `addDeathMessage(key, message)` | [`deathMessages`](/ja/modpack/death-messages) と同じ |
| `addTemplate(output, lines)` | `hover`、`line`、`summary`、`alert`、`copy` の[テンプレート](/ja/modpack/templates) |
| `addRule(rule)` | [`rules`](/ja/modpack/rules) の 1 項目（KubeJS はオブジェクト、ZenScript は JSON の文字列） |

名前に使えるのは英数字と `_` `.` `-` で、大文字と小文字は区別しません。同じ名前で登録し直すと置き換わります。実行時の設定はパックのファイルの**後に**合成される追加の設定元で、パックのファイル自体は変更されません。`/whm reload` では `script:<mod>` として表示されます。

### イベント

| イベント | プロパティ | メソッド |
|---|---|---|
| `damageRecorded` | `player`、`damage` | `setName(text)`、`setHint(text)`（`""` = ヒントなし）、`addBadge(text)`、`hide(field)`、`cancel()` |
| `death` | `player`、`death`、`killingBlow` | `setName`、`setHint`、`addBadge`、`hide`（すべてとどめの一撃に対して）、`addSummaryLine(text)` |
| `respawn` | `player`、`death` | - |
| `alert` | `player`、`kind`（`totem`、`heavy`、`prevented`）、`damage`、`snapshot` | - |

`hide` にはルールの `hide` リストと同じ項目名を使います。一致するルールがあれば、名前とヒントはルールが優先されます。ルールとスクリプトの非表示項目は合わせて適用されます。まとめの行とバッジには `&` の色コード、`lang:` キー、`{player}` などのテンプレート変数を使えます。

### Damage

すべてのスクリプト mod: `damageId`、`damageType`、`name`（英語。名前の変更やルールを反映した表示名）、`attackerName`、`attackerId`、`modId`、`weaponName`、`weaponId`、`amount`（体力のポイント）、`hearts`、`healthAfter`、`maxHealth`、`distance`（-1 = 不明）、`hits`、`dimension`、`killingBlow`、`projectile`、`fire`、`explosion`、`magic`、`variable(name)`。
KubeJS のみ: `directName`、`modName`、`ownerName`、`rawAmount`、`absorbed`、`time`、`totem`、`heavy`、`reflected`、`bypassArmor`、`properties`、`hidden`、`badges`。

### Death

`kind`、`time`、`records`（古い順）、`killingBlow`、`deathMessage`（英語の文字列）、`totalAmount`。

### 読み取り

| メソッド | 結果 |
|---|---|
| `lastDeath(player)` | 最後の死亡のまとめ（なければ空） |
| `history(player, n)` | 最新の `n` 件の記録（古い順） |
| `stats(player)` | `deaths`、`damageTaken`、`causes`（KubeJS） |
| `test(player, damageId[, entityId])` | そのプレイヤーで `/whm test` が表示する内容: `name`、`hint`、`rule`、`hidden`、`deathMessage`、`commands` |
| `check(player, condition)` | 任意のルール条件。例: `check(player, 'advancement:minecraft:story/mine_diamond')` |
| `registrations()`（KubeJS） | スクリプトが登録したもの: `kind`、`owner`、`name`、`value`、`fromHook`、`failed` |
| `apiVersion`（KubeJS） | What Hit Me? の API のバージョン。現在は `1` で、メソッドが増えると上がります |

## 再読み込みとエラー {#reload-and-errors}

<!-- TODO media: screenshot: /whm reload output with the "Scripts: ..." line -->

- `/whm reload` とスクリプト mod の再読み込みは、すべてをきれいに作り直します。スクリプトが登録したものを消し、スクリプトが登録し直し、実行時の設定を 1 回だけ合成します。
- `/whm reload` はスクリプトが登録したものを一覧にし（`Scripts: 2 conditions (...), 1 variable (...), listeners: death, ...`）、未登録の `script:` 名、失敗した関数、スクリプト mod がないことを警告します。
- 例外を投げたスクリプトの関数は、条件なら偽、変数なら空として扱い、リスナーは飛ばします。エラーは関数ごとに 1 回だけログに出ます。スクリプトが壊れていても死亡のまとめが壊れることはありません。
- スクリプト mod がないときは `script:` 条件が偽になり、`/whm reload` でそのことを伝えます。ほかは何も変わりません。

## 連携なしでできること

次の方法は、連携がまだないバージョンも含めてどのバージョンでも使えます。

- **スクリプトがタグやスコアを設定し、ルールが読む。** スクリプトからスコアボードタグを付けたり（`/tag <player> add boss_ready`）スコアを変えたりして、[ルール](/ja/modpack/rules)の `tag:` / `score:` で判定します。
- **死亡トリガーからスクリプトを呼ぶ。** [死亡トリガー](/ja/modpack/death-triggers)はどんなサーバーコマンドでも実行できるので、スクリプト mod が登録したコマンドや function も呼べます。連携があるバージョンでは、トリガーのコマンドでも `{script:<名前>}` を使えます。

```json
{
  "rules": [
    { "match": "*@mymod:boss", "unless": ["tag:boss_ready"], "name": "???", "hide": ["attacker", "mod"] }
  ],
  "deathTriggers": [
    { "match": "*@mymod:boss", "command": "function mypack:boss_death" }
  ]
}
```

## mod 開発者向け

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
