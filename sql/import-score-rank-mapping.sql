USE college_recommendation;

-- Score-to-rank import template.
-- 1. Prepare a CSV file with header:
--    mapping_year,province,subject_type,score,rank_value
-- 2. Replace the file path below with your real local file path.
-- 3. subject_type must match current system values exactly: 物理 / 历史
-- 4. rank_value means the candidate rank for that score. Smaller is better.

-- Optional cleanup for one province/year/subject before reimport:
-- DELETE FROM score_rank_mapping
-- WHERE mapping_year = 2025
--   AND province = '浙江'
--   AND subject_type = '物理';

LOAD DATA LOCAL INFILE 'D:/data/score-rank-mapping-template.csv'
INTO TABLE score_rank_mapping
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(mapping_year, province, subject_type, score, rank_value);

-- Verification:
-- SELECT mapping_year, province, subject_type, COUNT(*) AS row_count
-- FROM score_rank_mapping
-- GROUP BY mapping_year, province, subject_type
-- ORDER BY mapping_year DESC, province, subject_type;

-- Example lookup:
-- SELECT rank_value
-- FROM score_rank_mapping
-- WHERE mapping_year = 2025
--   AND province = '浙江'
--   AND subject_type = '物理'
--   AND score = 658;
