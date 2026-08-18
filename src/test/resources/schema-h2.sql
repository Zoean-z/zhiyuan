CREATE TABLE university (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  province VARCHAR(64) NOT NULL,
  tier VARCHAR(64),
  is_985 BOOLEAN NOT NULL DEFAULT FALSE,
  is_211 BOOLEAN NOT NULL DEFAULT FALSE,
  is_double_first_class BOOLEAN NOT NULL DEFAULT FALSE,
  city VARCHAR(64),
  nature VARCHAR(32),
  belong VARCHAR(120),
  logo_id INT,
  tags VARCHAR(255)
);

CREATE TABLE major (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  major_code VARCHAR(16),
  category VARCHAR(64),
  subcategory VARCHAR(64),
  duration VARCHAR(32),
  degree_type VARCHAR(64),
  gender_ratio VARCHAR(16),
  average_salary VARCHAR(32),
  popularity INT,
  employment_directions VARCHAR(512),
  demo_data BOOLEAN NOT NULL DEFAULT FALSE,
  tags VARCHAR(255),
  subject_requirement VARCHAR(255),
  description CLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_major_name UNIQUE (name)
);

CREATE TABLE major_offering (
  major_id BIGINT NOT NULL,
  university_id BIGINT NOT NULL,
  PRIMARY KEY (major_id, university_id),
  CONSTRAINT fk_major_offering_major FOREIGN KEY (major_id) REFERENCES major(id),
  CONSTRAINT fk_major_offering_university FOREIGN KEY (university_id) REFERENCES university(id)
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

CREATE TABLE major_admission_cutoff (
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
  CONSTRAINT fk_major_cutoff_major FOREIGN KEY (major_id) REFERENCES major(id)
);

CREATE TABLE score_rank_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  mapping_year INT NOT NULL,
  province VARCHAR(64) NOT NULL,
  subject_type VARCHAR(16) NOT NULL,
  score INT NOT NULL,
  rank_value INT NOT NULL,
  CONSTRAINT uk_rank_mapping UNIQUE (mapping_year, province, subject_type, score)
);

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  email VARCHAR(254) NULL UNIQUE,
  score INT NULL,
  subject_type VARCHAR(16) NULL,
  exam_province VARCHAR(64) NULL,
  elective_subjects VARCHAR(64) NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE email_verification_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(254) NOT NULL,
  code_hash VARCHAR(128) NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  consumed_at TIMESTAMP NULL
);

CREATE TABLE ai_runtime_config (
  id INT PRIMARY KEY,
  provider VARCHAR(64) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  model VARCHAR(160) NOT NULL,
  encrypted_api_key CLOB NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

CREATE TABLE application_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_name VARCHAR(128) NOT NULL,
  source_type VARCHAR(16) NULL,
  source_query CLOB NULL,
  result_json CLOB NOT NULL,
  ai_summary CLOB NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE recommendation_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  request_id VARCHAR(64) NOT NULL,
  source_type VARCHAR(16) NOT NULL,
  raw_query CLOB NOT NULL,
  request_json CLOB NULL,
  parsed_requirement_json CLOB NULL,
  result_json CLOB NULL,
  status VARCHAR(32) NOT NULL,
  recommendation_mode VARCHAR(32) NULL,
  result_count INT NOT NULL DEFAULT 0,
  duration_ms BIGINT NULL,
  error_message CLOB NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uk_task_request_id UNIQUE (request_id)
);

CREATE TABLE ai_parse_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NULL,
  request_id VARCHAR(64) NOT NULL,
  provider VARCHAR(64) NULL,
  model_name VARCHAR(64) NULL,
  parse_mode VARCHAR(32) NOT NULL,
  success_flag BOOLEAN NOT NULL DEFAULT TRUE,
  requirement_text CLOB NOT NULL,
  raw_response CLOB NULL,
  parsed_json CLOB NULL,
  error_message CLOB NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_parse_task FOREIGN KEY (task_id) REFERENCES recommendation_task(id)
);

CREATE TABLE agent_conversation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  last_message_at TIMESTAMP NULL,
  message_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_conversation_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE agent_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  message_type VARCHAR(32) NOT NULL,
  content CLOB NOT NULL,
  tool_name VARCHAR(64) NULL,
  payload_json CLOB NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_message_conversation FOREIGN KEY (conversation_id) REFERENCES agent_conversation(id)
);
