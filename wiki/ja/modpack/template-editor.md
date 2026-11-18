# テンプレートエディター <Badge type="tip" text="1.3.0+" />

[テンプレート](/ja/modpack/templates)を**プレビューを見ながら**書けるゲーム内の画面です。対応しているすべてのバージョンとローダーで動き、クライアントに mod が必要です。

<!-- TODO media: retake: current shot shows test strings ("WHMT"); also a short GIF of typing with the live preview updating -->

![テンプレートエディター](/images/template-editor.png)

## 開き方

- `/whm template`
- [設定画面](/ja/player/settings)の **Templates...** ボタン（Forge / NeoForge の Mods 一覧や Mod Menu からも）

## 手順

1. 上のタブで**出力を選びます**：Hover、Line、Summary、Alert、Copy。チート有効のシングルプレイでは **Death msgs** もあります。
2. **行を編集します。** テンプレートの 1 行が 1 つの入力欄です。
   - `+ Line` で行を追加、`↑` / `↓` で移動、`✕` で削除します。
   - カーソル移動、範囲選択、Ctrl+A / C / V / X、Home / End、Backspace / Delete など、普通の文字入力の操作が使えます。
3. 右の一覧から**変数を挿入します。** 変数をクリックするとカーソルの位置に入ります。マウスを乗せると（または選ぶと）、下に 1 行の説明が出ます。変数は Damage、Damage type、Attacker、Context、Other、Script（サーバーの[スクリプト](/ja/modpack/scripts)が登録した変数。登録時の説明付き）に分かれています。
4. 行の下の**プレビューを見ます。** 実際の表示と同じ処理で描かれます：Hover はツールチップ、Line はパネルの 1 行、Summary と Alert はチャットの行、Copy は文字だけの枠です。入力するたびに更新されます。
5. **Sample death** / **Latest death** で**プレビューのデータを切り替えます**：自分の最新の死亡か、組み込みのサンプルの死亡です。
6. **保存します。**

不明な変数は入力欄で赤く表示されるので、打ち間違いにすぐ気づけます。

## ボタン

| ボタン | 動作 | 使えるとき |
|---|---|---|
| **Save** | 個人用テンプレート（`whathitme-client-templates.json`）に保存 | 出力がロックされていないとき |
| **Save to pack** | `config/whathitme/pack/templates/<output>.json` に書き込む（死亡メッセージは `pack/templates/deathmessages.json`） | チート有効のシングルプレイ |
| **Reset** | この出力の個人用テンプレートを削除 | 出力がロックされていないとき |
| **Sample death** / **Latest death** | プレビューのデータを切り替え | 常に |
| **Close** | エディターを閉じる | 常に |

[パックエディター](/ja/modpack/pack-editor)のテンプレート項目の **Edit in the template editor...** から開いたときのボタンは **Done**（行をパックエディターの項目に戻し、その項目を保存）、**Sample death** / **Latest death**、**Cancel** です。

入力欄の下の行には、表示中のテンプレートの出どころが出ます：*Your personal template*（個人用）、*Modpack template*（pack）、*Built-in output (editing starts from an example)*（組み込み。例から編集を始めます）。

## ロックされた出力

pack が出力をロックしている場合（`lockedFeatures` に `templates` または `templates.<出力名>`）、エディターは pack のテンプレートを**読み取り専用**で表示し、*Locked by the modpack (read-only)* と出ます。Save と Reset は使えません。

<!-- TODO media: retake: locked output without test strings -->

![テンプレートエディターでロックされた出力](/images/template-editor-locked.png)

このスクリーンショットでは書式の例も見られます：代わりの文字（`{attacker_owner|nobody}`）、小数の桁数（`{damage:2}`）、`mob_` の別名（`{mob_name}`）、`lang:` の行。

## Death msgs タブ {#death-messages-tab}

チート有効のシングルプレイでは、**Death msgs** タブで pack の[カスタム死亡メッセージ](/ja/modpack/death-messages)を編集できます。1 行に 1 項目です。

```
mekanism:acid = {player} dissolved in acid
*@twilightforest:lich = {player} was outsmarted by the Lich
```

プレビューには、その項目が**プレビューの死亡に当てはまるか**が表示されます。死亡メッセージは pack の設定なので、このタブには **Save to pack** だけがあり、個人用の Save はありません。

## プレビューと進行度ルール

最新の死亡が[進行度ルール](/ja/modpack/rules)で変わっている場合、プレビューに `progression rule: enforce, attacker, ...` のように表示されます。ルール自体はファイルで編集します（エディターにルールのタブはありません）。確認には [`/whm test --as / --assume`](/ja/modpack/scanner#whm-test) を使ってください。

::: tip modpack 制作者のおすすめの流れ
チート有効のシングルプレイのテストワールドでテンプレートを作り、**Save to pack** で保存して、`config/whathitme/pack/templates/` を pack にコピーします。
:::
