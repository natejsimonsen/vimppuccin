# Privacy

Vimppuccin is built around a single rule: **nothing leaves your browser.**

## What the extension does *not* do

- No analytics. No metrics. No usage tracking. No event reporting.
- No telemetry. No crash reporting. No "anonymous diagnostic data."
- No remote configuration. No A/B tests.
- No `fetch`, no `XMLHttpRequest`, no `WebSocket`, no `navigator.sendBeacon`. Searchable: `grep -RIn 'fetch\|XMLHttpRequest\|WebSocket\|sendBeacon' src entrypoints` returns nothing.
- No third-party scripts. The `content_security_policy` is `script-src 'self'; object-src 'self'`.
- No `chrome.storage.sync` — settings never traverse Google's infrastructure.
- No external fonts or stylesheets. UI uses the system monospace stack.
- No identifiers generated, stored, or transmitted.

## What the extension *does* do

- Reads the DOM of pages you load, **only while you're on the page**, **only to find clickable elements when you press `f` / `F`** and to keep its keyboard handler alive. The DOM data never leaves the page's tab.
- Listens for keypresses on the page so it can implement vim-style scrolling and link hints. Modifier-key combinations (Ctrl/Alt/Cmd) are never intercepted.
- Stores your chosen Catppuccin flavor in `chrome.storage.local`, which lives on your device only.

## Permissions in `manifest.json`, explained

| Permission                     | Why                                                                                |
| ------------------------------ | ---------------------------------------------------------------------------------- |
| `"storage"`                    | To remember your selected theme flavor across sessions, on-device.                 |
| `"host_permissions": ["<all_urls>"]` | Required to inject the keyboard handler/content script into the pages you visit. It is **not** used to read or transmit page contents. |

The extension does **not** request `tabs`, `activeTab`, `cookies`, `webRequest`, `webNavigation`, `history`, `bookmarks`, `downloads`, `identity`, or `clipboardRead`/`clipboardWrite`.

## Reproducible builds

Releases are built from a tagged commit with `npm run build && npm run zip`. The build is deterministic given the same `package-lock.json`. To verify a release matches the source:

```bash
git checkout v<tag>
npm ci
npm run build && npm run zip
sha256sum .output/vimppuccin-*-chrome.zip
# compare with the SHA256 attached to the GitHub release
```

## Reporting

If you find anything that contradicts this document, open an issue at <https://github.com/natejsimonsen/vimppuccin/issues>. This policy is part of the source tree; changes are visible in `git log PRIVACY.md`.
