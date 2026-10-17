# -*- coding: utf-8 -*-
"""只读探针：2024 各省 school-admission 的 category 分布。stdout 输出。"""
import csv
import os.path

BASE = r"C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan\tmp\gaokao-compass\2024"

for pinyin in ("guangdong", "jiangsu", "henan", "sichuan", "hubei"):
    path = os.path.join(BASE, pinyin, "school-admission.csv")
    if not os.path.exists(path):
        print(pinyin, "MISSING")
        continue
    with open(path, encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle))
    cats = {}
    for row in rows:
        key = row.get("category") or "<empty>"
        cats[key] = cats.get(key, 0) + 1
    top = dict(sorted(cats.items(), key=lambda item: -item[1])[:5])
    print(pinyin, "rows=", len(rows), top)
