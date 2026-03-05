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
(2, 2025, '浙江', '历史', 618, 21000);

INSERT INTO users (id, username, password, score) VALUES
(1, 'testuser', '123456', NULL);
INSERT INTO users (id, username, password, score) VALUES (2, 'freshuser', '123456', NULL);
