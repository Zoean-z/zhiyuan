import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  fetchRankLookup,
  fetchScoreRankCurve,
  normalizeRankLookup,
  normalizeScoreRankCurve
} from "../src/utils/scoreRankApi.js";

test("score-rank normalization preserves unavailable values as null", () => {
  const curve = normalizeScoreRankCurve({
    province: "湖南",
    subjectType: "物理",
    mappingYear: null,
    points: [{ score: 650, rankValue: 5230, segmentCount: null }]
  });
  const lookup = normalizeRankLookup({
    province: "湖南",
    subjectType: "物理",
    mappingYear: null,
    score: 650,
    rank: null,
    rankSource: "NONE"
  });

  assert.equal(curve.mappingYear, null);
  assert.equal(curve.points[0].segmentCount, null);
  assert.equal(lookup.mappingYear, null);
  assert.equal(lookup.rank, null);
});

test("score-rank clients request the selected province and subject", async () => {
  const urls = [];
  const fetchImpl = async (url) => {
    urls.push(url);
    return {
      ok: true,
      status: 200,
      async json() {
        if (url.startsWith("/api/meta/score-rank?")) {
          return {
            province: "湖南",
            subjectType: "历史",
            mappingYear: 2025,
            points: [{ score: 650, rankValue: 1200, segmentCount: 80 }]
          };
        }
        return {
          province: "湖南",
          subjectType: "历史",
          mappingYear: 2025,
          score: 650,
          rank: 1200,
          rankSource: "EXACT",
          rankSourceLabel: "一分一段精确位次"
        };
      }
    };
  };

  const curve = await fetchScoreRankCurve("湖南", "HISTORY", fetchImpl);
  const lookup = await fetchRankLookup("湖南", "HISTORY", 650, fetchImpl);

  assert.equal(urls[0], "/api/meta/score-rank?province=%E6%B9%96%E5%8D%97&subjectType=HISTORY");
  assert.equal(urls[1], "/api/meta/rank?province=%E6%B9%96%E5%8D%97&subjectType=HISTORY&score=650");
  assert.equal(curve.points[0].segmentCount, 80);
  assert.equal(lookup.rank, 1200);
});

test("P2-B production consumers no longer import local score-rank generators", async () => {
  const segmentsView = await readFile(new URL("../src/views/SegmentsView.vue", import.meta.url), "utf8");
  const examProfile = await readFile(new URL("../src/utils/examProfile.js", import.meta.url), "utf8");

  assert.doesNotMatch(segmentsView, /buildSegments|exploreData|rankOfScore|scoreOfRank/);
  assert.doesNotMatch(examProfile, /rankOfScore|scoreOfRank|beatPercent/);
  assert.match(segmentsView, /fetchScoreRankCurve/);
  assert.match(examProfile, /fetchRankLookup/);
});
