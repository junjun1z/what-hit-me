# カスタム死亡メッセージ <Badge type="tip" text="1.3.0+" />

選んだ死亡について、バニラの死亡メッセージ（「Steve was slain by Zombie」）を独自の文章に置き換えます。modpack 制作者向けの機能で、**初期状態ではオフ**です。項目がなければ何も変わりません。

## 書き方

どの pack ファイルにでも `deathMessages` を書きます（`pack/deathmessages/_example.json` にコメントアウトされたサンプルがあります）。

```json
{
  "deathMessages": {
    "mekanism:acid": "{player}は酸に溶けた",
    "*@twilightforest:lich": "{player}はリッチに出し抜かれた",
    "minecraft:lava @dim:minecraft:the_nether": "&c{player}はネザーの一部になった"
  }
}
```

- **キー**は[マッチキー](/ja/modpack/match-keys)で、`renameMap` と同じく「最も具体的なものが勝つ」優先順位です。
- **値**は 1 行の[テンプレート](/ja/modpack/templates)です：すべてのテンプレート変数、`{変数|代わりの文字}`、`&` の色、`lang:` キーが使えます。
- `lang:` キーは、メッセージを作る場所で翻訳されます。Forge / NeoForge では mod 入りの各クライアント（その人の言語）、Fabric ではリソースパックを読めないサーバーです。Fabric のサーバーでは文章を直接書いてください。
- 当てはまるキーのない死亡はバニラのメッセージのままです。それ以外の死亡のメッセージを自動で作ることはありません。

よく使う変数：`{player}`、`{name}`、`{attacker_name}`、`{weapon}`、`{damage_name}`、`{dimension}`、`{death_message}`（バニラの文章）。

```json
"mob:minecraft:zombie @item:*sword*": "{player}は{weapon|刃物}を持った{attacker_name}に斬られた"
```

## 対応表 {#support-matrix}

| サーバー | mod ありのプレイヤー | mod なしのプレイヤー | 死亡画面 | サーバーのログ |
|---|---|---|---|---|
| **Fabric**（全バージョン） | カスタム | カスタム | カスタム | カスタム |
| **Forge / NeoForge**（全バージョン） | カスタム（クライアントがバニラのチャット行を置き換え） | **バニラのまま** | カスタム（Forge 1.7.10 は死亡画面にメッセージが出ない） | `[What Hit Me?] Death message: ...` の行 |

違いがある理由：Fabric ではゲームが死亡メッセージを作る場所で mod が書き換えられるので、全員に届きます。Forge と NeoForge にはそのためのイベントがないため、サーバーがカスタムの文章を mod 入りのクライアントに送り、クライアントが該当するチャット行と死亡画面の文章を置き換えます。mod なしのプレイヤーにはバニラのメッセージが見えます。

::: warning Forge / NeoForge のサーバー
バニラのメッセージを**誰にも**見せたくない場合（たとえばルールで倒した相手を隠しているとき）は、[`requireClientMod`](/ja/modpack/require-client-mod) をオンにしてください。オンにしないと、サーバーは起動時と `/whm reload` 時に警告をログに出します。
:::

死亡のまとめ（`/whm last`、パネル）には、プレイヤーが実際に見たメッセージが引用されます。

## ルールで隠した死亡メッセージ {#hidden-death-messages-from-rules}

[進行度ルール](/ja/modpack/rules)が `death_message` を隠し、当てはまる `deathMessages` の項目がない場合は、中立的な組み込みのメッセージを使います。

| 倒した相手 | メッセージ |
|---|---|
| エンティティ | `{player} was killed by {name}` —— `{name}` はルールの名前（例：`???`） |
| エンティティなし | `{player} died` |

これらは mod 入りのプレイヤーには翻訳されて表示されます（10 言語）。Fabric では 1.19.4 以降、英語を予備にした翻訳として送られ、それより古い Fabric では英語です。mod なしのプレイヤーには英語で表示されます。

当てはまる `deathMessages` の項目があれば、常に組み込みの隠しメッセージより優先されます。

## 確認

```
/whm test mekanism:acid
/whm test minecraft:generic twilightforest:lich
```

`/whm test` で、使われる死亡メッセージを確認できます。チート有効のシングルプレイなら、[テンプレートエディター](/ja/modpack/template-editor#death-messages-tab)で編集とプレビューもできます。
