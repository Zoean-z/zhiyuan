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
