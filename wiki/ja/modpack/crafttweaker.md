# CraftTweaker <Badge type="tip" text="1.3.0+" />

CraftTweaker のスクリプトは ZenScript のクラス `mods.whathitme.WhatHitMe` を使います。CraftTweaker 1.12.2 と、1.14.4 以降の新しい ZenScript の 2 つの書き方があります。1.7.10 〜 1.11.2 は [MineTweaker 3 / CraftTweaker 3.x](/ja/modpack/minetweaker) を見てください。スクリプトでできることは[スクリプト連携](/ja/modpack/scripts)、すべてのメソッドは [API リファレンス](/ja/modpack/script-api)にあります。

## 対応バージョン

| CraftTweaker | Minecraft / ローダー |
|---|---|
| CraftTweaker 5 〜 21（新しい ZenScript） | 1.21.1、1.20.6 Fabric / NeoForge、1.20.4 Forge / Fabric / NeoForge、1.20.2 - 1.18.2 Forge / Fabric、1.17.1 Forge、1.16.5・1.15.2・1.14.4 Forge |
| CraftTweaker 4.1 | 1.12.2 Forge |

1.21.3 〜 1.21.11 と 26.x 用の CraftTweaker はなく、Fabric 1.17.1 では What Hit Me? が必要とする Fabric Loader で起動する CraftTweaker がありません。

## CraftTweaker 1.14.4 以降

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

## 再読み込み

- **1.14.4 以降：** `/reload` でスクリプトが実行し直され、前回の実行で登録したものは What Hit Me? が置き換えます。
- **1.12.2：** スクリプトはゲームの起動時に 1 回だけ実行されます。スクリプトの登録を変えるにはゲームを再起動してください。

再読み込みの後、`/whm reload` でスクリプトが登録したものを確認できます（[再読み込みとエラー](/ja/modpack/scripts#reload-and-errors)）。
