import assert from "node:assert/strict";
import test from "node:test";

import {
  readStoredAuth,
  refreshStoredAuthProfile
} from "../src/utils/recommendation.js";

function createStorage(initial = {}) {
  const values = new Map(Object.entries(initial));
  return {
    getItem(key) { return values.has(key) ? values.get(key) : null; },
    setItem(key, value) { values.set(key, String(value)); },
    removeItem(key) { values.delete(key); }
  };
}

test("refreshStoredAuthProfile replaces stale local candidate fields with server fields", async () => {
  globalThis.localStorage = createStorage({
    zhiyuan_auth: JSON.stringify({
      token: "token-1",
      user: { username: "testuser", score: 650, subjectType: "PHYSICS", examProvince: "浙江" }
    })
  });

  const refreshed = await refreshStoredAuthProfile(async (url, options) => {
    assert.equal(url, "/api/auth/profile");
    assert.equal(options.method, "GET");
    assert.equal(options.headers.Authorization, "Bearer token-1");
    return {
      ok: true,
      status: 200,
      async json() {
        return {
          token: "token-1",
          username: "testuser",
          score: 612,
          subjectType: "HISTORY",
          examProvince: "湖南",
          role: "USER"
        };
      }
    };
  });

  assert.deepEqual(refreshed.user, {
    username: "testuser",
    score: 612,
    subjectType: "HISTORY",
    examProvince: "湖南",
    role: "USER"
  });
  assert.deepEqual(readStoredAuth(), refreshed);
});

test("refreshStoredAuthProfile clears an invalid authenticated session", async () => {
  globalThis.localStorage = createStorage({
    zhiyuan_auth: JSON.stringify({ token: "expired", user: { username: "testuser" } })
  });

  await assert.rejects(
    refreshStoredAuthProfile(async () => ({ ok: false, status: 401 })),
    /Failed to refresh authenticated profile/
  );
  assert.equal(readStoredAuth(), null);
});

test("authenticated server profile overrides a confirmed stale exam profile", async () => {
  globalThis.localStorage = createStorage({
    zhiyuan_exam_profile: JSON.stringify({
      province: "浙江",
      firstSubject: "物理",
      score: 650,
      confirmed: true
    })
  });

  const { applyAuthenticatedProfile, profile } = await import("../src/utils/examProfile.js");
  applyAuthenticatedProfile({
    username: "testuser",
    score: 612,
    subjectType: "HISTORY",
    examProvince: "湖南",
    role: "USER"
  });

  assert.equal(profile.province, "湖南");
  assert.equal(profile.firstSubject, "历史");
  assert.equal(profile.score, 612);
  assert.equal(profile.confirmed, true);
});
