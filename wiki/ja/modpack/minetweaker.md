# MineTweaker 3 / CraftTweaker 3.x <Badge type="tip" text="1.3.0+" />

Forge 1.7.10 〜 1.11.2 の MineTweaker 3 と CraftTweaker 3.x（[GTNH 版](https://github.com/GTNewHorizons/CraftTweaker)も）のスクリプトは、`mods.whathitme.WhatHitMe` と 2 つの補助オブジェクト `Api`・`Player` を使います。スクリプトでできることは[スクリプト連携](/ja/modpack/scripts)、すべてのメソッドは [API リファレンス](/ja/modpack/script-api)にあります。

## 対応バージョン

| スクリプト mod | Minecraft | 対応 |
|---|---|---|
| CraftTweaker 3.1.0 | 1.7.10 | すべて |
| CraftTweaker の GTNH 版（3.5.0） | 1.7.10 | すべて |
| MineTweaker 3.0.10B | 1.7.10 | [一部のみ](#partial-support) |
| MineTweaker 3 / CraftTweaker 3.x | 1.8.9、1.10.2、1.11.2 | すべて |
| CraftTweaker 3.0.9 | 1.9.4 | [一部のみ](#partial-support) |

## スクリプトの書き方

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

## 一部のみの対応 {#partial-support}

::: warning MineTweaker 3.0.10B（1.7.10）と CraftTweaker 3.0.9（1.9.4）
この 2 つの ZenScript コンパイラーは戻り値のない関数をコンパイルできないため、`onRegister` とイベント（`onDamageRecorded`、`onDeath`、`onRespawn`、`onAlert`）は使えません。一番外側での登録、条件、変数、`addRule` / `addRename` などと読み取りメソッドは使えます。1.7.10 ですべての機能を使うには CraftTweaker 3.1.0 か GTNH 版を使ってください。
:::

## 再読み込み

スクリプトはサーバーの起動時と `/minetweaker reload` で実行され、前回の実行で登録したものは What Hit Me? が置き換えます。`/whm reload` でスクリプトが登録したものを確認できます（[再読み込みとエラー](/ja/modpack/scripts#reload-and-errors)）。
