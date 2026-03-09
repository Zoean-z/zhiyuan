CREATE TABLE university (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  province VARCHAR(64) NOT NULL,
  tier VARCHAR(64),
  tags VARCHAR(255)
);

CREATE TABLE admission_cutoff (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  university_id BIGINT NOT NULL,
  admission_year INT NOT NULL,
  province VARCHAR(64) NOT NULL,
  subject_type VARCHAR(16) NOT NULL,
  cutoff_score INT NOT NULL,
  min_rank INT,
  CONSTRAINT fk_cutoff_university FOREIGN KEY (university_id) REFERENCES university(id)
);

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  score INT NULL,
  subject_type VARCHAR(16) NULL,
  exam_province VARCHAR(64) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recommendation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  query_type VARCHAR(16) NOT NULL,
  query_content CLOB NOT NULL,
  result_json CLOB NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(id)
);
