CREATE DATABASE IF NOT EXISTS college_recommendation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE college_recommendation;

CREATE TABLE IF NOT EXISTS university (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  province VARCHAR(64) NOT NULL,
  tier VARCHAR(64),
  tags VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

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
  major_name VARCHAR(120) NOT NULL,
  admission_year INT NOT NULL,
  province VARCHAR(64) NOT NULL,
  subject_type VARCHAR(16) NOT NULL,
  cutoff_score INT NULL,
  min_rank INT NULL,
  CONSTRAINT fk_major_cutoff_university FOREIGN KEY (university_id) REFERENCES university(id),
  INDEX idx_major_cutoff_query (province, subject_type, admission_year, major_name)
);

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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

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
