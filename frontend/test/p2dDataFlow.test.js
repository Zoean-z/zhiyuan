import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import {
  isExtremelyLowProbability,
  normalizeItem,
  probabilityDisplayValue
} from "../src/utils/recommendation.js";

const currentDir = dirname(fileURLToPath(import.meta.url));

async function source(path) {
  return readFile(resolve(currentDir, path), "utf8");
}

test("recommendation normalization never derives probability from risk score", () => {
  assert.equal(normalizeItem({ riskScore: 18 }).admissionProbability, null);
  assert.equal(normalizeItem({ riskScore: 18, admissionProbability: 82 }).admissionProbability, 82);
});

test("only the backend explicit extremely-low state is displayed as zero", () => {
  const extremelyLow = {
    probability: null,
    recommended: false,
    explanation: "差距超出模型可测算区间，视为极低概率。"
  };
  assert.equal(isExtremelyLowProbability(extremelyLow), true);
  assert.equal(probabilityDisplayValue(extremelyLow), 0);
  assert.equal(probabilityDisplayValue({ probability: null, explanation: "请先设置分数" }), null);
  assert.equal(probabilityDisplayValue({ probability: 37, recommended: true }), 37);
});

test("P2-D university pages use the shared province without a Hunan fallback", async () => {
  const files = await Promise.all([
    source("../src/views/SchoolsView.vue"),
    source("../src/views/SchoolDetailView.vue"),
    source("../src/views/ChooseView.vue"),
    source("../src/components/VolunteerSheet.vue")
  ]);

  for (const content of files) {
    assert.doesNotMatch(content, /profile\.province\s*\|\|\s*["']湖南["']/);
  }
  assert.match(files[0], /withDataOnly:\s*["']true["']/);
  assert.match(files[2], /withDataOnly:\s*["']true["']/);
  assert.match(files[3], /withDataOnly:\s*["']true["']/);
});

test("P2-D probability badges consume backend strategy instead of local thresholds", async () => {
  const schools = await source("../src/views/SchoolsView.vue");
  const detail = await source("../src/views/SchoolDetailView.vue");
  const choose = await source("../src/views/ChooseView.vue");
  const sheet = await source("../src/components/VolunteerSheet.vue");

  for (const content of [schools, detail, choose, sheet]) {
    assert.doesNotMatch(content, /strategyOf\s*\(/);
    assert.doesNotMatch(content, /["']<1%?["']/);
  }
  assert.match(schools, /detail\.strategy/);
  assert.match(schools, /label:\s*["']概率极低["'].*value:\s*["']0%["']/s);
  assert.match(detail, /detail\.value\.strategy/);
  assert.match(detail, /full:\s*["']概率极低["']/);
  assert.match(choose, /probability\?\.strategy/);
  assert.match(choose, /extremelyLow\s*\?\s*["']0%["']\s*:\s*["']待测["']/);
  assert.match(sheet, /probability\?\.strategy/);
  assert.match(sheet, /prob == null \|\| prob === ["']["']\) return null/);
  assert.match(sheet, /isExtremelyLowProbability\(detail\)\s*\?\s*["']0%["']\s*:\s*["']待测["']/);
});

test("volunteer flow contains no smart fill, purity, or fabricated history", async () => {
  const view = await source("../src/views/VolunteerView.vue");
  const sheet = await source("../src/components/VolunteerSheet.vue");
  const header = await source("../src/components/GkHeader.vue");

  assert.doesNotMatch(`${view}\n${sheet}`, /smartFill|智能填充/);
  assert.doesNotMatch(sheet, /纯净度|purity|f\.years/);
  assert.doesNotMatch(header, /label:\s*["']我的志愿表["']/);
});

test("recommendation result does not reconstruct score or probability", async () => {
  const result = await source("../src/components/RecommendationResult.vue");
  assert.doesNotMatch(result, /cutoffScore\s*\)\s*-\s*Number\(s\.scoreGap/);
  assert.doesNotMatch(result, /100\s*-\s*Number\(risk/);
  assert.doesNotMatch(result, /:\s*50\s*;/);
});
