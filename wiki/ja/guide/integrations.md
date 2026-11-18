# 他の mod との連携

What Hit Me? は、何も設定しなくてもすべての mod と一緒に使えます。どの mod のダメージも記録され、名前や説明が表示されます。さらに、下の mod には専用の連携機能があります。どれも必須ではなく、入っていない mod の機能は自動でオフになります。

## 一覧

| mod | What Hit Me? でできること | Minecraft バージョン | 状態 | 詳しく |
|---|---|---|---|---|
| **FTB Quests** | クエストやチャプターを達成したかをルールの条件にできる（`ftbquests:`） | 1.12.2、1.15.2 以降（公開されている Fabric / Forge / NeoForge） | <Badge type="tip" text="1.3.0+" /> | [条件とクエスト mod](/ja/modpack/conditions#quest-mods) |
| **Better Questing** | クエストの達成を条件にできる（`bq:`）。GTNH 版にも対応 | 1.12.2、1.7.10（GTNH 版） | <Badge type="tip" text="1.3.0+" /> | [条件とクエスト mod](/ja/modpack/conditions#quest-mods) |
| **Heracles / Odyssey Quests** | クエストの達成を条件にできる（`heracles:`） | 1.20 〜 1.21.1 | <Badge type="tip" text="1.3.0+" /> | [条件とクエスト mod](/ja/modpack/conditions#quest-mods) |
| **Game Stages** | ステージを条件にできる（`stage:`） | 1.12.2 〜 1.20.x（Forge） | <Badge type="tip" text="1.3.0+" /> | [条件とクエスト mod](/ja/modpack/conditions#quest-mods) |
| **KubeJS** | スクリプトでルール条件（`script:`）、テンプレート変数（`{script:}`）、イベント、実行時の設定を追加できる | 26.1.2、1.21.1、1.20.4（NeoForge）、1.20.1・1.19.2・1.18.2・1.16.5（Forge、Fabric）、1.17.1・1.16.1（Fabric）、1.15.2・1.14.4・1.12.2（Forge） | <Badge type="tip" text="1.3.0+" /> | [スクリプト連携](/ja/modpack/scripts) |
| **CraftTweaker** | KubeJS と同じことを ZenScript で（`mods.whathitme.WhatHitMe`） | 1.21.1・1.20.6（Fabric、NeoForge）、1.20.4（Forge、Fabric、NeoForge）、1.18.2 - 1.20.2（Forge、Fabric）、1.17.1・1.16.5・1.15.2・1.14.4・1.12.2（Forge） | <Badge type="tip" text="1.3.0+" /> | [スクリプト連携](/ja/modpack/scripts) |
| **MineTweaker 3** | 同じことを MineTweaker 3 / CraftTweaker 3.x で（GTNH 版も） | 1.7.10、1.8.9、1.9.4、1.10.2、1.11.2（Forge） | <Badge type="tip" text="1.3.0+" /> | [スクリプト連携](/ja/modpack/scripts) |
| **Mod Menu** | mod 一覧から What Hit Me? の設定画面を開ける（Fabric） | Fabric の全バージョン | ✔ | [設定画面](/ja/player/settings) |
| **ダメージを与えるすべての mod** | どの mod のダメージ ID や攻撃者も記録。スキャナーなら誰も被弾していないうちに一覧を出せる | 全バージョン | ✔ | [スキャナー](/ja/modpack/scanner)、[ダメージ ID](/ja/reference/damage-ids) |

::: tip どのバージョンでもスクリプトと組み合わせられます
専用の連携がまだないバージョンでも、KubeJS や CraftTweaker から What Hit Me? を動かせます。スクリプトでスコアボードのタグやスコアを設定し、ルールの `tag:` / `score:` で判定する方法です。[死亡トリガー](/ja/modpack/death-triggers)でスクリプト側のコマンドを実行することもできます。詳しくは[スクリプト連携](/ja/modpack/scripts)を見てください。
:::

## 連携のしくみ

- **必須の依存はありません。** 連携部分は、その mod が入っているときだけ読み込まれます。What Hit Me? を動かすためにクエスト mod やスクリプト mod を入れる必要はありません。
- **バージョンごとの違いに対応しています。** クエスト mod はバージョンによって中身の作りが違いますが、古い作りにも新しい作りにも対応しています。実際にゲーム内で試したバージョンは[条件とクエスト mod](/ja/modpack/conditions#tested-in-game)にあります。
- **合わなくても落ちません。** 入っている mod の版を読み取れなかったときは、その条件を「満たしていない」として扱い、ログと `/whm reload` に警告を 1 回出します。ゲームが落ちることはありません。
- **判定はサーバー側です。** クエストの進み具合はサーバーで確認するので、マルチプレイでも使え、プレイヤー側でごまかすこともできません。
