import assert from "node:assert/strict";
import test from "node:test";
import { MOCK_EXPLORE_MAJORS, MOCK_OFFERING_SCHOOLS, mockMajorDetail } from "./majorExploreMockData.js";

test("major explore mock mirrors the public backend contract", () => {
  const law = mockMajorDetail("030101K");
  assert.equal(law.major.name, "法学");
  assert.equal(law.major.offeringSchoolCount, MOCK_OFFERING_SCHOOLS.length);
  assert.ok(law.employmentDirections.includes("律师"));
  assert.ok(MOCK_EXPLORE_MAJORS.every((major) => major.code && major.subcategory));
});

test("offering school mock has logo and school attributes without probability", () => {
  assert.ok(MOCK_OFFERING_SCHOOLS.length >= 15);
  assert.ok(MOCK_OFFERING_SCHOOLS.every((school) => school.logoId && school.name && school.province));
  assert.ok(MOCK_OFFERING_SCHOOLS.every((school) => !("probability" in school)));
});
