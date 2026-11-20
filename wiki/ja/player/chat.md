# チャットの要約と通知

## リスポーン後のチャット要約

リスポーンすると、**自分のチャットにだけ**短い要約が表示されます（他のプレイヤーには見えません）。

![リスポーン後のチャット要約](/images/ja/respawn-chat.webp)

![チャット画面で要約の行にカーソルを合わせたところ](/images/ja/chat-hover.webp)

```
========== [ What Hit Me? ] ==========
☠ Killing Blow: Creeper (Explosion) [26.4 ❤] (Minecraft) [Copy ID] [Mute]
⚠ Properties: Explosion
Σ Last 10s Total: 35 ❤ taken (8 hits)
✎ Tip: Blast Protection reduces explosions a lot. Keep your distance from creepers and TNT.
➜ [Click here for full death recap] (/whm last)
✖ To disable this message, run /whm togglechat
=====================================
```

（日本語の設定なら、mod 入りのクライアントでは日本語で表示されます。）

| 部分 | できること |
|---|---|
| とどめの一撃 | マウスを乗せると[詳細](/ja/player/death-screen#hover-details)が出ます。 |
| `[Copy ID]` | ダメージ ID をクリップボードにコピー（mod なしのプレイヤーはチャット欄に入ります）。不具合報告や [pack ファイル](/ja/modpack/names-and-tips)の作成に便利です。 |
| `[Mute]` | このダメージ ID を以後表示しない（`/whm ignore <id>` と同じ）。`/whm` でもう一度押すと元に戻ります。 |
| `[Click here for full death recap]` | `/whm last` を実行し、すべての被ダメージを含むまとめを表示します。 |
| `⌂ Surroundings` | 攻撃者のいないダメージで、ブロック / 液体と手に持っていたアイテム |

**オフにする：** `/whm togglechat`（押すたびにオン / オフ）。mod なしのプレイヤーも使え、サーバーがプレイヤーごとに記憶します。最後の行は `showToggleChatNotice=false` で消せます。

## 通知とスナップショット {#alerts-and-snapshots}

死ななかった場合でも、次の瞬間は**スナップショット**として保存され、チャットに通知が出ます。

![トーテムの通知](/images/totem.png)

| 通知 | いつ |
|---|---|
| `⚠ [What Hit Me?] A Totem of Undying saved you! Last hit: Zombie 17.6 ❤ [Details]` | 不死のトーテムで助かったとき |
| `⚠ [What Hit Me?] Lethal damage was prevented! Last hit: ... [Details]` | 別の mod が死亡をキャンセルした、または死亡直後に体力を戻したとき |
| `⚠ [What Hit Me?] Heavy hit: Wither took 16 ❤ at once.` | 1 回で最大体力の 80 % 以上を受けたとき（`heavyHitThreshold = 0.8`） |

- `[Details]` をクリックするか `/whm snapshot` で、その瞬間のまとめを見られます。`/whm snapshot 2` は 2 番目に新しいもの、以下同様です。
- プレイヤーごとに最大 5 件（`maxSnapshots`）保存します。
- スイッチ：`enableTotemChatAlert`（トーテムと死亡回避）、`enableHeavyHitSnapshot`（大ダメージ）

## Copy Recap {#copy-recap}

死亡画面の `[Copy]` と `/whm last` の `[Copy Recap]` は、まとめを **Markdown** でコピーします。Discord や不具合報告にそのまま貼れます。死亡メッセージ、とどめの一撃、すべての被ダメージ、合計、装備のブロックが入ります。

```markdown
## Equipment
- Dimension: minecraft:overworld
- Position: 120.5 / 64.0 / -33.2
- Max health: 20.0 (10 ❤)
- Armor:
  - Head: Iron Helmet [Protection II]
  - Chest: Diamond Chestplate (broken)
  - Legs: (empty)
  - Feet: Iron Boots
- Effects:
  ...
```

| スイッチ | 効果 |
|---|---|
| `includeEquipmentInCopy` | ディメンション、最大体力、防具 4 か所（エンチャント・mod・破損状態）、効果を追加 |
| `includeCoordsInCopy` | 死亡地点の座標を追加（`includeEquipmentInCopy` が必要） |
| `showActionButtons` | `[Copy ID]`・`[Mute]`・`[Copy Recap]` ボタン自体を表示するか |

::: tip サーバーでのプライバシー
プレイヤーに座標を貼られたくない場合は、`whathitme-common.cfg` で `includeCoordsInCopy=false` にし、[`lockedFeatures`](/ja/modpack/locked-features) にも追加します。
:::

1.3.0 からは、要約・通知・コピーする文章を[テンプレート](/ja/modpack/templates)で書き換えられます <Badge type="tip" text="1.3.0+" />。
