# 更新履歴

詳しい更新履歴は [GitHub の CHANGELOG.md](https://github.com/junjun1z/what-hit-me/blob/main/CHANGELOG.md)（英語）にあります。ダウンロード：[Modrinth](https://modrinth.com/mod/what-hit-me)、[CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-hit-me)、[GitHub Releases](https://github.com/junjun1z/what-hit-me/releases)。

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

- クエスト mod がある、すべてのバージョンとローダーでクエスト条件に対応（[対応表](/ja/modpack/quest-mods#quest-mod-matrix)）：Better Questing 1.7.10 〜 1.12.2（すべての API 世代、Better Questing Unofficial も）と 1.20.1（Forge、NeoForge）、Game Stages 1.11.2 〜 1.20.3 と NeoForge 1.20.1、Heracles 1.20 と Fabric 1.21、FTB Quests 1.16.4（1604）、Forge / Fabric 1.20.4、1.21、26.1。すべてゲーム内でテストし、サーバーのルールでのクエスト条件も専用サーバーで確認しました。

- `/whm reload` に、スクリプトが登録した内容と失敗したスクリプトの関数も表示されます。

- 修正（Forge 1.7.10 〜 1.12.1）：同じ tick に回復（満腹度による自然回復、再生）があると、受けたダメージが少なく記録され、燃焼ダメージのような 1 ❤ のヒットが記録から抜けることがありました（まとめた行が x3 ではなく x2 になる）。

- 公開前テストでの修正：深く入れ子になった pack ファイルが 1 つあっても pack フォルダー全体の読み込みが止まらないように（型の違うセクションや深すぎるフォルダーも報告）。UTF-8 以外で保存した `.cfg` が初期値に戻らないように。小数の設定で `NaN` を受け付けないように。mod 入りクライアントの `/whm reload` で、読み込んだ pack ファイル数とエラーのあるファイルを表示。古いサーバーで `/whm pack` を開くと読み込み中のままにならず、その旨を表示。1.3 のサーバーにいる 1.2 のクライアントに翻訳キーではなく英語を表示。`/kill` の最大HP比を `2147483647%` ではなく `∞%` と表示。Forge / NeoForge 1.13 以降で First Aid の被弾を「死亡の回避」ではなく大ダメージとして扱うように。シングルプレイでワールドを開くと pack フォルダーを読み直すように。`/whm reload` で開いているパックエディターも更新。Windows で大文字小文字だけのカテゴリー名変更を元に戻してもファイルが消えないように。死亡画面のパネルで合計の行を折り返すように。

- 1.3 のサーバーが 1.3 のクライアントに送る名前・ヒント・テンプレートなどのリストに上限がなくなりました（これまでは mod 入りのプレイヤー向けに各リストが 1000 項目で切られていました）。1.2 のクライアントには引き続き各リストの最初の 1000 項目が届き、それより長いリストは `/whm reload` とサーバーログで警告します。[大きな pack](/ja/modpack/names-and-tips#large-packs) を参照。

- とても大きな返信（長いヒント付きの長い履歴や死亡リキャップ、巨大な `/whm dump`）がパケット上限（1 MiB、Forge 1.7.10 は 2 MiB）を超え、プレイヤーが切断されることがありました。1.3 のクライアントにはどの版でも大きな返信を分割して送り、`/whm dump` はダメージ ID をすべて出力します（これまでは 4000 件まで）。1.2 のクライアントには 1 パケットに収まる分だけを送り、省いた記録や項目の数を表示します。

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
