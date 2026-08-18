CREATE TABLE IF NOT EXISTS university (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  province VARCHAR(64) NOT NULL,
  tier VARCHAR(64),
  is_985 TINYINT(1) NOT NULL DEFAULT 0,
  is_211 TINYINT(1) NOT NULL DEFAULT 0,
  is_double_first_class TINYINT(1) NOT NULL DEFAULT 0,
  city VARCHAR(64) NULL,
  nature VARCHAR(32) NULL,
  belong VARCHAR(120) NULL,
  logo_id INT NULL,
  tags VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS major (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  major_code VARCHAR(16) NULL,
  category VARCHAR(64) NULL,
  subcategory VARCHAR(64) NULL,
  duration VARCHAR(32) NULL,
  degree_type VARCHAR(64) NULL,
  gender_ratio VARCHAR(16) NULL,
  average_salary VARCHAR(32) NULL,
  popularity INT NULL,
  employment_directions VARCHAR(512) NULL,
  demo_data TINYINT(1) NOT NULL DEFAULT 0,
  tags VARCHAR(255) NULL,
  subject_requirement VARCHAR(255) NULL,
  description TEXT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_major_name (name)
);

SET @major_code_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major' AND COLUMN_NAME = 'major_code'
);
SET @sql = IF(@major_code_exists = 0, 'ALTER TABLE major ADD COLUMN major_code VARCHAR(16) NULL AFTER name', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @major_subcategory_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major' AND COLUMN_NAME = 'subcategory'
);
SET @sql = IF(@major_subcategory_exists = 0, 'ALTER TABLE major ADD COLUMN subcategory VARCHAR(64) NULL AFTER category', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @major_duration_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major' AND COLUMN_NAME = 'duration'
);
SET @sql = IF(@major_duration_exists = 0, 'ALTER TABLE major ADD COLUMN duration VARCHAR(32) NULL AFTER subcategory', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @major_gender_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major' AND COLUMN_NAME = 'gender_ratio'
);
SET @sql = IF(@major_gender_exists = 0, 'ALTER TABLE major ADD COLUMN gender_ratio VARCHAR(16) NULL AFTER degree_type', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @major_salary_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major' AND COLUMN_NAME = 'average_salary'
);
SET @sql = IF(@major_salary_exists = 0, 'ALTER TABLE major ADD COLUMN average_salary VARCHAR(32) NULL AFTER gender_ratio', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @major_popularity_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major' AND COLUMN_NAME = 'popularity'
);
SET @sql = IF(@major_popularity_exists = 0, 'ALTER TABLE major ADD COLUMN popularity INT NULL AFTER average_salary', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @major_employment_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major' AND COLUMN_NAME = 'employment_directions'
);
SET @sql = IF(@major_employment_exists = 0, 'ALTER TABLE major ADD COLUMN employment_directions VARCHAR(512) NULL AFTER popularity', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @major_demo_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major' AND COLUMN_NAME = 'demo_data'
);
SET @sql = IF(@major_demo_exists = 0, 'ALTER TABLE major ADD COLUMN demo_data TINYINT(1) NOT NULL DEFAULT 0 AFTER employment_directions', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @university_city_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'university' AND COLUMN_NAME = 'city'
);
SET @sql = IF(@university_city_exists = 0, 'ALTER TABLE university ADD COLUMN city VARCHAR(64) NULL AFTER province', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @university_nature_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'university' AND COLUMN_NAME = 'nature'
);
SET @sql = IF(@university_nature_exists = 0, 'ALTER TABLE university ADD COLUMN nature VARCHAR(32) NULL AFTER city', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @university_belong_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'university' AND COLUMN_NAME = 'belong'
);
SET @sql = IF(@university_belong_exists = 0, 'ALTER TABLE university ADD COLUMN belong VARCHAR(120) NULL AFTER nature', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @university_logo_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'university' AND COLUMN_NAME = 'logo_id'
);
SET @sql = IF(@university_logo_exists = 0, 'ALTER TABLE university ADD COLUMN logo_id INT NULL AFTER belong', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS major_offering (
  major_id BIGINT NOT NULL,
  university_id BIGINT NOT NULL,
  PRIMARY KEY (major_id, university_id),
  CONSTRAINT fk_major_offering_major FOREIGN KEY (major_id) REFERENCES major(id),
  CONSTRAINT fk_major_offering_university FOREIGN KEY (university_id) REFERENCES university(id),
  INDEX idx_major_offering_university (university_id)
);

SET @is_985_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'university' AND COLUMN_NAME = 'is_985'
);
SET @sql = IF(@is_985_exists = 0, 'ALTER TABLE university ADD COLUMN is_985 TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @is_211_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'university' AND COLUMN_NAME = 'is_211'
);
SET @sql = IF(@is_211_exists = 0, 'ALTER TABLE university ADD COLUMN is_211 TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @is_double_first_class_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'university' AND COLUMN_NAME = 'is_double_first_class'
);
SET @sql = IF(@is_double_first_class_exists = 0, 'ALTER TABLE university ADD COLUMN is_double_first_class TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS admission_cutoff (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  university_id BIGINT NOT NULL,
  admission_year INT NOT NULL,
  province VARCHAR(64) NOT NULL,
  subject_type VARCHAR(16) NOT NULL,
  cutoff_score INT NOT NULL,
  min_rank INT,
  CONSTRAINT fk_cutoff_university FOREIGN KEY (university_id) REFERENCES university(id),
  INDEX idx_cutoff_query (province, subject_type, admission_year)
);

CREATE TABLE IF NOT EXISTS major_admission_cutoff (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  university_id BIGINT NOT NULL,
  major_id BIGINT NULL,
  major_name VARCHAR(120) NOT NULL,
  admission_year INT NOT NULL,
  province VARCHAR(64) NOT NULL,
  subject_type VARCHAR(16) NOT NULL,
  cutoff_score INT NULL,
  min_rank INT NULL,
  professional_group_code VARCHAR(16) NULL,
  professional_group_name VARCHAR(120) NULL,
  primary_subject VARCHAR(16) NULL,
  elective_subjects VARCHAR(64) NULL,
  CONSTRAINT fk_major_cutoff_university FOREIGN KEY (university_id) REFERENCES university(id),
  CONSTRAINT fk_major_cutoff_major FOREIGN KEY (major_id) REFERENCES major(id),
  INDEX idx_major_cutoff_query (province, subject_type, admission_year, major_name)
);

SET @major_id_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_admission_cutoff' AND COLUMN_NAME = 'major_id'
);
SET @sql = IF(@major_id_exists = 0, 'ALTER TABLE major_admission_cutoff ADD COLUMN major_id BIGINT NULL AFTER university_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS score_rank_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  mapping_year INT NOT NULL,
  province VARCHAR(64) NOT NULL,
  subject_type VARCHAR(16) NOT NULL,
  score INT NOT NULL,
  rank_value INT NOT NULL,
  UNIQUE KEY uk_rank_mapping (mapping_year, province, subject_type, score),
  INDEX idx_rank_mapping_lookup (province, subject_type, mapping_year, score)
);

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  email VARCHAR(254) NULL,
  score INT NULL,
  subject_type VARCHAR(16) NULL,
  exam_province VARCHAR(64) NULL,
  elective_subjects VARCHAR(64) NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @user_role_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role'
);
SET @sql = IF(@user_role_exists = 0, 'ALTER TABLE users ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT ''USER''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_email_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email'
);
SET @sql = IF(@user_email_exists = 0, 'ALTER TABLE users ADD COLUMN email VARCHAR(254) NULL AFTER password', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_email_index_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uk_users_email'
);
SET @sql = IF(@user_email_index_exists = 0, 'ALTER TABLE users ADD UNIQUE KEY uk_users_email (email)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS email_verification_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(254) NOT NULL,
  code_hash VARCHAR(128) NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  consumed_at TIMESTAMP NULL,
  INDEX idx_email_verification_lookup (email, purpose, consumed, id)
);

CREATE TABLE IF NOT EXISTS ai_runtime_config (
  id INT PRIMARY KEY,
  provider VARCHAR(64) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  model VARCHAR(160) NOT NULL,
  encrypted_api_key TEXT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @professional_group_code_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_admission_cutoff' AND COLUMN_NAME = 'professional_group_code'
);
SET @sql = IF(@professional_group_code_exists = 0, 'ALTER TABLE major_admission_cutoff ADD COLUMN professional_group_code VARCHAR(16) NULL AFTER min_rank', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @professional_group_name_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_admission_cutoff' AND COLUMN_NAME = 'professional_group_name'
);
SET @sql = IF(@professional_group_name_exists = 0, 'ALTER TABLE major_admission_cutoff ADD COLUMN professional_group_name VARCHAR(120) NULL AFTER professional_group_code', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @primary_subject_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_admission_cutoff' AND COLUMN_NAME = 'primary_subject'
);
SET @sql = IF(@primary_subject_exists = 0, 'ALTER TABLE major_admission_cutoff ADD COLUMN primary_subject VARCHAR(16) NULL AFTER professional_group_name', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @major_elective_subjects_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'major_admission_cutoff' AND COLUMN_NAME = 'elective_subjects'
);
SET @sql = IF(@major_elective_subjects_exists = 0, 'ALTER TABLE major_admission_cutoff ADD COLUMN elective_subjects VARCHAR(64) NULL AFTER primary_subject', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @user_elective_subjects_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'elective_subjects'
);
SET @sql = IF(@user_elective_subjects_exists = 0, 'ALTER TABLE users ADD COLUMN elective_subjects VARCHAR(64) NULL AFTER exam_province', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Idempotent demo professional-group mapping for existing databases. The values
-- mirror the fixed competition demo dataset; no runtime formula is involved.
UPDATE major_admission_cutoff
SET professional_group_code = '101', professional_group_name = '计算机与智能类', primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 1 AND major_name IN ('计算机科学与技术', '软件工程', '人工智能') AND professional_group_code IS NULL;
UPDATE major_admission_cutoff
SET professional_group_code = '102', professional_group_name = '电子信息类', primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 1 AND major_name IN ('数据科学与大数据技术', '电子信息工程', '通信工程') AND professional_group_code IS NULL;
UPDATE major_admission_cutoff
SET professional_group_code = '201', professional_group_name = '工科试验班', primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 2 AND major_name IN ('计算机科学与技术', '软件工程', '数据科学与大数据技术') AND professional_group_code IS NULL;
UPDATE major_admission_cutoff
SET professional_group_code = '202', professional_group_name = '智能科学类', primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 2 AND major_name IN ('人工智能', '电子信息工程', '通信工程') AND professional_group_code IS NULL;
UPDATE major_admission_cutoff
SET professional_group_code = '301', professional_group_name = '信息技术类', primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 3 AND major_name IN ('计算机科学与技术', '软件工程', '人工智能') AND professional_group_code IS NULL;
UPDATE major_admission_cutoff
SET professional_group_code = '302', professional_group_name = '电子工程类', primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 3 AND major_name IN ('数据科学与大数据技术', '电子信息工程', '通信工程') AND professional_group_code IS NULL;

SET @user_enabled_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'enabled'
);
SET @sql = IF(@user_enabled_exists = 0, 'ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS recommendation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  query_type VARCHAR(16) NOT NULL,
  query_content TEXT NOT NULL,
  result_json LONGTEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_log_user_created (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS application_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_name VARCHAR(128) NOT NULL,
  source_type VARCHAR(16) NULL,
  source_query TEXT NULL,
  result_json LONGTEXT NOT NULL,
  ai_summary TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_plan_user_created (user_id, created_at)
);

ALTER TABLE application_plan MODIFY COLUMN source_type VARCHAR(16) NULL;
ALTER TABLE application_plan MODIFY COLUMN source_query TEXT NULL;

CREATE TABLE IF NOT EXISTS recommendation_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  request_id VARCHAR(64) NOT NULL,
  source_type VARCHAR(16) NOT NULL,
  raw_query TEXT NOT NULL,
  request_json LONGTEXT NULL,
  parsed_requirement_json LONGTEXT NULL,
  result_json LONGTEXT NULL,
  status VARCHAR(32) NOT NULL,
  recommendation_mode VARCHAR(32) NULL,
  result_count INT NOT NULL DEFAULT 0,
  duration_ms BIGINT NULL,
  error_message TEXT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_task_request_id (request_id),
  INDEX idx_task_user_created (user_id, created_at),
  INDEX idx_task_source_created (source_type, created_at)
);

SET @task_duration_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recommendation_task' AND COLUMN_NAME = 'duration_ms'
);
SET @sql = IF(@task_duration_exists = 0, 'ALTER TABLE recommendation_task ADD COLUMN duration_ms BIGINT NULL AFTER result_count', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @task_error_exists = (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recommendation_task' AND COLUMN_NAME = 'error_message'
);
SET @sql = IF(@task_error_exists = 0, 'ALTER TABLE recommendation_task ADD COLUMN error_message TEXT NULL AFTER duration_ms', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ai_parse_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NULL,
  request_id VARCHAR(64) NOT NULL,
  provider VARCHAR(64) NULL,
  model_name VARCHAR(64) NULL,
  parse_mode VARCHAR(32) NOT NULL,
  success_flag TINYINT(1) NOT NULL DEFAULT 1,
  requirement_text TEXT NOT NULL,
  raw_response LONGTEXT NULL,
  parsed_json LONGTEXT NULL,
  error_message TEXT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_parse_task FOREIGN KEY (task_id) REFERENCES recommendation_task(id),
  INDEX idx_ai_parse_task_created (task_id, created_at),
  INDEX idx_ai_parse_request_created (request_id, created_at)
);

CREATE TABLE IF NOT EXISTS agent_conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  last_message_at TIMESTAMP NULL,
  message_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_conversation_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_agent_conversation_user_updated (user_id, updated_at),
  INDEX idx_agent_conversation_user_last_message (user_id, last_message_at)
);

CREATE TABLE IF NOT EXISTS agent_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  message_type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  tool_name VARCHAR(64) NULL,
  payload_json LONGTEXT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_message_conversation FOREIGN KEY (conversation_id) REFERENCES agent_conversation(id),
  INDEX idx_agent_message_conversation_created (conversation_id, created_at)
);
