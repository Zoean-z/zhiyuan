import assert from "node:assert/strict";
import test from "node:test";
import { setupMockInterceptor } from "./mock.js";

globalThis.window = {
  fetch: globalThis.fetch.bind(globalThis),
  location: { origin: "http://localhost:5173" }
};
setupMockInterceptor({ latencyMs: 0 });

async function request(path, options = {}) {
  const response = await window.fetch(path, options);
  const data = await response.json();
  return { response, data };
}

test("mock mode honors the free-text, plan, school-detail, and conversation contracts", async () => {
  const freeText = await request("/api/recommendations/free-text", {
    method: "POST",
    body: JSON.stringify({ requirementText: "浙江物理630分，推荐计算机" })
  });
  assert.equal(freeText.response.status, 200);
  assert.ok(Array.isArray(freeText.data.recommendations));
  assert.ok(freeText.data.recommendations.length > 0);
  assert.equal(freeText.data.parsed.candidateProvince, "浙江");

  const current = await request("/api/plans/current");
  assert.equal(current.response.status, 200);
  const updatedName = "当前方案草稿";
  const updated = await request("/api/plans/current", {
    method: "PUT",
    body: JSON.stringify({ ...current.data, planName: updatedName, aiSummary: "updated" })
  });
  assert.equal(updated.data.aiSummary, "updated");
  const plans = await request("/api/plans");
  assert.equal(plans.data.find((item) => item.planName === updatedName).aiSummary, "updated");

  const school = await request("/api/recommendations/schools/3/majors?province=浙江&subjectType=PHYSICS");
  assert.equal(school.data.universityName, "浙江大学");
  assert.ok(Array.isArray(school.data.majors));

  const created = await request("/api/agent/conversations", {
    method: "POST",
    body: JSON.stringify({ title: "契约测试" })
  });
  assert.ok(Array.isArray(created.data.messages));
  const turn = await request(`/api/agent/conversations/${created.data.id}/messages`, {
    method: "POST",
    body: JSON.stringify({ content: "帮我推荐学校", planId: current.data.id })
  });
  assert.ok(Array.isArray(turn.data.generatedMessages));
  assert.equal(turn.data.generatedMessages[0].messageType, "text");
  const detail = await request(`/api/agent/conversations/${created.data.id}`);
  assert.equal(detail.data.messages.length, 2);
  assert.equal(detail.data.messageCount, 2);
});
