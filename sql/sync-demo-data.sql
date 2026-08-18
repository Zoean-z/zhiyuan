-- Non-destructive, idempotent sync for the public school/major showcase.
-- It intentionally does not truncate or modify users, plans, logs, cutoffs, or AI settings.
USE college_recommendation;
SET NAMES utf8mb4;

INSERT INTO university
  (id, name, province, city, tier, nature, belong, logo_id, is_985, is_211, is_double_first_class, tags)
VALUES
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
(120, '厦门大学', '福建', '厦门', '985', '公办', '教育部', 20, 1, 1, 1, '综合类')
ON DUPLICATE KEY UPDATE
name = VALUES(name), province = VALUES(province), city = VALUES(city), tier = VALUES(tier),
nature = VALUES(nature), belong = VALUES(belong), logo_id = VALUES(logo_id),
is_985 = VALUES(is_985), is_211 = VALUES(is_211),
is_double_first_class = VALUES(is_double_first_class), tags = VALUES(tags);

INSERT INTO major
  (name, major_code, category, subcategory, duration, degree_type, gender_ratio, average_salary, popularity, demo_data)
VALUES
('计算机科学与技术', '080901', '工学', '计算机类', '四年', '工学学士', '71:29', '8.4万', 1, 1),
('软件工程', '080902', '工学', '计算机类', '四年', '工学学士', '72:28', '9.1万', 2, 1),
('人工智能', '080717T', '工学', '电子信息类', '四年', '工学学士', '74:26', '9.8万', 3, 1),
('数据科学与大数据技术', '080910T', '工学', '计算机类', '四年', '工学学士', '68:32', '10.5万', 4, 1),
('电子信息工程', '080701', '工学', '电子信息类', '四年', '工学学士', '70:30', '11.2万', 5, 1),
('电气工程及其自动化', '080601', '工学', '电气类', '四年', '工学学士', '76:24', '11.9万', 6, 1),
('自动化', '080801', '工学', '自动化类', '四年', '工学学士', '71:29', '12.6万', 7, 1),
('机械设计制造及其自动化', '080202', '工学', '机械类', '四年', '工学学士', '72:28', '13.3万', 8, 1),
('土木工程', '081001', '工学', '土木类', '四年', '工学学士', '74:26', '8.4万', 9, 1),
('数学与应用数学', '070101', '理学', '数学类', '四年', '理学学士', '68:32', '9.1万', 10, 1),
('临床医学', '100201K', '医学', '临床医学类', '五年', '医学学士', '70:30', '9.8万', 11, 1),
('口腔医学', '100301K', '医学', '口腔医学类', '五年', '医学学士', '76:24', '10.5万', 12, 1),
('护理学', '101101', '医学', '护理学类', '四年', '理学学士', '71:29', '11.2万', 13, 1),
('中医学', '100501K', '医学', '中医学类', '五年', '医学学士', '72:28', '11.9万', 14, 1),
('法学', '030101K', '法学', '法学类', '四年', '法学学士', '74:26', '12.6万', 15, 1),
('汉语言文学', '050101', '文学', '中国语言文学类', '四年', '文学学士', '68:32', '13.3万', 16, 1),
('英语', '050201', '文学', '外国语言文学类', '四年', '文学学士', '70:30', '8.4万', 17, 1),
('新闻学', '050301', '文学', '新闻传播学类', '四年', '文学学士', '76:24', '9.1万', 18, 1),
('金融学', '020301K', '经济学', '金融学类', '四年', '经济学学士', '71:29', '9.8万', 19, 1),
('经济学', '020101', '经济学', '经济学类', '四年', '经济学学士', '72:28', '10.5万', 20, 1),
('会计学', '120203K', '管理学', '工商管理类', '四年', '管理学学士', '74:26', '11.2万', 21, 1),
('学前教育', '040106', '教育学', '教育学类', '四年', '教育学学士', '68:32', '11.9万', 22, 1),
('动物医学', '090401', '农学', '动物医学类', '五年', '农学学士', '70:30', '12.6万', 23, 1),
('视觉传达设计', '130502', '艺术学', '设计学类', '四年', '艺术学学士', '76:24', '13.3万', 24, 1)
ON DUPLICATE KEY UPDATE
major_code = VALUES(major_code), category = VALUES(category), subcategory = VALUES(subcategory),
duration = VALUES(duration), degree_type = VALUES(degree_type), gender_ratio = VALUES(gender_ratio),
average_salary = VALUES(average_salary), popularity = VALUES(popularity), demo_data = VALUES(demo_data);

-- Older demo seeds contained six extra majors that are not present in the current public dataset.
UPDATE major
SET major_code = NULL, demo_data = 0
WHERE name IN ('心理学', '数字媒体技术', '生物医学工程', '历史学', '哲学', '通信工程');

UPDATE major
SET description = CONCAT(name, '专业属于', category, '门类下的', subcategory,
        '，主要培养掌握相关基础理论与专业技能、能够解决实际问题的复合型人才。'),
    employment_directions = CASE category
      WHEN '工学' THEN '研发工程师,技术工程师,产品经理,科研人员'
      WHEN '医学' THEN '医疗机构,医学科研,公共卫生,健康管理'
      WHEN '法学' THEN '律师,法务专员,公务员,合规审查'
      WHEN '文学' THEN '教育,编辑出版,内容运营,公共文化'
      WHEN '经济学' THEN '金融机构,经济分析,风险管理,公共部门'
      WHEN '管理学' THEN '企业管理,财务分析,市场运营,公共管理'
      ELSE '教育科研,企事业单位,公共服务,继续深造'
    END
WHERE major_code IN (
  '080901','080902','080717T','080910T','080701','080601','080801','080202',
  '081001','070101','100201K','100301K','101101','100501K','030101K','050101',
  '050201','050301','020301K','020101','120203K','040106','090401','130502'
);

INSERT IGNORE INTO major_offering (major_id, university_id)
SELECT m.id, u.id
FROM major m
JOIN university u ON u.logo_id BETWEEN 1 AND 20
WHERE m.major_code IN (
  '080901','080902','080717T','080910T','080701','080601','080801','080202',
  '081001','070101','100201K','100301K','101101','100501K','030101K','050101',
  '050201','050301','020301K','020101','120203K','040106','090401','130502'
);

-- Competition demo data only. These deterministic 2026 Zhejiang cutoffs are not
-- official admission data. Replacing only this slice keeps the script idempotent
-- without touching legacy cutoffs, users, recommendation logs, or saved plans.
DELETE FROM major_admission_cutoff
WHERE admission_year = 2026
  AND province = '浙江'
  AND university_id BETWEEN 101 AND 120;

DELETE FROM admission_cutoff
WHERE admission_year = 2026
  AND province = '浙江'
  AND university_id BETWEEN 101 AND 120;

INSERT INTO admission_cutoff
  (university_id, admission_year, province, subject_type, cutoff_score, min_rank)
VALUES
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

-- Four deterministic demonstration majors per university and subject stream.
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
WHERE c.admission_year = 2026
  AND c.province = '浙江'
  AND c.subject_type = '物理'
  AND c.university_id BETWEEN 101 AND 120;

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
WHERE c.admission_year = 2026
  AND c.province = '浙江'
  AND c.subject_type = '历史'
  AND c.university_id BETWEEN 101 AND 120;

INSERT INTO score_rank_mapping
  (mapping_year, province, subject_type, score, rank_value)
VALUES
(2026, '浙江', '物理', 610, 31000),
(2026, '浙江', '物理', 620, 26000),
(2026, '浙江', '物理', 630, 22000)
ON DUPLICATE KEY UPDATE rank_value = VALUES(rank_value);
