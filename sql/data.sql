USE college_recommendation;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE major_admission_cutoff;
TRUNCATE TABLE admission_cutoff;
TRUNCATE TABLE university;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO university (id, name, province, tier, tags) VALUES
(1, '浙江大学', '浙江', '985', '综合类'),
(2, '宁波大学', '浙江', '双一流', '综合类'),
(3, '杭州电子科技大学', '浙江', '重点', '工科'),
(4, '温州大学', '浙江', '普通', '综合类');

INSERT INTO admission_cutoff (university_id, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
(1, 2025, '浙江', '物理', 658, 5000),
(2, 2025, '浙江', '物理', 612, 28000),
(3, 2025, '浙江', '物理', 598, 42000),
(4, 2025, '浙江', '物理', 585, 55000),
(1, 2025, '浙江', '历史', 650, 4200),
(2, 2025, '浙江', '历史', 618, 21000),
(3, 2025, '浙江', '历史', 601, 36000),
(4, 2025, '浙江', '历史', 589, 49000);

INSERT INTO major_admission_cutoff (university_id, major_name, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
(1, '计算机科学与技术', 2025, '浙江', '物理', 660, 4500),
(1, '软件工程', 2025, '浙江', '物理', 654, 5600),
(2, '计算机科学与技术', 2025, '浙江', '物理', 618, 24000),
(2, '软件工程', 2025, '浙江', '物理', 614, 27000),
(3, '计算机科学与技术', 2025, '浙江', '物理', 603, 36000),
(3, '电子信息工程', 2025, '浙江', '物理', 600, 40000),
(4, '计算机科学与技术', 2025, '浙江', '物理', 590, NULL),
(4, '汉语言文学', 2025, '浙江', '历史', 594, 43000),
(2, '法学', 2025, '浙江', '历史', 620, 19000),
(4, '法学', 2025, '浙江', '历史', 592, NULL);

INSERT INTO users (id, username, password, score, subject_type, exam_province) VALUES
(1, 'testuser', '123456', NULL, NULL, NULL),
(2, 'freshuser', '123456', NULL, NULL, NULL);
