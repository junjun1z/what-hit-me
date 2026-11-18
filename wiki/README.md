# What Hit Me? Wiki

Documentation site for What Hit Me?, built with [VitePress](https://vitepress.dev/). English is the default language (site root), Japanese is under `/ja/` with the same page structure.

## Local preview

Requires Node.js 18 or newer.

```
cd wiki
npm install
npm run docs:dev       # live preview at http://localhost:5173
npm run docs:build     # static site in wiki/.vitepress/dist
npm run docs:preview   # serve the built site at http://localhost:4173
```

## Layout

```
wiki/
  .vitepress/config.mts   site config: languages, navigation, sidebars, local search
  public/                 static files (images/, favicon.png), served from the site root
  index.md                English home page
  guide/ player/ reference/ modpack/   English pages
  cookbook.md troubleshooting.md changelog.md
  ja/                     Japanese pages, same file names as the English ones
```

When you add a page, add it in both languages and to both sidebars (`sidebar()` in `.vitepress/config.mts` builds them from one list).

Markers used in the pages:

- `<Badge type="tip" text="1.3.0+" />`: feature new in 1.3.0
- `<!-- TODO media: ... -->`: an HTML comment (not shown on the page) where an in-game screenshot or GIF will be added after QA. `grep -rn "TODO media" --include=*.md --exclude-dir=node_modules .` lists them.

## Deploying on Vercel

Create a Vercel project from this repository with these settings:

| Setting | Value |
|---|---|
| Framework preset | VitePress (or "Other") |
| Root directory | `wiki` |
| Install command | `npm install` |
| Build command | `npm run docs:build` |
| Output directory | `.vitepress/dist` |
| Node.js version | 18 or newer |

The site uses `cleanUrls`, so pages are served without `.html`. Vercel serves the generated `page.html` files for `/page` URLs automatically. No server-side code is needed; the search index is built into the static files.
