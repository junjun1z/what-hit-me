# What Hit Me?

「何にやられたのか」をはっきり見せる Minecraft 1.12.2 Forge 用の Mod です。**どんな Mod パックでも、どんな Mod の組み合わせでも動きます。必須の前提 Mod はありません。**

[English README](README.md)

![死亡画面のパネル](docs/screenshots/death-screen.png)

- Minecraft 1.12.2、Forge 14.23.5.2847 以降、Java 8
- Mod ID は `whathitme`
- クライアントだけ、サーバーだけ、両方、どの入れ方でも動作。Mixin・コアMod・ブロックやアイテムの追加はなし
- ライセンス: MIT

## 機能

- **死亡画面のパネル**: 致命傷、死亡メッセージ、ダメージの属性、直前30秒のすべての被弾、「直前10秒の合計」、ヒントを表示。行にマウスを乗せると詳細。ヘッダーをドラッグで移動、`[-]` で最小化、ホイールでスクロール、`[コピー]` で内容をコピー。
- **リスポーン時のチャット通知**（自分のチャット欄だけ）: 致命傷、属性、直前10秒の合計、ヒント、詳細へのリンク。`/whm togglechat` で消せます。
- **被ダメージ履歴**（`/whm`）: 生きている間も含めた最近の被弾。同じ発生源からの3秒以内の連続ダメージは1行にまとめてヒット数を表示（`x12回`）。
- **マウスオーバーの詳細**: 防具による軽減前後のダメージ、軽減率、衝撃吸収（黄色いハート）の消費量、被弾後の体力、攻撃者の武器とエンチャント、距離、`[透明]` の攻撃者、タレット・ペット・矢の所有者、追加した Mod、内部ダメージID。`[IDをコピー]` `[非表示]` ボタン付き。
- **トーテム・大ダメージのスナップショット**: 不死のトーテムが発動したとき、他の Mod が死亡を防いだとき、1撃で最大体力の80%以上を失ったときに、その瞬間を保存して `[詳細]` リンク付きでチャットに通知（`/whm snapshot`）。
- **効果一覧**（`/whm effects`）: 現在の効果、追加した Mod、レベル、残り時間、牛乳で消せるか（`[牛乳 ✔]` / `[牛乳 ✖]`）。
- **検索**（`/whm search`）: ダメージの種類・モブ・効果を `#bypasses_armor`、`#absolute`、`#magic`、`#fire`、`#explosion`、`#projectile`、`#damage`、`#mob`、`#effect`、`#debuff`、`@modid`、または文字列で検索。組み合わせも可（`@minecraft zombie`）。
- **自動ヒント**: ダメージの種類と属性に応じた対策を表示。ダメージIDごとに差し替え可能。
- 10言語対応。

1.12.2 では絵文字を表示できないため、ゲームのフォントにある記号（☠ ⚠ ❤ Σ）と文字のラベルを使っています。

## 導入パターン

| 入れる場所 | プレイヤーに見えるもの |
|---|---|
| クライアントとサーバー | サーバーからの正確なデータ（軽減前ダメージ、軽減率、衝撃吸収、武器、所有者、盾・無敵時間のフラグ）。各自の言語で表示。クリップボードへのコピーボタン。サーバーの `whathitme-common.cfg` が全員に適用されます。 |
| クライアントだけ | クライアント側の推定モード。自分の体力の変化と周囲の状況から原因を推測します（落下、溶岩、炎、溺れ、窒息、奈落、近くのモブ、飛び道具、爆発、毒、ウィザー、空腹）。記録には `[クライアント推定]` が付きます。`/whm inspect` は使えません。 |
| サーバーだけ | Mod を入れていないプレイヤー（素の Forge クライアントでも）に英語のチャットを送ります。リスポーン時の通知、トーテム・大ダメージの通知、すべての `/whm` コマンド。`[Copy ID]` は ID をチャット欄に入力します。`togglechat` と `ignore` はプレイヤーごとに保存されます。 |

シングルプレイは「クライアントとサーバー」の扱いです。入れている人と入れていない人が混在するサーバーでは、各プレイヤーにそのクライアントで使える最良のモードが選ばれます。

## Mod パック製作者の方へ

### 入れる理由

- 「何で死んだの？」という質問が減ります。攻撃者、その Mod、ダメージの種類をプレイヤーが自分で確認できます。
- パック独自の危険をヒントで案内できます（`hintMap`）。分かりにくいダメージ名の変更（`renameMap`）や、不要な表示の非表示（`ignoredDamageIds`）もできます。
- 前提 Mod なし、ワールドデータなし、ログイン時のチャット表示なし。いつ追加・削除しても安全です。

### 設定ファイル

場所は `config/whathitme/`。Forge の設定形式で、`/whm reload` で再起動なしに再読み込みできます。

| ファイル | 対象 | 内容 |
|---|---|---|
| `whathitme-common.cfg` | パック製作者・サーバー管理者 | 表示の ON/OFF、フィルター（`ignoredDamageIds`、`renameMap`、`hintMap`、`minDamageThreshold`、`heavyHitThreshold`）、記録の設定（`historySize`、`mergeWindowSeconds`、`summarySeconds`、`recapSeconds`、`maxSnapshots`）。Mod 入りのサーバーは、ログイン時と再読み込み・設定変更時にこのファイルを Mod 入りクライアントへ送り、クライアント側の common ファイルの代わりに使わせます。 |
| `whathitme-client.cfg` | 各プレイヤー | 個人用の表示 ON/OFF とフィルター、死亡画面パネルの配置（`anchorPoint`、`offsetX`、`offsetY`、`scale`、`backgroundOpacity`、`maxVisibleLines`、`minimized`、`chatHistoryLines`）。 |

表示は **両方のファイルで** `true` の項目だけです。パックやサーバーで全員分を OFF にでき、各プレイヤーはさらに自分で OFF にできます。個人の `renameMap` / `hintMap` はパックの設定の上に適用されます。

<details>
<summary>全設定項目</summary>

**表示の ON/OFF**（両方のファイル、カテゴリ `display`、初期値はすべて `true`）:
`enableDeathScreenPanel`、`enableRespawnChatSummary`、`enableTotemChatAlert`、`enableHeavyHitSnapshot`、`showTimestamp`、`showDamageAmount`、`showMitigationDetails`、`showSmartMergeHitCount`、`showSourceModName`、`showDamageProperties`、`showInternalDamageId`、`showAttackerName`、`showAttackerWeapon`、`showAttackerDistance`、`showAttackerInvisibility`、`showEntityOwner`、`showHintsAndTips`、`showActionButtons`、`showToggleChatNotice`

**フィルター**（両方のファイル、カテゴリ `filters`）:

| キー | 初期値 | 意味 |
|---|---|---|
| `minDamageThreshold` | 0.05 | これ未満のダメージは記録しない |
| `heavyHitThreshold` | 0.8 | 最大体力のこの割合以上を1撃で失うと大ダメージ扱い |
| `ignoredDamageIds` | 空 | 非表示にするID。`*` ワイルドカード可 |
| `renameMap` | 空 | `ID = 名前` または `ID = lang:キー` |
| `hintMap` | 空 | `ID = ヒント` または `ID = lang:キー` |

`whathitme-client.cfg` 側のしきい値はクライアントだけの推定モードでのみ使われます。それ以外はサーバーが記録します。

**記録**（common ファイルのみ、カテゴリ `tracking`）:

| キー | 初期値 | 意味 |
|---|---|---|
| `historySize` | 50 | プレイヤーごとに保持する記録数（5〜500） |
| `mergeWindowSeconds` | 3.0 | 連続ダメージをまとめる秒数（0 で無効） |
| `summarySeconds` | 10 | 「直前X秒の合計」の秒数 |
| `recapSeconds` | 30 | 死亡の詳細にさかのぼって含める秒数 |
| `maxSnapshots` | 5 | プレイヤーごとのトーテム・大ダメージのスナップショット数 |

**配置**（client ファイルのみ、カテゴリ `layout`）:

| キー | 初期値 | 意味 |
|---|---|---|
| `anchorPoint` | `CENTER_LEFT` | `TOP_LEFT`、`TOP_RIGHT`、`BOTTOM_LEFT`、`BOTTOM_RIGHT`、`CENTER_LEFT`、`CENTER_RIGHT` |
| `offsetX` / `offsetY` | 4 / 0 | 基準位置からのずれ。パネルをドラッグすると保存 |
| `scale` | 0.85 | パネルの大きさ |
| `backgroundOpacity` | 0.75 | 背景の不透明度（0.0〜1.0） |
| `maxVisibleLines` | 8 | スクロールせずに見える行数 |
| `minimized` | false | `[-]` ボタンで保存 |
| `chatHistoryLines` | 15 | `/whm history` で表示する行数 |

</details>

### 独自ヒントと表示名

```
S:hintMap <
    mob:minecraft:creeper = Creepers hiss before exploding - sprint away!
    mob:lycanitesmobs:* = Check the Beastiary for this mob's weaknesses.
 >

S:renameMap <
    minecraft:outOfWorld = &5The Abyss
    draconicevolution:chaos = lang:mypack.damage.chaos
 >
```

- ID は、エンティティからの攻撃なら `mob:<エンティティID>`、それ以外は `<modid>:<ダメージの種類>`（`minecraft:fall`、`minecraft:outOfWorld`、`minecraft:explosion.player` など）。`*` ワイルドカードが使えます。
- `&` のカラーコードが使えます。
- `lang:<翻訳キー>` と書くと各プレイヤーのクライアントで翻訳されます。リソースパックで各言語の文を用意できます。
- `hintMap` にない ID には自動ヒントが出ます。
- ゲーム内でも設定できます: `/whm rename <ID> <名前>`、`/whm sethint <ID> <ヒント>`。Mod 入りクライアントからは自分の個人ファイルが変わります。サーバーの common ファイルを変えるには `/whm server rename ...`（OP）を使います。

### `/whm dump` の使い方

1. パックを入れたワールドで `/whm dump` を実行します（サーバーでは OP）。
2. `whathitme_dump.csv`（ゲームまたはサーバーのフォルダ。発生したダメージの種類、モブ、効果、その Mod と属性、現在の設定）と `config/whathitme/hint_template.cfg` が書き出されます。
3. テンプレートには `S:hintMap <...>` と `S:renameMap <...>` のブロックがあり、バニラの全ダメージ種類、そのワールドで発生した全ダメージID、登録されている全モブの `mob:<ID>` が値なしで並んでいます。
4. `whathitme-common.cfg` に貼り付けて、必要な行だけ値を書きます。値が空の行は無視されます。

Mod のダメージ種類は、そのワールドで一度誰かに当たると一覧に載ります。

### 動作の軽さ

- 記録するのはプレイヤーだけです。ダメージ処理はそれ以外のエンティティなら最初の1行で終わるので、モブ同士の戦闘には負荷がかかりません。偽プレイヤーは無視します。
- サーバーで毎 tick 行う処理は、空のリストを確認するだけです。
- 履歴はプレイヤーごとの固定長リングバッファで、エンティティへの参照を持たない小さな不変レコードです。ログアウト時に破棄します。
- 通信は死亡時、トーテム・大ダメージ時、コマンド実行時だけです。被弾ごとには送りません。

### Mod パックに入れてもいいですか？

はい。MIT ライセンスです。連絡も不要です。

## コマンド

`/whm`（別名 `/whathitme`）。すべて Tab キーで補完できます。

| コマンド | 内容 | 使える人 |
|---|---|---|
| `/whm` または `/whm history` | 最近の被ダメージ | 全員 |
| `/whm last` | 直前の死亡の詳細 | 全員 |
| `/whm snapshot [番号]` | 保存されたトーテム・大ダメージのスナップショット（1 が最新） | 全員 |
| `/whm effects` | 現在の効果、Mod、牛乳で消せるか | 全員 |
| `/whm search <文字列\|#タグ\|@modid>` | ダメージの種類・モブ・効果を検索 | 全員 |
| `/whm togglechat` | リスポーン時のチャット通知の ON/OFF | 全員 |
| `/whm ignore <ID>` | ダメージIDの非表示・再表示 | 全員（個人）、サーバーのリストは OP |
| `/whm rename <ID> <名前...>` | 表示名の変更（空で削除） | クライアント: 個人、サーバー: OP |
| `/whm sethint <ID> <ヒント...>` | ヒントの設定（空で削除） | クライアント: 個人、サーバー: OP |
| `/whm config [キー] [値]` | 設定の一覧・表示・変更（`true`/`false`/`on`/`off`/`toggle`） | クライアント: 個人、サーバー: OP |
| `/whm inspect <プレイヤー>` | オンラインのプレイヤーの直前の死亡と履歴 | OP（レベル2） |
| `/whm dump` | `whathitme_dump.csv` と `hint_template.cfg` を書き出す | クライアント: 全員、サーバー: OP |
| `/whm reload` | 設定ファイルの再読み込み | クライアント: 全員、サーバー: OP |
| `/whm server <サブコマンド>` | Mod 入りクライアントからサーバー側で実行 | サーバーに Mod が必要 |
| `/whm help` | コマンド一覧 | 全員 |

`/whm copy` と `/whm copyrecap` は `[IDをコピー]` `[詳細をコピー]` ボタンが内部で使うコマンドです。

## 対応言語

English、日本語、简体中文、繁體中文、한국어、Русский、Deutsch、Français、Español、Português (Brasil)。

`python tools/check_lang.py` で、各言語ファイルに `en_us.lang` と比べて足りないキーや余分なキーがないか確認できます。

## ビルド

Gradle の実行には JDK 25 が必要です。Mod 本体は Java 8 向けにコンパイルされ、そのツールチェーンは Gradle が自動でダウンロードします。

```
./gradlew build -Pmod_version=1.0.0
```

jar は `build/libs/`（`whathitme-<バージョン>.jar`）に出力されます。`-Pmod_version` を省略すると環境変数 `VERSION`、それもなければ `1.0.0` になります。

リリース: `1.0.0` のようなタグを push すると `.github/workflows/release.yml` が jar をビルドし、[CHANGELOG.md](CHANGELOG.md) の該当バージョンの節を添えて GitHub Releases、Modrinth、CurseForge に公開します。

## ライセンス

[MIT](LICENSE) © junjun1z
