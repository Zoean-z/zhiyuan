INSERT INTO university (id, name, province, tier, is_985, is_211, is_double_first_class, tags) VALUES
(1, '浙江大学', '浙江', '985', TRUE, TRUE, TRUE, '综合类'),
(2, '宁波大学', '浙江', '双一流', FALSE, FALSE, TRUE, '综合类'),
(3, '杭州电子科技大学', '浙江', '重点', FALSE, FALSE, FALSE, '工科'),
(4, '温州大学', '浙江', '普通', FALSE, FALSE, FALSE, '综合类'),
(5, '南京师范大学', '江苏', '211', FALSE, TRUE, TRUE, '师范类'),
(6, '浙江中医药大学', '浙江', '普通', FALSE, FALSE, FALSE, '医药类');

INSERT INTO university (id, name, province, city, tier, nature, belong, logo_id, is_985, is_211, is_double_first_class, tags) VALUES
(101, '清华大学', '北京', '北京', '985', '公办', '教育部', 1, TRUE, TRUE, TRUE, '综合类'),
(102, '北京大学', '北京', '北京', '985', '公办', '教育部', 2, TRUE, TRUE, TRUE, '综合类'),
(103, '浙江大学', '浙江', '杭州', '985', '公办', '教育部', 3, TRUE, TRUE, TRUE, '综合类'),
(104, '复旦大学', '上海', '上海', '985', '公办', '教育部', 4, TRUE, TRUE, TRUE, '综合类'),
(105, '上海交通大学', '上海', '上海', '985', '公办', '教育部', 5, TRUE, TRUE, TRUE, '综合类'),
(106, '南京大学', '江苏', '南京', '985', '公办', '教育部', 6, TRUE, TRUE, TRUE, '综合类'),
(107, '中国科学技术大学', '安徽', '合肥', '985', '公办', '中国科学院', 7, TRUE, TRUE, TRUE, '理工类'),
(108, '华中科技大学', '湖北', '武汉', '985', '公办', '教育部', 8, TRUE, TRUE, TRUE, '综合类'),
(109, '武汉大学', '湖北', '武汉', '985', '公办', '教育部', 9, TRUE, TRUE, TRUE, '综合类'),
(110, '中山大学', '广东', '广州', '985', '公办', '教育部', 10, TRUE, TRUE, TRUE, '综合类'),
(111, '哈尔滨工业大学', '黑龙江', '哈尔滨', '985', '公办', '工业和信息化部', 11, TRUE, TRUE, TRUE, '理工类'),
(112, '西安交通大学', '陕西', '西安', '985', '公办', '教育部', 12, TRUE, TRUE, TRUE, '综合类'),
(113, '同济大学', '上海', '上海', '985', '公办', '教育部', 13, TRUE, TRUE, TRUE, '理工类'),
(114, '北京航空航天大学', '北京', '北京', '985', '公办', '工业和信息化部', 14, TRUE, TRUE, TRUE, '理工类'),
(115, '天津大学', '天津', '天津', '985', '公办', '教育部', 15, TRUE, TRUE, TRUE, '理工类'),
(116, '华南理工大学', '广东', '广州', '985', '公办', '教育部', 16, TRUE, TRUE, TRUE, '理工类'),
(117, '东南大学', '江苏', '南京', '985', '公办', '教育部', 17, TRUE, TRUE, TRUE, '综合类'),
(118, '大连理工大学', '辽宁', '大连', '985', '公办', '教育部', 18, TRUE, TRUE, TRUE, '理工类'),
(119, '山东大学', '山东', '济南', '985', '公办', '教育部', 19, TRUE, TRUE, TRUE, '综合类'),
(120, '厦门大学', '福建', '厦门', '985', '公办', '教育部', 20, TRUE, TRUE, TRUE, '综合类');

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

INSERT INTO major_admission_cutoff (university_id, major_name, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
(1, '软件工程', 2025, '浙江', '物理', 654, 5600),
(1, '人工智能', 2025, '浙江', '物理', 652, 6100),
(1, '数据科学与大数据技术', 2025, '浙江', '物理', 651, 6400),
(1, '电子信息工程', 2025, '浙江', '物理', 649, 6900),
(1, '通信工程', 2025, '浙江', '物理', 647, 7300),
(2, '数据科学与大数据技术', 2025, '浙江', '物理', 613, 27500),
(2, '人工智能', 2025, '浙江', '物理', 612, 28200),
(2, '电子信息工程', 2025, '浙江', '物理', 609, 30500),
(2, '通信工程', 2025, '浙江', '物理', 608, 31400),
(3, '软件工程', 2025, '浙江', '物理', 602, 37200),
(3, '人工智能', 2025, '浙江', '物理', 601, 38600),
(3, '数据科学与大数据技术', 2025, '浙江', '物理', 599, 41000),
(3, '电子信息工程', 2025, '浙江', '物理', 600, 40000),
(3, '通信工程', 2025, '浙江', '物理', 598, 42300);

UPDATE major_admission_cutoff SET professional_group_code = '101', professional_group_name = '计算机与智能类', primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 1 AND major_name IN ('计算机科学与技术', '软件工程', '人工智能');
UPDATE major_admission_cutoff SET professional_group_code = '102', professional_group_name = '电子信息类', primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 1 AND major_name IN ('数据科学与大数据技术', '电子信息工程', '通信工程');
UPDATE major_admission_cutoff SET professional_group_code = '201', professional_group_name = '工科试验班', primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 2 AND major_name IN ('计算机科学与技术', '软件工程', '数据科学与大数据技术');
UPDATE major_admission_cutoff SET professional_group_code = '202', professional_group_name = '智能科学类', primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 2 AND major_name IN ('人工智能', '电子信息工程', '通信工程');
UPDATE major_admission_cutoff SET professional_group_code = '301', professional_group_name = '信息技术类', primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 3 AND major_name IN ('计算机科学与技术', '软件工程', '人工智能');
UPDATE major_admission_cutoff SET professional_group_code = '302', professional_group_name = '电子工程类', primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 3 AND major_name IN ('数据科学与大数据技术', '电子信息工程', '通信工程');

INSERT INTO major (name)
SELECT DISTINCT major_name
FROM major_admission_cutoff;

MERGE INTO major (name, major_code, category, subcategory, duration, degree_type, gender_ratio, average_salary, popularity, demo_data) KEY(name) VALUES
('临床医学', '100201K', '医学', '临床医学类', '五年', '医学学士', '44:56', '12.8万', 1, TRUE),
('法学', '030101K', '法学', '法学类', '四年', '法学学士', '37:63', '10.5万', 2, TRUE),
('口腔医学', '100301K', '医学', '口腔医学类', '五年', '医学学士', '41:59', '13.9万', 3, TRUE),
('计算机科学与技术', '080901', '工学', '计算机类', '四年', '工学学士', '71:29', '14.2万', 4, TRUE),
('电气工程及其自动化', '080601', '工学', '电气类', '四年', '工学学士', '76:24', '11.6万', 5, TRUE),
('人工智能', '080717T', '工学', '电子信息类', '四年', '工学学士', '74:26', '15.6万', 7, TRUE),
('汉语言文学', '050101', '文学', '中国语言文学类', '四年', '文学学士', '26:74', '8.4万', 8, TRUE),
('自动化', '080801', '工学', '自动化类', '四年', '工学学士', '73:27', '11.2万', 9, TRUE),
('会计学', '120203K', '管理学', '工商管理类', '四年', '管理学学士', '30:70', '9.6万', 10, TRUE),
('软件工程', '080902', '工学', '计算机类', '四年', '工学学士', '72:28', '13.8万', 12, TRUE),
('动物医学', '090401', '农学', '动物医学类', '五年', '农学学士', '45:55', '8.9万', 13, TRUE),
('中医学', '100501K', '医学', '中医学类', '五年', '医学学士', '39:61', '9.4万', 14, TRUE),
('土木工程', '081001', '工学', '土木类', '四年', '工学学士', '82:18', '10.1万', 15, TRUE),
('护理学', '101101', '医学', '护理学类', '四年', '理学学士', '18:82', '8.6万', 17, TRUE),
('电子信息工程', '080701', '工学', '电子信息类', '四年', '工学学士', '70:30', '12.1万', 19, TRUE),
('金融学', '020301K', '经济学', '金融学类', '四年', '经济学学士', '46:54', '11.3万', 20, TRUE),
('英语', '050201', '文学', '外国语言文学类', '四年', '文学学士', '22:78', '8.7万', 21, TRUE),
('新闻学', '050301', '文学', '新闻传播学类', '四年', '文学学士', '31:69', '9.2万', 22, TRUE),
('数学与应用数学', '070101', '理学', '数学类', '四年', '理学学士', '63:37', '10.8万', 23, TRUE),
('数据科学与大数据技术', '080910T', '工学', '计算机类', '四年', '工学学士', '68:32', '14.8万', 24, TRUE),
('学前教育', '040106', '教育学', '教育学类', '四年', '教育学学士', '12:88', '7.6万', 25, TRUE),
('视觉传达设计', '130502', '艺术学', '设计学类', '四年', '艺术学学士', '36:64', '8.9万', 26, TRUE),
('经济学', '020101', '经济学', '经济学类', '四年', '经济学学士', '48:52', '10.2万', 27, TRUE),
('历史学', '060101', '历史学', '历史学类', '四年', '历史学学士', '42:58', '7.9万', 28, TRUE),
('哲学', '010101', '哲学', '哲学类', '四年', '哲学学士', '52:48', '8.2万', 29, TRUE),
('通信工程', '080703', '工学', '电子信息类', '四年', '工学学士', '72:28', '10.8万', 30, TRUE);

UPDATE major
SET description = name || '专业属于' || category || '门类下的' || subcategory || '，主要培养掌握相关基础理论与专业技能、能够解决实际问题的复合型人才。',
    employment_directions = CASE category
      WHEN '工学' THEN '研发工程师,技术工程师,产品经理,科研人员'
      WHEN '医学' THEN '医疗机构,医学科研,公共卫生,健康管理'
      WHEN '法学' THEN '律师,法务专员,公务员,合规审查'
      WHEN '文学' THEN '教育,编辑出版,内容运营,公共文化'
      WHEN '经济学' THEN '金融机构,经济分析,风险管理,公共部门'
      WHEN '管理学' THEN '企业管理,财务分析,市场运营,公共管理'
      ELSE '教育科研,企事业单位,公共服务,继续深造'
    END
WHERE demo_data = TRUE;

UPDATE major_admission_cutoff
SET major_id = (
    SELECT m.id
    FROM major m
    WHERE m.name = major_admission_cutoff.major_name
);

INSERT INTO major_offering (major_id, university_id)
SELECT m.id, u.id
FROM major m
CROSS JOIN university u
WHERE m.demo_data = TRUE AND u.logo_id BETWEEN 1 AND 20;

INSERT INTO score_rank_mapping (mapping_year, province, subject_type, score, rank_value) VALUES
(2025, '浙江', '物理', 620, 26000),
(2025, '浙江', '物理', 630, 22000),
(2025, '浙江', '物理', 610, 31000);

-- Fixed competition-demo cutoffs. They intentionally use a newer year so the
-- recommendation mappers select the same 20 universities as the public pages.
INSERT INTO admission_cutoff (university_id, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
(101, 2026, '浙江', '物理', 690, 600),
(102, 2026, '浙江', '物理', 688, 720),
(103, 2026, '浙江', '物理', 674, 2200),
(104, 2026, '浙江', '物理', 678, 1600),
(105, 2026, '浙江', '物理', 680, 1300),
(106, 2026, '浙江', '物理', 665, 4100),
(107, 2026, '浙江', '物理', 672, 2600),
(108, 2026, '浙江', '物理', 653, 8200),
(109, 2026, '浙江', '物理', 650, 9300),
(110, 2026, '浙江', '物理', 641, 14000),
(111, 2026, '浙江', '物理', 638, 15800),
(112, 2026, '浙江', '物理', 647, 10800),
(113, 2026, '浙江', '物理', 660, 5500),
(114, 2026, '浙江', '物理', 668, 3400),
(115, 2026, '浙江', '物理', 632, 20100),
(116, 2026, '浙江', '物理', 629, 22600),
(117, 2026, '浙江', '物理', 644, 12300),
(118, 2026, '浙江', '物理', 625, 28000),
(119, 2026, '浙江', '物理', 621, 30400),
(120, 2026, '浙江', '物理', 635, 17800),
(101, 2026, '浙江', '历史', 684, 760),
(102, 2026, '浙江', '历史', 686, 650),
(103, 2026, '浙江', '历史', 669, 2600),
(104, 2026, '浙江', '历史', 676, 1500),
(105, 2026, '浙江', '历史', 672, 2100),
(106, 2026, '浙江', '历史', 665, 3400),
(107, 2026, '浙江', '历史', 618, 30500),
(108, 2026, '浙江', '历史', 645, 10300),
(109, 2026, '浙江', '历史', 659, 4900),
(110, 2026, '浙江', '历史', 655, 6200),
(111, 2026, '浙江', '历史', 633, 17000),
(112, 2026, '浙江', '历史', 642, 11800),
(113, 2026, '浙江', '历史', 648, 9000),
(114, 2026, '浙江', '历史', 636, 15100),
(115, 2026, '浙江', '历史', 630, 19000),
(116, 2026, '浙江', '历史', 627, 21400),
(117, 2026, '浙江', '历史', 639, 13400),
(118, 2026, '浙江', '历史', 620, 28000),
(119, 2026, '浙江', '历史', 624, 24100),
(120, 2026, '浙江', '历史', 652, 7400);

INSERT INTO major_admission_cutoff
  (university_id, major_id, major_name, admission_year, province, subject_type,
   cutoff_score, min_rank, professional_group_code, professional_group_name,
   primary_subject, elective_subjects)
SELECT c.university_id, m.id, m.name, 2026, '浙江', '物理',
       c.cutoff_score + CASE m.major_code
         WHEN '080901' THEN 8 WHEN '080902' THEN 5
         WHEN '080717T' THEN 6 ELSE 2 END,
       GREATEST(1, c.min_rank - CASE m.major_code
         WHEN '080901' THEN 1200 WHEN '080902' THEN 800
         WHEN '080717T' THEN 1000 ELSE 400 END),
       CASE WHEN m.major_code = '080601'
         THEN CONCAT(u.logo_id, '02') ELSE CONCAT(u.logo_id, '01') END,
       CASE WHEN m.major_code = '080601'
         THEN '电子与电气类' ELSE '计算机与智能类' END,
       'PHYSICS', 'CHEMISTRY'
FROM admission_cutoff c
JOIN university u ON u.id = c.university_id
JOIN major m ON m.major_code IN ('080901', '080902', '080717T', '080601')
WHERE c.admission_year = 2026 AND c.province = '浙江' AND c.subject_type = '物理';

INSERT INTO major_admission_cutoff
  (university_id, major_id, major_name, admission_year, province, subject_type,
   cutoff_score, min_rank, professional_group_code, professional_group_name,
   primary_subject, elective_subjects)
SELECT c.university_id, m.id, m.name, 2026, '浙江', '历史',
       c.cutoff_score + CASE m.major_code
         WHEN '030101K' THEN 6 WHEN '050101' THEN 4
         WHEN '020301K' THEN 3 ELSE 2 END,
       GREATEST(1, c.min_rank - CASE m.major_code
         WHEN '030101K' THEN 1000 WHEN '050101' THEN 700
         WHEN '020301K' THEN 500 ELSE 300 END),
       CONCAT(u.logo_id, '03'), '人文社科类', 'HISTORY', NULL
FROM admission_cutoff c
JOIN university u ON u.id = c.university_id
JOIN major m ON m.major_code IN ('030101K', '050101', '020301K', '120203K')
WHERE c.admission_year = 2026 AND c.province = '浙江' AND c.subject_type = '历史';

INSERT INTO score_rank_mapping (mapping_year, province, subject_type, score, rank_value) VALUES
(2026, '浙江', '物理', 610, 31000),
(2026, '浙江', '物理', 620, 26000),
(2026, '浙江', '物理', 630, 22000);

INSERT INTO users (id, username, password, score, subject_type, exam_province, role) VALUES
(1, 'testuser', '123456', NULL, NULL, NULL, 'USER');
INSERT INTO users (id, username, password, score, subject_type, exam_province, role) VALUES
(2, 'freshuser', '123456', NULL, NULL, NULL, 'USER');
INSERT INTO users (id, username, password, score, subject_type, exam_province, role) VALUES
(3, 'adminuser', '123456', 650, '物理', '浙江', 'ADMIN');

UPDATE users SET role = 'ADMIN' WHERE username = 'adminuser';
