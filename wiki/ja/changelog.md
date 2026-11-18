# 更新履歴

詳しい更新履歴は [GitHub の CHANGELOG.md](https://github.com/junjun1z/what-hit-me/blob/main/CHANGELOG.md)（英語）にあります。ダウンロード：[GitHub Releases](https://github.com/junjun1z/what-hit-me/releases)、Modrinth、CurseForge。

## 1.3.0 <Badge type="tip" text="公開予定" />

- 詳細表示、パネル / 履歴の行、リスポーン後の要約、通知、コピー文の[テンプレート](/ja/modpack/templates)。個人（`whathitme-client-templates.json`）> pack（`templates`）> 組み込み。`lockedFeatures` でロック可能。
- プレビュー付きのゲーム内[テンプレートエディター](/ja/modpack/template-editor)（`/whm template`、設定画面の **Templates...**）。
- `/whm template preview <output>` と `/whm template reset [output]`。
- [カスタム死亡メッセージ](/ja/modpack/death-messages)（`deathMessages`）：Fabric のサーバーは全員分、Forge / NeoForge のサーバーは mod 入りのプレイヤーの分を置き換え。
- [進行度ルール](/ja/modpack/rules)（`rules`）：プレイヤーの進行度に応じた名前・ヒント・非表示・コマンド。[条件](/ja/modpack/conditions)は `advancement:`、`tag:`、`score:`、`stage:`、`ftbquests:`、`bq:`、`heracles:`。
- `deathTriggers` で `if` / `any` / `unless` と、すべてのテンプレート変数をプレースホルダーとして使用可能に。
- `/whm test ... --as <player> --assume <condition>`。
- [`requireClientMod`](/ja/modpack/require-client-mod)（common 設定）。
- サンプルフォルダー `pack/templates/`、`pack/deathmessages/`、`pack/rules/` を追加。
- 通信プロトコル 3。1.0 - 1.2 のクライアントとサーバーも引き続き動作。

- [スクリプト連携](/ja/modpack/scripts)：KubeJS・CraftTweaker・MineTweaker のスクリプトから `script:` 条件、`{script:}` テンプレート変数、イベント、実行時の設定を追加（公開 Java API `io.github.junjun1z.whathitme.api`）。これらのスクリプト mod があるすべてのバージョンとローダーに対応：KubeJS 1.12.2 - 26.1.2、CraftTweaker 1.12.2 - 1.21.1、MineTweaker 3 / CraftTweaker 3.x 1.7.10 - 1.11.2（GTNH 版も）。

- すべての pack セクションとサーバー設定を編集できるゲーム内[パックエディター](/ja/modpack/pack-editor)（`/whm pack`、設定画面の **パックエディター...**）。判定はサーバー、バックアップと元に戻す付き。pack ファイルに `order`・`enabled`・メモを書けます。個人モード（`/whm pack personal`）で自分のリネーム・ヒント・非表示 ID を編集。画面は対応するすべてのバージョンとローダーにあります（Forge 1.7.10 - 26.3、Fabric 1.14 - 26.3、NeoForge 1.20.1 - 26.3）。

- クエスト mod がある、すべてのバージョンとローダーでクエスト条件に対応（[対応表](/ja/modpack/conditions#quest-mod-matrix)）：Better Questing 1.7.10 〜 1.12.2（すべての API 世代、Better Questing Unofficial も）と 1.20.1（Forge、NeoForge）、Game Stages 1.11.2 〜 1.20.3 と NeoForge 1.20.1、Heracles 1.20 と Fabric 1.21、FTB Quests 1.16.4（1604）、Forge / Fabric 1.20.4、1.21、26.1。すべてゲーム内でテストし、サーバーのルールでのクエスト条件も専用サーバーで確認しました。

## 1.2.0

- pack フォルダー `config/whathitme/pack/`：`renameMap`、`hintMap`、`defaultIgnoredDamageIds`、`lockedFeatures`、`deathTriggers`。
- 複合マッチキー（`@エンティティ`、`@item:`、`@dim:`、ワイルドカード、最も具体的なものが勝つ）。
- `/whm test`、`/whm stats`、ダメージ ID スキャナー、カタログのキャッシュ。
- すべてのバージョン・ローダーで設定画面（Fabric は Mod Menu、任意）。
- 最大体力の割合、急所、周囲の状況、反射ダメージのバッジ、防具の破損の行、`[Copy Recap]` に装備。
- ヒントのスイッチ `enableAutoHints`、`enableCustomHints`、`showHints`。
- 権限の変更：`rename`、`sethint`、`dump`、`reload` はクライアントでも OP が必要に。

## 1.1.0

主要な Minecraft のバージョンとローダーすべてに対応：Forge 1.7.10 - 26.3、Fabric 1.14 - 26.3、NeoForge 1.20.1 - 26.3（63 ファイル）。

## 1.0.1

ゲーム内での全項目テストで見つかった不具合の修正。Forge 1.7.10 - 1.12.2 でゲーム内の設定画面。

## 1.0.0

最初の公開。
