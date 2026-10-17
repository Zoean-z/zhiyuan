# -*- coding: utf-8 -*-
"""W6 属性统一管线：
- 掌上高考学校列表（2991 所全量）-> university.school_type / nature(补空) / is_985 / is_211 / is_double_first_class / tags（标准格式重算）
- 软科 bcur_type=10 总榜（812 所）-> university.soft_ranking（总榜口径重刷）
SQL 输出到 stdout（由调用方重定向），本脚本不做任何文件写入。
"""
import json
import os.path
import re

TMP = r"C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan\tmp"
EOL_DIR = os.path.join(TMP, "univ-attrs", "eol")
BCUR10 = os.path.join(TMP, "univ-attrs", "bcur-10.json")


def esc(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "\\'") + "'"


def norm(value):
    return (value or "").strip().replace("（", "(").replace("）", ")")


# ---------------- 掌上高考全量列表 ----------------
schools = {}
pg = 1
while True:
    path = os.path.join(EOL_DIR, "p{}.json".format(pg))
    if not os.path.exists(path):
        break
    with open(path, encoding="utf-8") as handle:
        data = json.load(handle)
    items = (data.get("data") or {}).get("item") or []
    if not items:
        break
    for item in items:
        name = norm(item.get("name"))
        if not name:
            continue
        schools[name] = item
    pg += 1
    if pg > 150:
        break

# ---------------- 软科 type=10 总榜 ----------------
with open(BCUR10, encoding="utf-8") as handle:
    rankings = json.load(handle)["data"]["rankings"]
soft_rank = {}
for item in rankings:
    name = norm(item.get("univNameCn"))
    match = re.match(r"(\d+)", str(item.get("ranking") or "").strip())
    if name and match:
        soft_rank[name] = int(match.group(1))

# ---------------- 生成 SQL ----------------
out = ["SET NAMES utf8mb4;", "-- W6a 掌上高考：院校类型/性质/标签（全量标准化重算 tags）"]
for name, item in schools.items():
    school_type = (item.get("type_name") or "").strip().replace("类", "") or None
    nature = (item.get("nature_name") or "").strip() or None
    is985 = 1 if str(item.get("f985")) == "1" else 0
    is211 = 1 if str(item.get("f211")) == "1" else 0
    is_double = 1 if (item.get("dual_class_name") or "") == "双一流" else 0
    tag_parts = []
    if is985:
        tag_parts.append("985")
    if is211:
        tag_parts.append("211")
    if is_double:
        tag_parts.append("双一流")
    if school_type:
        tag_parts.append(school_type + "类")
    tags = "|".join(tag_parts) or None
    out.append(
        "UPDATE university SET school_type={}, nature=COALESCE(nature,{}), "
        "is_985=GREATEST(is_985,{}), is_211=GREATEST(is_211,{}), "
        "is_double_first_class=GREATEST(is_double_first_class,{}), tags={} WHERE name={};".format(
            esc(school_type), esc(nature), is985, is211, is_double, esc(tags), esc(name)))

out.append("-- W6b 软科 type=10 总榜排名重刷")
for name, rank in soft_rank.items():
    out.append("UPDATE university SET soft_ranking={} WHERE name={};".format(rank, esc(name)))

import sys
sys.stdout.reconfigure(encoding="utf-8")
print("\n".join(out))
print("-- STATS eol_schools={} soft_rank={}".format(len(schools), len(soft_rank)), file=sys.stderr)
