import assert from "node:assert/strict";
import test from "node:test";
import {
  buildPlanResult,
  electiveSubjectsLabel,
  flattenPlanItems,
  groupPlanItemsBySchool,
  isUserProfileComplete,
  mergePlanItems,
  parsePlanResult
} from "./recommendation.js";

const baseItem = {
  recommendationMode: "MAJOR_FIRST",
  universityId: 3,
  universityName: "浙江大学",
  majorName: "软件工程",
  professionalGroupCode: "301",
  professionalGroupName: "信息技术类",
  subjectRequirements: "首选物理，再选化学",
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
  assert.equal(flattened[0].professionalGroupCode, "301");
  assert.equal(flattened[0].professionalGroupName, "信息技术类");
  assert.equal(flattened[0].subjectRequirements, "首选物理，再选化学");
  assert.equal(result.aiSummary, "摘要");
  assert.deepEqual(result.tips, ["核对章程"]);
  assert.equal(result.safe[0].obeyAdjustment, true);
  assert.equal(flattened[0].obeyAdjustment, true);
});

test("plan school grouping merges majors and keeps adjustment compatible", () => {
  const items = [
    { ...structuredClone(baseItem), obeyAdjustment: false },
    { ...structuredClone(baseItem), majorName: "计算机科学与技术", obeyAdjustment: false },
    { ...structuredClone(baseItem), universityId: 9, universityName: "同济大学", majorName: "人工智能" }
  ];

  const groups = groupPlanItemsBySchool(items);
  const roundTripped = flattenPlanItems(JSON.stringify(buildPlanResult(items)));

  assert.equal(groups.length, 2);
  assert.deepEqual(groups[0].majors, ["软件工程", "计算机科学与技术"]);
  assert.equal(groups[0].obeyAdjustment, false);
  assert.equal(roundTripped[0].obeyAdjustment, false);
  assert.equal(roundTripped[2].obeyAdjustment, true);
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

test("3+1+2 profile requires two distinct elective subjects", () => {
  const base = { score: 630, subjectType: "PHYSICS", examProvince: "浙江" };
  assert.equal(isUserProfileComplete({ ...base, electiveSubjects: ["CHEMISTRY", "BIOLOGY"] }), true);
  assert.equal(isUserProfileComplete({ ...base, electiveSubjects: ["CHEMISTRY"] }), false);
  assert.equal(isUserProfileComplete({ ...base, electiveSubjects: ["CHEMISTRY", "CHEMISTRY"] }), false);
  assert.equal(electiveSubjectsLabel(["BIOLOGY", "CHEMISTRY"]), "化学生物");
});
