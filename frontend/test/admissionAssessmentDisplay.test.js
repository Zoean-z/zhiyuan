import test from 'node:test';
import assert from 'node:assert/strict';
import { admissionLevel, admissionDisplayText, isExtremelyLowProbability } from '../src/utils/recommendation.js';

test('display preserves unknown versus veto and does not mutate numeric inputs', () => {
  const detail = { probability: 0, strategy: null, recommended: false };
  assert.equal(admissionLevel(null), '待评估');
  assert.equal(admissionLevel(null, detail), '极低');
  assert.equal(isExtremelyLowProbability(detail), true);
  assert.deepEqual([-1, 0, 0.1, 19.9, 20, 34.9, 35, 54.9, 55, 74.9, 75, 89.9, 90, 96].map(p => admissionLevel(p)),
    ['极低', '极低', '较低', '较低', '低', '低', '中', '中', '高', '高', '较高', '较高', '极高', '极高']);
  assert.equal(admissionLevel(NaN), '待评估');
  assert.equal(admissionLevel(90, detail), '极低');
  assert.equal(detail.probability, 0);
});

test('legacy admission table/prose are converted while factual rates and scores remain', () => {
  const input = '| 院校 | 录取概率 | 保研率 |\n|---|---|---|\n| A | 62% | 20% |\n规则测算录取概率约为 96%；风险指数 38/100；最低分591';
  const output = admissionDisplayText(input);
  assert.match(output, /\| A \| 高 \| 20% \|/);
  assert.match(output, /录取机会极高/);
  assert.match(output, /最低分591/);
  assert.doesNotMatch(output, /62%|96%|38\/100/);
  assert.equal(admissionDisplayText('录取概率18%；录取概率80%'), '录取机会较低；录取机会较高');
});
