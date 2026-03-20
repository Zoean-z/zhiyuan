INSERT INTO university (id, name, province, tier, is_985, is_211, is_double_first_class, tags) VALUES
(1, '浙江大学', '浙江', '985', TRUE, TRUE, TRUE, '综合类'),
(2, '宁波大学', '浙江', '双一流', FALSE, FALSE, TRUE, '综合类'),
(3, '杭州电子科技大学', '浙江', '重点', FALSE, FALSE, FALSE, '工科'),
(4, '温州大学', '浙江', '普通', FALSE, FALSE, FALSE, '综合类'),
(5, '南京师范大学', '江苏', '211', FALSE, TRUE, TRUE, '师范类'),
(6, '浙江中医药大学', '浙江', '普通', FALSE, FALSE, FALSE, '医药类');

INSERT INTO admission_cutoff (university_id, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
(1, 2025, '浙江', '物理', 658, 5000),
(2, 2025, '浙江', '物理', 612, 28000),
(3, 2025, '浙江', '物理', 598, 42000),
(4, 2025, '浙江', '物理', 585, 55000),
(5, 2025, '浙江', '物理', 590, 30000),
(6, 2025, '浙江', '物理', 606, 34000),
(1, 2025, '浙江', '历史', 650, 4200),
(2, 2025, '浙江', '历史', 618, 21000),
(5, 2025, '浙江', '历史', 587, 28000),
(6, 2025, '浙江', '历史', 600, 32000);

INSERT INTO major_admission_cutoff (university_id, major_name, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
(1, '计算机科学与技术', 2025, '浙江', '物理', 660, 4500),
(2, '计算机科学与技术', 2025, '浙江', '物理', 618, 24000),
(2, '软件工程', 2025, '浙江', '物理', 614, 27000),
(2, '网络工程', 2025, '浙江', '物理', 611, 29000),
(2, '信息安全', 2025, '浙江', '物理', 610, 30000),
(3, '计算机科学与技术', 2025, '浙江', '物理', 603, 36000),
(4, '计算机科学与技术', 2025, '浙江', '物理', 590, NULL),
(6, '临床医学', 2025, '浙江', '物理', 608, 33000),
(6, '护理学', 2025, '浙江', '物理', 595, NULL),
(2, '法学', 2025, '浙江', '历史', 620, 19000),
(4, '法学', 2025, '浙江', '历史', 592, NULL),
(6, '护理学', 2025, '浙江', '历史', 590, NULL);

INSERT INTO score_rank_mapping (mapping_year, province, subject_type, score, rank_value) VALUES
(2025, '浙江', '物理', 620, 26000),
(2025, '浙江', '物理', 630, 22000),
(2025, '浙江', '物理', 610, 31000);

INSERT INTO users (id, username, password, score, subject_type, exam_province) VALUES
(1, 'testuser', '123456', NULL, NULL, NULL);
INSERT INTO users (id, username, password, score, subject_type, exam_province) VALUES
(2, 'freshuser', '123456', NULL, NULL, NULL);
