CREATE TABLE IF NOT EXISTS university (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  province VARCHAR(64) NOT NULL,
  tier VARCHAR(64),
  is_985 TINYINT(1) NOT NULL DEFAULT 0,
  is_211 TINYINT(1) NOT NULL DEFAULT 0,
  is_double_first_class TINYINT(1) NOT NULL DEFAULT 0,
  tags VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS major (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(64) NULL,
  degree_type VARCHAR(64) NULL,
  tags VARCHAR(255) NULL,
  subject_requirement VARCHAR(255) NULL,
  description TEXT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_major_name (name)
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
  score INT NULL,
  subject_type VARCHAR(16) NULL,
  exam_province VARCHAR(64) NULL,
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
  source_type VARCHAR(16) NOT NULL,
  source_query TEXT NOT NULL,
  result_json LONGTEXT NOT NULL,
  ai_summary TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_plan_user_created (user_id, created_at)
);

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
