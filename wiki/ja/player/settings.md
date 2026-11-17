# 設定画面

## 開き方

| バージョン / ローダー | 方法 |
|---|---|
| すべてのバージョン・ローダー | `/whm config`（引数なし） |
| Forge、NeoForge | Mods 一覧 > What Hit Me? > Config |
| Fabric | Mod Menu > What Hit Me? > 設定ボタン（[Mod Menu](https://modrinth.com/mod/modmenu) を入れているときのみ。任意です） |

![設定画面](/images/config-gui.png)

## 中身

ファイルごとに 2 つのグループがあります。

| グループ | ファイル | 変更できる人 |
|---|---|---|
| **個人（Personal）** | `whathitme-client.cfg` | 自分 |
| **Modpack / サーバー** | `whathitme-common.cfg` | OP、またはチート有効のシングルプレイ |

各グループにカテゴリーがあります：表示スイッチ、フィルター / 名前 / ヒント、死亡画面パネルの配置（個人）または記録（Modpack / サーバー）。すべてのキーの説明は[設定ファイル](/ja/reference/configuration)にあります。

- 変更は **Done** を押したときに保存・適用されます。シングルプレイではサーバー側にもすぐ反映されます。
- modpack が [`lockedFeatures`](/ja/modpack/locked-features) でロックしたキーは操作できない状態で表示されます。
- **Templates...** ボタンで[テンプレートエディター](/ja/modpack/template-editor)が開きます <Badge type="tip" text="1.3.0+" />。

::: tip 表示には両方のファイルが必要
表示項目は**両方**のファイルで `true` のときだけ表示されます。自分のスイッチがオンなのに出てこない場合は、pack かサーバーがオフにしています。
:::

## コマンドで変更する

```
/whm config                       画面を開く（mod なしのプレイヤーには設定の一覧）
/whm config showTimestamp         今の値を表示
/whm config showTimestamp false   変更
/whm config showTimestamp toggle  切り替え（true / false / on / off / toggle）
```

個人のキーは誰でも変更できます。common（Modpack / サーバー）のキーは誰でも見られ、変更は OP だけです。mod 入りのクライアントから `/whm server config <key> <value>` でサーバーのファイルを変更できます。[コマンド一覧](/ja/reference/commands)も参照してください。

## キー割り当て

What Hit Me? は**キー割り当てを追加しません**。操作はすべて死亡画面（マウス）、チャット（クリック / マウスオーバー）、`/whm` コマンドで行うので、他の mod の操作とぶつかることはありません。

## 言語

Minecraft の言語設定に従います。収録：English、日本語、简体中文、繁體中文、한국어、Русский、Deutsch、Français、Español、Português (Brasil)。mod なしのプレイヤーにはサーバーから英語で送られます。
