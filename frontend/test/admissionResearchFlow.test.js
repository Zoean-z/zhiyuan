import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { admissionResearchRoute } from '../src/utils/recommendation.js';

test('research entry snapshots current conditions and preserves school/major identity', () => {
  globalThis.localStorage = { getItem: () => JSON.stringify({ province: '湖南', firstSubject: '历史', score: 610, manualRank: 10000, batch: '本科批', secondSubjects: ['政治', '地理'] }) };
  const route = admissionResearchRoute(7, '测试大学', '法学', { score: 620, userRank: 8000 });
  const request = JSON.parse(route.query.research);
  assert.equal(route.path, '/agent');
  assert.equal(request.schoolId, 7);
  assert.equal(request.subjectType, 'HISTORY');
  assert.equal(request.score, 620);
  assert.equal(request.userRank, 8000);
  assert.equal(request.majorName, '法学');
  assert.match(route.query.q, new RegExp(String(request.targetYear)));
  assert.equal(request.probability, undefined);
  assert.equal(request.riskScore, undefined);
  delete globalThis.localStorage;
});

test('research progress follows server evidence and stops on completion or interruption', () => {
  const source = readFileSync(new URL('../src/components/AgentWorkspace.vue', import.meta.url), 'utf8');
  const body = source.slice(source.indexOf('function researchStatus('), source.indexOf('function toolChipText('));
  const groups = { value: [{}, {}] };
  const sending = { value: true };
  const status = new Function('groups', 'sending', 'turnResearchContext', `${body}; return researchStatus;`)(groups, sending, { schoolId: 45 });
  const group = { tools: [{ toolName: 'admissionResearch', messageType: 'tool_call', content: '联网检索 2/8：湖南科技大学专业热度' }] };
  assert.equal(status(group, 1).title, '正在搜索…');
  assert.match(status(group, 1).detail, /2\/8/);
  assert.equal(status(group, 0).active, false);
  group.tools[0].content = '正在综合本地数据与检索资料';
  assert.equal(status(group, 1).title, '正在分析并生成报告…');
  sending.value = false;
  assert.equal(status(group, 1).title, '研判未完成');
  group.tools.push({ toolName: 'admissionResearch', messageType: 'tool_result', payload: { sources: [{ url: 'https://example.com' }], searchesCompleted: 2 } });
  assert.equal(status(group, 1).active, false);
  assert.match(status(group, 1).detail, /2 次搜索 · 1 条资料来源/);
  const ordinaryStatus = new Function('groups', 'sending', 'turnResearchContext', `${body}; return researchStatus;`)(groups, sending, null);
  assert.equal(ordinaryStatus({ tools: [] }, 1), null);
});

test('legacy automatic source footer is hidden while inline references and gap notices remain', () => {
  const source = readFileSync(new URL('../src/components/AgentWorkspace.vue', import.meta.url), 'utf8');
  const body = source.slice(source.indexOf('function stripResearchSourceFooter('), source.indexOf('function answerHtml('));
  const strip = new Function(`${body}; return stripResearchSourceFooter;`)();
  const report = '结论：机会高，参考来源[1]。\n\n资料状态：计划尚未公布';
  assert.equal(strip(report + '\n\n本次检索来源（截止 2026-10-17）：\n- [1] 招生章程 — https://school.edu.cn/plan\n- [2] 政策 — https://exam.cn/policy\n'), report);
  const ordinary = '资料来源\n- 官方章程 https://school.edu.cn/plan\n以下是分析正文';
  assert.equal(strip(ordinary), ordinary);
  assert.equal(strip('结论：参考来源[1]。'), '结论：参考来源[1]。');
});
