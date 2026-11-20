# Modpack 制作者向け：概要

## What Hit Me? を入れる理由

- **「何に殺されたの？」という質問が減ります。** プレイヤー自身が攻撃者、その mod、ダメージの種類を確認できます。
- **pack の危険をプレイヤーに案内できます。** ダメージの種類・攻撃者・武器・ディメンションごとに、独自の名前とヒントを付けられます。
- **進むまで秘密にしておけます** <Badge type="tip" text="1.3.0+" />。ボスのクエストを終えるまでは `???` と表示し、ヒントも出しません。
- **途中から入れても安全です。** 前提 mod なし、ワールドデータなし、ブロックやアイテムなし、参加時のチャットなし。いつでも外せます。
- **MIT ライセンス**なので、許可なくどの pack にも入れられます。

## カスタマイズできること

| やりたいこと | 使うもの | ページ |
|---|---|---|
| ダメージにわかりやすい名前を付ける | `renameMap` | [名前・ヒント・非表示 ID](/ja/modpack/names-and-tips) |
| 独自のヒントを書く | `hintMap` | [名前・ヒント・非表示 ID](/ja/modpack/names-and-tips) |
| 邪魔な記録を隠す（サボテン、mod の継続ダメージ） | `defaultIgnoredDamageIds` | [名前・ヒント・非表示 ID](/ja/modpack/names-and-tips#hiding-damage) |
| 攻撃者・武器・ディメンションで分ける | 複合マッチキー | [マッチキー](/ja/modpack/match-keys) |
| 全員分の機能をオフにする | `whathitme-common.cfg` | [設定ファイル](/ja/reference/configuration) |
| プレイヤーに設定を変えさせない | `lockedFeatures` | [設定のロックと権限](/ja/modpack/locked-features) |
| 死んだときにコマンドを実行する | `deathTriggers` | [死亡トリガー](/ja/modpack/death-triggers) |
| pack のダメージ ID をすべて調べる | スキャナー、`/whm dump` | [スキャナー・dump・/whm test](/ja/modpack/scanner) |
| パネル・詳細・要約・通知・コピー文を書き換える <Badge type="tip" text="1.3.0+" /> | `templates` | [テンプレート](/ja/modpack/templates) |
| バニラの死亡メッセージを置き換える <Badge type="tip" text="1.3.0+" /> | `deathMessages` | [カスタム死亡メッセージ](/ja/modpack/death-messages) |
| 進行度に応じて名前・ヒント・非表示を変える <Badge type="tip" text="1.3.0+" /> | `rules` | [進行度ルール](/ja/modpack/rules) |
| すべてのクライアントに mod を必須にする <Badge type="tip" text="1.3.0+" /> | `requireClientMod` | [requireClientMod](/ja/modpack/require-client-mod) |
| クエストの進行度を使う（FTB Quests、Better Questing、Heracles、Game Stages） <Badge type="tip" text="1.3.0+" /> | `if` / `any` / `unless` の条件 | [クエスト mod](/ja/modpack/quest-mods)、[条件](/ja/modpack/conditions) |
| KubeJS / CraftTweaker / MineTweaker から動かす <Badge type="tip" text="1.3.0+" /> | `script:`、`{script:}`、イベント、API | [スクリプト連携](/ja/modpack/scripts) |
| ここまでの設定をゲーム内で編集する <Badge type="tip" text="1.3.0+" /> | `/whm pack` | [パックエディター](/ja/modpack/pack-editor) |

## 5 分でわかる使い方

1. mod を入れてゲーム（またはサーバー）を一度起動します。`config/whathitme/pack/` がコメントアウトされた例と一緒に作られます。
2. pack の何かで死んで、とどめの一撃にマウスを乗せ、`[Copy ID]` をクリックします。`/whm dump` ですべての ID を書き出すこともできます。
3. `config/whathitme/pack/mypack.json` を作ります。

   ```json
   {
     "renameMap": {
       "minecraft:outOfWorld": "&5奈落"
     },
     "hintMap": {
       "mob:minecraft:creeper": "クリーパーはシューッと鳴ってから爆発する。走って離れよう！"
     }
   }
   ```

4. `/whm reload`（OP）を実行します。読み込んだ pack ファイルの数と、エラーのあるファイルが表示されます。
5. 死ななくても結果を確認できます：`/whm test mob:minecraft:creeper`

## 設定の優先順位

```
プレイヤー自身の設定（whathitme-client.cfg、whathitme-client-templates.json）
        ↑ 上に重なる
サーバー / pack の .cfg（whathitme-common.cfg）
        ↑ 上に重なる
pack フォルダーの JSON（config/whathitme/pack/**/*.json）
        ↑ 上に重なる
組み込みの初期値と自動ヒント
```

- **表示スイッチ：** 両方の `.cfg` でオンのときだけ表示されます。
- **名前 / ヒント：** 個人の設定が pack より優先され、`.cfg` の項目が pack の JSON より優先されます。カスタムヒントは自動ヒントより優先されます。
- **テンプレート：** 個人 > pack > 組み込み。ただし pack がロックした場合は除きます。
- **進行度ルール**は `renameMap` / `hintMap` より優先されます。`"enforce": true`（または `hide`）なら、個人の名前・ヒント・テンプレートよりも優先されます。

## modpack に入れるとき

- `config/whathitme/whathitme-common.cfg` と `config/whathitme/pack/` フォルダーを同梱します。
- パネルの配置を固定したいのでなければ、`whathitme-client.cfg` は同梱**しない**でください。プレイヤーが変更するファイルです。
- `.cache/` は同梱してもしなくても構いません。自動で作り直されます。`.backup/`（パックエディターのバックアップ）は同梱しないでください。
- 専用サーバーでは、サーバーのファイルが使われ、mod 入りの全クライアントに送られます。

::: tip 翻訳
名前・ヒント・テンプレートの行・死亡メッセージは、どれも翻訳キーにできます：`lang:mypack.hint.lava`。キーをリソースパック（または pack のアセット読み込みの仕組み）に入れれば、各プレイヤーが自分の言語で読めます。
:::
