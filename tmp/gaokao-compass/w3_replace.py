# -*- coding: utf-8 -*-
"""W3 替换管线：删除湖南 SIMULATED 比赛验证数据，导入真实数据（2024+2025 两年）。
SQL 输出到 stdout（由调用方重定向），本脚本不做任何文件写入。
- score_rank_mapping / admission_cutoff / admission_group_cutoff：按 province='湖南' 删除
- major_admission_cutoff：按 data_kind='SIMULATED' 删除（精确限定湖南模拟行）
- 导入幂等：NOT EXISTS / ON DUPLICATE KEY UPDATE
"""
import csv
import os.path

DATA_ROOT = r"C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan\tmp\gaokao-compass"
PROVINCE = "湖南"
YEARS = (2024, 2025)
SUBJ = {"物理类": "物理", "历史类": "历史"}
BATCH_RANK = {"本科批": 1, "本科提前批": 2, "特殊类型招生批": 3, "专科批": 4, "专科提前批": 5}
SOURCE = "Gaokao-Compass-11M (MIT)"


def esc(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "\\'") + "'"


def num(value):
    if value is None:
        return None
    text = str(value).strip()
    if not text or text == "<NA>":
        return None
    try:
        return int(float(text))
    except ValueError:
        return None


def norm(value):
    return (value or "").strip().replace("（", "(").replace("）", ")")


def norm_group(value):
    text = norm(value)
    if text.startswith("(") and text.endswith(")"):
        text = text[1:-1]
    return text


def read_rows(year, table):
    path = os.path.join(DATA_ROOT, str(year), "hunan", table + ".csv")
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


srm = {}      # (year, subject, score) -> (batch_rank, rank, segment)
cutoffs = {}  # (year, name, subject) -> (batch_rank, cutoff, min_rank)
unis = {}     # name -> flags
mcut = {}     # (year, univ, major, subject) -> dict(row, batch_rank)

for year in YEARS:
    # --- enrollment-plan -> 计划/学制/学费字典 ---
    plan_primary = {}
    plan_by_name = {}
    for row in read_rows(year, "enrollment-plan"):
        ucode = (row.get("university_code") or "").strip()
        if not ucode:
            continue
        name = norm(row.get("major_name"))
        value = (num(row.get("plan_count")), num(row.get("duration")), num(row.get("tuition")))
        key = (ucode, norm_group(row.get("major_group")), name)
        if key not in plan_primary:
            plan_primary[key] = value
        by_name = (ucode, name)
        if by_name not in plan_by_name:
            plan_by_name[by_name] = value

    # --- school-admission -> 校线 + 大学属性 ---
    for row in read_rows(year, "school-admission"):
        subject = SUBJ.get((row.get("category") or "").strip())
        if subject is None:
            continue
        batch = (row.get("batch") or "").strip()
        priority = BATCH_RANK.get(batch)
        if priority is None:
            continue
        name = norm(row.get("university_name") or "")
        if not name:
            continue
        flags = unis.setdefault(name, {
            "province": (row.get("school_province") or "").strip() or PROVINCE,
            "nature": (row.get("school_nature") or "").strip() or None,
            "is985": 0, "is211": 0, "benke": False})
        flags["is985"] = max(flags["is985"], num(row.get("is_985")) or 0)
        flags["is211"] = max(flags["is211"], num(row.get("is_211")) or 0)
        if batch.startswith("本科") or batch.startswith("特殊"):
            flags["benke"] = True
        min_score = num(row.get("min_score"))
        if min_score is None:
            continue
        key = (year, name, subject)
        candidate = (priority, min_score, num(row.get("min_rank")))
        if key not in cutoffs or candidate[0] < cutoffs[key][0]:
            cutoffs[key] = candidate

    # --- score-range -> 一分一段 ---
    for row in read_rows(year, "score-range"):
        subject = SUBJ.get((row.get("category") or "").strip())
        if subject is None:
            continue
        score = num(row.get("score"))
        rank_value = num(row.get("cumulative_count"))
        if score is None or rank_value is None:
            continue
        batch = (row.get("batch") or "").strip()
        priority = 1 if "本科" in batch else 2
        key = (year, subject, score)
        if key not in srm or priority < srm[key][0]:
            srm[key] = (priority, rank_value, num(row.get("segment_count")))

    # --- major-admission + 计划关联 -> 专业线 ---
    for row in read_rows(year, "major-admission"):
        subject = SUBJ.get((row.get("category") or "").strip())
        if subject is None:
            continue
        batch = (row.get("batch") or "").strip()
        priority = BATCH_RANK.get(batch)
        if priority is None:
            continue
        major_name = norm(row.get("major_name"))
        univ_name = norm(row.get("university_name"))
        if not major_name or not univ_name:
            continue
        ucode = (row.get("university_code") or "").strip()
        value = plan_primary.get((ucode, norm_group(row.get("major_group")), major_name))
        if value is None:
            value = plan_by_name.get((ucode, major_name))
        key = (year, univ_name, major_name, subject)
        candidate = {"cutoff": num(row.get("min_score")), "min_rank": num(row.get("min_rank")),
                     "plan": value[0] if value else None,
                     "duration": value[1] if value else None,
                     "tuition": value[2] if value else None,
                     "batch_rank": priority}
        kept = mcut.get(key)
        if kept is None or candidate["batch_rank"] < kept["batch_rank"]:
            mcut[key] = candidate

# ---------------- 生成 SQL ----------------
out = ["SET NAMES utf8mb4;",
       "-- 替换：删除湖南模拟数据（比赛验证数据）",
       "DELETE FROM score_rank_mapping WHERE province = " + esc(PROVINCE) + ";",
       "DELETE FROM admission_cutoff WHERE province = " + esc(PROVINCE) + ";",
       "DELETE FROM major_admission_cutoff WHERE data_kind = 'SIMULATED';",
       "DELETE FROM admission_group_cutoff WHERE province = " + esc(PROVINCE) + ";",
       "",
       "-- 大学扩容（防重插入）",
       "CREATE TEMPORARY TABLE stg_univ (name VARCHAR(120) PRIMARY KEY, province VARCHAR(64), nature VARCHAR(16), is985 TINYINT, is211 TINYINT, tier VARCHAR(64));"]
buf = []
for name, flag in unis.items():
    tier = "本科批" if flag["benke"] else "专科批"
    buf.append("({},{},{},{},{},{})".format(
        esc(name), esc(flag["province"]), esc(flag["nature"]),
        flag["is985"], flag["is211"], esc(tier)))
for i in range(0, len(buf), 400):
    out.append("INSERT INTO stg_univ VALUES " + ",".join(buf[i:i + 400]) + ";")
out.append(
    "INSERT INTO university (name, province, tier, nature, is_985, is_211) "
    "SELECT s.name, s.province, s.tier, s.nature, s.is985, s.is211 FROM stg_univ s "
    "WHERE NOT EXISTS (SELECT 1 FROM university u WHERE u.name = s.name);")

rows = ["({},{},{},{},{},{},{})".format(
    year, esc(PROVINCE), esc(subject), score, rank,
    "NULL" if segment is None else segment, esc(SOURCE + "/" + str(year)))
    for (year, subject, score), (_, rank, segment) in srm.items()]
for i in range(0, len(rows), 500):
    out.append("INSERT INTO score_rank_mapping (mapping_year, province, subject_type, score, rank_value, segment_count, source_url) VALUES "
               + ",".join(rows[i:i + 500])
               + " ON DUPLICATE KEY UPDATE rank_value=VALUES(rank_value), segment_count=VALUES(segment_count), source_url=VALUES(source_url);")

out.append("CREATE TEMPORARY TABLE stg_cut (name VARCHAR(120), year INT, subject VARCHAR(16), cutoff INT, min_rank INT, PRIMARY KEY (name, year, subject));")
buf = []
for (year, name, subject), (_, cutoff, min_rank) in cutoffs.items():
    buf.append("({},{},{},{},{})".format(
        esc(name), year, esc(subject), cutoff,
        "NULL" if min_rank is None else min_rank))
for i in range(0, len(buf), 400):
    out.append("INSERT INTO stg_cut VALUES " + ",".join(buf[i:i + 400]) + ";")
out.append(
    "INSERT INTO admission_cutoff (university_id, admission_year, province, subject_type, cutoff_score, min_rank) "
    "SELECT u.id, c.year, " + esc(PROVINCE) + ", c.subject, c.cutoff, c.min_rank FROM stg_cut c "
    "JOIN university u ON u.name = c.name "
    "ON DUPLICATE KEY UPDATE cutoff_score = VALUES(cutoff_score), min_rank = VALUES(min_rank);")

out.append("CREATE TEMPORARY TABLE stg_mcut (univ VARCHAR(120), major VARCHAR(120), year INT, subject VARCHAR(16), cutoff INT, min_rank INT, plan INT, duration INT, tuition INT, PRIMARY KEY (univ, major, year, subject));")
buf = []
for (year, univ, major, subject), row in mcut.items():
    buf.append("({},{},{},{},{},{},{},{},{})".format(
        esc(univ), esc(major), year, esc(subject),
        "NULL" if row["cutoff"] is None else row["cutoff"],
        "NULL" if row["min_rank"] is None else row["min_rank"],
        "NULL" if row["plan"] is None else row["plan"],
        "NULL" if row["duration"] is None else row["duration"],
        "NULL" if row["tuition"] is None else row["tuition"]))
for i in range(0, len(buf), 400):
    out.append("INSERT INTO stg_mcut VALUES " + ",".join(buf[i:i + 400]) + ";")
out.append(
    "INSERT INTO major_admission_cutoff (university_id, major_id, major_name, admission_year, province, subject_type, "
    "cutoff_score, min_rank, plan_count, duration_years, tuition_per_year, data_kind, calibration_source) "
    "SELECT u.id, maj.id, s.major, s.year, " + esc(PROVINCE) + ", s.subject, s.cutoff, s.min_rank, s.plan, s.duration, s.tuition, "
    "'REAL', " + esc(SOURCE) + " FROM stg_mcut s "
    "JOIN university u ON u.name = s.univ "
    "LEFT JOIN major maj ON maj.name = s.major "
    "ON DUPLICATE KEY UPDATE cutoff_score=VALUES(cutoff_score), min_rank=VALUES(min_rank), plan_count=VALUES(plan_count), "
    "duration_years=VALUES(duration_years), tuition_per_year=VALUES(tuition_per_year), data_kind='REAL', calibration_source=VALUES(calibration_source);")

import sys
sys.stdout.reconfigure(encoding="utf-8")
print("\n".join(out))
print("-- STATS srm={} cutoffs={} unis={} mcut={}".format(
    len(srm), len(cutoffs), len(unis), len(mcut)), file=sys.stderr)
