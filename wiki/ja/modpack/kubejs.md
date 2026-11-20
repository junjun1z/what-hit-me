# KubeJS <Badge type="tip" text="1.3.0+" />

What Hit Me? は、Forge 1.12.2 の KubeJS 1.1 から NeoForge 26.1.2 の KubeJS 8 まで、すべての KubeJS の世代に対応しています。server_scripts でグローバル `WhatHitMe` と What Hit Me? のイベントが使えます。スクリプトでできることは[スクリプト連携](/ja/modpack/scripts)、すべてのメソッドは [API リファレンス](/ja/modpack/script-api)にあります。

## 対応バージョン

| KubeJS | イベント | Minecraft / ローダー |
|---|---|---|
| KubeJS 8 / 7 | `WhatHitMeEvents.*` | 26.1.2 NeoForge、1.21.1 NeoForge |
| KubeJS 6（NeoForge 1.20.4 の KubeJS 7 ベータも） | `WhatHitMeEvents.*` | 1.20.4 NeoForge、1.20.1 Forge / Fabric（Forge 版の jar を使う NeoForge 1.20.1 も）、1.19.2 Forge / Fabric |
| KubeJS 5 / 3 | `onEvent('whathitme.*')` | 1.18.2 Forge / Fabric、1.17.1 Fabric、1.16.5 Forge / Fabric |
| 古い KubeJS（1.x / 2.x） | `events.listen('whathitme.*')` | 1.16.1 Fabric、1.15.2 Forge、1.14.4 Forge、1.12.2 Forge |

Fabric / Forge 1.21 以降、1.21.3 〜 1.21.11、26.2、26.3 用の KubeJS はありません。26.1.2 の KubeJS 8 は Java 25 が必要です（Java 27 では起動しません）。

## KubeJS 6・7・8

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

## 再読み込み

| KubeJS | 再読み込み |
|---|---|
| 6、7、8 | `/reload`。`/whm reload` でも登録を作り直します |
| 3、5（1.16.5 〜 1.18.2） | `/reload` |
| 古い版（1.14.4 〜 1.16.1） | `/reload` |
| 1.12.2 | `/kubejs reload` |

再読み込みの後、`/whm reload` でスクリプトが登録したものを確認できます（[再読み込みとエラー](/ja/modpack/scripts#reload-and-errors)）。
