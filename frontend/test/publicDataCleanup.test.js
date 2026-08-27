import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = dirname(fileURLToPath(import.meta.url));

async function source(path) {
  return readFile(resolve(currentDir, path), "utf8");
}

test("home page contains no local heat, salary, rank-range, or probability classification", async () => {
  const home = await source("../src/views/HomeView.vue");
  assert.doesNotMatch(home, /from\s+["']\.\.\/utils\/exploreData["']/);
  assert.doesNotMatch(home, /毕业年薪|院校热度|heatList|hotMajors|strategyOf\s*\(/);
  assert.doesNotMatch(home, /位次区间|profile\.province\s*\|\|\s*["']湖南["']/);
  assert.match(home, /detail\.strategy/);
});

test("ranking pages contain no local 20-school list", async () => {
  const rank = await source("../src/views/RankView.vue");
  const side = await source("../src/components/GkSidePanel.vue");
  assert.doesNotMatch(`${rank}\n${side}`, /RANK_LIST|topSchools/);
  assert.match(rank, /暂无可追溯的权威院校排行数据/);
});

test("enrollment page uses shared subject data without a fabricated trend", async () => {
  const enroll = await source("../src/views/EnrollPlanView.vue");
  assert.match(enroll, /subjectType/);
  assert.match(enroll, /withDataOnly=true/);
  assert.match(enroll, /school\.admissionYear/);
  assert.doesNotMatch(enroll, /trendBars|TrendCharts|计划趋势|base\s*-\s*12/);
});

test("news pages contain no generated read-count formula", async () => {
  const list = await source("../src/views/NewsView.vue");
  const detail = await source("../src/views/NewsDetailView.vue");
  const data = await source("../src/utils/newsData.js");
  assert.doesNotMatch(`${list}\n${detail}\n${data}`, /viewsOf|阅读\s*\{\{|12800\s*\+/);
});
