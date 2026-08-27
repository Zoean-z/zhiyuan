import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

import { normalizeSchoolLike } from "../src/utils/volunteerCore.js";
import { planItemsToSheet, sheetToPlanItems } from "../src/utils/planSync.js";

const currentDir = dirname(fileURLToPath(import.meta.url));

test("normalizeSchoolLike does not synthesize missing school facts", () => {
  assert.deepEqual(normalizeSchoolLike({ universityName: "示例大学" }), {
    id: null,
    name: "示例大学",
    type: "",
    province: "",
    cutoffScore: null,
    minRank: null
  });
});

test("sheetToPlanItems preserves a legacy user choice but strips untrusted facts", () => {
  const slots = Array.from({ length: 45 }, () => null);
  slots[0] = {
    schoolId: 7,
    schoolName: "旧本地院校",
    majorNames: ["本地生成专业"],
    prob: 88,
    minRank: 1234
  };

  assert.deepEqual(sheetToPlanItems(slots), [{
    universityName: "旧本地院校",
    universityId: null,
    majorName: "院校志愿",
    admissionProbability: null,
    minRank: null,
    strategy: "rush",
    volunteerIndex: 1,
    adjust: true
  }]);
});

test("sheetToPlanItems persists only backend-sourced facts", () => {
  const slots = Array.from({ length: 45 }, () => null);
  slots[15] = {
    schoolId: 42,
    schoolName: "后端院校",
    majorNames: ["计算机科学与技术"],
    prob: 76,
    minRank: 2345,
    adjust: true,
    schoolSource: "backend",
    majorSource: "backend",
    probabilitySource: "backend",
    dataSource: "backend"
  };

  assert.deepEqual(sheetToPlanItems(slots), [{
    universityName: "后端院校",
    universityId: 42,
    majorName: "计算机科学与技术",
    admissionProbability: 76,
    minRank: 2345,
    strategy: "safe",
    volunteerIndex: 16,
    adjust: true
  }]);
});

test("cloud plan choices remain editable without treating stored facts as freshly verified", () => {
  const slots = planItemsToSheet([{
    universityId: 42,
    universityName: "已保存院校",
    majorName: "法学",
    admissionProbability: 66,
    minRank: 3456,
    strategy: "safe",
    volunteerIndex: 16,
    adjust: false
  }]);

  assert.equal(slots[15].schoolSource, "plan");
  assert.equal(slots[15].majorSource, "plan");
  assert.equal(slots[15].probabilitySource, null);
  assert.equal(slots[15].dataSource, null);

  const [resaved] = sheetToPlanItems(slots);
  assert.equal(resaved.universityName, "已保存院校");
  assert.equal(resaved.majorName, "法学");
  assert.equal(resaved.admissionProbability, null);
  assert.equal(resaved.minRank, null);
});

test("volunteerCore contains no admission or major fabrication functions", async () => {
  const source = await readFile(resolve(currentDir, "../src/utils/volunteerCore.js"), "utf8");
  assert.doesNotMatch(source, /schoolCutoff|cutoffHistory|majorDetailsOfSchool|majorCutoff|probDetailOf|nameHash/);
});

test("major picker reads backend majors without local probability or salary fallbacks", async () => {
  const source = await readFile(resolve(currentDir, "../src/components/MajorPickDialog.vue"), "utf8");
  assert.match(source, /\/api\/recommendations\/schools\/\$\{universityId\}\/majors/);
  assert.doesNotMatch(source, /majorDetailsOfSchool|probOf\(|salary|月薪/);
});
