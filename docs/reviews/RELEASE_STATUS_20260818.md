# 2026-08-18 交付状态

## 本次已完成
- 合并审计产物中的后端公开院校 / 概率 / 位次接口与相关 DTO、Mapper、Service。
- 修复前端概率与位次展示的 null-safe 语义，避免把 unknown/null 伪装成 `0%`、`50%` 或错误位次。
- 修复志愿草稿云端同步：改为 `replaceCurrentPlanDraftItems(...)` 一次性覆盖。
- 去除 `MajorDetailView.vue` 中会误导为真实专业录取数据的 hash 派生口径，改为明确的院校线参考展示。
- 新增并离线验证聚焦测试：
  - `src/test/java/com/zhiyuan/college/controller/ScoreRankControllerTest.java`
  - `src/test/java/com/zhiyuan/college/service/ProbabilityServiceTest.java`
- 在沙箱内生成可恢复检查点 ZIP，便于重置后恢复。

## 已验证结果
- 手工离线编译：新增聚焦测试编译通过。
- 手工离线执行：6 个聚焦测试全部通过。
- 当前提交分支：`audit/full-review-20260818`

## 当前仍受沙箱环境限制的部分
- `npm ci` 受离线网络 / 缓存缺失影响，无法恢复前端依赖包 tarball。
- Maven Wrapper 需要在线下载 Maven 发行版，本沙箱当前无法下载。
- 基线本地 Maven 仓库缺少部分 `spring-security` / `jjwt` / `rocketmq` / `redis` 相关 jar，因此无法在当前沙箱完成全量 Maven 测试。

## 可复现恢复材料
- Git 提交可用于 `git bundle` 恢复。
- 全量工作树 ZIP 可用于直接解压恢复。
- 检查点文件会放在 `/data/` 下。

## 建议外部恢复后执行
1. `npm ci && npm run build`
2. `./mvnw -B test`
3. 启动 MySQL + 后端 + 前端后跑浏览器回归
4. 若需要，基于当前提交继续补做完整 E2E
