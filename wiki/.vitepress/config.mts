import { defineConfig, type DefaultTheme } from 'vitepress'

const repo = 'https://github.com/junjun1z/what-hit-me'

// Sidebar per language; prefix is '' (English) or '/ja'
function sidebar(p: string, t: Record<string, string>): DefaultTheme.Sidebar {
  return [
    {
      text: t.start,
      items: [
        { text: t.intro, link: `${p}/guide/` },
        { text: t.install, link: `${p}/guide/getting-started` },
        { text: t.multiplayer, link: `${p}/guide/multiplayer` },
        { text: t.integrations, link: `${p}/guide/integrations` },
      ],
    },
    {
      text: t.players,
      items: [
        { text: t.deathScreen, link: `${p}/player/death-screen` },
        { text: t.chat, link: `${p}/player/chat` },
        { text: t.tools, link: `${p}/player/tools` },
        { text: t.settings, link: `${p}/player/settings` },
      ],
    },
    {
      text: t.reference,
      items: [
        { text: t.commands, link: `${p}/reference/commands` },
        { text: t.config, link: `${p}/reference/configuration` },
        { text: t.ids, link: `${p}/reference/damage-ids` },
      ],
    },
    {
      text: t.modpack,
      items: [
        { text: t.mpOverview, link: `${p}/modpack/` },
        { text: t.packFolder, link: `${p}/modpack/pack-folder` },
        { text: t.matchKeys, link: `${p}/modpack/match-keys` },
        { text: t.names, link: `${p}/modpack/names-and-tips` },
        { text: t.locked, link: `${p}/modpack/locked-features` },
        { text: t.triggers, link: `${p}/modpack/death-triggers` },
        { text: t.scanner, link: `${p}/modpack/scanner` },
        { text: t.templates, link: `${p}/modpack/templates` },
        { text: t.editor, link: `${p}/modpack/template-editor` },
        { text: t.deathMessages, link: `${p}/modpack/death-messages` },
        { text: t.rules, link: `${p}/modpack/rules` },
        { text: t.conditions, link: `${p}/modpack/conditions` },
        { text: t.requireMod, link: `${p}/modpack/require-client-mod` },
        { text: t.scripts, link: `${p}/modpack/scripts` },
        { text: t.packEditor, link: `${p}/modpack/pack-editor` },
      ],
    },
    {
      text: t.more,
      items: [
        { text: t.cookbook, link: `${p}/cookbook` },
        { text: t.faq, link: `${p}/troubleshooting` },
        { text: t.changelog, link: `${p}/changelog` },
      ],
    },
  ]
}

const en = {
  start: 'Getting started', intro: 'What is What Hit Me?', install: 'Installation', multiplayer: 'Client, server or both', integrations: 'Mod integrations',
  players: 'Player guide', deathScreen: 'Death screen panel', chat: 'Chat summary & alerts', tools: 'History, stats & search',
  settings: 'Settings screen',
  reference: 'Reference', commands: 'Commands', config: 'Configuration files', ids: 'Damage ids & badges',
  modpack: 'Modpack makers', mpOverview: 'Overview', packFolder: 'Pack folder', matchKeys: 'Match keys',
  names: 'Names, tips & ignored ids', locked: 'Locked features & permissions', triggers: 'Death triggers',
  scanner: 'Scanner, dump & /whm test', templates: 'Templates (1.3.0+)', editor: 'Template editor (1.3.0+)',
  deathMessages: 'Custom death messages (1.3.0+)', rules: 'Progression rules (1.3.0+)', conditions: 'Conditions & quest mods (1.3.0+)',
  requireMod: 'requireClientMod (1.3.0+)', scripts: 'Script integration (planned)', packEditor: 'Pack editor (planned)',
  more: 'More', cookbook: 'Cookbook', faq: 'Troubleshooting & FAQ', changelog: 'Changelog',
}

const ja = {
  start: 'はじめに', intro: 'What Hit Me? とは', install: 'インストール', multiplayer: 'クライアント・サーバーの組み合わせ', integrations: '他の mod との連携',
  players: 'プレイヤー向け', deathScreen: '死亡画面のパネル', chat: 'チャットの要約と通知', tools: '履歴・統計・検索',
  settings: '設定画面',
  reference: 'リファレンス', commands: 'コマンド一覧', config: '設定ファイル', ids: 'ダメージ ID とバッジ',
  modpack: 'Modpack 制作者向け', mpOverview: '概要', packFolder: 'pack フォルダー', matchKeys: 'マッチキー',
  names: '名前・ヒント・非表示 ID', locked: '設定のロックと権限', triggers: '死亡トリガー',
  scanner: 'スキャナー・dump・/whm test', templates: 'テンプレート (1.3.0+)', editor: 'テンプレートエディター (1.3.0+)',
  deathMessages: 'カスタム死亡メッセージ (1.3.0+)', rules: '進行度ルール (1.3.0+)', conditions: '条件とクエスト mod (1.3.0+)',
  requireMod: 'requireClientMod (1.3.0+)', scripts: 'スクリプト連携（予定）', packEditor: 'pack エディター（予定）',
  more: 'その他', cookbook: 'レシピ集', faq: 'トラブルシューティング・FAQ', changelog: '更新履歴',
}

export default defineConfig({
  title: 'What Hit Me? Wiki',
  description: 'Documentation for the Minecraft mod What Hit Me?',
  cleanUrls: true,
  srcExclude: ['README.md'],
  lastUpdated: false,
  head: [['link', { rel: 'icon', type: 'image/png', href: '/favicon.png' }]],
  themeConfig: {
    logo: '/images/icon_128.png',
    search: {
      provider: 'local',
      options: {
        locales: {
          ja: {
            translations: {
              button: { buttonText: '検索', buttonAriaLabel: '検索' },
              modal: {
                noResultsText: '見つかりませんでした',
                resetButtonTitle: 'リセット',
                footer: { selectText: '選択', navigateText: '移動', closeText: '閉じる' },
              },
            },
          },
        },
      },
    },
    socialLinks: [{ icon: 'github', link: repo }],
  },
  locales: {
    root: {
      label: 'English',
      lang: 'en-US',
      themeConfig: {
        nav: [
          { text: 'Guide', link: '/guide/' },
          { text: 'Players', link: '/player/death-screen' },
          { text: 'Modpack makers', link: '/modpack/' },
          { text: 'Commands', link: '/reference/commands' },
          { text: 'Cookbook', link: '/cookbook' },
        ],
        sidebar: sidebar('', en),
        outline: { level: [2, 3], label: 'On this page' },
        footer: { message: 'What Hit Me? by junjun1z. MIT License.' },
      },
    },
    ja: {
      label: '日本語',
      lang: 'ja-JP',
      link: '/ja/',
      title: 'What Hit Me? Wiki',
      description: 'Minecraft mod「What Hit Me?」のドキュメント',
      themeConfig: {
        nav: [
          { text: 'ガイド', link: '/ja/guide/' },
          { text: 'プレイヤー', link: '/ja/player/death-screen' },
          { text: 'Modpack 制作者', link: '/ja/modpack/' },
          { text: 'コマンド', link: '/ja/reference/commands' },
          { text: 'レシピ集', link: '/ja/cookbook' },
        ],
        sidebar: sidebar('/ja', ja),
        outline: { level: [2, 3], label: 'このページの内容' },
        docFooter: { prev: '前のページ', next: '次のページ' },
        darkModeSwitchLabel: 'ダークモード',
        sidebarMenuLabel: 'メニュー',
        returnToTopLabel: 'ページの先頭へ',
        langMenuLabel: '言語',
        footer: { message: 'What Hit Me? by junjun1z. MIT License.' },
      },
    },
  },
})
