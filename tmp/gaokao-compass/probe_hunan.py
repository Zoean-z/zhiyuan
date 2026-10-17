# -*- coding: utf-8 -*-
"""只读探针：核对湖南数据集文件的 category 取值分布。输出到 stdout，无文件写入。"""
import csv
import os.path

BASE = r"C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan\tmp\gaokao-compass"

for year in ("2024", "2025"):
    for table in ("score-range", "school-admission", "major-admission", "enrollment-plan"):
        path = os.path.join(BASE, year, "hunan", table + ".csv")
        if not os.path.exists(path):
            print(year, table, "MISSING")
            continue
        with open(path, encoding="utf-8-sig", newline="") as handle:
            rows = list(csv.DictReader(handle))
        cats = {}
        for row in rows:
            key = row.get("category") or "<empty>"
            cats[key] = cats.get(key, 0) + 1
        top = dict(sorted(cats.items(), key=lambda item: -item[1])[:6])
        print(year, table, "rows=", len(rows), "cats=", top)
