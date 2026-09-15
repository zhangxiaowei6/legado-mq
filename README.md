# 墨栖阅读（Moqi Reader）

> **第三方修改版声明：** 本项目是基于 Legado/阅读及 Luoyacheng 分支的第三方修改版，并非官方发行版。独立维护基线见 [UPSTREAM_BASELINE.md](UPSTREAM_BASELINE.md)。

<p align="center">
  <img src="docs/branding/moqi-reader-banner.svg" width="720" alt="墨栖阅读 / Moqi Reader">
</p>

墨栖阅读是一款开源 Android 阅读器。它保留 Legado 的可配置书源、RSS、本地书籍、WebDAV、朗读和 Web 服务等能力，并在独立维护中继续改进导出与阅读体验。

墨栖阅读本身不提供内容。书源、订阅源和可访问内容均由用户自行配置并承担相应责任。

## 下载与更新

- 正式版只在 [GitHub Releases](https://github.com/zhangxiaowei6/legado-mq/releases) 发布。
- 首个独立版本为 `v1.0.0`，安装包固定命名为 `moqi-reader-v1.0.0.apk`。
- 从旧版迁移的用户需要先手动下载 `v1.0.0`，并由 Android 系统执行覆盖安装。应用 ID 和签名证书保持不变，正常情况下书架、书源、阅读进度与设置会保留。
- 从 `v1.0.1` 起，应用内“检查更新”和每日自动检查只查询本仓库的 GitHub Releases。下载完成后仍需用户在系统安装界面确认，不支持静默安装。

覆盖安装前建议先使用应用内备份功能保存数据。请勿卸载旧版后再安装，否则 Android 会清除旧应用数据。

## 构建

环境要求：JDK 17、Android SDK 36。Windows 下可执行：

```powershell
.\gradlew.bat testAppDebugUnitTest
.\gradlew.bat lintAppDebug
.\gradlew.bat assembleAppDebug
```

Web UI 位于 `modules/web`，要求 Node.js 20 及 pnpm 9：

```bash
pnpm install
pnpm type-check
pnpm build
```

正式签名材料不进入仓库。发布构建通过 Gradle 属性或 `local.properties` 提供 `RELEASE_STORE_FILE`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS` 和 `RELEASE_KEY_PASSWORD`。

## 版本制度

版本使用 `MAJOR.MINOR.PATCH`。`versionCode` 公式为：

```text
1,000,000 + MAJOR × 10,000 + MINOR × 100 + PATCH
```

`MINOR` 和 `PATCH` 的取值范围均为 `0..99`。正式发布只接受与 `VERSION_NAME` 完全对应的 `vX.Y.Z` 标签。

## 上游与致谢

墨栖阅读从 [`Luoyacheng/legado-E@44e07fea`](https://github.com/Luoyacheng/legado-E/commit/44e07fea541287804cc58d0168940a756cd11cfd) 开始独立维护；该分支基于 [`gedoor/legado`](https://github.com/gedoor/legado)。感谢 gedoor、Luoyacheng 以及 Legado 历代贡献者。上游技术帮助与规则文档可参考 [Legado 官方仓库](https://github.com/gedoor/legado)及其 Wiki。

基线、清理内容及后续人工同步记录详见 [UPSTREAM_BASELINE.md](UPSTREAM_BASELINE.md)，版权与第三方发行说明详见 [NOTICE.md](NOTICE.md)。

## 许可证

项目继续按 [GNU General Public License v3.0](LICENSE) 发布。上游作者和历代贡献者对其各自贡献保留版权；墨栖阅读新增和修改部分同样按 GPL-3.0 发布。

English documentation: [English.md](English.md)
