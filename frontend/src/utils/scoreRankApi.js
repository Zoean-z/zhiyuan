function requireOk(response, fallbackMessage) {
  if (!response?.ok) {
    throw new Error(`${fallbackMessage}（HTTP ${response?.status ?? "unknown"}）`);
  }
  return response;
}

function finiteInteger(value) {
  if (value == null || value === "") return null;
  const number = Number(value);
  return Number.isFinite(number) ? Math.round(number) : null;
}

export function normalizeScoreRankCurve(data) {
  const points = Array.isArray(data?.points)
    ? data.points
        .map((point) => ({
          score: finiteInteger(point?.score),
          rankValue: finiteInteger(point?.rankValue),
          segmentCount: finiteInteger(point?.segmentCount)
        }))
        .filter((point) => point.score != null && point.rankValue != null)
        .sort((a, b) => b.score - a.score)
    : [];
  return {
    province: String(data?.province || ""),
    subjectType: String(data?.subjectType || ""),
    mappingYear: finiteInteger(data?.mappingYear),
    pointCount: points.length,
    points
  };
}

export async function fetchScoreRankCurve(province, subjectType, fetchImpl = fetch) {
  const params = new URLSearchParams({ province, subjectType });
  const response = requireOk(
    await fetchImpl(`/api/meta/score-rank?${params.toString()}`),
    "一分一段数据加载失败"
  );
  return normalizeScoreRankCurve(await response.json());
}

export async function fetchRankLookup(province, subjectType, score, fetchImpl = fetch) {
  const params = new URLSearchParams({ province, subjectType, score: String(score) });
  const response = requireOk(
    await fetchImpl(`/api/meta/rank?${params.toString()}`),
    "位次查询失败"
  );
  return normalizeRankLookup(await response.json());
}

export function normalizeRankLookup(data) {
  return {
    province: String(data?.province || ""),
    subjectType: String(data?.subjectType || ""),
    mappingYear: finiteInteger(data?.mappingYear),
    score: finiteInteger(data?.score),
    rank: finiteInteger(data?.rank),
    rankSource: String(data?.rankSource || "NONE"),
    rankSourceLabel: String(data?.rankSourceLabel || "暂无位次数据")
  };
}
