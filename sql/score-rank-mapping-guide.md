# 分数位次导入模板

当前系统使用表 `score_rank_mapping` 保存分数到位次的映射，字段如下：

- `mapping_year`: 年份，例如 `2025`
- `province`: 生源省份，例如 `浙江`
- `subject_type`: 科类，当前必须使用 `物理` 或 `历史`
- `score`: 分数
- `rank_value`: 对应位次，数字越小表示位次越靠前

## 推荐导入流程

1. 按 [score-rank-mapping-template.csv](D:\Java\IntelliJIDEA\zhiyuan\sql\score-rank-mapping-template.csv) 的表头准备真实数据。
2. 每个 `年份 + 省份 + 科类 + 分数` 只能有一条记录。
3. 打开 [import-score-rank-mapping.sql](D:\Java\IntelliJIDEA\zhiyuan\sql\import-score-rank-mapping.sql)，把 `LOAD DATA LOCAL INFILE` 的文件路径改成你的真实 CSV 路径。
4. 用 MySQL 客户端执行导入。

## CSV 示例

```csv
mapping_year,province,subject_type,score,rank_value
2025,浙江,物理,658,5000
2025,浙江,物理,657,5300
2025,浙江,历史,650,4200
```

## 常用校验 SQL

```sql
SELECT COUNT(*) FROM score_rank_mapping;

SELECT *
FROM score_rank_mapping
WHERE province = '浙江'
  AND subject_type = '物理'
ORDER BY mapping_year DESC, score DESC
LIMIT 20;
```

## 注意

- 你的真实一分一段表如果是 Excel，先另存为 UTF-8 CSV 再导入。
- Windows 路径建议写成正斜杠，例如 `D:/data/zhejiang-2025-physics.csv`。
- 如果 MySQL 禁用了 `LOCAL INFILE`，可以改用 Navicat / DataGrip 的 CSV 导入向导，列名仍按同一模板对应即可。
