# AGENTS.md

本文档为编码代理提供本仓库的项目级上下文，帮助后续修改更安全、更贴合现有工程。

## 项目概览

本仓库是“墨栖阅读 / Moqi Reader”（仓库名 `legado-mq`），一个从 `Luoyacheng/legado-E@44e07fea541287804cc58d0168940a756cd11cfd` 开始独立维护的 Legado 第三方修改版，并非官方发行版。Android 桌面显示名称固定为“阅读”，Debug 显示为“阅读·D”；文档、关于页和项目品牌仍使用“墨栖阅读 / Moqi Reader”。核心应用是原生 Android 工程，主要使用 Kotlin 编写；同时内置了一个 Vue/Vite Web UI，作为应用资产打包，用于 Web 端书架和源编辑。来源与同步规则见 `UPSTREAM_BASELINE.md`。

这个产品的核心是“可配置内容源”：

- 书源定义搜索、发现、书籍信息、目录、正文、登录、Cookie、请求头和 JavaScript 规则。
- RSS 源定义文章列表、条目内容解析和 WebView 行为。
- 规则由应用内规则引擎、Rhino JavaScript 运行时、Jsoup/XPath/JsonPath 解析、OkHttp/Cronet 网络层和 Room 持久化共同执行。

由于源规则可以执行 JavaScript 并发起网络请求，规则引擎、HTTP 层、源导入导出和 Web 服务 API 的改动影响范围都很大。

## 仓库结构

- `app/` - 主 Android 应用模块。
- `app/src/main/java/io/legado/app/App.kt` - 应用初始化、全局服务、Rhino 包装、缓存清理和启动任务。
- `app/src/main/java/io/legado/app/data/` - Room 数据库、实体、DAO、迁移和导出的 schema。
- `app/src/main/java/io/legado/app/model/` - 核心业务逻辑。
- `app/src/main/java/io/legado/app/model/analyzeRule/` - 源规则解析引擎。
- `app/src/main/java/io/legado/app/model/webBook/` - 在线书籍搜索、详情、目录和正文获取。
- `app/src/main/java/io/legado/app/model/localBook/` - 本地 TXT/EPUB/PDF/MOBI/UMD 解析。
- `app/src/main/java/io/legado/app/ui/` - Android Activity、Fragment、Dialog、控件和阅读界面。
- `app/src/main/java/io/legado/app/web/` - 嵌入式 Web UI 使用的 NanoHTTPD HTTP/WebSocket 服务。
- `app/src/main/java/io/legado/app/api/` - Web API 和 ContentProvider API 控制器。
- `app/src/main/assets/web/vue/` - 随 Android 应用发布的已构建 Web UI 资产。
- `modules/book/` - 处理本地书籍格式和相关读取能力的 Android library。
- `modules/rhino/` - 封装 Mozilla Rhino 脚本执行能力的 Android library。
- `modules/web/` - Web 书架/源编辑器的 Vue 3 + Vite 源码。
- `gradle/libs.versions.toml` - 统一管理依赖和插件版本。
- `api.md` - 对外 Web API 和 ContentProvider API 文档。

## 构建系统

Android 工程使用 Gradle 和 version catalog：

- Android Gradle Plugin：在 `gradle/libs.versions.toml` 中配置。
- Kotlin：在 `gradle/libs.versions.toml` 中配置。
- Java toolchain：17。
- Android SDK：`compileSdk 36`、`targetSdk 36`、`minSdk 21`。
- 已包含 Android 模块：`:app`、`:modules:book`、`:modules:rhino`。

使用 Gradle wrapper。Windows 下：

```powershell
.\gradlew.bat assembleAppDebug
.\gradlew.bat testAppDebugUnitTest
.\gradlew.bat lintAppDebug
```

类 Unix shell 下：

```bash
./gradlew assembleAppDebug
./gradlew testAppDebugUnitTest
./gradlew lintAppDebug
```

Instrumented tests 需要 Android 设备或模拟器：

```powershell
.\gradlew.bat connectedAppDebugAndroidTest
```

## Web 模块

Web 源码位于 `modules/web`。

`modules/web/package.json` 中声明的环境要求：

- Node.js `>=20`
- pnpm `>=9`

常用命令：

```bash
pnpm install
pnpm dev
pnpm type-check
pnpm build
```

注意：`modules/web/scripts/sync.js` 只有在存在 `GITHUB_ENV` 时，才会把构建后的 Vite 输出复制到 `app/src/main/assets/web/vue`。本地 Web 构建不一定会自动更新 Android 资产。如果任务修改了 Web UI，并且 Android 应用需要发布这些改动，请有意识地同步构建产物。

除非任务明确要求修改已发布资产，并且无法从 `modules/web` 重新构建，否则不要手工编辑 `app/src/main/assets/web/vue/` 下的构建产物。

## 核心架构

### 应用启动

`App.kt` 会初始化崩溃日志、昼夜主题行为、LiveEventBus、Cronet 预下载、GMS TLS provider 安装、Rhino 绑定、缓存清理、简繁转换预加载、源排序修复，以及可选的 WebDAV 阅读进度同步。

给启动流程增加工作时要谨慎。除非功能必须立刻可用，否则优先使用懒加载或后台任务。

### 数据库

`AppDatabase.kt` 定义 Room 数据库。当前 schema 版本较高，导出的 schema 位于 `app/schemas/io.legado.app.data.AppDatabase/`。

准则：

- 修改实体时，新增或更新导出的 Room schema 文件。
- 涉及 schema 变化且需要保留用户数据时，添加迁移。
- 不要依赖 `fallbackToDestructiveMigrationFrom` 处理新的 schema 变化。
- 注意数据库 builder 当前使用了 `allowMainThreadQueries()`，旧调用点可能默认同步访问数据库。新代码仍应在合理情况下优先使用 suspend/IO 路径。
- 默认书籍分组会在数据库 callback 中插入。修改分组 ID 或名称可能影响整个应用的排序和过滤。

### 源规则

`BookSource.kt` 和 `RssSource.kt` 是用户可配置的源定义。`AnalyzeRule.kt`、`AnalyzeUrl.kt` 以及 `AnalyzeBy*` 系列类实现了解析和脚本执行。

规则相关改动风险较高，因为它们会影响：

- 搜索结果。
- 发现页面。
- 书籍元数据。
- TOC/目录解析。
- 章节正文提取。
- 登录检查。
- Cookie/请求头处理。
- 基于 WebView 的 JavaScript 规则。
- 源调试输出。

修改这一区域时，如果条件允许，至少用一个普通文本源、一个使用 JavaScript 的源，以及一个失败/空正文场景进行测试。

### 阅读流程

在线书籍的高层流程：

1. `SearchModel` 选择已启用的源，并并发执行搜索。
2. `WebBook.searchBookAwait` 获取并解析搜索页或发现页。
3. `WebBook.getBookInfoAwait` 填充书籍详情。
4. `WebBook.getChapterListAwait` 构建章节记录。
5. `BookContent.analyzeContent` 获取、解析、格式化、分页并保存章节正文。
6. `ReadBook` 协调当前/上一章/下一章加载、阅读进度、预下载、替换规则和同步。

本地书籍流程位于 `model/localBook`，按格式分别处理 TXT、EPUB、PDF、MOBI 和 UMD。

### Web Service API

`WebService` 启动本地 HTTP 和 WebSocket 服务器。`HttpServer` 暴露的端点包括：

- `GET /getBookshelf`
- `GET /getChapterList`
- `GET /getBookContent`
- `GET /cover`
- `GET /image`
- `POST /saveBookSource`
- `POST /saveBookSources`
- `POST /deleteBookSources`
- `POST /saveBook`
- `POST /saveBookProgress`
- `POST /saveRssSource`
- `POST /saveRssSources`
- `POST /deleteRssSources`
- `POST /saveReplaceRule`

`modules/web/src/api/api.ts` 中的 Vue Web UI 依赖这些路由。修改此 API 时，请同步更新 `api.md`、Android 控制器和 Web 客户端类型。

### ContentProvider API

`ReaderProvider.kt` 通过 ContentProvider 暴露应用数据。应把它视为对外 API。

扩展它之前需要检查的已知问题：RSS URI matcher 中有多处当前看起来映射到了书源 request code。依赖 RSS ContentProvider 路由之前，请先验证并修复这些映射。

## 测试建议

围绕你修改的区域做聚焦测试：

- 规则引擎或 Rhino 行为：参考 `app/src/test/java/io/legado/app/JsTest.kt` 编写单元测试。
- Room schema 变化：做迁移测试和 schema 校验。当前存在 `MigrationTest.kt`，但迁移列表为空，触碰迁移时应补强。
- HTTP/Web API 变化：同时验证 `HttpServer` 控制器行为和 `modules/web/src/api/api.ts` 的预期。
- 阅读/正文变化：视情况测试已缓存正文、未缓存正文、下一页正文、空正文、本地书籍正文和替换规则。
- Web UI 变化：在 `modules/web` 下运行 `pnpm type-check` 和 `pnpm build`。

如果依赖尚未在本地可用，Gradle 或 pnpm 命令可能需要网络访问。

## 编码约定

- 遵循现有 Kotlin 风格和包组织方式。
- 优先使用 `help/`、`utils/`、`model/` 和 DAO 层已有 helper，不要新增平行抽象。
- Android UI 改动保持与现有 XML/view-binding 模式一致。
- 协程工作放在合适的 dispatcher 上。即使部分旧数据库访问是同步的，新代码也应避免阻塞主线程。
- 保护用户数据。数据库迁移、缓存删除、源删除和阅读进度变更都要保守处理。
- 修复局部问题时，不要引入大范围重构。
- 除非任务需要，不要重排 version catalog 依赖或生成的 schema 文件。
- 避免编辑 `modules/web/src/auto-imports.d.ts`、`modules/web/src/components.d.ts` 等生成文件，或已构建的 Vite 资产，除非任务要求或重新生成。

## 安全与隐私注意事项

这个应用具备较广的 Android 能力：网络访问、文件访问、前台服务、WebView 使用、JavaScript 执行、ContentProvider 导出、本地 Web 服务，以及通过自定义 URL scheme 导入。

修改这些区域时：

- 校验来自 HTTP、WebSocket、ContentProvider、intent、文件关联和 `legado://` 导入的输入。
- 避免通过日志或 API 响应泄漏本地文件路径、Cookie、请求头、源变量或阅读进度。
- 谨慎处理 JavaScript bridge 和暴露给 Rhino 的对象。
- 保持 `HttpServer` 中 CORS 行为的意图明确。
- 把源 JSON 当作不可信的用户输入处理。

## 依赖说明

部分依赖被有意固定版本，因为新版本曾引入行为变化或平台问题。升级前请先阅读 `gradle/libs.versions.toml` 中的注释：

- `jsoup`
- `commons-text`
- `rhino`
- `media3`
- `gsyvideoplayer`
- `hutool`
- `protobuf-javalite`

Cronet 二进制文件和版本由 `gradle.properties`、`app/download.gradle` 和 GitHub Actions 管理。除非任务明确与 Cronet 有关，否则不要手工更新 Cronet jar。

## GitHub 仓库与分支治理

权威公开仓库是 `https://github.com/zhangxiaowei6/legado-mq`，默认分支为 `main`。仓库不是 GitHub Fork，但必须继续保留 `NOTICE.md`、`UPSTREAM_BASELINE.md`、GPL-3.0 许可证和上游版权说明。

`main` 已启用分支保护，规则如下：

- 所有更新必须通过 Pull Request，禁止直接推送。
- 必需状态检查的名称固定为 `android` 和 `web`，合并前分支必须与最新 `main` 同步。
- 所需批准数为 `0`，单人维护者可以在 CI 成功后自行合并。
- 未解决的 review conversation 必须全部处理。
- 规则同样适用于管理员。
- 禁止 force push，禁止删除 `main`。
- 不要求签名提交或线性历史，也不限制普通功能分支推送。
- `release` 只在版本标签上运行，不得设成普通 PR 的必需检查；Dependabot 也不是固定必需检查。

建议使用以下分支名称：

- `feat/<topic>` - 新功能。
- `fix/<topic>` - 缺陷修复。
- `docs/<topic>` - 仅文档改动。
- `refactor/<topic>` - 不改变外部行为的重构。
- `test/<topic>` - 测试改动。
- `build/<topic>` 或 `ci/<topic>` - 构建和工作流改动。
- `release/vX.Y.Z` - 正式版本准备。
- `sync/upstream-YYYYMMDD` - 人工同步上游。

除非用户明确要求，否则编码代理不得擅自修改 GitHub 仓库设置、Branch Protection、Actions Secrets、Environment、Topics、Release、标签或远程仓库，也不得删除旧仓库。任何 push、合并 PR、创建或删除标签、发布 Release 等外部写操作都要先确认目标仓库和分支。

## 提交规范

提交信息遵循 Conventional Commits，格式为：

```text
type(scope): 简洁的祈使句说明
```

允许的主要 `type`：

- `feat` - 用户可见的新功能。
- `fix` - 缺陷修复。
- `docs` - 文档和说明更新。
- `refactor` - 不改变功能的内部整理。
- `perf` - 性能优化。
- `test` - 测试新增或调整。
- `build` - Gradle、依赖、打包配置。
- `ci` - GitHub Actions 和自动化。
- `chore` - 其他维护工作。
- `revert` - 撤销已有提交。

推荐的 `scope` 包括 `reader`、`source`、`rss`、`web`、`api`、`db`、`update`、`release`、`signing`、`docs` 和 `upstream`。例如：

```text
fix(update): 拒绝 MIME 类型错误的发行包
feat(reader): 支持新的章节排版选项
docs(release): 补充签名轮换步骤
ci(release): 校验发布标签与版本号一致
```

提交还应遵守以下约束：

- 每个提交只处理一个逻辑主题，并包含对应测试或必要文档。
- 不使用“update”“fix bug”等无法审计的模糊标题。
- 不提交 keystore、口令、Token、`local.properties`、私钥导出、构建产物或包含用户数据的调试文件。
- 不为整理提交而重写已经推送或发布的公共历史；不得移动已经发布的版本标签。
- 人工移植上游改动时，在提交正文或 PR 中记录来源仓库、完整上游 SHA 和必要的 GPL-3.0 归属信息。
- Web 构建产物只有在 `modules/web` 源码确实变化且 Android 发布需要携带该变化时才同步提交；不要脱离源码单独修改压缩后的资产。
- 不伪造上游作者身份。移植代码可使用 `Co-authored-by` 或来源说明，但当前提交作者必须是真实执行者。

## Pull Request 规范

所有进入 `main` 的改动都通过 PR。PR 标题应符合提交规范，正文至少说明：

- 改了什么以及为什么改。
- 影响的 Android、Web、数据库、规则引擎或外部 API 范围。
- 实际执行过的测试命令和结果；未执行的检查必须说明原因。
- 是否涉及数据库迁移、用户数据、权限、网络、安全、签名、版本或自动更新。
- 如果来自上游，列出仓库和 SHA 范围，并说明人工冲突处理。
- 如果有界面变化，提供必要截图；如果有兼容性风险，提供回退方式。

合并前必须满足：

- `android` 和 `web` 两个必需检查成功。
- PR 分支已同步最新 `main`。
- Review conversation 已全部解决。
- 没有意外生成物、凭据或无关格式化改动。
- 版本、更新日志、文档和代码行为保持一致。

修改 `.github/workflows/ci.yml` 时，不得随意重命名 `android` 或 `web` job；它们是分支保护依赖的准确检查名称。如果确实要改名，必须先规划 Branch Protection 迁移，避免所有 PR 永久无法合并。

## CI 规范

`.github/workflows/ci.yml` 在 Pull Request 以及非版本标签的 `main` push 上运行，权限保持为只读 `contents: read`。

`android` job 当前执行：

```bash
./gradlew testAppDebugUnitTest --stacktrace
./gradlew lintAppDebug --stacktrace
./gradlew assembleAppDebug --stacktrace
```

`web` job 使用 Node.js 20 和 pnpm 9，在 `modules/web` 下执行：

```bash
pnpm install --frozen-lockfile
pnpm type-check
pnpm build
```

CI 规范：

- 普通 CI 不得读取发行密钥或拥有 `contents: write` 权限。
- 不使用 `continue-on-error` 掩盖测试、lint、构建或签名失败。
- Action 尽量使用官方稳定大版本；升级第三方 Action 前核对发布者和供应链风险。
- 新增缓存时不得缓存明文凭据、keystore 或本地用户数据。
- 改动工作流时至少进行 YAML/表达式检查，并在 PR 中说明权限变化。
- Cronet 更新、Web 资产同步或其他会写回仓库的自动化不得擅自恢复；需要时应单独设计最小权限和 PR 流程。

## 版本规范

Android 正式版本的唯一权威来源是根目录 `gradle.properties`：

```properties
VERSION_NAME=1.0.1
VERSION_CODE=1010001
```

`VERSION_NAME` 使用无前缀的 `MAJOR.MINOR.PATCH`：

- `PATCH`：向后兼容的缺陷修复，例如 `1.0.0 -> 1.0.1`。
- `MINOR`：向后兼容的新功能，例如 `1.0.1 -> 1.1.0`。
- `MAJOR`：明确的不兼容变更，例如 `1.9.0 -> 2.0.0`。

`VERSION_CODE` 固定按以下公式生成：

```text
1_000_000 + MAJOR * 10_000 + MINOR * 100 + PATCH
```

`MINOR` 和 `PATCH` 必须在 `0..99`。版本号不得使用日期、构建时间或 Git 提交数量，不得降低或复用已经发布的 `VERSION_CODE`。正式标签必须严格等于 `v${VERSION_NAME}`。

准备正式版本时：

- 同时更新 `VERSION_NAME` 和 `VERSION_CODE`，让 Gradle 内置校验通过。
- 根 `package.json` 和 `modules/web/package.json` 中表示项目发布版本的字段应与正式版本保持一致；不要仅为改版本而重写无关 lockfile。
- 在 `app/src/main/assets/updateLog.md` 顶部添加对应版本日志。
- 确认 APK 名称为 `moqi-reader-vX.Y.Z.apk`。
- 保持 Release application ID `io.legado.app.release`、namespace `io.legado.app`、Debug application ID `io.legado.app.debug` 和 `legado://` 协议不变，除非已经制定完整迁移方案。
- 数据库、备份格式或外部 API 有不兼容变化时，应提升 MAJOR，并在日志和 PR 中写明迁移影响。

## 更新日志规范

用户可见更新日志位于 `app/src/main/assets/updateLog.md`，采用倒序排列，最新版本放在最前面。每个正式版本的一级标题固定为：

```markdown
# 墨栖阅读 vX.Y.Z
```

发布工作流会截取第一个版本标题到下一个版本标题之间的内容作为 GitHub Release Notes，因此：

- 发布前必须保证第一节版本与标签完全一致。
- 保留旧版本记录，不覆盖或把上游日志伪装成本项目日志。
- 重点描述用户能感知的新增、修复、兼容性、安全和迁移事项，不堆砌内部实现细节。
- 破坏性变化、数据库迁移、权限变化、自动更新变化和已知问题必须明确写出。
- 上游移植内容应注明来源或链接，不宣称为墨栖原创。
- Release Notes 中不得出现内部路径、签名口令、Token、Cookie、用户书源或其他敏感数据。

运行时日志同样属于安全边界。新增日志时不得记录书源 Cookie、请求头、认证信息、完整本地文件路径、WebDAV 凭据、用户阅读内容或签名信息；发行版崩溃日志需要能被用户主动导出，不应自动上传到未声明的第三方服务。

## 发布规范

稳定版只通过 GitHub Releases 发布。发布工作流是 `.github/workflows/release.yml`，只接受 `vX.Y.Z` 标签，并且仅在仓库 `zhangxiaowei6/legado-mq` 中执行。工作流权限保持最小化，只授予 `contents: write`。

标准发布流程：

1. 从最新 `main` 创建 `release/vX.Y.Z`。
2. 更新版本、更新日志和必要文档，完成聚焦测试。
3. 创建 PR，等待 `android`、`web` 成功并合并。
4. 再次确认 `main` 上的版本、日志、签名 Secret 和待发布提交。
5. 在 `main` 的目标提交上创建 annotated tag `vX.Y.Z` 并推送。编码代理只有在用户明确授权发布时才能执行这一步。
6. 等待 `release` job 完成，不得忽略任何构建或签名错误。
7. 下载 GitHub Release 中的 APK 和 `SHA256SUMS.txt`，重新核对哈希、签名、包名和安装升级行为。
8. 验证应用内手动检查更新和每日自动检查可以识别该 Release。

Release 工作流必须继续完成以下动作：

- 校验标签等于 `v${VERSION_NAME}`。
- 校验 `VERSION_CODE` 符合公式且大于已有语义版本标签对应的值。
- 从 Runner 临时目录读取解码后的 keystore。
- 使用 JDK 17 执行 `assembleAppRelease`。
- 使用 `apksigner verify --verbose --print-certs` 验证 APK 和固定证书指纹。
- 生成 `SHA256SUMS.txt`。
- 上传 `moqi-reader-vX.Y.Z.apk`、校验文件和存在时的 R8 `mapping-vX.Y.Z.txt`。
- 创建标题为 `Moqi Reader vX.Y.Z` 的非草稿、非预发布 GitHub Release。

发布失败时不得手工上传未验证或不同签名的 APK，不得移动、覆盖或删除已经公开使用的标签。修正代码后发布新的补丁版本。Release 发布完成后再修改 Release Notes 时，不得替换 APK、校验文件或 mapping；如产物错误，应撤回有问题的 Release 并使用新版本号重新发布，同时给出明确说明。

## 签名规范

墨栖阅读必须继续使用现有私人发行证书，以保证旧版本能够覆盖升级并保留用户数据。证书 SHA-256 指纹固定为：

```text
FE:5E:48:99:2D:1B:0E:4F:8E:B5:56:A1:80:C7:80:1F:55:B3:1B:D5:EC:73:5E:1A:58:BC:48:49:5F:29:5F:B9
```

GitHub Actions 使用以下 Repository Secrets：

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

本地 release 构建通过 Gradle 属性或未跟踪的 `local.properties` 提供：

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

签名安全规则：

- 不生成新私钥替换现有证书，除非用户明确接受无法覆盖升级的后果并制定迁移方案。
- keystore 至少保留两份加密离线备份，并单独保存别名、恢复说明和证书指纹；不要把备份放在仓库或普通构建目录。
- 轮换 store/key 口令不会改变证书，但必须先验证离线备份，再同步更新全部 GitHub Secrets，并用测试构建确认指纹不变。
- 任何 `*.jks`、`*.keystore`、`*.p12`、`*.pfx`、`*.pem`、`*.key`、签名配置和 `local.properties` 都不得进入 Git 历史。
- 工作流不得输出 Base64 keystore、密码或包含密码的 Gradle 命令行；临时 keystore 只写到 `${RUNNER_TEMP}`。
- 每个正式 APK 都必须用 `apksigner` 验证，实际 SHA-256 指纹必须与固定指纹完全一致。
- 签名改动后必须执行旧版到新版的 `adb install -r` 覆盖安装测试，确认包仍为 `io.legado.app.release`，书架、书源、进度和设置完整保留。
- 怀疑私钥泄漏时停止发布，不要仅轮换密码掩盖问题；记录影响范围并由维护者决定证书迁移和用户公告。

## 自动更新规范

自动更新只使用以下 GitHub 端点：

- API：`https://api.github.com/repos/zhangxiaowei6/legado-mq/releases/latest`
- 下载页：`https://github.com/zhangxiaowei6/legado-mq/releases`

`AppUpdateGitHub` 和 `GithubRelease.toUpdateInfo` 必须继续执行以下校验：

- Release 不是 draft，也不是 prerelease。
- `tag_name` 严格符合 `vX.Y.Z`，且远端版本高于当前版本。
- `MINOR` 和 `PATCH` 在 `0..99`。
- 只接受唯一、已上传、名称精确为 `moqi-reader-vX.Y.Z.apk` 的资产。
- MIME 类型必须为 `application/vnd.android.package-archive`。
- 网络检查保持有限超时；失败不得阻塞应用启动。

“关于 -> 检查更新”和启动后的每日检查必须共用同一 GitHub 更新实现。保留一天一次的自动检查节流，保留用户关闭自动检查的选择；下载后只调用 Android 系统安装器，由用户确认覆盖安装，不申请静默安装或设备管理权限。

修改更新逻辑时，至少覆盖以下单元测试：

- `1.0.0 -> 1.0.1`。
- `1.9.9 -> 1.10.0`。
- 当前版本或更旧版本不提示。
- 非法标签、draft、prerelease。
- APK 缺失、重复、名称错误、状态错误或 MIME 错误。
- 网络失败、超时和 GitHub API 非成功响应。

不得恢复 Gitee、蓝奏云、Telegram、Google Play 或其他维护者仓库的自动更新渠道。如果仓库名、APK 前缀或更新地址发生变化，必须同时更新代码、测试、README、English.md、关于页、分享链接和发布工作流。

## 上游同步规范

新仓库历史与上游没有共同 Git 祖先，禁止直接对 `main` 执行普通 upstream merge。远程命名约定为：

- `origin`：`git@github.com:zhangxiaowei6/legado-mq.git`
- `upstream`：`https://github.com/gedoor/legado.git`
- `sigma-upstream`：`https://github.com/Luoyacheng/legado-E.git`

同步流程固定为：

1. 获取 `upstream` 和 `sigma-upstream`，从 `UPSTREAM_BASELINE.md` 的最后同步位置审阅提交和 diff。
2. 创建 `sync/upstream-YYYYMMDD` 分支，人工移植所需提交，不做整段无审查覆盖。
3. 运行受影响区域测试，通过 PR 合并，并在 PR 中记录上游仓库、完整 SHA 范围、冲突处理和未移植内容。
4. 追加 `sync_history` 并更新 `last_manual_sync`；最初的 `baseline_commit`、`baseline_tree` 和安全清理说明永久不变。
5. 检查上游改动是否重新引入旧品牌、旧更新地址、旧签名材料、外部发布任务、Firebase 配置或其他维护者账号。

## 实用变更检查清单

完成任务前，考虑：

- 这次改动是否触碰对外 API？如有需要，更新 `api.md` 和 Web 客户端类型。
- 这次改动是否触碰 Room 实体？更新 schema 和迁移。
- 这次改动是否触碰源规则？测试搜索、目录、正文和调试路径。
- 这次改动是否触碰 Web UI 源码？运行 Web type-check/build；如果 Android 应用需要包含这些改动，同步资产。
- 这次改动是否触碰权限、provider、service、导入流程或本地 Web 服务行为？重新检查安全暴露面。
- 这次改动是否触碰阅读进度或正文缓存？确认现有用户能保留进度，缓存行为也符合预期。
- 这次改动是否影响版本、APK 名称、更新 API 或 Release 资产筛选？同步更新测试、文档和工作流。
- 这次改动是否影响签名、application ID 或覆盖安装？核对固定证书指纹并执行升级测试。
- 这次改动是否修改 GitHub Actions？确认权限最小化，并保留必需检查名 `android`、`web`。
- 提交前是否检查了凭据、keystore、用户数据、构建产物和无关文件没有进入 diff？
- PR 描述是否记录了测试结果、兼容性风险、数据迁移和上游来源？
- 正式发布前是否完成版本公式、更新日志、标签、APK 签名、SHA-256、Release 资产和应用内更新的全链路核验？
