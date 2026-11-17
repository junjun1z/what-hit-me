# スクリプト連携 <Badge type="warning" text="予定" />

::: warning 1.3.0 で追加予定 —— まだ実装されていません
このページは**予定している**動作の説明です。現在のビルドでは、ここにあるものはどれも動きません。機能が公開されたら、実際の例を加えてこのページを完成させます。
:::

## 今すぐできること

特別な対応がなくても、スクリプトと What Hit Me? をつなげられます。

- **スクリプトがタグやスコアを設定し、ルールが読む。** KubeJS、CraftTweaker、コマンドブロックからスコアボードタグを付けたり（`/tag <player> add boss_ready`）スコアを変えたりして、[ルール](/ja/modpack/rules)の `tag:` / `score:` [条件](/ja/modpack/conditions)で使います。
- **死亡トリガーからスクリプトを呼ぶ。** [死亡トリガー](/ja/modpack/death-triggers)はどんなサーバーコマンドでも実行できるので、スクリプト mod が登録したコマンドや function も呼べます。

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

## 予定している機能

判定はサーバーが行い、前提 mod になることはありません。連携部分はスクリプト mod があるときだけ読み込まれ、スクリプトのエラーは受け止めて 1 回だけログに出し、偽 / 空として扱います。

| 予定の機能 | 内容 |
|---|---|
| スクリプトの条件 | スクリプトが `名前 -> predicate(player)` を登録し、ルールで `script:<名前>`（`!` の否定も可）として使う。不明な名前は偽になり、`/whm reload` が警告 |
| スクリプトの変数 | スクリプトが `名前 -> function(player, record) -> 文字列` を登録し、テンプレートで `{script:<名前>}` として使う。出力を作るときにサーバーで計算し、テンプレートエディターの一覧にも出る |
| イベント | `damageRecorded`（記録を読み、名前 / ヒントの設定、バッジの追加、項目の非表示、記録の取り消し）と `death`（まとめを読み、ヒントの設定、項目の非表示、要約への行の追加、任意の処理） |
| 読み書きの API | `lastDeath(player)`、`history(player, n)`、`stats(player)`、テストの結果。実行時に名前・ヒント・ルール・死亡メッセージ・テンプレートを追加（pack ファイルの後に合成され、読み直しで作り直す） |
| 読み直し | `/whm reload` とスクリプト mod の再読み込みで、きれいに作り直し、登録された条件・変数・リスナーを一覧表示 |

予定している対応：

| スクリプト mod | 予定の形 |
|---|---|
| KubeJS | グローバルの `WhatHitMe` とイベント `WhatHitMeEvents.*`。対応する Minecraft バージョン向けにあるすべての KubeJS で |
| CraftTweaker | 1.12.2 の ZenScript `mods.whathitme.WhatHitMe`、1.16.5 以降は新しい API とイベント |
| MineTweaker 3 | 1.7.10（違いがあれば GTNH 版も） |

名前などの細部は、公開までに変わることがあります。
