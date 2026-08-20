USE college_recommendation;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE major_admission_cutoff;
TRUNCATE TABLE major;
TRUNCATE TABLE admission_cutoff;
TRUNCATE TABLE university;
TRUNCATE TABLE score_rank_mapping;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO university (id, name, province, tier, is_985, is_211, is_double_first_class, tags) VALUES
(1, '浙江大学', '浙江', '985', 1, 1, 1, '综合类'),
(2, '宁波大学', '浙江', '双一流', 0, 0, 1, '综合类'),
(3, '杭州电子科技大学', '浙江', '重点', 0, 0, 0, '工科'),
(4, '温州大学', '浙江', '普通', 0, 0, 0, '综合类'),
(5, '南京师范大学', '江苏', '211', 0, 1, 1, '师范类'),
(6, '浙江中医药大学', '浙江', '普通', 0, 0, 0, '医药类');

INSERT INTO admission_cutoff (university_id, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
(1, 2025, '浙江', '物理', 658, 5000),
(2, 2025, '浙江', '物理', 612, 28000),
(3, 2025, '浙江', '物理', 598, 42000),
(4, 2025, '浙江', '物理', 585, 55000),
(5, 2025, '浙江', '物理', 590, 30000),
(6, 2025, '浙江', '物理', 606, 34000),
(1, 2025, '浙江', '历史', 650, 4200),
(2, 2025, '浙江', '历史', 618, 21000),
(3, 2025, '浙江', '历史', 601, 36000),
(4, 2025, '浙江', '历史', 589, 49000),
(5, 2025, '浙江', '历史', 587, 28000),
(6, 2025, '浙江', '历史', 600, 32000);

INSERT INTO major_admission_cutoff (university_id, major_name, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
(1, '计算机科学与技术', 2025, '浙江', '物理', 660, 4500),
(1, '软件工程', 2025, '浙江', '物理', 654, 5600),
(2, '计算机科学与技术', 2025, '浙江', '物理', 618, 24000),
(2, '软件工程', 2025, '浙江', '物理', 614, 27000),
(2, '网络工程', 2025, '浙江', '物理', 611, 29000),
(2, '信息安全', 2025, '浙江', '物理', 610, 30000),
(3, '计算机科学与技术', 2025, '浙江', '物理', 603, 36000),
(3, '电子信息工程', 2025, '浙江', '物理', 600, 40000),
(4, '计算机科学与技术', 2025, '浙江', '物理', 590, NULL),
(6, '临床医学', 2025, '浙江', '物理', 608, 33000),
(6, '护理学', 2025, '浙江', '物理', 595, NULL),
(4, '汉语言文学', 2025, '浙江', '历史', 594, 43000),
(2, '法学', 2025, '浙江', '历史', 620, 19000),
(4, '法学', 2025, '浙江', '历史', 592, NULL),
(6, '护理学', 2025, '浙江', '历史', 590, NULL);

INSERT INTO major (name)
SELECT DISTINCT major_name
FROM major_admission_cutoff;

UPDATE major_admission_cutoff mac
JOIN major m ON m.name = mac.major_name
SET mac.major_id = m.id;

INSERT INTO score_rank_mapping (mapping_year, province, subject_type, score, rank_value) VALUES
(2025, '浙江', '物理', 620, 26000),
(2025, '浙江', '物理', 630, 22000),
(2025, '浙江', '物理', 610, 31000);

INSERT INTO users (id, username, password, score, subject_type, exam_province, role) VALUES
(1, 'testuser', '123456', NULL, NULL, NULL, 'USER'),
(2, 'freshuser', '123456', NULL, NULL, NULL, 'USER'),
(3, 'adminuser', '123456', 650, '物理', '浙江', 'ADMIN');
