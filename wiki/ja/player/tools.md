# 履歴・統計・検索

ここにあるコマンドは誰でも使えます。全コマンドは[コマンド一覧](/ja/reference/commands)にあります。

## ダメージ履歴：`/whm`

`/whm`（または `/whm history`）は、生きていても死んでいても、最近受けたダメージを一覧にします。行にマウスを乗せると詳細が出て、各行の最後に `[Copy ID]` と `[Mute]` があります。

![/whm の履歴と詳細表示](/images/history.png)

- 3 秒以内の同じ原因の連続ダメージは 1 行にまとめます（`x4 hits`）。
- 最後の行は `Last 10s Total` です。
- 表示する行数は `chatHistoryLines`（初期値 15）。サーバーはプレイヤーごとに最大 `historySize` 件（初期値 50）を保持し、ログアウトすると破棄します。

## 最後の死亡：`/whm last`

最後に死んだときのまとめを、死亡画面のパネルと同じようにチャットに表示します。死亡メッセージ、とどめの一撃、属性、直前 30 秒の全ダメージ、`Last 10s Total`、ヒント、`[Copy Recap]` が出ます。

## スナップショット：`/whm snapshot [n]`

保存されたトーテム・死亡回避・大ダメージの瞬間です。`1` が最新です。[通知とスナップショット](/ja/player/chat#alerts-and-snapshots)を参照してください。

## 統計：`/whm stats`

合計の死亡回数、受けた合計ダメージ（実際に減った体力）、死因の上位 5 件を表示します。死因はプレイヤーごとに最大 100 種類まで数えます。死亡時・ログアウト時・ワールド保存時に `config/whathitme/.cache/stats_<uuid>.json` に保存します。

## 効果：`/whm effects`

今かかっているポーション効果を、レベル、残り時間、mod、牛乳で消せるか（`[Milk ✔]` / `[Milk ✖]`）と一緒に表示します。効果にマウスを乗せると ID と治す手段が出ます。

![/whm effects](/images/effects.png)

## 検索：`/whm search` {#search-whm-search}

pack にあるダメージの種類、mob、効果を探します。スペースで区切ると条件を組み合わせられます。

![/whm search fire](/images/search.png)

| 検索語 | 見つかるもの |
|---|---|
| `zombie` | ID か名前に「zombie」を含むもの |
| `@twilightforest` | その mod のものすべて |
| `@minecraft zombie` | バニラで「zombie」を含むもの |
| `#bypasses_armor` | 防具を無視するダメージ |
| `#absolute` | 防具・エンチャント・効果をすべて無視するダメージ |
| `#magic`、`#fire`、`#explosion`、`#projectile` | その属性を持つダメージ |
| `#damage` | ダメージの種類だけ |
| `#scanned` | [スキャナー](/ja/modpack/scanner)が、誰かが受ける前に見つけたダメージ ID |
| `#mob` | mob |
| `#effect`、`#debuff` | 効果、有害な効果だけ |

表示されるのは最大 30 件です。
