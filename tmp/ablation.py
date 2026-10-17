# -*- coding: utf-8 -*-
"""推荐顺序消融实验：对同一组考生档案拉取完整推荐结果，计算质量指标。

指标定义：
- rank_impossible: rankGap < -3000（位次模型明确判不可能）却进入推荐的条目数 —— 核心缺陷指标
- purity: 各桶内 rankGap 落在该桶模型位次窗口内的比例（rush -3000..+1000 / safe +1001..+10000 / guarantee >+10000）
- abs_gap_mean: 各桶 |rankGap| 均值（越小越贴合考生）
- demo_leak: admissionYear=2026 的条目数（2026 为演示年份数据）

凭据与地址全部从环境变量读取：SMOKE_BASE / SMOKE_USER / SMOKE_PASS；结果输出到 stdout。
"""
import http.client
import json
import os

HOST = "127.0.0.1"
PORT = 18080
PATH_LOGIN = "/api/auth/login"
PATH_RECOMMEND = "/api/recommendations"

PROFILES = [
    {"label": "湖南物理588", "province": "湖南", "subjectType": "PHYSICS", "score": 588},
    {"label": "湖南物理560", "province": "湖南", "subjectType": "PHYSICS", "score": 560},
    {"label": "湖南物理620", "province": "湖南", "subjectType": "PHYSICS", "score": 620},
    {"label": "广东物理580", "province": "广东", "subjectType": "PHYSICS", "score": 580},
]

BUCKET_WINDOW = {
    "RUSH": (-3000, 1000),
    "SAFE": (1001, 10000),
    "GUARANTEE": (10001, float("inf")),
}


def request_json(path, body, token=None):
    conn = http.client.HTTPConnection(HOST, PORT, timeout=60)
    payload = json.dumps(body, ensure_ascii=False).encode("utf-8")
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if token:
        headers["Authorization"] = "Bearer " + token
    conn.request("POST", path, body=payload, headers=headers)
    resp = conn.getresponse()
    data = json.loads(resp.read().decode("utf-8"))
    conn.close()
    return data


def main():
    login = request_json(PATH_LOGIN, {"username": os.environ["SMOKE_USER"], "password": os.environ["SMOKE_PASS"]})
    token = login["token"]

    report = []
    for p in PROFILES:
        body = {
            "score": p["score"],
            "province": p["province"],
            "subjectType": p["subjectType"],
            "recommendationMode": "SCHOOL_FIRST",
        }
        d = request_json(PATH_RECOMMEND, body, token)
        rows = []
        for bucket in ("rush", "safe", "guarantee"):
            for it in d.get(bucket) or []:
                rows.append({**it, "_bucket": bucket.upper()})
        user_rank = next((it.get("userRank") for it in rows if it.get("userRank")), None)

        bucket_stats = {}
        for name, (lo, hi) in BUCKET_WINDOW.items():
            items = [r for r in rows if r["_bucket"] == name]
            gaps = [r.get("rankGap") for r in items if r.get("rankGap") is not None]
            impossible = [g for g in gaps if g < -3000]
            pure = [g for g in gaps if lo <= g <= hi]
            years = [r.get("admissionYear") for r in items if r.get("admissionYear")]
            demo = sum(1 for y in years if y == 2026)
            bucket_stats[name] = {
                "count": len(items),
                "rank_impossible": len(impossible),
                "purity": round(len(pure) / len(gaps), 3) if gaps else None,
                "abs_gap_mean": round(sum(abs(g) for g in gaps) / len(gaps)) if gaps else None,
                "demo2026": demo,
            }
        total_impossible = sum(s["rank_impossible"] for s in bucket_stats.values())
        report.append({
            "profile": p["label"],
            "user_rank": user_rank,
            "total": len(rows),
            "total_rank_impossible": total_impossible,
            "buckets": bucket_stats,
        })

    print(json.dumps(report, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
