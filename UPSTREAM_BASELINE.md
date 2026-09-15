# 上游基线记录

此文件同时记录最初独立基线和后续人工同步位置。最初基线永久不变；后续同步只能追加记录。

```yaml
project: legado-mq
display_name: Moqi Reader / 墨栖阅读
maintainer: zhangxiaowei6
license: GPL-3.0
direct_upstream_repository: https://github.com/Luoyacheng/legado-E
baseline_commit: 44e07fea541287804cc58d0168940a756cd11cfd
baseline_tree: 96e1d0d3c931dad694182fb4984ba8754b145534
baseline_authored_at: 2026-03-21T22:03:14+08:00
baseline_committed_at: 2026-03-21T22:03:14+08:00
baseline_title: "[优化]：低安卓上图标圆角"
original_upstream_repository: https://github.com/gedoor/legado
last_confirmed_gedoor_merge: 0486da3c0255bb5b2d13427c7c4cb829ac37dd0d
first_independent_change: 5a4983052bca56e8716f62a20387637be6d5717a
first_independent_change_date: 2026-07-17T19:13:03+08:00
sanitized_snapshot_tag: baseline-legado-e-44e07fe
sanitized_exclusions:
  - path: .github/workflows/legado.jks
    reason: tracked legacy release keystore; excluded to prevent credential redistribution
  - path: .github/workflows/test.yml
    reason: legacy workflow embedded signing credentials and external publishing jobs; excluded from the sanitized root snapshot
  - path: .github/workflows/release.yml
    reason: legacy workflow targeted upstream release channels and signing infrastructure; excluded from the sanitized root snapshot
  - path: .github/workflows/cronet.yml
    reason: legacy maintenance workflow targeted the previous repository; excluded to prevent automated writes from the imported root snapshot
  - path: .github/workflows/web.yml
    reason: legacy asset workflow targeted the previous repository; excluded to prevent automated writes from the imported root snapshot
  - path: .github/workflows/stale.yml
    reason: legacy issue automation targeted the previous repository; excluded so the imported root snapshot has no active automation
  - path: app/google-services.json
    reason: legacy Firebase client configuration targeted an upstream-owned project; excluded to prevent the independent app from sending telemetry to another maintainer's service
  - path: local.properties
    reason: local SDK and signing configuration; never tracked or imported
last_manual_sync:
  direct_upstream: 44e07fea541287804cc58d0168940a756cd11cfd
  original_upstream: 0486da3c0255bb5b2d13427c7c4cb829ac37dd0d
sync_history: []
```

## 核验说明

权威基线链接：[`Luoyacheng/legado-E@44e07fea`](https://github.com/Luoyacheng/legado-E/commit/44e07fea541287804cc58d0168940a756cd11cfd)。上游当时使用构建时间动态生成版本号，没有与该提交一一对应的固定 Release 标签，因此完整 commit SHA 是权威标识。

`baseline-legado-e-44e07fe` 指向经过凭据清理的导入快照，不是上游 Release，也不是原始树的逐字节副本。原始上游树 SHA 固定记录为 `96e1d0d3c931dad694182fb4984ba8754b145534`。

由于新仓库历史与上游没有共同 Git 祖先，后续不得直接执行普通 upstream merge。同步时从这里记录的最后位置审阅差异，在 `sync/upstream-YYYYMMDD` 分支人工移植，通过 PR 记录上游 SHA 范围，然后追加 `sync_history` 并更新 `last_manual_sync`；不得修改最初的 `baseline_commit`。
