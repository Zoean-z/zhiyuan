import assert from "node:assert/strict";
import test from "node:test";
import { MAJORS, NEWS_ARTICLES, SCHOOLS, resolveSchoolLogoId, selectSchoolShowcase } from "./publicData.js";

const forbiddenFields = ["admissionProbability", "gender", "heat", "index", "planCount", "rank", "salary", "views"];

test("public demo data does not present generated metrics as facts", () => {
  [...SCHOOLS, ...MAJORS, ...NEWS_ARTICLES].forEach((record) => {
    forbiddenFields.forEach((field) => assert.equal(field in record, false, `${field} must not appear`));
  });
});

test("public data has stable identifiers and source links", () => {
  assert.equal(new Set(SCHOOLS.map((school) => school.id)).size, SCHOOLS.length);
  assert.equal(new Set(MAJORS.map((major) => major.code)).size, MAJORS.length);
  assert.ok(NEWS_ARTICLES.every((article) => article.url.startsWith("https://")));
});

test("school showcase filters and rotates existing schools without duplication", () => {
  const first = selectSchoolShowcase("全部", 0, 8);
  const next = selectSchoolShowcase("全部", 8, 8);
  const engineering = selectSchoolShowcase("理工类", 0, 8);

  assert.equal(first.length, 8);
  assert.equal(new Set(first.map((school) => school.id)).size, first.length);
  assert.notDeepEqual(first.map((school) => school.id), next.map((school) => school.id));
  assert.ok(engineering.every((school) => school.type === "理工类"));
  assert.ok([...first, ...next, ...engineering].every((school) => SCHOOLS.includes(school)));
});

test("school logo resolver supports showcase ids and legacy plan ids without false matches", () => {
  assert.equal(resolveSchoolLogoId({ id: 101, name: "清华大学" }), 1);
  assert.equal(resolveSchoolLogoId({ id: 120, name: "厦门大学" }), 20);
  assert.equal(resolveSchoolLogoId({ id: 1, name: "浙江大学" }), 3);
  assert.equal(resolveSchoolLogoId({ id: 999, logoId: 6, name: "自定义学校" }), 6);
  assert.equal(resolveSchoolLogoId({ id: 1, name: "未知学校" }), null);
});
