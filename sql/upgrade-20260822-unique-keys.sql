-- =============================================================================
-- Upgrade 2026-08-22: add the unique keys that the admission cutoff tables need.
--
-- Why: without them a re-import (or a double click in the admin console) silently
-- creates a second cutoff row for the same university/year/province/subject, and
-- the recommendation engine then picks an arbitrary one of the duplicates.
--
-- The script is idempotent and defensive:
--   * it does nothing when the index already exists;
--   * it does nothing when the table does not exist (or lost a column);
--   * it SKIPS the ALTER when the table already contains duplicates, so it can
--     never abort MySQL container initialisation or a manual upgrade run.
--
-- Manual run against an existing database:
--   mysql -u root -p college_recommendation < sql/upgrade-20260822-unique-keys.sql
-- If the script reports skipped_duplicates, deduplicate first, for example:
--   DELETE c1 FROM admission_cutoff c1
--     JOIN admission_cutoff c2
--       ON c1.university_id = c2.university_id
--      AND c1.admission_year = c2.admission_year
--      AND c1.province = c2.province
--      AND c1.subject_type = c2.subject_type
--      AND c1.id > c2.id;
-- =============================================================================

SET @schema_name := DATABASE();

-- -----------------------------------------------------------------------------
-- 1. admission_cutoff (university_id, admission_year, province, subject_type)
-- -----------------------------------------------------------------------------
SET @table_ready := (
    SELECT COUNT(1) = 4
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'admission_cutoff'
      AND COLUMN_NAME IN ('university_id', 'admission_year', 'province', 'subject_type')
);
SET @index_exists := (
    SELECT COUNT(1) > 0
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'admission_cutoff'
      AND INDEX_NAME = 'uk_cutoff_identity'
);
SET @count_duplicates := IF(@table_ready,
    'SELECT COUNT(1) INTO @duplicate_groups FROM (SELECT 1 FROM admission_cutoff GROUP BY university_id, admission_year, province, subject_type HAVING COUNT(*) > 1) AS duplicates',
    'SELECT 0 INTO @duplicate_groups');
PREPARE count_duplicates_stmt FROM @count_duplicates;
EXECUTE count_duplicates_stmt;
DEALLOCATE PREPARE count_duplicates_stmt;

SET @apply_index := IF(@table_ready AND NOT @index_exists AND @duplicate_groups = 0,
    'ALTER TABLE admission_cutoff ADD UNIQUE KEY uk_cutoff_identity (university_id, admission_year, province, subject_type)',
    CONCAT('SELECT ', IF(@duplicate_groups > 0, '\'skipped_duplicates\'', '\'skipped\''), ' AS admission_cutoff_unique_key'));
PREPARE apply_index_stmt FROM @apply_index;
EXECUTE apply_index_stmt;
DEALLOCATE PREPARE apply_index_stmt;

-- -----------------------------------------------------------------------------
-- 2. major_admission_cutoff
--    (university_id, major_name, admission_year, province, subject_type)
-- -----------------------------------------------------------------------------
SET @major_table_ready := (
    SELECT COUNT(1) = 5
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'major_admission_cutoff'
      AND COLUMN_NAME IN ('university_id', 'major_name', 'admission_year', 'province', 'subject_type')
);
SET @major_index_exists := (
    SELECT COUNT(1) > 0
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'major_admission_cutoff'
      AND INDEX_NAME = 'uk_major_cutoff_identity'
);
SET @count_major_duplicates := IF(@major_table_ready,
    'SELECT COUNT(1) INTO @major_duplicate_groups FROM (SELECT 1 FROM major_admission_cutoff GROUP BY university_id, major_name, admission_year, province, subject_type HAVING COUNT(*) > 1) AS duplicates',
    'SELECT 0 INTO @major_duplicate_groups');
PREPARE count_major_duplicates_stmt FROM @count_major_duplicates;
EXECUTE count_major_duplicates_stmt;
DEALLOCATE PREPARE count_major_duplicates_stmt;

SET @apply_major_index := IF(@major_table_ready AND NOT @major_index_exists AND @major_duplicate_groups = 0,
    'ALTER TABLE major_admission_cutoff ADD UNIQUE KEY uk_major_cutoff_identity (university_id, major_name, admission_year, province, subject_type)',
    CONCAT('SELECT ', IF(@major_duplicate_groups > 0, '\'skipped_duplicates\'', '\'skipped\''), ' AS major_admission_cutoff_unique_key'));
PREPARE apply_major_index_stmt FROM @apply_major_index;
EXECUTE apply_major_index_stmt;
DEALLOCATE PREPARE apply_major_index_stmt;
