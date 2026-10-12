# Privacy

Vimppuccin is built around one rule: **your browsing data stays on your device.**

## What the app does *not* do

- No analytics, metrics, usage tracking or crash reporting.
- No accounts and no identifiers, generated or collected.
- No remote VPN server. The VPN runs entirely on your phone.
- No logging of your DNS queries to disk. The "Recently blocked" list lives only in memory and is gone when the app stops.
- No third-party SDKs or libraries beyond AndroidX and Kotlin coroutines.

## Network connections the app makes

| Connection | When | Why |
| --- | --- | --- |
| Your network's DNS server (e.g. your router or carrier) | For every DNS lookup that is **not** blocked | Forwards the lookup exactly as Android would without the blocker. |
| `1.1.1.1` / `9.9.9.9` | Only if the current network reports no DNS servers | Fallback resolver so lookups still work. |
| The URLs under **Blocklist sources** (default: `raw.githubusercontent.com/StevenBlack/hosts`) | Only when you tap **Download / update blocklists** | Fetches the blocklist. Nothing about you is sent beyond a normal HTTPS request. |

## VPN permission

Android requires VPN permission for any app that intercepts DNS without root. Vimppuccin routes only one internal address (`10.111.222.2`, the fake DNS server) through the VPN. All other traffic bypasses it and is never read.

## Permissions

| Permission | Why |
| --- | --- |
| `INTERNET` | Forward allowed DNS queries and download blocklists. |
| `ACCESS_NETWORK_STATE` | Find the current network's DNS servers to use as the upstream resolver. |
| `BIND_VPN_SERVICE` (held by the system, not the app) | Lets Android start the local VPN service. |
| `BIND_QUICK_SETTINGS_TILE` (held by the system) | Lets Android show the Quick Settings tile. |

## Storage

Your allowlist, custom blocklist, blocklist sources and the downloaded list are stored in the app's private storage. Backups are disabled (`allowBackup="false"`).

## Reporting

If anything contradicts this document, open an issue at <https://github.com/natejsimonsen/vimppuccin/issues>.
