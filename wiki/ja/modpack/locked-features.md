# 設定のロックと権限

## `lockedFeatures`

設定画面や `/whm config` から**誰も**変更できない設定キーのリストです。シングルプレイでも変更できません。

```json
{
  "lockedFeatures": [
    "showInternalDamageId",
    "includeCoordsInCopy",
    "templates.summary"
  ]
}
```

- キーの大文字・小文字は区別しません。`whathitme-common.cfg` と `whathitme-client.cfg` のどのキーでもロックできます。
- ロックしたキーは設定画面で操作できない状態になり、`/whm config <key> <value>` はロックされている旨を返します。
- 複数の pack ファイルのリストは足し合わされます。

### 表示スイッチをロックしたとき

| pack の設定 | プレイヤーの設定 | 結果 |
|---|---|---|
| `whathitme-common.cfg` で `true`、ロックあり | `false` | **表示される。** プレイヤー自身の値は無視されます。 |
| `whathitme-common.cfg` で `false`、ロックあり | 何でも | 全員に非表示 |
| `whathitme-common.cfg` で `false`、ロックなし | 何でも | 全員に非表示（2 つのファイルのルール） |
| `true`、ロックなし | `false` | そのプレイヤーだけ非表示 |

つまり、**必ず表示させたい**ならロックします。**必ず消したい**なら `whathitme-common.cfg` で `false` にします（ロックもしておけば、プレイヤーが変えようとすることもありません）。

### テンプレートのロック <Badge type="tip" text="1.3.0+" /> {#locking-templates}

| 項目 | 効果 |
|---|---|
| `templates` | すべての出力で、プレイヤーの個人用テンプレートを無視 |
| `templates.hover`、`templates.line`、`templates.summary`、`templates.alert`、`templates.copy` | その出力だけ |

ロックしたテンプレートは、[テンプレートエディター](/ja/modpack/template-editor)で読み取り専用として表示されます。

## 権限

What Hit Me? には独自の権限ノードはなく、バニラの OP レベルを使います。

| 誰が | できること |
|---|---|
| 全員 | `history`、`last`、`snapshot`、`effects`、`stats`、`search`、`togglechat`、個人の `ignore`、個人の `/whm config`、common の `/whm config` の表示、`/whm template` |
| **OP**（権限レベル 2、またはチート有効のシングルプレイ） | `rename`、`sethint`、`inspect`、`test`、`dump`、`reload`、common の `/whm config` の変更、サーバーのリストへの `ignore` |
| チート有効のシングルプレイだけ | テンプレートエディターの「Save to pack」 |
| サーバーのコンソール | サーバー側のすべて。死亡トリガーとルールのコマンドはコンソール権限で実行されます |

::: info ロックは OP より強い
`lockedFeatures` のキーは、OP も含めて誰も設定画面や `/whm config` から変更できません。ファイルを編集して `/whm reload` するか、[パックエディター](/ja/modpack/pack-editor)の **サーバー設定** タブ（OP。ファイルそのものを編集します）を使ってください。
:::
