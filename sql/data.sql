USE college_recommendation;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE major_offering;
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

INSERT INTO university (id, name, province, city, tier, nature, belong, logo_id, is_985, is_211, is_double_first_class, tags) VALUES
(101, '清华大学', '北京', '北京', '985', '公办', '教育部', 1, 1, 1, 1, '综合类'),
(102, '北京大学', '北京', '北京', '985', '公办', '教育部', 2, 1, 1, 1, '综合类'),
(103, '浙江大学', '浙江', '杭州', '985', '公办', '教育部', 3, 1, 1, 1, '综合类'),
(104, '复旦大学', '上海', '上海', '985', '公办', '教育部', 4, 1, 1, 1, '综合类'),
(105, '上海交通大学', '上海', '上海', '985', '公办', '教育部', 5, 1, 1, 1, '综合类'),
(106, '南京大学', '江苏', '南京', '985', '公办', '教育部', 6, 1, 1, 1, '综合类'),
(107, '中国科学技术大学', '安徽', '合肥', '985', '公办', '中国科学院', 7, 1, 1, 1, '理工类'),
(108, '华中科技大学', '湖北', '武汉', '985', '公办', '教育部', 8, 1, 1, 1, '综合类'),
(109, '武汉大学', '湖北', '武汉', '985', '公办', '教育部', 9, 1, 1, 1, '综合类'),
(110, '中山大学', '广东', '广州', '985', '公办', '教育部', 10, 1, 1, 1, '综合类'),
(111, '哈尔滨工业大学', '黑龙江', '哈尔滨', '985', '公办', '工业和信息化部', 11, 1, 1, 1, '理工类'),
(112, '西安交通大学', '陕西', '西安', '985', '公办', '教育部', 12, 1, 1, 1, '综合类'),
(113, '同济大学', '上海', '上海', '985', '公办', '教育部', 13, 1, 1, 1, '理工类'),
(114, '北京航空航天大学', '北京', '北京', '985', '公办', '工业和信息化部', 14, 1, 1, 1, '理工类'),
(115, '天津大学', '天津', '天津', '985', '公办', '教育部', 15, 1, 1, 1, '理工类'),
(116, '华南理工大学', '广东', '广州', '985', '公办', '教育部', 16, 1, 1, 1, '理工类'),
(117, '东南大学', '江苏', '南京', '985', '公办', '教育部', 17, 1, 1, 1, '综合类'),
(118, '大连理工大学', '辽宁', '大连', '985', '公办', '教育部', 18, 1, 1, 1, '理工类'),
(119, '山东大学', '山东', '济南', '985', '公办', '教育部', 19, 1, 1, 1, '综合类'),
(120, '厦门大学', '福建', '厦门', '985', '公办', '教育部', 20, 1, 1, 1, '综合类');

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

INSERT INTO major_admission_cutoff (university_id, major_name, admission_year, province, subject_type, cutoff_score, min_rank) VALUES
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
(3, '通信工程', 2025, '浙江', '物理', 598, 42300);

UPDATE major_admission_cutoff
SET professional_group_code = '101', professional_group_name = '计算机与智能类',
    primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 1 AND major_name IN ('计算机科学与技术', '软件工程', '人工智能');
UPDATE major_admission_cutoff
SET professional_group_code = '102', professional_group_name = '电子信息类',
    primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 1 AND major_name IN ('数据科学与大数据技术', '电子信息工程', '通信工程');
UPDATE major_admission_cutoff
SET professional_group_code = '201', professional_group_name = '工科试验班',
    primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 2 AND major_name IN ('计算机科学与技术', '软件工程', '数据科学与大数据技术');
UPDATE major_admission_cutoff
SET professional_group_code = '202', professional_group_name = '智能科学类',
    primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 2 AND major_name IN ('人工智能', '电子信息工程', '通信工程');
UPDATE major_admission_cutoff
SET professional_group_code = '301', professional_group_name = '信息技术类',
    primary_subject = 'PHYSICS', elective_subjects = 'CHEMISTRY'
WHERE university_id = 3 AND major_name IN ('计算机科学与技术', '软件工程', '人工智能');
UPDATE major_admission_cutoff
SET professional_group_code = '302', professional_group_name = '电子工程类',
    primary_subject = 'PHYSICS', elective_subjects = 'BIOLOGY'
WHERE university_id = 3 AND major_name IN ('数据科学与大数据技术', '电子信息工程', '通信工程');

INSERT INTO major (name)
SELECT DISTINCT major_name
FROM major_admission_cutoff;

INSERT INTO major (name, major_code, category, subcategory, duration, degree_type, gender_ratio, average_salary, popularity, demo_data) VALUES
('临床医学', '100201K', '医学', '临床医学类', '五年', '医学学士', '44:56', '12.8万', 1, 1),
('法学', '030101K', '法学', '法学类', '四年', '法学学士', '37:63', '10.5万', 2, 1),
('口腔医学', '100301K', '医学', '口腔医学类', '五年', '医学学士', '41:59', '13.9万', 3, 1),
('计算机科学与技术', '080901', '工学', '计算机类', '四年', '工学学士', '71:29', '14.2万', 4, 1),
('电气工程及其自动化', '080601', '工学', '电气类', '四年', '工学学士', '76:24', '11.6万', 5, 1),
('心理学', '071101', '理学', '心理学类', '四年', '理学学士', '33:67', '9.8万', 6, 1),
('人工智能', '080717T', '工学', '电子信息类', '四年', '工学学士', '74:26', '15.6万', 7, 1),
('汉语言文学', '050101', '文学', '中国语言文学类', '四年', '文学学士', '26:74', '8.4万', 8, 1),
('自动化', '080801', '工学', '自动化类', '四年', '工学学士', '73:27', '11.2万', 9, 1),
('会计学', '120203K', '管理学', '工商管理类', '四年', '管理学学士', '30:70', '9.6万', 10, 1),
('数字媒体技术', '080906', '工学', '计算机类', '四年', '工学学士', '58:42', '10.9万', 11, 1),
('软件工程', '080902', '工学', '计算机类', '四年', '工学学士', '72:28', '13.8万', 12, 1),
('动物医学', '090401', '农学', '动物医学类', '五年', '农学学士', '45:55', '8.9万', 13, 1),
('中医学', '100501K', '医学', '中医学类', '五年', '医学学士', '39:61', '9.4万', 14, 1),
('土木工程', '081001', '工学', '土木类', '四年', '工学学士', '82:18', '10.1万', 15, 1),
('生物医学工程', '082601', '工学', '生物医学工程类', '四年', '工学学士', '61:39', '11.8万', 16, 1),
('护理学', '101101', '医学', '护理学类', '四年', '理学学士', '18:82', '8.6万', 17, 1),
('机械设计制造及其自动化', '080202', '工学', '机械类', '四年', '工学学士', '84:16', '10.4万', 18, 1),
('电子信息工程', '080701', '工学', '电子信息类', '四年', '工学学士', '70:30', '12.1万', 19, 1),
('金融学', '020301K', '经济学', '金融学类', '四年', '经济学学士', '46:54', '11.3万', 20, 1),
('英语', '050201', '文学', '外国语言文学类', '四年', '文学学士', '22:78', '8.7万', 21, 1),
('新闻学', '050301', '文学', '新闻传播学类', '四年', '文学学士', '31:69', '9.2万', 22, 1),
('数学与应用数学', '070101', '理学', '数学类', '四年', '理学学士', '63:37', '10.8万', 23, 1),
('数据科学与大数据技术', '080910T', '工学', '计算机类', '四年', '工学学士', '68:32', '14.8万', 24, 1),
('学前教育', '040106', '教育学', '教育学类', '四年', '教育学学士', '12:88', '7.6万', 25, 1),
('视觉传达设计', '130502', '艺术学', '设计学类', '四年', '艺术学学士', '36:64', '8.9万', 26, 1),
('经济学', '020101', '经济学', '经济学类', '四年', '经济学学士', '48:52', '10.2万', 27, 1),
('历史学', '060101', '历史学', '历史学类', '四年', '历史学学士', '42:58', '7.9万', 28, 1),
('哲学', '010101', '哲学', '哲学类', '四年', '哲学学士', '52:48', '8.2万', 29, 1),
('通信工程', '080703', '工学', '电子信息类', '四年', '工学学士', '72:28', '10.8万', 30, 1)
ON DUPLICATE KEY UPDATE
major_code = VALUES(major_code), category = VALUES(category), subcategory = VALUES(subcategory),
duration = VALUES(duration), degree_type = VALUES(degree_type), gender_ratio = VALUES(gender_ratio),
average_salary = VALUES(average_salary), popularity = VALUES(popularity), demo_data = VALUES(demo_data);

UPDATE major
SET description = CONCAT(name, '专业属于', category, '门类下的', subcategory, '，主要培养掌握相关基础理论与专业技能、能够解决实际问题的复合型人才。'),
    employment_directions = CASE category
      WHEN '工学' THEN '研发工程师,技术工程师,产品经理,科研人员'
      WHEN '医学' THEN '医疗机构,医学科研,公共卫生,健康管理'
      WHEN '法学' THEN '律师,法务专员,公务员,合规审查'
      WHEN '文学' THEN '教育,编辑出版,内容运营,公共文化'
      WHEN '经济学' THEN '金融机构,经济分析,风险管理,公共部门'
      WHEN '管理学' THEN '企业管理,财务分析,市场运营,公共管理'
      ELSE '教育科研,企事业单位,公共服务,继续深造'
    END
WHERE demo_data = 1;

UPDATE major_admission_cutoff mac
JOIN major m ON m.name = mac.major_name
SET mac.major_id = m.id;

INSERT IGNORE INTO major_offering (major_id, university_id)
SELECT m.id, u.id
FROM major m
JOIN university u ON u.logo_id BETWEEN 1 AND 20
WHERE m.demo_data = 1;

INSERT INTO score_rank_mapping (mapping_year, province, subject_type, score, rank_value) VALUES
(2025, '浙江', '物理', 620, 26000),
(2025, '浙江', '物理', 630, 22000),
(2025, '浙江', '物理', 610, 31000);

INSERT INTO users (id, username, password, score, subject_type, exam_province, role) VALUES
(1, 'testuser', '123456', NULL, NULL, NULL, 'USER'),
(2, 'freshuser', '123456', NULL, NULL, NULL, 'USER'),
(3, 'adminuser', '123456', 650, '物理', '浙江', 'ADMIN');
