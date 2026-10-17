# -*- coding: utf-8 -*-
"""W4 前端补全管线：软科院校属性 + 教育部专业目录 + 他省 2024 校线。
SQL 输出到 stdout（由调用方重定向），本脚本不做任何文件写入。
- 软科 bcur 主榜 -> university.soft_ranking / school_type / is_985 / is_211 / is_double_first_class / tags
- 教育部 2024 本科专业目录 PDF -> major 扩容（按名防重；描述沿用站内模板风格）
- 2024 school-admission（广东/江苏/湖北/四川）-> admission_cutoff 历史深度
"""
import csv
import json
import os.path
import re

TMP = r"C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan\tmp"
BCUR = os.path.join(TMP, "univ-attrs", "bcur-11.json")
PDF = os.path.join(TMP, "moe-catalog-2024.pdf")
GAOKAO = os.path.join(TMP, "gaokao-compass", "2024")

SUBJ = {"物理类": "物理", "历史类": "历史", "理科": "物理", "文科": "历史"}
BATCH_RANK = {"本科批": 1, "本科提前批": 2, "特殊类型招生批": 3, "专科批": 4, "专科提前批": 5}
TYPE_TAGS = {"综合": "综合类", "理工": "理工类", "师范": "师范类", "农业": "农林类", "林业": "农林类",
             "医药": "医药类", "财经": "财经类", "政法": "政法类", "语言": "语言类",
             "艺术": "艺术类", "体育": "体育类", "民族": "民族类"}
CODE2CATEGORY = {"01": "哲学", "02": "经济学", "03": "法学", "04": "教育学", "05": "文学",
                 "06": "历史学", "07": "理学", "08": "工学", "09": "农学", "10": "医学",
                 "11": "军事学", "12": "管理学", "13": "艺术学"}


def esc(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "\\'") + "'"


def to_int(value):
    text = str(value or "").strip()
    if not text or text == "<NA>":
        return None
    try:
        return int(float(text))
    except ValueError:
        return None


def norm(value):
    return (value or "").strip().replace("（", "(").replace("）", ")")


# ================= 1. 软科院校属性 =================
with open(BCUR, encoding="utf-8") as handle:
    rankings = json.load(handle)["data"]["rankings"]

attr_updates = []
for item in rankings:
    name = norm(item.get("univNameCn"))
    if not name:
        continue
    tags = item.get("univTags") or []
    category = (item.get("univCategory") or "").strip()
    school_type = TYPE_TAGS.get(category, category or None)
    rank = to_int(str(item.get("ranking") or "").split(".")[0])
    is_double = 1 if "双一流" in tags else 0
    is_985 = 1 if "985" in tags else 0
    is_211 = 1 if "211" in tags else 0
    tag_parts = [tag for tag in ("985", "211", "双一流") if tag in tags]
    if category:
        tag_parts.append(TYPE_TAGS.get(category, category + "类"))
    attr_updates.append((name, rank, school_type, is_985, is_211, is_double, "|".join(tag_parts) or None))

# ================= 2. 教育部专业目录 =================
from pypdf import PdfReader

reader = PdfReader(PDF)
majors = {}
order = []
seq_re = re.compile(r"^\d{2,4}$")
code_re = re.compile(r"^0[1-9][A-Z]{0,2}[0-9]{4}[A-Z]{0,2}$")
year_re = re.compile(r"^[二三四五]年$")
for page in reader.pages:
    tokens = [t.strip() for t in page.extract_text().split("\n") if t.strip()]
    i = 0
    while i < len(tokens):
        if not seq_re.match(tokens[i]) or i + 3 >= len(tokens):
            i += 1
            continue
        if not code_re.match(tokens[i + 2]):
            i += 1
            continue
        seq_no = int(tokens[i])
        major_class = tokens[i + 1]
        code = tokens[i + 2]
        name = tokens[i + 3]
        i += 4
        degree_parts = []
        duration = None
        while i < len(tokens):
            token = tokens[i]
            if year_re.match(token):
                duration = token
                i += 1
                break
            if seq_re.match(token) and i + 3 < len(tokens) and code_re.match(tokens[i + 2]):
                break
            degree_parts.append(token)
            i += 1
        degree = re.sub(r"\s+", "", ",".join(degree_parts)).replace(",", "、")
        if name and code not in majors:
            prefix = code[:2]
            majors[code] = {
                "name": name, "class": major_class, "code": code,
                "category": CODE2CATEGORY.get(prefix, major_class),
                "degree": degree + "学士" if degree else None,
                "duration": duration, "seq": seq_no}
            order.append(code)

# ================= 3. 2024 校线（广东/江苏/湖北/四川） =================
PROV = {"guangdong": "广东", "jiangsu": "江苏", "hubei": "湖北", "sichuan": "四川"}
cutoffs = {}
for pinyin, province_cn in PROV.items():
    path = os.path.join(GAOKAO, pinyin, "school-admission.csv")
    if not os.path.exists(path):
        continue
    with open(path, encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            subject = SUBJ.get((row.get("category") or "").strip())
            if subject is None:
                continue
            batch = (row.get("batch") or "").strip()
            priority = BATCH_RANK.get(batch)
            if priority is None:
                continue
            name = norm(row.get("university_name") or "")
            min_score = to_int(row.get("min_score"))
            if not name or min_score is None:
                continue
            key = (province_cn, name, subject)
            candidate = (priority, min_score, to_int(row.get("min_rank")))
            if key not in cutoffs or candidate[0] < cutoffs[key][0]:
                cutoffs[key] = candidate

# ---------------- 生成 SQL ----------------
out = ["SET NAMES utf8mb4;", "-- W4a 软科院校属性（仅更新未排名行；已排名行跳过保证幂等）"]
for name, rank, school_type, is985, is211, is_double, tags in attr_updates:
    out.append(
        "UPDATE university SET soft_ranking={}, school_type=COALESCE(school_type,{}), "
        "is_985=GREATEST(is_985,{}), is_211=GREATEST(is_211,{}), "
        "is_double_first_class=GREATEST(is_double_first_class,{}), tags=COALESCE(tags, {}) WHERE name={};".format(
            "NULL" if rank is None else rank, esc(school_type), is985, is211, is_double,
            esc(tags), esc(name)))

out.append("-- W4b 教育部本科专业目录扩容")
out.append("CREATE TEMPORARY TABLE stg_major (name VARCHAR(120) PRIMARY KEY, category VARCHAR(32), degree_type VARCHAR(64), description VARCHAR(1000));")
buf = []
for code in order:
    info = majors[code]
    degree = info["degree"] or "学士"
    duration_text = info["duration"] or "四年"
    description = "{}专业属于{}门类{}，专业代码 {}，毕业后授予{}，修业年限{}。".format(
        info["name"], info["category"], info["class"], info["code"], degree, duration_text)
    buf.append("({},{},{},{})".format(
        esc(info["name"]), esc(info["category"]), esc(info["degree"]), esc(description[:900])))
for i in range(0, len(buf), 300):
    out.append("INSERT INTO stg_major VALUES " + ",".join(buf[i:i + 300]) + ";")
out.append(
    "INSERT INTO major (name, category, degree_type, subject_requirement, description) "
    "SELECT s.name, s.category, s.degree_type, NULL, s.description FROM stg_major s "
    "WHERE NOT EXISTS (SELECT 1 FROM major m WHERE m.name = s.name);")

out.append("-- W4c 2024 校线（广东/江苏/湖北/四川，近三年录取表深度）")
out.append("CREATE TEMPORARY TABLE stg_cut24 (province VARCHAR(64), name VARCHAR(120), subject VARCHAR(16), cutoff INT, min_rank INT, PRIMARY KEY (province, name, subject));")
buf = []
for (province_cn, name, subject), (_, cutoff, min_rank) in cutoffs.items():
    buf.append("({},{},{},{},{})".format(
        esc(province_cn), esc(name), esc(subject), cutoff,
        "NULL" if min_rank is None else min_rank))
for i in range(0, len(buf), 400):
    out.append("INSERT INTO stg_cut24 VALUES " + ",".join(buf[i:i + 400]) + ";")
out.append(
    "INSERT INTO admission_cutoff (university_id, admission_year, province, subject_type, cutoff_score, min_rank) "
    "SELECT u.id, 2024, c.province, c.subject, c.cutoff, c.min_rank FROM stg_cut24 c "
    "JOIN university u ON u.name = c.name "
    "ON DUPLICATE KEY UPDATE cutoff_score = VALUES(cutoff_score), min_rank = VALUES(min_rank);")

import sys
sys.stdout.reconfigure(encoding="utf-8")
print("\n".join(out))
print("-- STATS attr={} catalog={} cut2024={}".format(
    len(attr_updates), len(majors), len(cutoffs)), file=sys.stderr)
