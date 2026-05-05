# Vimppuccin

Vim-mode browser shortcuts in a [Catppuccin](https://github.com/catppuccin/catppuccin) palette. Free, open source, MIT-licensed. Zero telemetry, zero analytics, zero outbound network calls.

> Status: v0.1 — early. Chromium (Chrome / Edge / Brave / Arc) only. Manifest V3.

## Install (v0.1, unpacked)

1. Download the latest `vimppuccin-<version>.zip` from [Releases](https://github.com/natejsimonsen/vimppuccin/releases) and extract it, **or** clone this repo and run `npm install && npm run build` (output goes to `.output/chrome-mv3/`).
2. Visit `chrome://extensions`.
3. Enable **Developer mode** (top right).
4. Click **Load unpacked** and select the build directory.

A Chrome Web Store listing may follow once the project stabilizes; see [`PRIVACY.md`](./PRIVACY.md) before that happens.

## Keymap

| Keys      | Action                                |
| --------- | ------------------------------------- |
| `j` / `k` | scroll down / up                      |
| `h` / `l` | scroll left / right                   |
| `d` / `u` | scroll half page down / up            |
| `gg`      | scroll to top                         |
| `G`       | scroll to bottom                      |
| `f`       | show link hints (activate in current tab) |
| `F`       | show link hints (activate in new tab) |
| `?`       | toggle keymap overlay                 |
| `i`       | enter insert mode (passthrough)       |
| `Esc`     | exit hints / help / back to normal    |

Modifier keys (`Ctrl`/`Alt`/`Cmd`) are never intercepted — your browser shortcuts work as-is. When focus is on a text input, textarea, contenteditable, or `<select>`, Vimppuccin automatically yields (insert mode); press `Esc` to return to normal.

### Link hints

The headline feature. Press `f` and a small label appears next to every clickable element. Type the label to activate.

The labels follow a hybrid scheme:

- A label is the first two alphabetic characters of the link's visible text (`About` → `ab`, `Contact us` → `co`, `1. Item` → `it`).
- If two links would collide on the same prefix, the second one (and any further duplicates) gets a random unused label drawn from the home row: `asdfghjkl`.
- Labels with no extractable text fall back to random home-row labels.

This keeps the most-frequent links memorable across sessions while guaranteeing every label is unique on every page.

`F` is the same flow but opens anchor links in a new tab via `window.open`.

## Theme

All four Catppuccin flavors are bundled. Open the extension's options page from `chrome://extensions` to switch.

- Latte (light)
- Frappé
- Macchiato
- **Mocha** (default)

Setting is stored in `chrome.storage.local` only — never `chrome.storage.sync`.

## Privacy

See [`PRIVACY.md`](./PRIVACY.md) for the full statement. Short version:

- The extension declares only `storage` and `host_permissions: ["<all_urls>"]`. The latter is required to inject the keyboard handler on web pages; it is **not** used for any kind of data collection.
- No `fetch`, no `XMLHttpRequest`, no analytics SDK, no Sentry, no anything. There are zero outbound network calls in the codebase. You can verify this with `grep -RIn 'fetch\|XMLHttpRequest' src entrypoints` — empty.
- `chrome.storage.local` only. Settings never leave your device.
- Reproducible build: clone, `npm install`, `npm run build`. The published zip is built from the tagged commit.

## Build from source

Requires Node 18+.

```bash
git clone https://github.com/natejsimonsen/vimppuccin
cd vimppuccin
npm install        # also runs `wxt prepare` to generate types
npm run build      # → .output/chrome-mv3/
npm run zip        # → .output/vimppuccin-<version>-chrome.zip
npm test           # vitest unit tests for the hint algorithm
```

## Project layout

```
entrypoints/
  content.ts                  # injected into pages; hooks keydown
  background.ts               # MV3 service worker (currently a no-op)
  options/                    # settings page (theme picker, keymap, privacy)
src/
  modes/ModeManager.ts        # normal | insert | hint | help dispatch
  hints/
    collect.ts                # find visible interactive elements
    label.ts                  # the prefix-or-random labeling algorithm
    render.ts                 # shadow-DOM overlay
    activate.ts               # click / open-in-new-tab
  scroll.ts                   # j/k/h/l/gg/G/d/u
  help.ts                     # keymap data
  storage.ts                  # typed wrapper over chrome.storage.local
  theme/
    flavors/                  # latte, frappe, macchiato, mocha (tokens)
    apply.ts                  # → CSS custom properties
```

## Acknowledgements

- [Catppuccin](https://github.com/catppuccin/catppuccin) — pastel palette (MIT).
- The vim-on-the-web pattern owes everything to [Vimium](https://github.com/philc/vimium), [Tridactyl](https://github.com/tridactyl/tridactyl), and [SurfingKeys](https://github.com/brookhong/Surfingkeys). This project is an independent implementation, not a fork.

## License

MIT — see [LICENSE](./LICENSE).
