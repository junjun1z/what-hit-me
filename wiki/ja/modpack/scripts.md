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

| スクリプト mod | 書き方 | Minecraft / ローダー | ページ |
|---|---|---|---|
| KubeJS 8 / 7 | グローバル `WhatHitMe`、イベント `WhatHitMeEvents.*`（server_scripts） | 26.1.2 NeoForge、1.21.1 NeoForge | [KubeJS](/ja/modpack/kubejs) |
| KubeJS 6（NeoForge 1.20.4 の KubeJS 7 ベータも） | 同じ | 1.20.4 NeoForge、1.20.1 Forge / Fabric（Forge 版の jar を使う NeoForge 1.20.1 も）、1.19.2 Forge / Fabric | [KubeJS](/ja/modpack/kubejs) |
| KubeJS 5 / 3 | グローバル `WhatHitMe`、イベント `onEvent('whathitme.*')` | 1.18.2 Forge / Fabric、1.17.1 Fabric、1.16.5 Forge / Fabric | [KubeJS](/ja/modpack/kubejs) |
| 古い KubeJS（1.x / 2.x） | グローバル `WhatHitMe`、イベント `events.listen('whathitme.*')` | 1.16.1 Fabric、1.15.2 Forge、1.14.4 Forge、1.12.2 Forge | [KubeJS](/ja/modpack/kubejs) |
| CraftTweaker（新しい ZenScript） | ZenScript のクラス `mods.whathitme.WhatHitMe` | 1.21.1、1.20.6 Fabric / NeoForge、1.20.4 Forge / Fabric / NeoForge、1.20.2 - 1.18.2 Forge / Fabric、1.17.1 Forge、1.16.5・1.15.2・1.14.4 Forge | [CraftTweaker](/ja/modpack/crafttweaker) |
| CraftTweaker 1.12.2 | ZenScript のクラス `mods.whathitme.WhatHitMe` | 1.12.2 Forge | [CraftTweaker](/ja/modpack/crafttweaker) |
| MineTweaker 3 / CraftTweaker 3.x（GTNH 版も） | ZenScript のクラス `mods.whathitme.WhatHitMe` | 1.11.2、1.10.2、1.9.4、1.8.9、1.7.10 Forge（MineTweaker 3.0.10B と CraftTweaker 3.0.9 は[一部のみ](/ja/modpack/minetweaker#partial-support)） | [MineTweaker 3](/ja/modpack/minetweaker) |

ほかのバージョン（1.21.3 - 1.21.11、26.2、26.3、Fabric / Forge 1.21 以降の KubeJS）にはスクリプト mod がありません。そこでも[連携なしでできること](#without-a-binding)の方法は使えます。26.1.2 の KubeJS 8 は Java 25 が必要です（Java 27 では起動しません）。

## ページ

| ページ | 内容 |
|---|---|
| [KubeJS](/ja/modpack/kubejs) | KubeJS 1.x 〜 8：スクリプトのフォルダー、世代ごとのイベント名、例 |
| [CraftTweaker](/ja/modpack/crafttweaker) | CraftTweaker 1.12.2 と 1.14.4 〜 1.21.1（ZenScript）、バージョンごとのプレイヤーの型 |
| [MineTweaker 3 / CraftTweaker 3.x](/ja/modpack/minetweaker) | 1.7.10 〜 1.11.2、`Api` / `Player` オブジェクト、GTNH 版 |
| [API リファレンス](/ja/modpack/script-api) | 登録・イベント・Damage・Death・読み取りのメソッド、mod 開発者向けの Java API |

## 最初のスクリプト

どのスクリプト mod でも考え方は同じです。ルールで使う条件 `script:veteran` と、まとめに 1 行足す死亡イベントのリスナー。KubeJS 7 / 8 の例：

```js
// kubejs/server_scripts/whathitme.js
WhatHitMeEvents.register(event => {
  event.condition('veteran', 'veteran タグを持つ', player => WhatHitMe.check(player, 'tag:veteran'))
  event.addRule({ match: '*@minecraft:wither', unless: ['script:veteran'], name: '???', hide: ['attacker', 'mod'] })
})

WhatHitMeEvents.death(event => {
  event.addSummaryLine('&7これまでの死亡回数: ' + WhatHitMe.stats(event.player).deaths)
})
```

ほかのスクリプト mod での書き方：[CraftTweaker](/ja/modpack/crafttweaker)、[MineTweaker 3](/ja/modpack/minetweaker)。

## 再読み込みとエラー {#reload-and-errors}

![/whm reload の「Scripts:」の行：スクリプトが登録したもの](/images/ja/scripts-reload.webp)

- `/whm reload` とスクリプト mod の再読み込みは、すべてをきれいに作り直します。スクリプトが登録したものを消し、スクリプトが登録し直し、実行時の設定を 1 回だけ合成します。
- `/whm reload` はスクリプトが登録したものを一覧にし（`Scripts: 2 conditions (...), 1 variable (...), listeners: death, ...`）、未登録の `script:` 名、失敗した関数、スクリプト mod がないことを警告します。
- 例外を投げたスクリプトの関数は、条件なら偽、変数なら空として扱い、リスナーは飛ばします。エラーは関数ごとに 1 回だけログに出ます。スクリプトが壊れていても死亡のまとめが壊れることはありません。
- スクリプト mod がないときは `script:` 条件が偽になり、`/whm reload` でそのことを伝えます。ほかは何も変わりません。

## 連携なしでできること {#without-a-binding}

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
