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
