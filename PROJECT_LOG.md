# Project Log

## 2026-08-07T08:42:06+08:00

- Initialized project memory files.
- Awaiting the planning prompt from the user.

## 2026-08-07T08:52:00+08:00

- 修复专业自动联想的数据来源：改用实际有数据的专业分数线表，并保留关键词、地区和科类过滤。
- 登录页顶部栏与业务页统一桌面和移动端尺寸规范。
- 影响文件：`MajorMapper.java`、`frontend/src/styles.css`、静态构建产物及项目状态记录。
- 验证：真实接口返回专业候选，前端构建通过，全量 67 项测试无失败。
- 下一步：用户刷新页面验收联想下拉与顶部栏一致性。

## 2026-08-18 offline recovery checkpoint
- Copied audited backend public API controllers/services/DTOs/mappers into current worktree.
- Repaired current frontend null-safe probability UI and cloud-draft replacement sync.
- Removed misleading hash-derived major cutoff presentation from `MajorDetailView.vue`.
- Added focused offline tests: `ScoreRankControllerTest`, `ProbabilityServiceTest`.
- Added test shims/resources for offline sandbox verification:
  - `src/test/java/org/springframework/data/redis/core/StringRedisTemplate.java`
  - `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- Verified offline focused tests passed via manual JUnit launcher in sandbox.
- Created checkpoint zips under `/data/zhiyuan-fixed-checkpoint-*.zip`.
- Remaining blockers are sandbox dependency/network limits for full npm/Maven restore.

## 2026-08-20 student-first UX pass
- Explored the reference product from a student task perspective: profile first, then school/major discovery, then one working volunteer sheet, with AI as optional assistance.
- Removed the full volunteer big-data block (statistics, school/major TOP lists, and destination-city ranking) from `VolunteerView.vue`.
- Moved the active top-navigation underline to the bottom edge and vertically centered labels so the line no longer crosses text.
- Unblocked ordinary AI conversation when no plan is selected; nullable plan context is now sent as `null`, not `0`.
- Added automatic local-sheet to cloud-current-draft bridging when a browser has a 45-slot sheet but the user has no current cloud draft.
- Added an explicit, non-blocking plan-context hint in the AI welcome screen.
- Added `UX_REVIEW_20260820.md` with reference-product findings and competition-focused design decisions.
- Validation: Vue/CSS parsed through Prettier, extracted script blocks passed `node --check`, targeted source assertions passed, and local Chromium geometry confirmed the active underline does not overlap navigation text.
- Full Vite build is still blocked in this sandbox because the npm dependency tarballs are unavailable; `npm ci` ends with npm's `Exit handler never called!` error.

## 2026-08-20 functional-design correction
- Restored the original color theme, brand, navigation names, page descriptions, home layout, side panel, login copy, and consultation copy.
- Limited the new pass to functional behavior and data correctness.
- Kept query reactivity and null-safe sorting in `SchoolsView.vue`.
- Removed generated professional-group IDs and fixed missing-value/validation behavior in `ChooseView.vue`.
- Replaced page-local fabricated yearly admission figures with the shared cutoff history source in `VolunteerSheet.vue` and removed fabricated plan/ranking metrics.
- Added explicit invalid-score feedback in the score-segment query.
- Added `FUNCTION_DESIGN_20260820.md` to document the preserved UI boundary and functional changes.
