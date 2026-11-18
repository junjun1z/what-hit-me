# テンプレート <Badge type="tip" text="1.3.0+" />

テンプレートを使うと、What Hit Me? の表示を `{変数}` を使って 1 行ずつ書き換えられます。modpack 制作者は pack フォルダーで設定し、プレイヤーは自分用のテンプレートも持てます。

::: info 書くまでは何も変わりません
テンプレートがなければ、どの出力も 1.2.0 とまったく同じ見た目になります（どの言語でも）。変えたい出力だけを置き換えれば十分です。
:::

## 出力の種類

| 出力 | 変わるもの | 同じテンプレートを使う場所 |
|---|---|---|
| `hover` | 記録の詳細表示（死亡画面のツールチップ） | `/whm` 履歴の行とチャット要約のマウスオーバー |
| `line` | 死亡画面パネルのタイムラインの 1 行 | `/whm history` の各行 |
| `summary` | リスポーン後のチャット要約 | |
| `alert` | トーテム / 死亡回避 / 大ダメージの通知 | |
| `copy` | `[Copy]` / `[Copy Recap]` でコピーする文章 | |

カスタム死亡メッセージも同じ書き方です。[カスタム死亡メッセージ](/ja/modpack/death-messages)を参照してください。

## テンプレートの出どころ

| 優先順位 | 出どころ | ファイル |
|---|---|---|
| 1（最優先） | 個人 | `config/whathitme/whathitme-client-templates.json`（クライアント） |
| 2 | pack | `config/whathitme/pack/**/*.json` の `"templates"` |
| 3 | 組み込み | 通常の 1.2.0 の表示 |

- `lockedFeatures`（`templates` または `templates.<出力名>`）で出力をロックすると、その出力では個人用テンプレートが無視されます。[設定のロック](/ja/modpack/locked-features#locking-templates)を参照。
- `enforce`（または `hide`）付きの[進行度ルール](/ja/modpack/rules)に当てはまった記録では個人用テンプレートを使わないので、隠した項目をプレイヤーが表示させることはできません。
- サーバーでは、mod なしのプレイヤーに送る英語のチャットにも pack のテンプレートが使われます。

## テンプレートの書き方

pack ファイルでは、出力ごとに行のリスト（または `\n` を含む 1 つの文字列）を書きます。

```json
{
  "templates": {
    "summary": [
      "&6&l===== What Hit Me? =====",
      "&cとどめ: &f{killing_blow} &c({damage} ❤)",
      "&3属性: {properties}",
      "&6直前 {total_seconds} 秒: {total} ❤（{total_hits} 回）",
      "&aヒント: {tip}",
      "&b{recap_link}",
      "{toggle_notice}"
    ]
  }
}
```

個人用ファイルも同じ形です。

```json
{
  "templates": {
    "line": ["&4{killing} &8{time_ago} &f{name} &c-{damage} ❤ &e{badges}"]
  }
}
```

いちばん簡単なのは、プレビューを見ながら書けるゲーム内の[テンプレートエディター](/ja/modpack/template-editor)です。

## 書式

| 書式 | 意味 | 例 |
|---|---|---|
| `{変数}` | 変数の値。名前の大文字・小文字は区別しません。 | `{attacker_name}` |
| `{変数\|代わりの文字}` | 変数が空のときに出す文字 | `{attacker_owner\|なし}` |
| `{変数:2}` | 数値の小数点以下の桁数（0〜9）。多くの数値は初期値 1 桁 | `{damage:2}` → `3.75` |
| `&` + コード | 色と書式：`&0`〜`&f` が色、`&l` 太字、`&n` 下線、`&r` リセット | `&c危険` |
| `lang:<キー>` だけの行 | 行全体を翻訳キーとして、読む人の言語に翻訳 | `lang:mypack.summary.header` |
| 既知の翻訳キーだけの行 | これも翻訳されます | `whathitme.summary.header` |
| `{mob_...}` | `{attacker_...}` の別名 | `{mob_name}` = `{attacker_name}` |

**空の行は自動で消えます：** 行の中の変数が**すべて空**なら、その行は表示されません。たとえば `"武器: {weapon}"` は、攻撃者が武器を持っていなければ消えます。変数のない行は常に表示されます。

不明な変数は空になります。`/whm reload` がファイル名と一緒に一覧を出し、エディターでは赤く表示されます。

長い行は、ツールチップやパネルで自動的に折り返されます。

## 変数 {#variables}

::: tip 変数は表示スイッチに従います
表示スイッチがオフ（どちらかのファイル、またはサーバーが無効化）の変数は**空**になり、その行は消えます。テンプレートで、サーバーが無効にした情報を表示することはできません。[ルール](/ja/modpack/rules)で隠した項目も空になります。
:::

### ダメージ

| 変数 | 値 | スイッチ |
|---|---|---|
| `{damage}` | 受けたダメージ（ハート） | `showDamageAmount` |
| `{damage_hp}` | 受けたダメージ（体力ポイント） | `showDamageAmount` |
| `{raw_damage}` | 防具で軽減する前のダメージ（ハート） | `showMitigationDetails` |
| `{mitigation}` | 防具や効果で防いだ割合（初期値は小数 0 桁） | `showMitigationDetails` |
| `{absorbed}` | 使われた衝撃吸収（黄色いハート） | `showMitigationDetails` |
| `{health_after}` | 被弾後の体力（ハート） | `showMitigationDetails` |
| `{max_health}` | 最大体力（ハート） | `showMitigationDetails` |
| `{max_hp_percent}` | 最大体力に対するダメージの割合（小数 0 桁） | `showMaxHpPercentage` |
| `{hits}` | まとめた回数（1 回だけなら空） | `showSmartMergeHitCount` |

### ダメージの種類

| 変数 | 値 | スイッチ |
|---|---|---|
| `{name}` | メインの名前：ルールの名前、カスタム名、攻撃者、ダメージの種類 | |
| `{damage_name}` | ダメージの種類の名前（名前の変更を反映、攻撃者は含まない） | |
| `{damage_id}` | 内部ダメージ ID | `showInternalDamageId` |
| `{damage_mod}` | ダメージの mod | `showSourceModName` |
| `{properties}` | ダメージの属性（炎、魔法など） | `showDamageProperties` |
| `{death_message}` | バニラの死亡メッセージ（summary、alert、copy、死亡メッセージ） | |

### 攻撃者

| 変数 | 値 | スイッチ |
|---|---|---|
| `{attacker_name}` | 攻撃者の名前 | `showAttackerName` |
| `{attacker_id}` | 攻撃者のエンティティ ID | `showAttackerName` |
| `{attacker_mod}` | 攻撃者の mod | `showAttackerName` + `showSourceModName` |
| `{attacker_distance}` | 攻撃者との距離（ブロック） | `showAttackerDistance` |
| `{attacker_owner}` | ペット・タレット・飛び道具の持ち主 | `showEntityOwner` |
| `{direct}` | 飛び道具 / 直接当たったエンティティ（攻撃者と違うとき） | `showAttackerName` |
| `{weapon}` | 攻撃者の武器 | `showAttackerWeapon` |
| `{weapon_id}` | 攻撃者の武器のアイテム ID | `showAttackerWeapon` |
| `{weapon_enchants}` | 武器のエンチャント | `showAttackerWeapon` |

### 状況

| 変数 | 値 | スイッチ |
|---|---|---|
| `{environment}` | 足元と体の位置のブロック / 液体、手に持っていたアイテム | `showEnvironmentContext` |
| `{dimension}` | ディメンション ID | `includeEquipmentInCopy` |
| `{x}` `{y}` `{z}` | 死亡地点（ブロック単位。小数は `{x:1}`） | `includeEquipmentInCopy` + `includeCoordsInCopy` |
| `{time_ago}` | ダメージを受けてからの時間 | `showTimestamp` |
| `{time}` | ダメージを受けた時刻（HH:mm:ss） | `showTimestamp` |

### バッジなど

| 変数 | 値 | スイッチ |
|---|---|---|
| `{badges}` | バッジ：推定、トーテム、大ダメージ、反射、透明、急所 | バッジごと |
| `{killing}` | とどめの一撃なら `☠` | |
| `{tip}` | 対策のヒント | `showHints` |
| `{player}` | プレイヤーの名前 | |
| `{script:<name>}` | スクリプト変数の値。サーバーで計算されます（[スクリプト連携](/ja/modpack/scripts)） | |

### 要約・通知・コピー用

死亡やスナップショット全体が必要な変数です。`summary`・`alert`・`copy`（と死亡メッセージ）で値が入り、`hover` / `line` では空になります。

| 変数 | 値 |
|---|---|
| `{killing_blow}` | とどめの一撃の名前（マウスを乗せると詳細） |
| `{total}` | 直前 `summarySeconds` 秒のダメージ（ハート） |
| `{total_hits}` | 直前 `summarySeconds` 秒の回数 |
| `{total_seconds}` | その時間（`summarySeconds`） |
| `{recap_link}` | まとめ全体へのクリックできるリンク（`/whm last`） |
| `{toggle_notice}` | `/whm togglechat` の案内（`showToggleChatNotice`） |
| `{alert_kind}` | 不死のトーテム、大ダメージ、死亡回避のどれか |
| `{details_link}` | 通知のクリックできる `[Details]` リンク |
| `{timeline}` | まとめのすべてのダメージ行（copy） |
| `{equipment}` | 装備のブロック：ディメンション、防具、効果（`includeEquipmentInCopy`） |
| `{effects}` | 死亡時にかかっていた効果（`includeEquipmentInCopy`） |
| `{recap}` | 組み込みのまとめの文章全体 |

## 出力ごとの例

<!-- TODO media: screenshot: a custom summary template in chat next to the built-in one, and a custom panel line -->

### `hover`

```json
"hover": [
  "&l{name}",
  "{badges}",
  "&8ID: {damage_id}",
  "&cダメージ: {damage} ❤ ({damage_hp} HP)",
  "&7軽減前: {raw_damage} ❤、{mitigation}% 軽減",
  "&e衝撃吸収: {absorbed} ❤",
  "&7体力: {health_after} / {max_health} ❤",
  "&eまとめた回数: x{hits}",
  "&f攻撃者: {attacker_name}",
  "&7経由: {direct}",
  "&b武器: {weapon} &d{weapon_enchants}",
  "&7距離: {attacker_distance}m",
  "&7持ち主: {attacker_owner}",
  "&3属性: {properties}",
  "&9Mod: {damage_mod}",
  "&7{environment}",
  "&aヒント: {tip}"
]
```

### `line`

```json
"line": ["&4{killing} &8{time_ago} &f{name} &c-{damage} ❤ &e{badges}"]
```

最大体力に対する割合を入れた短い行：

```json
"line": ["&f{name} &c{damage:0}❤ &7({max_hp_percent|?}%)"]
```

### `summary`

2 行だけの短い要約：

```json
"summary": [
  "&c☠ {killing_blow} &7({damage} ❤、{total_seconds} 秒で {total} ❤)",
  "&a✎ {tip} &b{recap_link}"
]
```

### `alert`

```json
"alert": ["&6{alert_kind}: &f{name} &c({damage} ❤) {details_link}"]
```

### `copy`

Discord 向けの Markdown：

```json
"copy": [
  "**死亡のまとめ** ({time})",
  "とどめ: {killing_blow}（{damage} ハート）",
  "死亡メッセージ: {death_message}",
  "{timeline}",
  "直前 {total_seconds} 秒: {total} ハート（{total_hits} 回）",
  "{equipment}"
]
```

### 翻訳する行

```json
"summary": [
  "lang:mypack.whm.header",
  "&c{killing_blow} &7- {tip}"
]
```

## テンプレートの確認

| 方法 | わかること |
|---|---|
| [テンプレートエディター](/ja/modpack/template-editor) | 入力するたびに更新されるプレビュー。最新の死亡か、サンプルの死亡で表示 |
| `/whm template preview <output>` | 最新の死亡（なければサンプル）でテンプレートをチャットに表示 |
| `/whm reload` | 不明な変数と出力名をファイル名付きで表示 |

## 個人用テンプレート

pack がロックしていなければ、プレイヤーは自分のテンプレートを書けます。

- ゲーム内：`/whm template`、または設定画面の **Templates...** で編集して **Save**
- ファイル：`config/whathitme/whathitme-client-templates.json`（コメントアウトされた例と一緒に作られます）を編集して `/whm reload`
- pack / 組み込みの見た目に戻す：エディターの **Reset**、`/whm template reset <output>`、すべてなら `/whm template reset`
