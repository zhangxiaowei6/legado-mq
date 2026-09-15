# Moqi Reader (墨栖阅读)

> **Third-party distribution:** This project is a modified distribution based on Legado and the Luoyacheng branch. It is not an official Legado release. See [UPSTREAM_BASELINE.md](UPSTREAM_BASELINE.md) for the exact provenance.

<p align="center">
  <img src="docs/branding/moqi-reader-banner.svg" width="720" alt="Moqi Reader / 墨栖阅读">
</p>

Moqi Reader is an open-source Android reading application. It retains Legado's configurable book sources, RSS feeds, local book support, WebDAV, text-to-speech, and web service features while being maintained independently.

Moqi Reader does not provide content. Users configure their own book and subscription sources and are responsible for using them lawfully.

## Downloads and updates

- Stable builds are published only through [GitHub Releases](https://github.com/zhangxiaowei6/legado-mq/releases).
- The first independent release is `v1.0.0`; its APK is named `moqi-reader-v1.0.0.apk`.
- Existing users must manually download and install `v1.0.0` over the old application once. The release application ID and signing certificate are retained, so Android can preserve the bookshelf, sources, reading progress, and settings during an in-place upgrade.
- Beginning with `v1.0.1`, both manual and daily update checks use only this repository's GitHub Releases. Android always asks the user to approve installation; silent installation is not supported.

Back up application data before upgrading. Do not uninstall the previous app first, because Android removes application data on uninstall.

## Build

Use JDK 17 and Android SDK 36. On Windows:

```powershell
.\gradlew.bat testAppDebugUnitTest
.\gradlew.bat lintAppDebug
.\gradlew.bat assembleAppDebug
```

The web UI in `modules/web` requires Node.js 20 and pnpm 9. Release signing material is supplied locally or through GitHub Actions secrets and is never committed.

## Versioning

Releases use `MAJOR.MINOR.PATCH`. The Android version code is calculated as `1,000,000 + MAJOR × 10,000 + MINOR × 100 + PATCH`, with `MINOR` and `PATCH` limited to `0..99`. Release tags must exactly match `v${VERSION_NAME}`.

## Upstream and acknowledgements

Independent maintenance began from [`Luoyacheng/legado-E@44e07fea`](https://github.com/Luoyacheng/legado-E/commit/44e07fea541287804cc58d0168940a756cd11cfd). That branch is based on [`gedoor/legado`](https://github.com/gedoor/legado). Thanks to gedoor, Luoyacheng, and every contributor to Legado. See [NOTICE.md](NOTICE.md) and [UPSTREAM_BASELINE.md](UPSTREAM_BASELINE.md) for attribution and audit details.

## License

Moqi Reader is distributed under the [GNU General Public License v3.0](LICENSE). Upstream authors and contributors retain copyright in their respective work; independent Moqi Reader changes are released under the same license.
