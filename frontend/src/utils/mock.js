// Mock 数据 - 用于无后端的演示模式

export const MOCK_USER = {
  id: 1,
  username: "testuser",
  score: 630,
  subjectType: "PHYSICS",
  examProvince: "浙江",
  role: "USER",
  token: "mock-token-12345"
};

const MOCK_ADMIN_USER = {
  id: 100,
  username: "admin",
  score: null,
  subjectType: null,
  examProvince: null,
  role: "ADMIN",
  token: "mock-admin-token-12345"
};

const MOCK_PROVINCES = [
  "北京", "天津", "河北", "山西", "内蒙古",
  "辽宁", "吉林", "黑龙江", "上海", "江苏",
  "浙江", "安徽", "福建", "江西", "山东",
  "河南", "湖北", "湖南", "广东", "广西",
  "海南", "重庆", "四川", "贵州", "云南",
  "西藏", "陕西", "甘肃", "青海", "宁夏", "新疆"
];

const MOCK_SCHOOLS = [
  { id: 1, name: "清华大学", province: "北京", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 2, name: "北京大学", province: "北京", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 3, name: "浙江大学", province: "浙江", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 4, name: "复旦大学", province: "上海", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 5, name: "上海交通大学", province: "上海", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 6, name: "南京大学", province: "江苏", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 7, name: "中国科学技术大学", province: "安徽", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 8, name: "华中科技大学", province: "湖北", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 9, name: "武汉大学", province: "湖北", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 10, name: "中山大学", province: "广东", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 11, name: "哈尔滨工业大学", province: "黑龙江", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 12, name: "西安交通大学", province: "陕西", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 13, name: "同济大学", province: "上海", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 14, name: "北京航空航天大学", province: "北京", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 15, name: "天津大学", province: "天津", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 16, name: "华南理工大学", province: "广东", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 17, name: "东南大学", province: "江苏", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 18, name: "大连理工大学", province: "辽宁", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 19, name: "山东大学", province: "山东", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 20, name: "厦门大学", province: "福建", tier: "985", is985: true, is211: true, isDoubleFirstClass: true }
];

const MOCK_MAJORS = [
  "计算机科学与技术", "软件工程", "人工智能", "数据科学与大数据技术",
  "电子信息工程", "通信工程", "自动化", "电气工程及其自动化",
  "机械工程", "车辆工程", "土木工程", "建筑学",
  "临床医学", "口腔医学", "药学", "护理学",
  "法学", "金融学", "会计学", "工商管理",
  "英语", "日语", "法语", "德语"
];

function stableHash(input) {
  const text = String(input || "");
  let hash = 0;
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 131 + text.charCodeAt(i)) >>> 0;
  }
  return hash;
}

function stableRange(seed, min, max) {
  if (max <= min) return min;
  return min + (stableHash(seed) % (max - min + 1));
}

function stablePick(list, seed) {
  return list[stableHash(seed) % list.length];
}

function generateRecommendation(school, major, strategy) {
  const seed = `${school.id}:${major}:${strategy}`;
  const baseScore = stableRange(`${seed}:score`, 620, 659);
  const scoreGap = stableRange(`${seed}:scoreGap`, -15, 14);
  const rankGap = stableRange(`${seed}:rankGap`, -1000, 999);
  const probability = strategy === "rush"
    ? stableRange(`${seed}:prob`, 35, 54)
    : strategy === "safe"
      ? stableRange(`${seed}:prob`, 55, 74)
      : stableRange(`${seed}:prob`, 75, 94);

  return {
    universityId: school.id,
    universityName: school.name,
    universityProvince: school.province,
    universityTier: school.tier,
    is985: school.is985,
    is211: school.is211,
    isDoubleFirstClass: school.isDoubleFirstClass,
    majorName: major,
    cutoffScore: baseScore,
    scoreGap: scoreGap,
    userRank: stableRange(`${seed}:userRank`, 5000, 7999),
    minRank: stableRange(`${seed}:minRank`, 4000, 7999),
    rankGap: rankGap,
    admissionProbability: probability,
    strategy: strategy,
    strategyLabel: strategy === "rush" ? "冲刺" : strategy === "safe" ? "稳妥" : "保底",
    recommendationBasis: "SCORE",
    riskScore: 100 - probability,
    matchReasons: ["分数匹配", "位次相近", "专业对口"],
    explanation: `该校${major}专业近年录取分数相对稳定，根据你的分数和位次，有一定录取机会。`
  };
}

function generateRecommendations(count = 15) {
  const rush = [];
  const safe = [];
  const guarantee = [];

  for (let i = 0; i < count; i++) {
    const school = MOCK_SCHOOLS[i % MOCK_SCHOOLS.length];
    const major = stablePick(MOCK_MAJORS, `${school.id}:${i}`);

    if (i < 5) {
      rush.push(generateRecommendation(school, major, "rush"));
    } else if (i < 10) {
      safe.push(generateRecommendation(school, major, "safe"));
    } else {
      guarantee.push(generateRecommendation(school, major, "guarantee"));
    }
  }

  return {
    rush,
    safe,
    guarantee,
    summary: `根据你的分数630分和位次信息，为你推荐了${count}所院校。其中冲刺${rush.length}所，稳妥${safe.length}所，保底${guarantee.length}所。`,
    aiSummary: "综合分析你的分数和位次情况，推荐策略以冲稳保梯度为主，建议重点关注稳妥档位的院校。",
    finalAdvice: "建议优先考虑稳妥档位的院校，冲刺档位可以作为备选。保底档位确保有学可上。",
    tips: ["关注目标院校的招生计划变化", "注意专业调剂风险", "建议多了解目标城市的就业环境"]
  };
}

const MOCK_PLANS = [
  {
    id: 1,
    planName: "当前方案草稿",
    sourceType: "score",
    sourceQuery: "模式：专业优先，分数：650，省份：浙江，科类：物理，专业：计算机科学与技术",
    createdAt: "2026-08-07T08:53:23",
    resultJson: JSON.stringify(generateRecommendations(5))
  },
  {
    id: 2,
    planName: "冲稳保方案-A",
    sourceType: "score",
    sourceQuery: "模式：学校优先，分数：630，省份：浙江，科类：物理",
    createdAt: "2026-03-21T11:22:17",
    resultJson: JSON.stringify(generateRecommendations(8))
  },
  {
    id: 3,
    planName: "最终提交版",
    sourceType: "score",
    sourceQuery: "模式：学校优先，分数：600，省份：浙江，科类：物理",
    createdAt: "2026-03-21T11:18:44",
    resultJson: JSON.stringify(generateRecommendations(6))
  }
];

const MOCK_HISTORY = [
  {
    id: 1,
    queryType: "score",
    queryContent: "模式:专业优先，分数:650，省份:浙江，科类:PHYSICS，专业:计算机科学与技术",
    createdAt: "2026-08-07T09:00:33",
    summary: "返回 3 个稳妥院校"
  },
  {
    id: 2,
    queryType: "score",
    queryContent: "模式:专业优先，分数:630，省份:浙江，科类:PHYSICS，专业:计算机科学与技术",
    createdAt: "2026-08-07T08:57:07",
    summary: "返回 5 个推荐院校"
  },
  {
    id: 3,
    queryType: "score",
    queryContent: "模式:专业优先，分数:630，省份:浙江，科类:PHYSICS，专业:计算机科学与技术",
    createdAt: "2026-08-07T08:53:22",
    summary: "返回 5 个推荐院校"
  },
  {
    id: 4,
    queryType: "score",
    queryContent: "模式:学校优先，分数:630，省份:浙江，科类:PHYSICS",
    createdAt: "2026-08-07T08:53:04",
    summary: "返回 8 个推荐院校"
  },
  {
    id: 5,
    queryType: "text",
    queryContent: "推荐几个华东地区的学校",
    createdAt: "2026-03-21T11:28:03",
    summary: "生成 8 条推荐"
  },
  {
    id: 6,
    queryType: "text",
    queryContent: "推荐几个华东地区的学校",
    createdAt: "2026-03-21T11:22:54",
    summary: "生成 8 条推荐"
  },
  {
    id: 7,
    queryType: "score",
    queryContent: "模式:学校优先，分数:630，省份:浙江，科类:PHYSICS",
    createdAt: "2026-03-21T11:21:53",
    summary: "返回 8 个推荐院校"
  },
  {
    id: 8,
    queryType: "agent",
    queryContent: "帮我推荐计算机专业",
    createdAt: "2026-03-21T10:59:16",
    summary: "进入 AI 顾问对话"
  }
];

const MOCK_CONVERSATIONS = [
  {
    id: 1,
    title: "关于志愿填报的咨询",
    messageCount: 3,
    createdAt: "2026-08-07T08:00:00",
    updatedAt: "2026-08-07T08:03:00"
  },
  {
    id: 2,
    title: "推荐计算机专业院校",
    messageCount: 2,
    createdAt: "2026-08-06T15:30:00",
    updatedAt: "2026-08-06T15:35:00"
  }
];

const MOCK_CONVERSATION_MESSAGES = {
  1: [
    {
      id: 101,
      role: "user",
      content: "湖南 物理类 560 分，能上哪些大学？"
    },
    {
      id: 102,
      role: "assistant",
      messageType: "tool_result",
      toolName: "getUserProfile",
      content: "",
      payload: { score: 620, subjectType: "物理", examProvince: "湖南" }
    },
    {
      id: 103,
      role: "assistant",
      content: "结合你的画像（湖南 · 物理类 · 620 分），给你一版冲稳保思路：\n\n## 冲刺档（录取概率 30%~50%）\n- 武汉大学（计算机类）：近三年最低位次 4500~5200，可冲\n- 华中科技大学（电子信息类）：位次贴合度较高\n\n## 稳妥档（50%~80%）\n- 湖南大学（土木工程）：本省招生计划多，稳妥\n- 中南大学（材料科学）：王牌专业，位次匹配\n\n## 保底档（>90%）\n- 湖南师范大学（数学与应用数学）\n- 长沙理工大学（电气工程）：省内就业口碑好\n\n建议按「2 冲 + 4 稳 + 3 保」铺开志愿梯度，避免全冲导致滑档。需要我把哪所加入志愿表，随时告诉我～"
    }
  ],
  2: [
    {
      id: 201,
      role: "user",
      content: "帮我推荐计算机专业实力强的院校"
    },
    {
      id: 202,
      role: "assistant",
      content: "计算机专业推荐这几所：\n\n1. **清华大学**：A+ 学科，体系结构与人工智能方向顶尖\n2. **北京大学**：理论与语言方向强，图灵班培养体系完整\n3. **浙江大学**：CS 科研产出稳居前列，实习资源丰富\n4. **国防科技大学**：系统软件与超算方向王牌\n5. **哈尔滨工业大学**：威海/深圳校区性价比高\n\n结合分数告诉我你的位次，我可以帮你算每所的录取概率。"
    }
  ]
};

const MOCK_ADMIN_USERS = [
  {
    id: 100, username: "admin", score: null, subjectType: null, examProvince: null,
    role: "ADMIN", enabled: true, createdAt: "2026-03-01T09:10:00", updatedAt: "2026-08-08T08:30:00",
    recommendationCount: 0, planCount: 0, conversationCount: 0
  },
  {
    id: 1, username: "testuser", score: 630, subjectType: "PHYSICS", examProvince: "浙江",
    role: "USER", enabled: true, createdAt: "2026-03-21T10:18:00", updatedAt: "2026-08-07T09:00:33",
    recommendationCount: 8, planCount: 3, conversationCount: 2
  },
  {
    id: 2, username: "linxi", score: 598, subjectType: "HISTORY", examProvince: "江苏",
    role: "USER", enabled: true, createdAt: "2026-05-12T14:26:00", updatedAt: "2026-08-06T16:42:00",
    recommendationCount: 5, planCount: 2, conversationCount: 4
  },
  {
    id: 3, username: "chenyu", score: 655, subjectType: "PHYSICS", examProvince: "山东",
    role: "USER", enabled: true, createdAt: "2026-06-19T11:05:00", updatedAt: "2026-08-05T19:20:00",
    recommendationCount: 12, planCount: 4, conversationCount: 6
  },
  {
    id: 4, username: "pending_user", score: null, subjectType: null, examProvince: null,
    role: "USER", enabled: false, createdAt: "2026-07-28T17:40:00", updatedAt: "2026-08-02T10:12:00",
    recommendationCount: 0, planCount: 0, conversationCount: 1
  }
];

const MOCK_ADMIN_UNIVERSITIES = MOCK_SCHOOLS.map((school) => ({
  ...school,
  tags: [school.is985 ? "985" : null, (school.is211 || school.isDoubleFirstClass) ? "双一流" : null]
    .filter(Boolean)
    .join(",")
}));

const MOCK_ADMIN_MAJORS = [
  { id: 1, name: "计算机科学与技术", category: "计算机类", degreeType: "工学", tags: "热门,信息技术", subjectRequirement: "物理", description: "研究计算机系统、算法与软件设计。" },
  { id: 2, name: "软件工程", category: "计算机类", degreeType: "工学", tags: "热门,工程实践", subjectRequirement: "物理", description: "面向软件系统分析、设计、开发与维护。" },
  { id: 3, name: "人工智能", category: "电子信息类", degreeType: "工学", tags: "新兴,交叉学科", subjectRequirement: "物理", description: "涵盖机器学习、智能系统与数据处理。" },
  { id: 4, name: "电子信息工程", category: "电子信息类", degreeType: "工学", tags: "信息技术", subjectRequirement: "物理", description: "学习电子技术、信息获取与处理。" },
  { id: 5, name: "临床医学", category: "临床医学类", degreeType: "医学", tags: "医学", subjectRequirement: "物理,化学", description: "培养临床诊疗与医学研究能力。" },
  { id: 6, name: "法学", category: "法学类", degreeType: "法学", tags: "人文社科", subjectRequirement: "不限", description: "学习法律规范、法学理论与实务。" },
  { id: 7, name: "金融学", category: "金融学类", degreeType: "经济学", tags: "财经", subjectRequirement: "不限", description: "研究金融市场、机构与风险管理。" },
  { id: 8, name: "建筑学", category: "建筑类", degreeType: "工学", tags: "设计,工程", subjectRequirement: "物理", description: "学习建筑设计、技术与城市空间。" }
];

const MOCK_ADMIN_CUTOFFS = [
  { id: 1, universityId: 3, admissionYear: 2025, province: "浙江", subjectType: "PHYSICS", cutoffScore: 664, minRank: 6790 },
  { id: 2, universityId: 4, admissionYear: 2025, province: "浙江", subjectType: "PHYSICS", cutoffScore: 697, minRank: 1980 },
  { id: 3, universityId: 6, admissionYear: 2025, province: "江苏", subjectType: "HISTORY", cutoffScore: 635, minRank: 620 },
  { id: 4, universityId: 8, admissionYear: 2025, province: "湖北", subjectType: "PHYSICS", cutoffScore: 648, minRank: 4200 },
  { id: 5, universityId: 10, admissionYear: 2024, province: "广东", subjectType: "PHYSICS", cutoffScore: 624, minRank: 9630 }
];

const MOCK_ADMIN_MAJOR_CUTOFFS = [
  { id: 1, universityId: 3, majorId: 1, majorName: "计算机科学与技术", admissionYear: 2025, province: "浙江", subjectType: "PHYSICS", cutoffScore: 685, minRank: 3520 },
  { id: 2, universityId: 4, majorId: 2, majorName: "软件工程", admissionYear: 2025, province: "浙江", subjectType: "PHYSICS", cutoffScore: 699, minRank: 1760 },
  { id: 3, universityId: 6, majorId: 6, majorName: "法学", admissionYear: 2025, province: "江苏", subjectType: "HISTORY", cutoffScore: 642, minRank: 430 },
  { id: 4, universityId: 8, majorId: 3, majorName: "人工智能", admissionYear: 2025, province: "湖北", subjectType: "PHYSICS", cutoffScore: 655, minRank: 3380 },
  { id: 5, universityId: 10, majorId: 5, majorName: "临床医学", admissionYear: 2024, province: "广东", subjectType: "PHYSICS", cutoffScore: 640, minRank: 6800 }
];

const MOCK_ADMIN_AI_CONFIG = {
  provider: "openai-compatible",
  baseUrl: "https://dashscope.aliyuncs.com/compatible-mode/v1",
  model: "qwen-plus",
  apiKeyConfigured: false,
  apiKeyMasked: "",
  apiKeySource: "none"
};

function nextId(items) {
  return Math.max(0, ...items.map((item) => Number(item.id) || 0)) + 1;
}

function readJsonBody(options) {
  return options.body ? JSON.parse(options.body) : {};
}

function filterAdmissionRows(items, searchParams, includeMajor = false) {
  const universityId = searchParams.get("universityId");
  const admissionYear = searchParams.get("admissionYear");
  const province = searchParams.get("province")?.trim().toLowerCase();
  const subjectType = searchParams.get("subjectType");
  const majorKeyword = searchParams.get("majorKeyword")?.trim().toLowerCase();
  return items.filter((item) => {
    if (universityId && Number(item.universityId) !== Number(universityId)) return false;
    if (admissionYear && Number(item.admissionYear) !== Number(admissionYear)) return false;
    if (province && !item.province.toLowerCase().includes(province)) return false;
    if (subjectType && item.subjectType !== subjectType) return false;
    return !includeMajor || !majorKeyword || item.majorName.toLowerCase().includes(majorKeyword);
  });
}

function saveAdminRecord(items, body, id = null) {
  if (id == null) {
    const created = { id: nextId(items), ...body };
    items.unshift(created);
    return created;
  }
  const index = items.findIndex((item) => Number(item.id) === Number(id));
  if (index < 0) return null;
  items[index] = { ...items[index], ...body, id: Number(id) };
  return items[index];
}

// 构建 Agent 回合消息（供全量接口与 SSE 流接口复用）
function buildAgentTurn(content) {
  const now = () => new Date().toISOString();
  const generatedMessages = [];
  const push = (role, messageType, toolName, text, payload) =>
    generatedMessages.push({
      id: Date.now() + generatedMessages.length,
      role,
      messageType,
      toolName: toolName || null,
      content: text,
      payload: payload || null,
      createdAt: now()
    });

  if (content.includes("画像")) {
    push("assistant", "tool_call", "getUserProfile", "读取用户画像…");
    push("assistant", "tool_result", "getUserProfile", "已读取用户画像", { score: 630, subjectType: "物理类", examProvince: "浙江", username: "testuser" });
    push("assistant", "text", null, "已读取你的画像：**浙江** · **物理类** · **630 分**。\n\n当前推荐策略按 **分数优先** 展开，位次约 **5000**。");
  } else if (content.includes("志愿方案") || content.includes("当前")) {
    push("assistant", "tool_call", "getCurrentPlan", "读取当前志愿表…");
    push("assistant", "tool_result", "getCurrentPlan", "已读取当前志愿表", { planName: "冲稳保方案-A", itemCount: 8 });
    push("assistant", "text", null, "当前志愿表《冲稳保方案-A》共 **8 条** 志愿：\n\n- 冲刺 3 条\n- 稳妥 3 条\n- 保底 2 条");
  } else if (content.includes("推荐")) {
    const rec = generateRecommendations(6);
    const topItems = [...rec.rush, ...rec.safe, ...rec.guarantee].map((item) => ({ ...item, label: item.universityName }));
    push("assistant", "tool_call", "recommendSchools", "生成院校推荐…");
    push("assistant", "tool_result", "recommendSchools", `已生成 ${topItems.length} 条院校推荐`, { topItems });
    push("assistant", "text", null, `根据你的 **630 分** 与位次，为你推荐 **${topItems.length}** 所院校，按 **冲 / 稳 / 保** 梯度排列：`);
  } else {
    push("assistant", "text", null, "这是演示模式的模拟回复。正式环境中，该问题将进入 Agent 工具流程，结合你的画像与志愿表生成结构化回答。");
  }

  return generatedMessages;
}

// Mock API 拦截器
export function setupMockInterceptor() {
  const originalFetch = window.fetch;

  window.fetch = async function(url, options = {}) {
    if (!url.toString().startsWith("/api")) {
      return originalFetch.call(this, url, options);
    }

    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 300 + Math.random() * 200));

    const method = (options.method || "GET").toUpperCase();
    const requestUrl = new URL(url.toString(), window.location.origin);
    const path = requestUrl.pathname;
    const searchParams = requestUrl.searchParams;

    // 登录
    if (path === "/api/auth/login" && method === "POST") {
      const body = JSON.parse(options.body);
      if (body.username === "admin" && body.password === "admin123") {
        return mockResponse(MOCK_ADMIN_USER);
      }
      return mockResponse(MOCK_USER);
    }

    // 注册
    if (path === "/api/auth/register" && method === "POST") {
      return mockResponse(MOCK_USER);
    }

    // 获取用户信息
    if (path === "/api/auth/profile" && method === "POST") {
      return mockResponse(MOCK_USER);
    }

    // 获取省份选项
    if (path === "/api/meta/options") {
      return mockResponse({ provinces: MOCK_PROVINCES });
    }

    // 获取专业选项
    if (path === "/api/meta/major-options") {
      return mockResponse(MOCK_MAJORS.slice(0, 10));
    }

    // 推荐查询
    if (path === "/api/recommendations" && method === "POST") {
      return mockResponse(generateRecommendations(15));
    }

    // 自由文本推荐
    if (path === "/api/recommendations/free-text" && method === "POST") {
      return mockResponse(generateRecommendations(10));
    }

    // 获取历史记录
    if (path === "/api/history" && method === "GET") {
      return mockResponse(MOCK_HISTORY);
    }

    // 获取历史详情
    if (path.match(/^\/api\/history\/\d+$/) && method === "GET") {
      return mockResponse({
        ...MOCK_HISTORY[0],
        resultJson: JSON.stringify(generateRecommendations(5))
      });
    }

    // 获取方案列表
    if (path === "/api/plans" && method === "GET") {
      return mockResponse(MOCK_PLANS);
    }

    // 获取当前方案草稿
    if (path === "/api/plans/current" && method === "GET") {
      const draft = MOCK_PLANS.find(p => p.planName === "当前方案草稿");
      if (!draft) {
        return mockResponse({ message: "not found" }, 404);
      }
      return mockResponse(draft);
    }

    // 更新当前方案草稿
    if (path === "/api/plans/current" && method === "PUT") {
      const body = JSON.parse(options.body);
      let draft = MOCK_PLANS.find(p => p.planName === "当前方案草稿");
      if (draft) {
        Object.assign(draft, body, { id: draft.id, planName: "当前方案草稿" });
      } else {
        draft = { id: Date.now(), createdAt: new Date().toISOString(), ...body, planName: "当前方案草稿" };
        MOCK_PLANS.unshift(draft);
      }
      return mockResponse(draft);
    }

    // 删除当前方案草稿
    if (path === "/api/plans/current" && method === "DELETE") {
      const idx = MOCK_PLANS.findIndex(p => p.planName === "当前方案草稿");
      if (idx >= 0) MOCK_PLANS.splice(idx, 1);
      return mockResponse({ success: true });
    }

    // 获取方案详情
    if (path.match(/^\/api\/plans\/\d+$/) && method === "GET") {
      const id = parseInt(path.split("/").pop());
      const plan = MOCK_PLANS.find(p => p.id === id) || MOCK_PLANS[0];
      return mockResponse(plan);
    }

    // 保存方案
    if (path === "/api/plans" && method === "POST") {
      const body = JSON.parse(options.body);
      return mockResponse({ id: Date.now(), ...body, createdAt: new Date().toISOString() });
    }

    // 更新方案
    if (path.match(/^\/api\/plans\/\d+$/) && method === "PUT") {
      const body = JSON.parse(options.body);
      return mockResponse({ id: parseInt(path.split("/").pop()), ...body });
    }

    // 删除方案
    if (path.match(/^\/api\/plans\/\d+$/) && method === "DELETE") {
      return mockResponse({ success: true });
    }

    // 获取对话列表
    if (path === "/api/agent/conversations" && method === "GET") {
      return mockResponse(MOCK_CONVERSATIONS);
    }

    // 创建对话
    if (path === "/api/agent/conversations" && method === "POST") {
      return mockResponse({ id: Date.now(), title: "新对话", createdAt: new Date().toISOString() });
    }

    // 获取对话详情
    if (path.match(/^\/api\/agent\/conversations\/\d+$/) && method === "GET") {
      const id = parseInt(path.split("/").pop());
      const item = MOCK_CONVERSATIONS.find((c) => c.id === id) || {};
      return mockResponse({ id, title: item.title || "新的志愿对话", messages: MOCK_CONVERSATION_MESSAGES[id] || [] });
    }

    // 发送消息（全量回合，旧契约降级路径）
    if (path.match(/^\/api\/agent\/conversations\/\d+\/messages$/) && method === "POST") {
      const body = JSON.parse(options.body);
      return mockResponse({
        conversationId: parseInt(path.split("/")[3]),
        generatedMessages: buildAgentTurn(String(body.content || ""))
      });
    }

    // 流式对话（模拟 SSE 事件流，协议与 AI_CHAT_API_DESIGN.md 一致）
    if (path.match(/^\/api\/agent\/conversations\/\d+\/stream$/) && method === "POST") {
      const body = JSON.parse(options.body);
      const conversationId = parseInt(path.split("/")[3]);
      const turn = buildAgentTurn(String(body.content || ""));
      const encoder = new TextEncoder();
      const events = [];
      let seq = 0;

      for (const message of turn) {
        if (message.messageType === "tool_call") {
          seq += 1;
          events.push({ event: "tool_call", data: { seq, toolName: message.toolName, content: message.content }, delay: 500 });
        } else if (message.messageType === "tool_result") {
          seq += 1;
          events.push({ event: "tool_result", data: { seq, toolName: message.toolName, content: message.content, payload: message.payload }, delay: 650 });
        } else {
          const text = String(message.content || "");
          for (let i = 0; i < text.length; i += 4) {
            seq += 1;
            events.push({ event: "delta", data: { seq, text: text.slice(i, i + 4) }, delay: 26 });
          }
          seq += 1;
          events.push({ event: "message", data: { seq, message }, delay: 60 });
        }
      }
      events.push({ event: "done", data: { conversationId, messageCount: turn.length }, delay: 120 });

      const stream = new ReadableStream({
        async start(controller) {
          for (const evt of events) {
            controller.enqueue(encoder.encode(`event: ${evt.event}\ndata: ${JSON.stringify(evt.data)}\n\n`));
            await new Promise(resolve => setTimeout(resolve, evt.delay));
          }
          controller.close();
        }
      });

      return new Response(stream, {
        status: 200,
        headers: {
          "Content-Type": "text/event-stream; charset=utf-8",
          "Cache-Control": "no-cache"
        }
      });
    }

    // 管理员用户概览与账号管理
    if (path === "/api/admin/users/overview" && method === "GET") {
      return mockResponse({
        totalCount: MOCK_ADMIN_USERS.length,
        userCount: MOCK_ADMIN_USERS.filter((item) => item.role === "USER").length,
        adminCount: MOCK_ADMIN_USERS.filter((item) => item.role === "ADMIN").length,
        disabledCount: MOCK_ADMIN_USERS.filter((item) => item.enabled === false).length
      });
    }

    if ((path === "/api/admin/users" || path === "/api/admin/users/count") && method === "GET") {
      const keyword = searchParams.get("keyword")?.trim().toLowerCase();
      const role = searchParams.get("role");
      const enabled = searchParams.get("enabled");
      const users = MOCK_ADMIN_USERS.filter((item) => {
        if (keyword && !item.username.toLowerCase().includes(keyword)) return false;
        if (role && item.role !== role) return false;
        if (enabled != null && String(item.enabled) !== enabled) return false;
        return true;
      });
      if (path.endsWith("/count")) return mockResponse({ total: users.length });
      const page = Math.max(1, Number(searchParams.get("page") || 1));
      const size = Math.max(1, Number(searchParams.get("size") || 200));
      return mockResponse(users.slice((page - 1) * size, page * size));
    }

    if (path === "/api/admin/ai-config" && method === "GET") {
      return mockResponse(MOCK_ADMIN_AI_CONFIG);
    }
    if (path === "/api/admin/ai-config/test" && method === "POST") {
      const body = readJsonBody(options);
      const hasKey = Boolean(body.apiKey) || MOCK_ADMIN_AI_CONFIG.apiKeyConfigured;
      const available = Boolean(body.provider && body.baseUrl && body.model && hasKey);
      return mockResponse({
        available,
        message: available ? "连接成功，模型响应正常" : "未配置 API Key，请填写后再检测",
        provider: body.provider || "openai-compatible",
        model: body.model || "",
        latencyMillis: available ? 128 : 0
      });
    }
    if (path === "/api/admin/ai-config" && method === "PUT") {
      const body = readJsonBody(options);
      Object.assign(MOCK_ADMIN_AI_CONFIG, {
        provider: body.provider,
        baseUrl: body.baseUrl,
        model: body.model,
        apiKeyConfigured: Boolean(body.apiKey) || MOCK_ADMIN_AI_CONFIG.apiKeyConfigured,
        apiKeyMasked: body.apiKey ? `••••${String(body.apiKey).slice(-4)}` : MOCK_ADMIN_AI_CONFIG.apiKeyMasked,
        apiKeySource: body.apiKey ? "database" : MOCK_ADMIN_AI_CONFIG.apiKeySource
      });
      return mockResponse(MOCK_ADMIN_AI_CONFIG);
    }

    const adminUserDetailMatch = path.match(/^\/api\/admin\/users\/(\d+)$/);
    if (adminUserDetailMatch && method === "GET") {
      const user = MOCK_ADMIN_USERS.find((item) => Number(item.id) === Number(adminUserDetailMatch[1]));
      return user ? mockResponse(user) : mockResponse({ message: "User not found" }, 404);
    }

    const adminUserSettingsMatch = path.match(/^\/api\/admin\/users\/(\d+)\/settings$/);
    if (adminUserSettingsMatch && method === "PUT") {
      const user = MOCK_ADMIN_USERS.find((item) => Number(item.id) === Number(adminUserSettingsMatch[1]));
      if (!user) return mockResponse({ message: "User not found" }, 404);
      const body = readJsonBody(options);
      if (user.username === MOCK_ADMIN_USER.username && (body.role !== "ADMIN" || body.enabled !== true)) {
        return mockResponse({ message: "当前管理员不能停用或降级自己的账号" }, 400);
      }
      Object.assign(user, { role: body.role, enabled: body.enabled, updatedAt: new Date().toISOString() });
      return mockResponse(user);
    }

    // 管理员基础数据：查询、新增和编辑均只修改当前页面会话内存
    if (path === "/api/admin/universities" && method === "GET") {
      return mockResponse(MOCK_ADMIN_UNIVERSITIES);
    }
    if (path === "/api/admin/universities" && method === "POST") {
      return mockResponse(saveAdminRecord(MOCK_ADMIN_UNIVERSITIES, readJsonBody(options)));
    }
    const adminUniversityMatch = path.match(/^\/api\/admin\/universities\/(\d+)$/);
    if (adminUniversityMatch && method === "PUT") {
      const saved = saveAdminRecord(MOCK_ADMIN_UNIVERSITIES, readJsonBody(options), adminUniversityMatch[1]);
      return saved ? mockResponse(saved) : mockResponse({ message: "University not found" }, 404);
    }

    if (path === "/api/admin/majors" && method === "GET") {
      return mockResponse(MOCK_ADMIN_MAJORS);
    }
    if (path === "/api/admin/majors" && method === "POST") {
      return mockResponse(saveAdminRecord(MOCK_ADMIN_MAJORS, readJsonBody(options)));
    }
    const adminMajorMatch = path.match(/^\/api\/admin\/majors\/(\d+)$/);
    if (adminMajorMatch && method === "PUT") {
      const saved = saveAdminRecord(MOCK_ADMIN_MAJORS, readJsonBody(options), adminMajorMatch[1]);
      return saved ? mockResponse(saved) : mockResponse({ message: "Major not found" }, 404);
    }

    if (path === "/api/admin/admission-cutoffs" && method === "GET") {
      return mockResponse(filterAdmissionRows(MOCK_ADMIN_CUTOFFS, searchParams));
    }
    if (path === "/api/admin/admission-cutoffs" && method === "POST") {
      return mockResponse(saveAdminRecord(MOCK_ADMIN_CUTOFFS, readJsonBody(options)));
    }
    const adminCutoffMatch = path.match(/^\/api\/admin\/admission-cutoffs\/(\d+)$/);
    if (adminCutoffMatch && method === "PUT") {
      const saved = saveAdminRecord(MOCK_ADMIN_CUTOFFS, readJsonBody(options), adminCutoffMatch[1]);
      return saved ? mockResponse(saved) : mockResponse({ message: "Admission cutoff not found" }, 404);
    }

    if (path === "/api/admin/major-admission-cutoffs" && method === "GET") {
      return mockResponse(filterAdmissionRows(MOCK_ADMIN_MAJOR_CUTOFFS, searchParams, true));
    }
    if (path === "/api/admin/major-admission-cutoffs" && method === "POST") {
      return mockResponse(saveAdminRecord(MOCK_ADMIN_MAJOR_CUTOFFS, readJsonBody(options)));
    }
    const adminMajorCutoffMatch = path.match(/^\/api\/admin\/major-admission-cutoffs\/(\d+)$/);
    if (adminMajorCutoffMatch && method === "PUT") {
      const saved = saveAdminRecord(MOCK_ADMIN_MAJOR_CUTOFFS, readJsonBody(options), adminMajorCutoffMatch[1]);
      return saved ? mockResponse(saved) : mockResponse({ message: "Major admission cutoff not found" }, 404);
    }

    // 默认返回404
    return mockResponse({ error: "Not Found" }, 404);
  };
}

function mockResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json"
    }
  });
}

// 检查是否启用 Mock 模式
export function isMockMode() {
  return import.meta.env.VITE_MOCK === "true";
}
