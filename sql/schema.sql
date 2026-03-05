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

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  score INT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
