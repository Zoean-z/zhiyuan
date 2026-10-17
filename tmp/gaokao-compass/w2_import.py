# -*- coding: utf-8 -*-
"""W2 导入管线：Gaokao-Compass-11M 2025 x 6省 -> major_admission_cutoff（含计划数/学制/学费）。
SQL 输出到 stdout（由调用方重定向），本脚本不做任何文件写入。
关联：major-admission 与 enrollment-plan 按 (校代码,归一组号,归一专业名) 主关联、(校代码,归一专业名) 兜底。
幂等：目标唯一键 (university_id, major_name, admission_year, province, subject_type) ON DUPLICATE KEY UPDATE。
"""
import csv
import os.path

DATA_ROOT = r"C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan\tmp\gaokao-compass\2025"

PROV = {"guangdong": "广东", "jiangsu": "江苏", "henan": "河南",
        "hubei": "湖北", "hebei": "河北", "sichuan": "四川"}
SUBJ = {"物理类": "物理", "历史类": "历史"}
BATCH_RANK = {"本科批": 1, "本科提前批": 2, "特殊类型招生批": 3, "专科批": 4, "专科提前批": 5}
YEAR = 2025
SOURCE = "Gaokao-Compass-11M/2025 (MIT)"


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


def read_rows(pinyin, table):
    path = DATA_ROOT + "\\" + pinyin + "\\" + table + ".csv"
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


rows = []
stats = {"ma": 0, "plan_hit_primary": 0, "plan_hit_fallback": 0, "plan_miss": 0}

for pinyin, province_cn in PROV.items():
    # --- enrollment-plan -> (计划数, 学制, 学费) 字典：主键(校,组,专业) + 兜底(校,专业) ---
    plan_primary = {}
    plan_by_name = {}
    for row in read_rows(pinyin, "enrollment-plan"):
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

    # --- major-admission -> 专业线 ---
    for row in read_rows(pinyin, "major-admission"):
        subject = SUBJ.get((row.get("category") or "").strip())
        if subject is None:
            continue
        batch = (row.get("batch") or "").strip()
        priority = BATCH_RANK.get(batch)
        if priority is None:
            continue
        major_name = norm(row.get("major_name"))
        ucode = (row.get("university_code") or "").strip()
        univ_name = norm(row.get("university_name"))
        if not major_name or not univ_name:
            continue
        stats["ma"] += 1
        key = (ucode, norm_group(row.get("major_group")), major_name)
        value = plan_primary.get(key)
        if value is not None:
            stats["plan_hit_primary"] += 1
        else:
            value = plan_by_name.get((ucode, major_name))
            if value is not None:
                stats["plan_hit_fallback"] += 1
            else:
                stats["plan_miss"] += 1
        rows.append({
            "univ": univ_name, "major": major_name, "province": province_cn,
            "subject": subject, "cutoff": num(row.get("min_score")),
            "min_rank": num(row.get("min_rank")), "batch_rank": priority,
            "plan": value[0] if value else None,
            "duration": value[1] if value else None,
            "tuition": value[2] if value else None})

# 去重：唯一键 (univ, major, province, subject)，保留批次优先级最高（数值小）的一行
dedup = {}
for row in rows:
    key = (row["univ"], row["major"], row["province"], row["subject"])
    kept = dedup.get(key)
    if kept is None or row["batch_rank"] < kept["batch_rank"]:
        dedup[key] = row

# ---------------- 生成 SQL ----------------
out = ["SET NAMES utf8mb4;",
       "CREATE TEMPORARY TABLE stg_mcut (univ VARCHAR(120), major VARCHAR(120), province VARCHAR(64), subject VARCHAR(16), cutoff INT, min_rank INT, plan INT, duration INT, tuition INT, PRIMARY KEY (univ, major, province, subject));"]
buf = []
for row in dedup.values():
    buf.append("({},{},{},{},{},{},{},{},{})".format(
        esc(row["univ"]), esc(row["major"]), esc(row["province"]), esc(row["subject"]),
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
    "SELECT u.id, maj.id, s.major, 2025, s.province, s.subject, s.cutoff, s.min_rank, s.plan, s.duration, s.tuition, "
    "'REAL', " + esc(SOURCE) + " FROM stg_mcut s "
    "JOIN university u ON u.name = s.univ "
    "LEFT JOIN major maj ON maj.name = s.major "
    "ON DUPLICATE KEY UPDATE cutoff_score=VALUES(cutoff_score), min_rank=VALUES(min_rank), plan_count=VALUES(plan_count), "
    "duration_years=VALUES(duration_years), tuition_per_year=VALUES(tuition_per_year), data_kind='REAL', calibration_source=VALUES(calibration_source);")

import sys
sys.stdout.reconfigure(encoding="utf-8")
print("\n".join(out))
print("-- STATS source_rows={} dedup_rows={} plan_primary={} plan_fallback={} plan_miss={}".format(
    stats["ma"], len(dedup), stats["plan_hit_primary"], stats["plan_hit_fallback"], stats["plan_miss"]),
    file=sys.stderr)
