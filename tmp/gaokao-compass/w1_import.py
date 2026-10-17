# -*- coding: utf-8 -*-
"""W1 导入管线：Gaokao-Compass-11M 2025 x 6省 -> score_rank_mapping / admission_cutoff / university 扩容。
SQL 输出到 stdout（由调用方重定向），本脚本不做任何文件写入。幂等：university 防重插入，其余 ON DUPLICATE KEY UPDATE。
"""
import csv

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


def norm_name(name):
    return name.strip().replace("（", "(").replace("）", ")")


def read_rows(pinyin, table):
    path = DATA_ROOT + "\\" + pinyin + "\\" + table + ".csv"
    if not os_path_exists(path):
        return []
    with open(path, encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


import os.path
os_path_exists = os.path.exists

unis = {}
cutoffs = {}
srm = {}

for pinyin, province_cn in PROV.items():
    for row in read_rows(pinyin, "school-admission"):
        subject = SUBJ.get((row.get("category") or "").strip())
        if subject is None:
            continue
        batch = (row.get("batch") or "").strip()
        priority = BATCH_RANK.get(batch)
        if priority is None:
            continue
        name = norm_name(row.get("university_name") or "")
        if not name:
            continue
        flags = unis.setdefault(name, {
            "province": (row.get("school_province") or "").strip() or province_cn,
            "nature": (row.get("school_nature") or "").strip() or None,
            "is985": 0, "is211": 0, "benke": False})
        flags["is985"] = max(flags["is985"], num(row.get("is_985")) or 0)
        flags["is211"] = max(flags["is211"], num(row.get("is_211")) or 0)
        if batch.startswith("本科") or batch.startswith("特殊"):
            flags["benke"] = True
        min_score = num(row.get("min_score"))
        if min_score is None:
            continue
        key = (pinyin, name, subject)
        candidate = (priority, min_score, num(row.get("min_rank")))
        if key not in cutoffs or candidate[0] < cutoffs[key][0]:
            cutoffs[key] = candidate

    for row in read_rows(pinyin, "score-range"):
        subject = SUBJ.get((row.get("category") or "").strip())
        if subject is None:
            continue
        score = num(row.get("score"))
        rank_value = num(row.get("cumulative_count"))
        if score is None or rank_value is None:
            continue
        batch = (row.get("batch") or "").strip()
        priority = 1 if "本科" in batch else 2
        segment = num(row.get("segment_count"))
        key = (province_cn, subject, score)
        if key not in srm or priority < srm[key][0]:
            srm[key] = (priority, rank_value, segment)

out = ["SET NAMES utf8mb4;",
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
    YEAR, esc(prov), esc(subject), score, rank,
    "NULL" if segment is None else segment, esc(SOURCE))
    for (prov, subject, score), (_, rank, segment) in srm.items()]
for i in range(0, len(rows), 500):
    out.append("INSERT INTO score_rank_mapping (mapping_year, province, subject_type, score, rank_value, segment_count, source_url) VALUES "
               + ",".join(rows[i:i + 500])
               + " ON DUPLICATE KEY UPDATE rank_value=VALUES(rank_value), segment_count=VALUES(segment_count), source_url=VALUES(source_url);")

out.append("CREATE TEMPORARY TABLE stg_cut (name VARCHAR(120), province VARCHAR(64), subject VARCHAR(16), cutoff INT, min_rank INT, PRIMARY KEY (name, province, subject));")
buf = []
for (pinyin, name, subject), (_, cutoff, min_rank) in cutoffs.items():
    buf.append("({},{},{},{},{})".format(
        esc(name), esc(PROV[pinyin]), esc(subject), cutoff,
        "NULL" if min_rank is None else min_rank))
for i in range(0, len(buf), 400):
    out.append("INSERT INTO stg_cut VALUES " + ",".join(buf[i:i + 400]) + ";")
out.append(
    "INSERT INTO admission_cutoff (university_id, admission_year, province, subject_type, cutoff_score, min_rank) "
    "SELECT u.id, 2025, c.province, c.subject, c.cutoff, c.min_rank FROM stg_cut c "
    "JOIN university u ON u.name = c.name "
    "ON DUPLICATE KEY UPDATE cutoff_score = VALUES(cutoff_score), min_rank = VALUES(min_rank);")

import sys
sys.stdout.reconfigure(encoding="utf-8")
print("\n".join(out))
print("-- STATS universities={} cutoff_rows={} score_rank_rows={}".format(
    len(unis), len(cutoffs), len(srm)), file=sys.stderr)
