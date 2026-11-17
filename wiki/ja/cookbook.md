# レシピ集

そのままコピーして使える pack ファイルです。それぞれ `config/whathitme/pack/`（ファイル名は `.json` で終わればなんでも可）に置き、`/whm reload` して `/whm test` で確認してください。

::: tip ダメージ ID はバージョンで違います
例ではよく使うバニラの ID を使っています。名前が Minecraft のバージョンで違うものがあります（1.19.4 より前は `minecraft:outOfWorld`、以降は `minecraft:out_of_world`）。`/whm search` や `/whm dump` で確認してください。
:::

## 名前とヒント

### バニラのダメージにわかりやすい名前を付ける

```json
{
  "renameMap": {
    "minecraft:outOfWorld": "&5奈落",
    "minecraft:inWall": "窒息",
    "minecraft:cramming": "エンティティの詰め込みすぎ",
    "minecraft:flyIntoWall": "運動エネルギー"
  }
}
```

### mod 全体に 1 つのヒント、ボスにはもっと詳しいヒント

最も具体的なキーが勝つので、ボスのヒントが mod 全体のヒントより優先されます。

```json
{
  "hintMap": {
    "@twilightforest:*": "Twilight Forest の mob は強い。地上より良い防具を用意しよう。",
    "@twilightforest:lich": "まずリッチの盾を壊してから本体を攻撃しよう。",
    "twilightforest:lich_bolt @twilightforest:lich": "リッチの弾は剣を振って跳ね返せる！"
  }
}
```

### ディメンションごとのヒント

```json
{
  "hintMap": {
    "minecraft:lava @dim:minecraft:the_nether": "ネザーでは耐火のポーションを持ち歩こう。",
    "minecraft:fall @dim:minecraft:the_end": "エンドには低速落下のポーションか水バケツを。",
    "* @dim:mypack:deep_dark_dimension": "ここでは何もかもが理不尽。仲間を連れてこよう。"
  }
}
```

### 武器を持った mob に別の名前を付ける

```json
{
  "renameMap": {
    "mob:minecraft:zombie @item:minecraft:iron_sword": "武装ゾンビ",
    "mob:minecraft:zombie @item:*diamond*": "&b重装備のゾンビ",
    "minecraft:arrow @minecraft:skeleton": "スケルトンの狙撃"
  }
}
```

### 翻訳するヒント

```json
{
  "hintMap": {
    "minecraft:lava": "lang:mypack.hint.lava",
    "mob:minecraft:creeper": "lang:mypack.hint.creeper"
  }
}
```

あとはリソースパックの `assets/mypack/lang/en_us.json`、`ja_jp.json` などに `"mypack.hint.lava": "..."` を書きます。

## 邪魔な記録を隠す

```json
{
  "defaultIgnoredDamageIds": [
    "minecraft:cactus",
    "minecraft:sweetBerryBush",
    "*radiation_tick*"
  ]
}
```

## 設定をロックする

ヒントと mod 名は常に表示し、座標は貼らせない：

```json
{
  "lockedFeatures": ["showHints", "showSourceModName", "includeCoordsInCopy"]
}
```

さらに `whathitme-common.cfg` で `includeCoordsInCopy=false` にします（ロックだけでは pack 側の値が使われるので、そこで望む値にしておきます）。

## 死亡トリガー

### 初めてクリーパーに倒されたらパンをあげる

```json
{
  "deathTriggers": [
    { "match": "mob:minecraft:creeper", "command": "give {player} minecraft:bread 4", "oncePerPlayer": true }
  ]
}
```

### 死因ごとの回数をスコアボードで数える

```json
{
  "deathTriggers": [
    { "match": "minecraft:lava", "command": "scoreboard players add {player} deaths_lava 1" },
    { "match": "mob:*", "command": "scoreboard players add {player} deaths_mob 1" }
  ]
}
```

### 倒した相手の名前付きでサーバーにお知らせ

```json
{
  "deathTriggers": [
    { "match": "@twilightforest:*", "command": "say {player} fell to {attacker_name} ({damage} HP hit)", "cooldownSeconds": 30 }
  ]
}
```

## テンプレート <Badge type="tip" text="1.3.0+" />

### 最小限のリスポーン要約

```json
{
  "templates": {
    "summary": [
      "&c☠ {killing_blow} &7({damage} ❤ / {total_seconds} 秒で {total} ❤)",
      "&a✎ {tip}",
      "&b{recap_link}"
    ]
  }
}
```

### 最大体力の割合付きの短いパネル行

```json
{
  "templates": {
    "line": ["&4{killing}&8{time_ago} &f{name} &c-{damage} ❤ &7{max_hp_percent|?}% &e{badges}"]
  }
}
```

### 短い詳細表示

```json
{
  "templates": {
    "hover": [
      "&l{name}",
      "&c{damage} ❤ &7（軽減前 {raw_damage} ❤、{mitigation}% 軽減）",
      "&f{attacker_name} &7{weapon}",
      "&9{damage_mod}",
      "&a{tip}"
    ]
  }
}
```

### Discord に貼りやすいコピー文

````json
{
  "templates": {
    "copy": [
      "**{player} が死亡** {time} / {dimension}",
      "> {death_message}",
      "```",
      "{timeline}",
      "```",
      "直前 {total_seconds} 秒: {total} ハート（{total_hits} 回）",
      "{equipment}"
    ]
  }
}
````

### 要約にはプレイヤーのテンプレートを使わせない

```json
{
  "lockedFeatures": ["templates.summary"]
}
```

## カスタム死亡メッセージ <Badge type="tip" text="1.3.0+" />

```json
{
  "deathMessages": {
    "*@twilightforest:lich": "{player}はリッチに出し抜かれた",
    "minecraft:lava @dim:minecraft:the_nether": "{player}はネザーの溶岩風呂に入った",
    "mob:minecraft:zombie @item:*sword*": "{player}は{weapon|刃物}を持った{attacker_name}に斬られた"
  }
}
```

Forge / NeoForge のサーバーでは、これが見えるのは mod 入りのプレイヤーだけです。[対応表](/ja/modpack/death-messages#support-matrix)を参照してください。

## 進行度ルール <Badge type="tip" text="1.3.0+" /> {#progression-rules}

### ボスのクエストを終えるまで ???（FTB Quests）

```json
{
  "rules": [
    {
      "match": "*@twilightforest:lich",
      "unless": ["ftbquests:1A2B3C4D5E6F7081"],
      "name": "&k???",
      "hint": "",
      "hide": ["damage_id", "attacker", "mod", "weapon", "death_message"]
    }
  ]
}
```

`1A2B3C4D5E6F7081` はボスのクエストの ID に置き換えてください。Forge / NeoForge では [`requireClientMod=true`](/ja/modpack/require-client-mod) にもして、バニラの死亡メッセージで倒した相手がわからないようにしましょう。

### 解放されるまでディメンションを丸ごと隠す（Game Stages）

```json
{
  "rules": [
    {
      "match": "* @dim:mypack:the_abyss",
      "unless": ["stage:abyss"],
      "name": "闇の中の何か",
      "hint": "まだこの場所に来る準備ができていない。",
      "hide": ["attacker", "mod", "weapon", "environment", "position", "death_message"]
    }
  ]
}
```

### 何度か死んだらヒントを変える（スコア）

バニラの目的で死亡回数を数えて（`/scoreboard objectives add deaths deathCount`）、次のように書きます。

```json
{
  "rules": [
    { "match": "minecraft:lava", "if": ["score:deaths>=3"], "hint": "&eまだ溶岩で死んでる？水バケツと耐火のポーションを持とう！" }
  ]
}
```

### 進捗で正体を明かし、初回だけ特別なメッセージ

```json
{
  "rules": [
    {
      "match": "*@minecraft:warden",
      "unless": ["advancement:minecraft:adventure/avoid_vibration"],
      "name": "&8地の底のもの",
      "hint": "スニークしよう。あれは何でも聞いている。",
      "hide": ["attacker", "mod"],
      "commands": ["tellraw {player} {\"text\":\"恐ろしいものに出会った。次はスニークしよう。\",\"color\":\"dark_gray\"}"],
      "oncePerPlayer": true
    }
  ]
}
```

### 特別なルールを先に、一般的なルールを後に

ルールは**最初に**当てはまったものが使われるので、特別なものから一般的なものの順に並べます。

```json
{
  "rules": [
    { "match": "twilightforest:lich_bolt @twilightforest:lich", "unless": ["tag:lich_known"], "name": "奇妙な弾", "hint": "剣を振ってみよう。" },
    { "match": "*@twilightforest:lich", "unless": ["tag:lich_known"], "name": "???", "hint": "", "hide": ["attacker", "mod"] }
  ]
}
```

### 新しいプレイヤーだけの死亡トリガー

```json
{
  "deathTriggers": [
    { "match": "*", "command": "give {player} minecraft:cooked_beef 8", "unless": ["tag:veteran"], "cooldownSeconds": 600 }
  ]
}
```
