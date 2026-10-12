# Vimppuccin

A free, open-source, no-root **ad blocker for Android**, themed in [Catppuccin](https://github.com/catppuccin/catppuccin). No telemetry, no analytics, no accounts. MIT-licensed.

> Status: v0.2, early. Android 7.0+ (API 24).

## How it works

Vimppuccin runs a **local VPN that only handles DNS**. Nothing is sent to a remote VPN server.

1. Android is told to use a fake DNS server (`10.111.222.2`) that only exists inside the VPN. Only that one address is routed through the tunnel. All your other traffic uses the normal network and is never touched.
2. Each DNS lookup is checked against the blocklist on your device.
   - **Blocked domain:** the app answers `NXDOMAIN` right away, so the ad or tracker never loads.
   - **Anything else:** the query goes to your network's normal DNS server and the answer is passed back.
3. Matching includes subdomains: blocking `doubleclick.net` also blocks `stats.g.doubleclick.net`.

This blocks ads and trackers in **every app and browser**, not just one browser.

## Install

1. Get `vimppuccin-debug-apk` from the latest [Actions run](https://github.com/natejsimonsen/vimppuccin/actions/workflows/android.yml), **or** build it yourself (see below).
2. Install the APK on your phone (you may need to allow installs from unknown sources).
3. Open Vimppuccin, tap **Start blocking** and accept the VPN prompt.
4. Tap **Download / update blocklists** to fetch the full list. Until then a small built-in list is used.

### Recommended settings

- **Private DNS:** set *Settings › Network & internet › Private DNS* to **Off** or **Automatic**. In "Private DNS provider hostname" mode, Android sends DNS straight to that provider and skips the blocker.
- **Browser secure DNS (DoH):** turn off "Use secure DNS" in Chrome, Firefox and similar browsers for the same reason.
- **Start at boot:** in *Settings › Network & internet › VPN › Vimppuccin ⚙*, turn on **Always-on VPN**.
- **Quick Settings tile:** add the "Ad blocking" tile to switch blocking on or off from the notification shade.

## Features

- System-wide DNS-based blocking without root
- Default lists: [HaGeZi Multi PRO mini](https://github.com/hagezi/dns-blocklists) (covers in-app ad SDKs and mediation networks) and [StevenBlack unified hosts](https://github.com/StevenBlack/hosts) (ads + malware). Add any HTTPS hosts file or domain list under **Blocklist sources**.
- Reads hosts files (`0.0.0.0 domain`), plain domain lists and `||domain^` Adblock rules
- Your own **allowlist** (never block) and **custom blocklist**
- Live counters and a log of recent lookups (blocked and allowed)
- Quick Settings tile and Always-on VPN support
- Catppuccin Latte (light) and Mocha (dark), following the system theme

## Limitations

- Ads served from the **same domain as the content** can't be blocked by DNS. YouTube video ads are the usual example.
- Only one VPN can be active at a time, so Vimppuccin can't run alongside another VPN app.
- Apps that use their own DNS-over-HTTPS or hardcoded resolvers bypass the blocker.
- **In-app ads can survive for a while after you turn blocking on.** Ad SDKs download ads ahead of time and keep connections open. Force-stop the app and clear its cache (*Settings › Apps › the app › Storage › Clear cache*) after starting Vimppuccin.
- Games usually run several ad networks through mediation. If one network is blocked, the next one fills the slot, so check **Recent lookups** for allowed (✓) domains that show up with the ads and add them to your custom blocklist.

## Build

Requires JDK 17+ and the Android SDK (API 35).

```bash
./gradlew :core:test          # unit tests for packet parsing, DNS and blocklist matching
./gradlew :app:assembleDebug  # APK at app/build/outputs/apk/debug/
```

## Project layout

| Path   | What                                                                                          |
| ------ | --------------------------------------------------------------------------------------------- |
| `core` | Pure Kotlin/JVM: IPv4/UDP parsing, DNS question parsing, NXDOMAIN replies, hosts-file parsing, blocklist matching. Fully unit-tested. |
| `app`  | Android app: `AdBlockVpnService` (VPN setup), `DnsTunnel` (TUN read loop and upstream forwarding), `BlocklistRepository` (bundled, downloaded and custom lists), `MainActivity`, `ToggleTileService`. |

## Privacy

See [`PRIVACY.md`](./PRIVACY.md).

## License

MIT. See [`LICENSE`](./LICENSE).
