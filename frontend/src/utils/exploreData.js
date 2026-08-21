// 公开数据查询页（查大学/院校排行/一分一段/招生计划）共享数据
// 与 utils/mock.js 的 MOCK_SCHOOLS 保持同源（id/name/province/tier 一致），补充展示字段
import { hasRealCurve, rankOfScore } from "./scoreModel";
//
// 数据来源（20260820 联网核实）：
//  - ruanke：软科2025中国大学排名（上海软科）
//  - xiaoyouhui：校友会2026中国大学排名（艾瑞深研究院）
//  - baoyan/baoyanCohort：各校2024/2025届毕业生就业质量报告（新高考网整理）
//  - masters/phds：一级学科硕士点/博士点数（各校研究生院统计，39所985盘点）
//  - cutoffs.浙江：浙江省教育考试院2026年普通类一段平行投档线（最低专业组；
//    rank 为官方位次号，null 时由真实一分一段曲线换算；note 标注专业组口径）
//  - clubTags：C9/华东五校/中坚九校/国防七子/建筑老八校/四大工学院/电气四虎/机械五虎（通行口径）
export const SCHOOLS = [
  { id: 1, name: "清华大学", province: "北京", city: "北京", district: "海淀区", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 320, majorCount: 82, planDelta: 12, clubTags: ["C9", "电气四虎", "机械五虎"], masters: 60, phds: 58, baoyan: 76.0, baoyanCohort: "2025届", ruanke: 1, xiaoyouhui: 2, cutoffs: { 浙江: { score: 700, rank: 94, year: 2026 } } },
  { id: 2, name: "北京大学", province: "北京", city: "北京", district: "海淀区", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 298, majorCount: 96, planDelta: 8, clubTags: ["C9"], masters: 49, phds: 52, baoyan: 65.07, baoyanCohort: "2025届", ruanke: 2, xiaoyouhui: 1, cutoffs: { 浙江: { score: 700, rank: 94, year: 2026 } } },
  { id: 3, name: "浙江大学", province: "浙江", city: "杭州", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 356, majorCount: 104, planDelta: 15, clubTags: ["C9", "华东五校", "电气四虎"], masters: 62, phds: 62, baoyan: 40.67, baoyanCohort: "2025届", ruanke: 3, xiaoyouhui: 3, cutoffs: { 浙江: { score: 665, rank: 6043, year: 2026 } } },
  { id: 4, name: "复旦大学", province: "上海", city: "上海", district: "杨浦区", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 286, majorCount: 78, planDelta: -6, clubTags: ["C9", "华东五校"], masters: 43, phds: 40, baoyan: 44.95, baoyanCohort: "2025届", ruanke: 5, xiaoyouhui: 4, cutoffs: { 浙江: { score: 691, rank: null, year: 2026 } } },
  { id: 5, name: "上海交通大学", province: "上海", city: "上海", district: "闵行区", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 312, majorCount: 88, planDelta: 17, clubTags: ["C9", "华东五校", "机械五虎"], masters: 58, phds: 52, baoyan: 45.95, baoyanCohort: "2025届", ruanke: 4, xiaoyouhui: 5, cutoffs: { 浙江: { score: 698, rank: 150, year: 2026 } } },
  { id: 6, name: "南京大学", province: "江苏", city: "南京", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 264, majorCount: 74, planDelta: 6, clubTags: ["C9", "华东五校"], masters: 48, phds: 44, baoyan: 44.72, baoyanCohort: "2025届", ruanke: 6, xiaoyouhui: 6, cutoffs: { 浙江: { score: 677, rank: 2617, year: 2026 } } },
  { id: 7, name: "中国科学技术大学", province: "安徽", city: "合肥", type: "理工类", nature: "公办", belong: "中国科学院直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 186, majorCount: 52, planDelta: 10, clubTags: ["C9", "华东五校"], masters: 38, phds: 30, baoyan: 56.22, baoyanCohort: "2025届", ruanke: 7, xiaoyouhui: 8, cutoffs: { 浙江: { score: 678, rank: 2397, year: 2026 } } },
  { id: 8, name: "华中科技大学", province: "湖北", city: "武汉", type: "理工类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 342, majorCount: 92, planDelta: 28, clubTags: ["中坚九校", "电气四虎", "机械五虎"], masters: 47, phds: 45, baoyan: 37.2, baoyanCohort: "2025届", ruanke: 9, xiaoyouhui: 10, cutoffs: { 浙江: { score: 644, rank: null, year: 2026, note: "最低专业组为中外合作办学" } } },
  { id: 9, name: "武汉大学", province: "湖北", city: "武汉", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 328, majorCount: 98, planDelta: 12, clubTags: ["中坚九校"], masters: 61, phds: 54, baoyan: 30.9, baoyanCohort: "2025届", ruanke: 8, xiaoyouhui: 9, cutoffs: { 浙江: { score: 664, rank: null, year: 2026 } } },
  { id: 10, name: "中山大学", province: "广东", city: "广州", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 368, majorCount: 108, planDelta: 20, clubTags: ["中坚九校"], masters: 65, phds: 57, baoyan: 29.29, baoyanCohort: "2024届", ruanke: 12, xiaoyouhui: 14, cutoffs: { 浙江: { score: 658, rank: 8820, year: 2026 } } },
  { id: 11, name: "哈尔滨工业大学", province: "黑龙江", city: "哈尔滨", type: "理工类", nature: "公办", belong: "工业和信息化部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 296, majorCount: 76, planDelta: 14, clubTags: ["C9", "国防七子", "中坚九校", "机械五虎"], masters: 41, phds: 29, baoyan: 36.1, baoyanCohort: "2024届", ruanke: 14, xiaoyouhui: 12, cutoffs: { 浙江: { score: 678, rank: null, year: 2026, note: "本部普通类专业组最低线" } } },
  { id: 12, name: "西安交通大学", province: "陕西", city: "西安", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 302, majorCount: 84, planDelta: 9, clubTags: ["C9", "中坚九校", "电气四虎", "机械五虎"], masters: 43, phds: 36, baoyan: 36.6, baoyanCohort: "2024届", ruanke: 10, xiaoyouhui: 13, cutoffs: { 浙江: { score: 666, rank: null, year: 2026 } } },
  { id: 13, name: "同济大学", province: "上海", city: "上海", district: "杨浦区", type: "理工类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 278, majorCount: 72, planDelta: -8, clubTags: ["建筑老八校"], masters: 46, phds: 37, baoyan: 41.41, baoyanCohort: "2025届", ruanke: 18, xiaoyouhui: 17, cutoffs: { 浙江: { score: 663, rank: null, year: 2026 } } },
  { id: 14, name: "北京航空航天大学", province: "北京", city: "北京", district: "海淀区", type: "理工类", nature: "公办", belong: "工业和信息化部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 232, majorCount: 64, planDelta: 11, clubTags: ["国防七子"], masters: 38, phds: 21, baoyan: 49.8, baoyanCohort: "2025届", ruanke: 11, xiaoyouhui: 18, cutoffs: { 浙江: { score: 663, rank: null, year: 2026 } } },
  { id: 15, name: "天津大学", province: "天津", city: "天津", district: "南开区", type: "理工类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 288, majorCount: 70, planDelta: 5, clubTags: ["建筑老八校", "中坚九校"], masters: 47, phds: 33, baoyan: 35.05, baoyanCohort: "2025届", ruanke: 20, xiaoyouhui: 22, cutoffs: { 浙江: { score: 657, rank: null, year: 2026 } } },
  { id: 16, name: "华南理工大学", province: "广东", city: "广州", type: "理工类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 316, majorCount: 86, planDelta: 22, clubTags: ["建筑老八校", "四大工学院"], masters: 43, phds: 35, baoyan: 23.76, baoyanCohort: "2025届", ruanke: 31, xiaoyouhui: 25, cutoffs: { 浙江: { score: 657, rank: null, year: 2026, note: "经济学类专业组最低线" } } },
  { id: 17, name: "东南大学", province: "江苏", city: "南京", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 274, majorCount: 80, planDelta: 7, clubTags: ["建筑老八校", "中坚九校", "四大工学院"], masters: 50, phds: 37, baoyan: 37.29, baoyanCohort: "2025届", ruanke: 16, xiaoyouhui: 20, cutoffs: { 浙江: { score: 652, rank: null, year: 2026, note: "最低专业组为中外合作办学" } } },
  { id: 18, name: "大连理工大学", province: "辽宁", city: "大连", type: "理工类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 262, majorCount: 68, planDelta: -5, clubTags: ["四大工学院"], masters: 42, phds: 29, baoyan: 23.99, baoyanCohort: "2025届", ruanke: 28, xiaoyouhui: 27, cutoffs: { 浙江: { score: 641, rank: null, year: 2026, note: "最低专业组为中外合作办学" } } },
  { id: 19, name: "山东大学", province: "山东", city: "济南", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 352, majorCount: 102, planDelta: 18, clubTags: [], masters: 52, phds: 44, baoyan: 29.8, baoyanCohort: "2025届", ruanke: 22, xiaoyouhui: 19, cutoffs: { 浙江: { score: 651, rank: null, year: 2026 } } },
  { id: 20, name: "厦门大学", province: "福建", city: "厦门", type: "综合类", nature: "公办", belong: "教育部直属", is985: true, is211: true, isDoubleFirstClass: true, planCount: 284, majorCount: 90, planDelta: 3, clubTags: ["中坚九校"], masters: 46, phds: 37, baoyan: 28.8, baoyanCohort: "2025届", ruanke: 24, xiaoyouhui: 23, cutoffs: { 浙江: { score: 660, rank: 7907, year: 2026 } } }
];

export const SCHOOL_PROVINCES = ["全部", ...Array.from(new Set(SCHOOLS.map((s) => s.province)))];
export const SCHOOL_TYPES = ["全部", ...Array.from(new Set(SCHOOLS.map((s) => s.type)))];
export const SCHOOL_LEVELS = [
  { label: "全部", key: "all" },
  { label: "985", key: "is985" },
  { label: "双一流", key: "isDoubleFirstClass" }
];

// 排行榜：按软科2025真实排名排序；index 为演示用加权指数
export const RANK_LIST = [...SCHOOLS]
  .sort((a, b) => a.ruanke - b.ruanke)
  .map((school, index) => ({
    ...school,
    rank: school.ruanke,
    index: (99.9 - index * 0.42).toFixed(1)
  }));

// 一分一段：浙江用真实曲线（省考试院 2025/2026 一分一段锚点）换算本段人数；
// 其余省份按省份+科类生成稳定演示分布（700 → 500 分段）

const SEGMENT_TEMPLATE = [
  3, 5, 8, 12, 18, 25, 34, 46, 60, 78,
  98, 122, 150, 182, 218, 258, 300, 346, 395, 446,
  498, 550, 601, 649, 692, 728, 756, 772, 775, 764,
  736, 690, 626, 546, 450, 340, 218, 82, 0, 0
];

export const SEGMENT_PROVINCES = ["浙江", "广东", "北京", "上海", "江苏"];
export const SEGMENT_SUBJECTS = ["物理类", "历史类"];

export function buildSegments(province, subject) {
  const opts = { province, subjectType: subject === "历史类" ? "HISTORY" : "PHYSICS" };
  if (hasRealCurve(province)) {
    // 真实锚点曲线：本段人数 = rank(score) - rank(score+5)，累计到 rank(score)
    const rows = [];
    let total = 0;
    for (let score = 700; score >= 500; score -= 5) {
      const current = rankOfScore(score, opts);
      const higher = rankOfScore(score + 5, opts);
      if (current == null) continue;
      const count = Math.max(0, current - (higher ?? 0));
      total = current;
      rows.push({ score, count, total });
    }
    return rows;
  }
  const seedBase = SEGMENT_PROVINCES.indexOf(province) * 7 + (subject === "历史类" ? 40 : 0);
  let total = 0;
  return SEGMENT_TEMPLATE.map((count, i) => {
    const score = 700 - i * 5;
    const wobble = ((seedBase + i * 13) % 9) - 4;
    const segCount = Math.max(0, count + wobble * (count > 40 ? 2 : 0));
    total += segCount;
    return { score, count: segCount, total };
  }).filter((row) => row.score >= 500);
}

// 高考资讯数据已迁移至 newsData.js（真实源：中国教育在线）

// ══════════ 查专业 ══════════
// 门类树（左侧分类导航，参考 gaokao.cn /special）
export const MAJOR_CATEGORIES = [
  { name: "工学", subs: ["计算机类", "电子信息类", "机械类", "自动化类", "土木类"] },
  { name: "理学", subs: ["数学类", "物理类", "化学类", "生物科学类"] },
  { name: "医学", subs: ["临床医学类", "口腔医学类", "护理学类", "中医学类"] },
  { name: "文学", subs: ["中国语言文学类", "外国语言文学类", "新闻传播学类"] },
  { name: "管理学", subs: ["工商管理类", "管理科学与工程类", "电子商务类"] },
  { name: "经济学", subs: ["经济学类", "金融学类", "财政学类"] },
  { name: "法学", subs: ["法学类", "政治学类", "社会学类"] },
  { name: "教育学", subs: ["教育学类", "体育学类"] },
  { name: "艺术学", subs: ["设计学类", "戏剧与影视学类"] },
  { name: "农学", subs: ["动物医学类", "植物生产类"] },
  { name: "哲学", subs: ["哲学类"] },
  { name: "历史学", subs: ["历史学类"] }
];

export const MAJOR_LEVELS = ["本科(普通)", "专科(高职)"];

// 专业库：name/code(专业代码)/category(门类)/sub(专业类)/duration/degree/gender/salary/schoolCount/level
export const MAJORS = [
  { name: "临床医学", code: "100201K", category: "医学", sub: "临床医学类", duration: "五年", degree: "医学学士", gender: "44:56", salary: "¥12.8万", schoolCount: 192, level: 0, hot: 1 },
  { name: "法学", code: "030101K", category: "法学", sub: "法学类", duration: "四年", degree: "法学学士", gender: "37:63", salary: "¥10.5万", schoolCount: 288, level: 0, hot: 2 },
  { name: "口腔医学", code: "100301K", category: "医学", sub: "口腔医学类", duration: "五年", degree: "医学学士", gender: "41:59", salary: "¥13.9万", schoolCount: 108, level: 0, hot: 3 },
  { name: "计算机科学与技术", code: "080901", category: "工学", sub: "计算机类", duration: "四年", degree: "工学学士", gender: "71:29", salary: "¥14.2万", schoolCount: 486, level: 0, hot: 4 },
  { name: "电气工程及其自动化", code: "080601", category: "工学", sub: "自动化类", duration: "四年", degree: "工学学士", gender: "76:24", salary: "¥11.6万", schoolCount: 302, level: 0, hot: 5 },
  { name: "心理学", code: "071101", category: "理学", sub: "心理学类", duration: "四年", degree: "理学学士", gender: "33:67", salary: "¥9.8万", schoolCount: 76, level: 0, hot: 6 },
  { name: "人工智能", code: "080717T", category: "工学", sub: "电子信息类", duration: "四年", degree: "工学学士", gender: "74:26", salary: "¥15.6万", schoolCount: 148, level: 0, hot: 7 },
  { name: "汉语言文学", code: "050101", category: "文学", sub: "中国语言文学类", duration: "四年", degree: "文学学士", gender: "26:74", salary: "¥8.4万", schoolCount: 398, level: 0, hot: 8 },
  { name: "自动化", code: "080801", category: "工学", sub: "自动化类", duration: "四年", degree: "工学学士", gender: "73:27", salary: "¥11.2万", schoolCount: 264, level: 0, hot: 9 },
  { name: "会计学", code: "120203K", category: "管理学", sub: "工商管理类", duration: "四年", degree: "管理学学士", gender: "30:70", salary: "¥9.6万", schoolCount: 412, level: 0, hot: 10 },
  { name: "数字媒体技术", code: "080906", category: "工学", sub: "计算机类", duration: "四年", degree: "工学学士", gender: "58:42", salary: "¥10.9万", schoolCount: 186, level: 0, hot: 11 },
  { name: "软件工程", code: "080902", category: "工学", sub: "计算机类", duration: "四年", degree: "工学学士", gender: "72:28", salary: "¥13.8万", schoolCount: 342, level: 0, hot: 12 },
  { name: "动物医学", code: "090401", category: "农学", sub: "动物医学类", duration: "五年", degree: "农学学士", gender: "45:55", salary: "¥8.9万", schoolCount: 82, level: 0, hot: 13 },
  { name: "中医学", code: "100501K", category: "医学", sub: "中医学类", duration: "五年", degree: "医学学士", gender: "39:61", salary: "¥9.4万", schoolCount: 46, level: 0, hot: 14 },
  { name: "土木工程", code: "081001", category: "工学", sub: "土木类", duration: "四年", degree: "工学学士", gender: "82:18", salary: "¥10.1万", schoolCount: 376, level: 0, hot: 15 },
  { name: "生物医学工程", code: "082601", category: "工学", sub: "生物医学工程类", duration: "四年", degree: "工学学士", gender: "61:39", salary: "¥11.8万", schoolCount: 98, level: 0, hot: 16 },
  { name: "护理学", code: "101101", category: "医学", sub: "护理学类", duration: "四年", degree: "理学学士", gender: "18:82", salary: "¥8.6万", schoolCount: 192, level: 0, hot: 17 },
  { name: "机械设计制造及其自动化", code: "080202", category: "工学", sub: "机械类", duration: "四年", degree: "工学学士", gender: "84:16", salary: "¥10.4万", schoolCount: 318, level: 0, hot: 18 },
  { name: "电子信息工程", code: "080701", category: "工学", sub: "电子信息类", duration: "四年", degree: "工学学士", gender: "70:30", salary: "¥12.1万", schoolCount: 356, level: 0, hot: 19 },
  { name: "金融学", code: "020301K", category: "经济学", sub: "金融学类", duration: "四年", degree: "经济学学士", gender: "46:54", salary: "¥11.3万", schoolCount: 288, level: 0, hot: 20 },
  { name: "英语", code: "050201", category: "文学", sub: "外国语言文学类", duration: "四年", degree: "文学学士", gender: "22:78", salary: "¥8.7万", schoolCount: 482, level: 0 },
  { name: "新闻学", code: "050301", category: "文学", sub: "新闻传播学类", duration: "四年", degree: "文学学士", gender: "31:69", salary: "¥9.2万", schoolCount: 224, level: 0 },
  { name: "数学与应用数学", code: "070101", category: "理学", sub: "数学类", duration: "四年", degree: "理学学士", gender: "63:37", salary: "¥10.8万", schoolCount: 386, level: 0 },
  { name: "数据科学与大数据技术", code: "080910T", category: "工学", sub: "计算机类", duration: "四年", degree: "工学学士", gender: "68:32", salary: "¥14.8万", schoolCount: 214, level: 0 },
  { name: "学前教育", code: "040106", category: "教育学", sub: "教育学类", duration: "四年", degree: "教育学学士", gender: "12:88", salary: "¥7.6万", schoolCount: 268, level: 0 },
  { name: "视觉传达设计", code: "130502", category: "艺术学", sub: "设计学类", duration: "四年", degree: "艺术学学士", gender: "36:64", salary: "¥8.9万", schoolCount: 342, level: 0 },
  { name: "经济学", code: "020101", category: "经济学", sub: "经济学类", duration: "四年", degree: "经济学学士", gender: "48:52", salary: "¥10.2万", schoolCount: 296, level: 0 },
  { name: "历史学", code: "060101", category: "历史学", sub: "历史学类", duration: "四年", degree: "历史学学士", gender: "42:58", salary: "¥7.9万", schoolCount: 128, level: 0 },
  { name: "哲学", code: "010101", category: "哲学", sub: "哲学类", duration: "四年", degree: "哲学学士", gender: "52:48", salary: "¥8.2万", schoolCount: 62, level: 0 },
  { name: "软件技术", code: "510203", category: "电子与信息大类", sub: "计算机类", duration: "三年", degree: "专科", gender: "69:31", salary: "¥8.1万", schoolCount: 588, level: 1 },
  { name: "大数据与会计", code: "530302", category: "财经商贸大类", sub: "财务会计类", duration: "三年", degree: "专科", gender: "28:72", salary: "¥7.2万", schoolCount: 512, level: 1 },
  { name: "护理", code: "520201", category: "医药卫生大类", sub: "护理类", duration: "三年", degree: "专科", gender: "15:85", salary: "¥7.5万", schoolCount: 446, level: 1 },
  { name: "学前教育（专科）", code: "570102K", category: "教育与体育大类", sub: "教育类", duration: "三年", degree: "专科", gender: "10:90", salary: "¥6.8万", schoolCount: 486, level: 1 },
  { name: "机电一体化技术", code: "460301", category: "装备制造大类", sub: "自动化类", duration: "三年", degree: "专科", gender: "81:19", salary: "¥7.8万", schoolCount: 428, level: 1 },
  { name: "电子商务", code: "530701", category: "财经商贸大类", sub: "电子商务类", duration: "三年", degree: "专科", gender: "44:56", salary: "¥7.4万", schoolCount: 502, level: 1 }
];

// ══════════ 志愿填报大数据 ══════════
export const VOLUNTEER_STATS = [
  { label: "模拟志愿表", value: "19.7万", unit: "份", desc: "累计生成" },
  { label: "填报院校", value: "161", unit: "所", desc: "覆盖全国" },
  { label: "填报专业", value: "126", unit: "个", desc: "12 大门类" },
  { label: "去向城市", value: "56", unit: "个", desc: "本省+省外" }
];

// 填报热度 TOP（院校/专业），数据由 SCHOOLS / MAJORS 派生
export const VOLUNTEER_SCHOOL_TOP = RANK_LIST.map((school, i) => ({
  name: school.name,
  count: 9860 - i * 386 - (school.id % 7) * 34
}));

export const VOLUNTEER_MAJOR_TOP = [...MAJORS]
  .filter((m) => m.hot)
  .sort((a, b) => a.hot - b.hot)
  .map((m, i) => ({ name: m.name, count: 8640 - i * 342 - (m.name.length % 5) * 26 }));

export const CITY_DESTINATIONS = {
  本省: [
    { name: "长沙", count: 28600 },
    { name: "株洲", count: 9800 },
    { name: "湘潭", count: 8600 },
    { name: "衡阳", count: 7200 },
    { name: "岳阳", count: 6800 }
  ],
  省外: [
    { name: "广州", count: 31200 },
    { name: "武汉", count: 27400 },
    { name: "北京", count: 24800 },
    { name: "深圳", count: 22600 },
    { name: "上海", count: 21800 },
    { name: "成都", count: 18600 },
    { name: "杭州", count: 17400 }
  ]
};

// ══════════ 智能选大学 ══════════
export const CHOOSE_SUBJECTS = ["物理", "历史"];
export const CHOOSE_OPTIONS = ["化学", "生物", "思想政治", "地理", "技术"];
export const CHOOSE_PROBABILITIES = ["全部", "概率大", "概率中", "概率小"];

// 由分数/位次/选科推导专业组结果（确定性生成，参考 gaokao.cn /choose/school/code）
export function buildChooseResults({ score = 620, subject = "物理", selections = [], probability = "全部" }) {
  const list = RANK_LIST.map((school) => {
    const seed = school.id * 37;
    const groupNo = ((seed % 24) + 6).toString().padStart(3, "0");
    const minScore = 701 - school.rank * 7 - (seed % 7) + (subject === "历史" ? 6 : 0);
    const minRank = Math.round((7200 + (school.rank - 1) * 4860) * (1 + (((seed % 11) - 5) * 0.9) / 100));
    const gap = score - minScore;
    const prob = gap >= 12 ? "概率大" : gap >= -8 ? "概率中" : "概率小";
    const rate = Math.max(4, Math.min(98, Math.round(52 + gap * 4.2)));
    const second = selections.length ? selections.join(" / ") : "不限";
    return {
      ...school,
      group: groupNo,
      rule: `首选${subject}，再选${second}`,
      minScore,
      minRank,
      prob,
      rate
    };
  });
  return probability === "全部" ? list : list.filter((item) => item.prob === probability);
}

export function schoolTags(school) {
  // 双一流 ≡ 211：按最高标准展示，211 不再单独作为标签（20260820 概念更新）
  return [school.is985 && "985", (school.is211 || school.isDoubleFirstClass) && "双一流"].filter(Boolean);
}

/** 联盟/圈层标签（C9、国防七子、建筑老八校……），排在 985/双一流 之后展示 */
export function schoolClubTags(school) {
  return Array.isArray(school?.clubTags) ? school.clubTags.filter(Boolean) : [];
}

export function schoolLoc(school) {
  // 直辖市精确到区（北京市海淀区），其余省显示到地级市（浙江杭州市）
  if (school.district) return `${school.city}市${school.district}`;
  return school.province === school.city ? `${school.city}市` : `${school.province}${school.city}`;
}
