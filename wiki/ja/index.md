---
layout: home

hero:
  name: What Hit Me?
  text: 何にやられたのか、ひと目でわかる。
  tagline: Forge・Fabric・NeoForge（1.7.10 - 26.3）対応の Minecraft mod。死因をわかりやすい言葉で説明します。どんな modpack でも動き、必須の前提 mod はありません。
  image:
    src: /images/icon_128.png
    alt: What Hit Me? のアイコン
  actions:
    - theme: brand
      text: はじめる
      link: /ja/guide/getting-started
    - theme: alt
      text: プレイヤー向けガイド
      link: /ja/player/death-screen
    - theme: alt
      text: Modpack 制作者向け
      link: /ja/modpack/

features:
  - title: 死亡画面のまとめ
    details: とどめの一撃、攻撃してきた相手とその mod、直前 30 秒間の全ダメージ、防具で軽減する前と後のダメージ、対策のヒント。各行にマウスを乗せると詳細が出ます。
    link: /ja/player/death-screen
  - title: クライアントだけでも、サーバーだけでも
    details: サーバーだけ、クライアントだけ、両方。どの組み合わせでも動き、各プレイヤーにはそのクライアントで使える一番よいモードが選ばれます。
    link: /ja/guide/multiplayer
  - title: Modpack のための機能
    details: ダメージの種類・攻撃者・武器・ディメンションごとの名前とヒント、ID の非表示、設定のロック、死亡時のコマンド実行。
    link: /ja/modpack/
  - title: テンプレートと死亡メッセージ
    details: "1.3.0+：すべての表示をテンプレートで書き換え。ゲーム内エディターでプレビューしながら編集でき、バニラの死亡メッセージも置き換えられます。"
    link: /ja/modpack/templates
  - title: 進行度ルール
    details: "1.3.0+：ボスのクエストを終えるまでは「???」と表示し、ヒントも出さない。進捗・スコアボード・Game Stages・FTB Quests・Better Questing・Heracles に対応。"
    link: /ja/modpack/rules
  - title: 軽くて安全
    details: 記録するのはプレイヤーだけ。サーバーの tick 処理なし、通信は死亡時と要求時だけ。他 mod の壊れたデータでもクラッシュしないよう、すべての処理を保護しています。
    link: /ja/guide/
---

<div class="vp-doc" style="max-width: 960px; margin: 48px auto 0; padding: 0 24px;">

## 実際の動き

![What Hit Me? の動作：死亡画面のまとめ、詳細表示、リスポーン後のチャット要約](/images/hero.gif)

## どこから読む？

| あなたは… | おすすめのページ |
|---|---|
| プレイヤー | [死亡画面のパネル](/ja/player/death-screen)、[チャットの要約と通知](/ja/player/chat)、[コマンド一覧](/ja/reference/commands) |
| サーバー管理者 | [インストール](/ja/guide/getting-started)、[クライアント・サーバーの組み合わせ](/ja/guide/multiplayer)、[設定ファイル](/ja/reference/configuration) |
| Modpack 制作者 | [概要](/ja/modpack/)、[pack フォルダー](/ja/modpack/pack-folder)、[レシピ集](/ja/cookbook) |

::: tip バージョンについて
この Wiki は **What Hit Me? 1.3.0** の内容です。<Badge type="tip" text="1.3.0+" /> の付いた項目は 1.3.0 で追加されたもので、1.2.0 にはありません。<Badge type="warning" text="予定" /> の付いたページは、まだ実装されていない機能の説明です。
:::

</div>
