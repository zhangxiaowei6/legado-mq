# AGENTS.md

本文档为编码代理提供本仓库的项目级上下文，帮助后续修改更安全、更贴合现有工程。

## 项目概览

本仓库是“墨栖阅读 / Moqi Reader”（仓库名 `legado-mq`），一个从 `Luoyacheng/legado-E@44e07fea541287804cc58d0168940a756cd11cfd` 开始独立维护的 Legado 第三方修改版，并非官方发行版。核心应用是原生 Android 工程，主要使用 Kotlin 编写；同时内置了一个 Vue/Vite Web UI，作为应用资产打包，用于 Web 端书架和源编辑。来源与同步规则见 `UPSTREAM_BASELINE.md`。

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

## CI 与发布

GitHub Actions 包括：

- Android 测试/发布构建。
- Web 构建和资产同步。
- Cronet 更新自动化。
- 发布产物上传。

发布构建可能会在 CI 中重写版本名和签名属性。本地改动不要固化 CI 专用 secret 或发布时文件变更。

## 实用变更检查清单

完成任务前，考虑：

- 这次改动是否触碰对外 API？如有需要，更新 `api.md` 和 Web 客户端类型。
- 这次改动是否触碰 Room 实体？更新 schema 和迁移。
- 这次改动是否触碰源规则？测试搜索、目录、正文和调试路径。
- 这次改动是否触碰 Web UI 源码？运行 Web type-check/build；如果 Android 应用需要包含这些改动，同步资产。
- 这次改动是否触碰权限、provider、service、导入流程或本地 Web 服务行为？重新检查安全暴露面。
- 这次改动是否触碰阅读进度或正文缓存？确认现有用户能保留进度，缓存行为也符合预期。
