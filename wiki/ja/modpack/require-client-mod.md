# requireClientMod <Badge type="tip" text="1.3.0+" />

クライアントに What Hit Me? が入って**いない**プレイヤーを拒否するサーバーの設定です（バージョンは問いません）。

```
# whathitme-common.cfg、カテゴリー tracking
B:requireClientMod=true
```

ゲーム内（OP）なら `/whm config requireClientMod true`（mod 入りクライアントからは `/whm server config requireClientMod true`）。

## プレイヤーに見えるもの

![mod のないプレイヤーに表示される画面（メッセージは常に英語）](/images/ja/require-client-mod.webp)

mod なしのプレイヤーは、次のメッセージで切断されます。

> This server requires the What Hit Me? mod on your client (any version). Install it and join again.

（mod のないクライアントには翻訳がないため、英語で表示されます。）確認するのは mod があるかどうかだけで、バージョンは確認しません。初期値は `false` です。

## しくみ

どのローダー・バージョンでも、サーバーはプレイヤーが参加してから最大 **5 秒**、クライアントからの挨拶（hello パケット）を待ちます。届かなければ上のメッセージで切断します。参加ごとにタイマーのタスクが 1 つ動くだけで、tick の処理はありません。サーバーのログには `[What Hit Me?] <name> has no What Hit Me? (requireClientMod): disconnected` と出ます。

## 使いどころ

- [進行度ルール](/ja/modpack/rules)で、**バニラの死亡メッセージ**に出てしまう情報（倒した相手など）を隠していて、サーバーが **Forge か NeoForge** のとき
- Forge / NeoForge で[カスタム死亡メッセージ](/ja/modpack/death-messages)を使っていて、全員に見せたいとき
- pack にどうせ mod が入っているので、全員にパネルや翻訳などの機能をそろえて使ってほしいとき

**Fabric** のサーバーは全員の死亡メッセージを置き換えるので、この設定が必要なのは 3 つ目の理由のときだけです。

## 死亡メッセージの警告 {#the-death-message-warning}

Forge か NeoForge のサーバーは、次の条件がそろうと、起動時と `/whm reload` 時にログへ警告を出します（`/whm reload` の出力にも表示されます）。

- ルールが `death_message` を隠している、または `deathMessages` の項目がある、**かつ**
- `requireClientMod` が `false`

```
On Forge 1.20.1, players without What Hit Me? still see the vanilla death message; set requireClientMod=true to hide it from everyone.
```

（意味：Forge 1.20.1 では、What Hit Me? のないプレイヤーにはバニラの死亡メッセージが見えます。全員から隠すには requireClientMod=true にしてください。）

Fabric ではこの警告は出ません。
