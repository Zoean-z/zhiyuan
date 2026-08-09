import assert from "node:assert/strict";
import test from "node:test";
import {
  buildPlanResult,
  flattenPlanItems,
  mergePlanItems,
  parsePlanResult
} from "./recommendation.js";

const baseItem = {
  recommendationMode: "MAJOR_FIRST",
  universityId: 3,
  universityName: "浙江大学",
  majorName: "软件工程",
  universityProvince: "浙江",
  universityTier: "985",
  is985: true,
  is211: true,
  isDoubleFirstClass: true,
  schoolTags: ["985", "211", "双一流"],
  cutoffScore: 650,
  strategy: "safe",
  matchReasons: ["专业匹配"]
};

test("plan codec round-trips without mutating source items", () => {
  const source = [structuredClone(baseItem)];
  const before = structuredClone(source);

  const result = buildPlanResult(source, { aiSummary: "摘要", tips: ["核对章程"] });
  const flattened = flattenPlanItems(JSON.stringify(result));

  assert.deepEqual(source, before);
  assert.equal(result.safe.length, 1);
  assert.equal(result.safe[0].strategy, "SAFE");
  assert.equal(flattened.length, 1);
  assert.equal(flattened[0].universityName, "浙江大学");
  assert.equal(flattened[0].majorName, "软件工程");
  assert.equal(result.aiSummary, "摘要");
  assert.deepEqual(result.tips, ["核对章程"]);
});

test("mergePlanItems deduplicates by stable plan key without mutating inputs", () => {
  const existing = [structuredClone(baseItem)];
  const incoming = [structuredClone(baseItem), { ...structuredClone(baseItem), majorName: "计算机科学与技术" }];
  const existingBefore = structuredClone(existing);
  const incomingBefore = structuredClone(incoming);

  const merged = mergePlanItems(existing, incoming);

  assert.equal(merged.addedCount, 1);
  assert.equal(merged.items.length, 2);
  assert.deepEqual(existing, existingBefore);
  assert.deepEqual(incoming, incomingBefore);
});

test("parsePlanResult rejects malformed or non-object JSON", () => {
  assert.deepEqual(parsePlanResult("not-json"), {});
  assert.deepEqual(parsePlanResult("[]"), {});
  assert.deepEqual(parsePlanResult(null), {});
});
